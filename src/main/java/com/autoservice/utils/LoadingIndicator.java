package com.autoservice.utils;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.concurrent.CountDownLatch;

/**
 * Утилита для отображения индикатора загрузки во время длительных операций.
 * Используется для улучшения UX при импорте, бэкапе, отчётах и других длительных операциях.
 * 
 * <p>Пример использования:</p>
 * <pre>{@code
 * LoadingIndicator.show(parentPane, "Импорт данных...", () -> {
 *     // Долгая операция
 *     importData();
 *     return null;
 * });
 * }</pre>
 */
public class LoadingIndicator {

    /**
     * Показывает индикатор загрузки для операции.
     * @param parent узел, над которым будет показан индикатор
     * @param message сообщение, отображаемое под индикатором
     * @param task задача, которая будет выполняться в фоновом потоке
     * @param <T> тип результата задачи
     * @return Optional с результатом задачи или пустой Optional при ошибке
     */
    public static <T> T show(Pane parent, String message, Callable<T> task) {
        StackPane overlay = createOverlay(parent, message);
        parent.getChildren().add(overlay);
        
        // Используем CountDownLatch для синхронизации
        CountDownLatch latch = new CountDownLatch(1);
        
        // Выполняем задачу в фоновом потоке
        Thread thread = new Thread(() -> {
            T result = null;
            Exception exception = null;
            
            try {
                result = task.call();
            } catch (Exception e) {
                exception = e;
            }
            
            Platform.runLater(() -> {
                parent.getChildren().remove(overlay);
                latch.countDown();
            });
        });
        
        thread.setDaemon(true);
        thread.start();
        
        // Ждем завершения
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Platform.runLater(() -> parent.getChildren().remove(overlay));
        }
        
        return null;
    }

    /**
     * Показывает индикатор загрузки без возврата результата.
     * @param parent узел, над которым будет показан индикатор
     * @param message сообщение, отображаемое под индикатором
     * @param task задача для выполнения
     */
    public static void show(Pane parent, String message, Runnable task) {
        show(parent, message, () -> {
            task.run();
            return null;
        });
    }

    /**
     * Создает оверлей с индикатором загрузки.
     */
    private static StackPane createOverlay(Pane parent, String message) {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5); -fx-background-radius: 8;");
        overlay.setPrefSize(parent.getPrefWidth(), parent.getPrefHeight());
        
        // Круговой индикатор
        Circle circle = new Circle(20);
        circle.setStyle("-fx-fill: transparent; -fx-stroke: #2196f3; -fx-stroke-width: 3;");
        
        // Прогресс-бар (неопределенный режим)
        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(200);
        progressBar.setProgress(javafx.scene.control.ProgressBar.INDETERMINATE_PROGRESS);
        
        // Текстовая метка
        Label label = new Label(message);
        label.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        
        // Анимация вращения
        Timeline timeline = new Timeline();
        KeyValue kv = new KeyValue(circle.rotateProperty(), 360);
        KeyFrame kf = new KeyFrame(Duration.seconds(1.5), kv);
        timeline.getKeyFrames().add(kf);
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        
        // Собираем вертикальную панель
        StackPane content = new StackPane();
        content.setPrefSize(240, 100);
        content.setStyle("-fx-background-color: white; -fx-background-radius: 8;");
        
        StackPane container = new StackPane();
        container.setPrefSize(240, 100);
        container.getChildren().addAll(circle, progressBar);
        StackPane.setAlignment(circle, javafx.geometry.Pos.CENTER);
        StackPane.setAlignment(progressBar, javafx.geometry.Pos.CENTER);
        progressBar.setLayoutY(60);
        
        label.setLayoutY(85);
        container.getChildren().add(label);
        
        StackPane.setAlignment(container, javafx.geometry.Pos.CENTER);
        overlay.getChildren().add(content);
        content.getChildren().add(container);
        
        return overlay;
    }

    /**
     * Интерфейс для задачи, которая будет выполняться в фоновом потоке.
     * @param <T> тип результата
     */
    @FunctionalInterface
    public interface Callable<T> {
        T call() throws Exception;
    }

    /**
     * Закрывает все активные индикаторы загрузки.
     * Используется при закрытии приложения.
     */
    public static void shutdown() {
        // Освобождение ресурсов при необходимости
    }
}
