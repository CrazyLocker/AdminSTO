package com.autoservice.services;

import com.autoservice.model.WindowState;

/**
 * Управление состоянием окон: сохранение и восстановление позиции и размеров.
 */
public class WindowStateManager extends JsonStateManager<WindowState> {
    private static final String STORAGE_DIR = "config/window-state";

    // Дефолтные размеры для окон (если файл не найден)
    private static final double DEFAULT_WINDOW_WIDTH = 1280;
    private static final double DEFAULT_WINDOW_HEIGHT = 720;
    private static final double DEFAULT_WINDOW_X = 50;
    private static final double DEFAULT_WINDOW_Y = 50;

    private static WindowStateManager instance;

    private WindowStateManager() {
        super(STORAGE_DIR, WindowState.class);
    }

    /**
     * Получить singleton-экземпляр менеджера.
     * @return экземпляр менеджера
     */
    public static synchronized WindowStateManager getInstance() {
        if (instance == null) {
            instance = new WindowStateManager();
        }
        return instance;
    }

    /**
     * Сохранить состояние окна по его геометрии.
     * @param windowId идентификатор окна
     * @param x координата окна по оси X
     * @param y координата окна по оси Y
     * @param width ширина окна
     * @param height высота окна
     */
    public void saveWindowState(String windowId, double x, double y, double width, double height) {
        save(windowId, new WindowState(x, y, width, height));
    }

    /**
     * Сохранить текущее состояние окна.
     * @param windowId идентификатор окна
     * @param stage окно, состояние которого нужно сохранить
     */
    public void saveWindowState(String windowId, javafx.stage.Stage stage) {
        if (stage == null) {
            logger.error("ERROR: stage is null for {}", windowId);
            return;
        }
        saveWindowState(windowId, stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
    }

    /**
     * Получить сохранённое состояние окна.
     * @param windowId идентификатор окна
     * @return сохранённое состояние или {@code null}, если оно не найдено
     */
    public WindowState getWindowState(String windowId) {
        return get(windowId);
    }

    /**
     * Восстановить позицию и размеры окна.
     *
     * <p>Если сохранённое состояние отсутствует, окну задаются значения по умолчанию.
     * @param windowId идентификатор окна
     * @param stage окно, которому применяется состояние
     * @return {@code true}, если сохранённое состояние было применено,
     *         {@code false}, если использованы значения по умолчанию
     */
    public boolean restoreWindowState(String windowId, javafx.stage.Stage stage) {
        WindowState state = getWindowState(windowId);

        if (state == null) {
            logger.debug("Using default state for window: {}", windowId);
            // Устанавливаем дефолтные значения
            stage.setX(DEFAULT_WINDOW_X);
            stage.setY(DEFAULT_WINDOW_Y);
            stage.setWidth(DEFAULT_WINDOW_WIDTH);
            stage.setHeight(DEFAULT_WINDOW_HEIGHT);
            return false;
        }

        stage.setX(state.getX());
        stage.setY(state.getY());
        stage.setWidth(state.getWidth());
        stage.setHeight(state.getHeight());

        logger.debug("Window state restored: {} -> {}", windowId, state);
        return true;
    }

    /**
     * Сбросить сохранённое состояние окна и удалить его файл.
     * @param windowId идентификатор окна
     */
    public void resetWindowState(String windowId) {
        reset(windowId);
    }

    /**
     * Проверить наличие сохранённого состояния окна.
     * @param windowId идентификатор окна
     * @return {@code true}, если состояние сохранено
     */
    public boolean hasSavedState(String windowId) {
        return exists(windowId);
    }
}
