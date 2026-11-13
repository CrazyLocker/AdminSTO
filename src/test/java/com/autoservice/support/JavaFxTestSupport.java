package com.autoservice.support;

import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Поддержка тестов, которым нужен настоящий JavaFX-тулкит в headless-режиме.
 *
 * <p>Образец взят из {@code ClientDoubleClickRegressionTest}: тулкит поднимается
 * через {@code Platform.startup}, а действия над узлами и окнами выполняются
 * на FX-потоке с таймаутами — чтобы тест падал с понятным сообщением, а не
 * зависал.</p>
 *
 * <p>Модальные диалоги показываются через {@code showAndWait()} и запускают
 * вложенный цикл событий, поэтому закрывать их можно только задачами,
 * отправленными на FX-поток — для этого есть {@link #closeStageWhenShown}.</p>
 */
public final class JavaFxTestSupport {

    private static final long FX_TIMEOUT_MS = 10_000;

    private JavaFxTestSupport() {
    }

    /**
     * Инициализировать JavaFX-тулкит в headless-режиме (Monocle).
     *
     * <p>Повторный вызов безопасен: если тулкит уже запущен, исключение
     * подавляется. {@code forkEvery = 1} в задаче {@code test} гарантирует,
     * что каждый тестовый класс получает свою JVM.</p>
     */
    public static void initToolkit() {
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
        // Без этого закрытие последнего окна завершает тулкит (implicit exit),
        // и все следующие задачи Platform.runLater молча теряются: тест, который
        // закрыл диалог, «подвешивает» последующие тесты того же класса.
        Platform.setImplicitExit(false);
    }

    /**
     * Выполнить действие на FX-потоке и дождаться завершения.
     *
     * @param action действие; бросает {@link AssertionError}, если FX-поток
     *               не ответил за 10 секунд
     */
    public static void runOnFxThread(Runnable action) throws InterruptedException {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                latch.countDown();
            }
        });
        if (!latch.await(FX_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            throw new AssertionError("FX-поток не выполнил действие за " + FX_TIMEOUT_MS + " мс");
        }
        if (failure.get() != null) {
            throw new AssertionError("Действие на FX-потоке завершилось ошибкой", failure.get());
        }
    }

    /**
     * Вычислить значение на FX-потоке и вернуть результат.
     *
     * @param supplier вычисление
     * @param <T>      тип результата
     * @return значение или {@code null}, если FX-поток не ответил
     */
    public static <T> T callOnFxThread(Supplier<T> supplier) throws InterruptedException {
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
        latch.await(FX_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        return result.get();
    }

    /**
     * Проверить, что FX-поток обслуживает задачи (не заблокирован).
     *
     * @param timeoutMs сколько ждать
     * @return {@code true}, если пустая задача выполнена за указанное время
     */
    public static boolean fxResponsive(long timeoutMs) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        return latch.await(timeoutMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Найти показываемое окно по заголовку.
     *
     * @param title заголовок окна
     * @return окно или {@code null}
     */
    public static Stage findShowingStage(String title) throws InterruptedException {
        return callOnFxThread(() -> {
            for (Window window : Window.getWindows()) {
                if (window instanceof Stage stage && title.equals(stage.getTitle()) && stage.isShowing()) {
                    return stage;
                }
            }
            return null;
        });
    }

    /**
     * Закрыть окно, как только оно появится.
     *
     * <p>Нужно для модальных диалогов: их {@code showAndWait()} блокирует
     * FX-поток, но вложенный цикл событий продолжает исполнять задачи, поэтому
     * отдельный поток может найти окно и закрыть его — так же, как это делает
     * пользователь крестиком или кнопкой «Отмена».</p>
     *
     * @param title     заголовок окна
     * @param timeoutMs сколько ждать появления окна
     * @return поток-закрыватель; его надо дождаться через {@code join},
     *         чтобы понять, что окно действительно было найдено и закрыто
     */
    public static Thread closeStageWhenShown(String title, long timeoutMs) {
        Thread closer = new Thread(() -> {
            long deadline = System.currentTimeMillis() + timeoutMs;
            try {
                while (System.currentTimeMillis() < deadline) {
                    Stage stage = findShowingStage(title);
                    if (stage != null) {
                        runOnFxThread(stage::close);
                        return;
                    }
                    Thread.sleep(25);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "fx-dialog-closer");
        closer.setDaemon(true);
        closer.start();
        return closer;
    }

    /**
     * Подождать выполнения условия.
     *
     * @param condition проверяемое условие
     * @param timeoutMs сколько ждать
     * @return {@code true}, если условие стало истинным
     */
    public static boolean waitUntil(BooleanSupplier condition, long timeoutMs) throws InterruptedException {
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
