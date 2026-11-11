package com.autoservice.views;

import com.autoservice.Client;
import com.autoservice.DataStore;
import com.autoservice.DatabaseFactory;
import com.autoservice.utils.LoggerManager;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Регрессионный тест на зависание приложения при двойном клике по клиенту.
 *
 * <p>Раньше {@code EditClientDialog.show()} вызывал {@code showAsync(...).join()}:
 * если диалог закрывали крестиком, future не завершался, {@code join()} блокировал
 * JavaFX Application Thread, и приложение зависало без сообщений об ошибках.
 * Тест воспроизводит сценарий «двойной клик → закрыть диалог крестиком» и
 * проверяет, что обработчик двойного клика завершается, а FX-поток остаётся живым.</p>
 */
class ClientDoubleClickRegressionTest {

    private static final String CLIENT_DIALOG_TITLE = "Редактирование клиента";

    @BeforeAll
    static void initJavaFxAndDatabase() {
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.text", "t2k");
        System.setProperty("glass.platform", "Monocle");
        System.setProperty("monocle.platform", "Headless");
        System.setProperty("javafx.headless", "true");

        try {
            Platform.startup(() -> {
                // инициализация FX-тулкита в headless-режиме
            });
        } catch (IllegalStateException alreadyStarted) {
            // FX-тулкит уже запущен — это нормально
        }
        LoggerManager.init();
        DatabaseFactory.initForTest();
        DataStore.load();
    }

    @AfterAll
    static void closeDatabase() {
        DatabaseFactory.close();
    }

    @Test
    void closingClientDialogWithCrossKeepsFxThreadAlive() throws InterruptedException {
        runOnFxThread(() -> DataStore.addClient(
                new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163")));

        AtomicReference<TableView<Client>> tableRef = new AtomicReference<>();
        runOnFxThread(() -> {
            Stage stage = new Stage();
            Scene scene = new Scene(ClientView.create(), 1200, 800);
            stage.setScene(scene);
            stage.show();
            tableRef.set(findClientTable(scene));
        });

        TableView<Client> table = tableRef.get();
        assertThat(table).as("таблица клиентов").isNotNull();
        assertThat(table.getItems()).as("клиенты в таблице").isNotEmpty();

        // Двойной клик по строке: обработчик открывает диалог и блокируется на нём.
        AtomicBoolean handlerReturned = new AtomicBoolean(false);
        Platform.runLater(() -> {
            table.getSelectionModel().select(0);
            MouseEvent event = new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0,
                    MouseButton.PRIMARY, 2,
                    false, false, false, false,
                    true, false, false, false, false, false, null);
            table.getOnMouseClicked().handle(event);
            handlerReturned.set(true);
        });

        assertThat(waitUntil(() -> findWindow(CLIENT_DIALOG_TITLE) != null, 5_000))
                .as("диалог клиента открылся")
                .isTrue();
        assertThat(fxResponsive(3_000)).as("FX-поток отзывчив при открытом диалоге").isTrue();

        // Пользователь закрывает диалог крестиком — future обязан завершиться.
        AtomicReference<Stage> dialogRef = new AtomicReference<>(findWindow(CLIENT_DIALOG_TITLE));
        runOnFxThread(() -> {
            Stage dialog = dialogRef.get();
            if (dialog != null) {
                dialog.close();
            }
        });

        assertThat(waitUntil(handlerReturned::get, 5_000))
                .as("обработчик двойного клика завершился после закрытия диалога")
                .isTrue();
        assertThat(fxResponsive(5_000)).as("FX-поток остался отзывчивым (нет зависания)").isTrue();
    }

    @SuppressWarnings("unchecked")
    private static TableView<Client> findClientTable(Scene scene) {
        return (TableView<Client>) scene.lookup("#clientTable");
    }

    private static Stage findWindow(String title) {
        try {
            return callOnFxThread(() -> {
                for (Window window : Window.getWindows()) {
                    if (window instanceof Stage stage && title.equals(stage.getTitle()) && stage.isShowing()) {
                        return stage;
                    }
                }
                return null;
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private static <T> T callOnFxThread(java.util.function.Supplier<T> supplier)
            throws InterruptedException {
        if (Platform.isFxApplicationThread()) {
            return supplier.get();
        }
        AtomicReference<T> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                result.set(supplier.get());
            } finally {
                latch.countDown();
            }
        });
        latch.await(10, TimeUnit.SECONDS);
        return result.get();
    }

    private static boolean fxResponsive(long timeoutMs) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        return latch.await(timeoutMs, TimeUnit.MILLISECONDS);
    }

    private static void runOnFxThread(Runnable action) throws InterruptedException {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                latch.countDown();
            }
        });
        assertThat(latch.await(10, TimeUnit.SECONDS)).as("действие выполнено на FX-потоке").isTrue();
    }

    private static boolean waitUntil(BooleanSupplier condition, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(50);
        }
        return condition.getAsBoolean();
    }
}
