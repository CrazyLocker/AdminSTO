package com.autoservice.testutils;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Вспомогательные методы для UI-тестов JavaFX.
 *
 * <p>Класс создан как альтернатива TestFX-подобному API, работающая в
 * headless-режиме без реальной эмуляции мыши. Все операции выполняются
 * напрямую на JavaFX Application Thread через {@link #runOnFx(Runnable)}.</p>
 *
 * <p>Статический дизайн позволяет использовать хелпер как из тестов,
 * наследующих {@code BaseUITest}, так и из самостоятельных тестовых классов,
 * управляющих собственным {@link Stage}.</p>
 */
public final class UIHelper {

    private UIHelper() {
    }

    /**
     * Поиск узла по ID в сцене.
     *
     * @param scene  сцена, в которой ищется узел
     * @param nodeId ID узла (без префикса "#")
     * @return найденный узел
     * @throws IllegalArgumentException если узел не найден
     */
    public static Node find(Scene scene, String nodeId) {
        if (scene == null) {
            throw new IllegalStateException("Сцена не инициализирована");
        }
        Node node = scene.lookup("#" + nodeId);
        if (node == null) {
            throw new IllegalArgumentException("Узел с ID '" + nodeId + "' не найден в сцене");
        }
        return node;
    }

    /**
     * Синхронно выполняет действие на JavaFX Application Thread.
     *
     * @param action действие
     */
    public static void runOnFx(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
            waitForFx();
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
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Таймаут ожидания FX-потока");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        waitForFx();
    }

    /**
     * Синхронно вычисляет значение на JavaFX Application Thread.
     *
     * @param supplier поставщик значения
     * @param <T>      тип значения
     * @return значение
     */
    public static <T> T getOnFx(Supplier<T> supplier) {
        if (Platform.isFxApplicationThread()) {
            return supplier.get();
        }
        AtomicReference<T> ref = new AtomicReference<>();
        runOnFx(() -> ref.set(supplier.get()));
        return ref.get();
    }

    /**
     * Ожидание отработки layout/pulse на FX-потоке.
     */
    public static void waitForFx() {
        for (int i = 0; i < 2; i++) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(latch::countDown);
            try {
                if (!latch.await(2, TimeUnit.SECONDS)) {
                    return;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * "Клик" по кнопке через прямой вызов {@link Button#fire()}.
     *
     * @param scene  сцена
     * @param buttonId ID кнопки
     */
    public static void clickButton(Scene scene, String buttonId) {
        Node node = find(scene, buttonId);
        runOnFx(() -> {
            if (node instanceof Button) {
                ((Button) node).fire();
            } else {
                node.requestFocus();
            }
        });
    }

    /**
     * Ввод текста в TextField.
     *
     * @param scene   сцена
     * @param fieldId ID поля
     * @param text    текст
     */
    public static void writeText(Scene scene, String fieldId, String text) {
        TextField field = (TextField) find(scene, fieldId);
        runOnFx(() -> {
            field.requestFocus();
            field.clear();
            field.setText(text);
        });
    }

    /**
     * Выбор значения в ComboBox.
     *
     * @param scene       сцена
     * @param comboBoxId  ID ComboBox
     * @param value       значение
     */
    public static void selectCombo(Scene scene, String comboBoxId, String value) {
        ComboBox<String> combo = (ComboBox<String>) find(scene, comboBoxId);
        runOnFx(() -> combo.setValue(value));
    }

    /**
     * Получение TableView по ID.
     *
     * @param scene   сцена
     * @param tableId ID таблицы
     * @param <T>     тип элементов
     * @return таблица
     */
    @SuppressWarnings("unchecked")
    public static <T> TableView<T> table(Scene scene, String tableId) {
        return (TableView<T>) find(scene, tableId);
    }

    /**
     * Количество строк в таблице (вычисляется на FX-потоке).
     *
     * @param scene   сцена
     * @param tableId ID таблицы
     * @return количество строк
     */
    public static int rowCount(Scene scene, String tableId) {
        TableView<?> table = table(scene, tableId);
        return getOnFx(() -> table.getItems().size());
    }
}
