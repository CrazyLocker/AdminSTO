package com.autoservice.services;

import com.autoservice.model.WindowState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Управление состоянием окон: сохранение и восстановление позиции и размеров.
 */
public class WindowStateManager {
    private static final Logger logger = LoggerFactory.getLogger(WindowStateManager.class);
    
    private static final String STORAGE_DIR = "config/window-state";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    // Дефолтные размеры для окон (если файл не найден)
    private static final double DEFAULT_WINDOW_WIDTH = 1280;
    private static final double DEFAULT_WINDOW_HEIGHT = 720;
    private static final double DEFAULT_WINDOW_X = 50;
    private static final double DEFAULT_WINDOW_Y = 50;
    
    private static WindowStateManager instance;
    private Map<String, WindowState> windowStates = new HashMap<>();
    
    private WindowStateManager() {
        loadAllStates();
    }
    
    public static synchronized WindowStateManager getInstance() {
        if (instance == null) {
            instance = new WindowStateManager();
        }
        return instance;
    }
    
    /**
     * Сохраняет состояние окна
     */
    public void saveWindowState(String windowId, double x, double y, double width, double height) {
        if (windowId == null || windowId.trim().isEmpty()) {
            logger.error("ERROR: windowId is null or empty");
            return;
        }
        
        WindowState state = new WindowState(x, y, width, height);
        windowStates.put(windowId, state);
        
        try {
            createDirectoryIfNotExists();
            String filePath = STORAGE_DIR + "/" + windowId + ".json";
            try (Writer writer = new FileWriter(filePath)) {
                gson.toJson(state, writer);
            }
            logger.debug("Window state saved: {} -> {}", windowId, state);
        } catch (IOException e) {
            logger.error("ERROR saving state for window {}: {}", windowId, e.getMessage());
        }
    }
    
    /**
     * Сохраняет состояние окна изjavafx.stage.Stage
     */
    public void saveWindowState(String windowId, javafx.stage.Stage stage) {
        if (stage == null) {
            logger.error("ERROR: stage is null for {}", windowId);
            return;
        }
        saveWindowState(windowId, stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
    }
    
    /**
     * Загружает состояние окна
     * @return WindowState или null если не найдено
     */
    public WindowState getWindowState(String windowId) {
        if (windowId == null || windowId.trim().isEmpty()) {
            logger.error("ERROR: windowId is null or empty");
            return null;
        }
        
        // Сначала проверяем кэш
        if (windowStates.containsKey(windowId)) {
            return windowStates.get(windowId);
        }
        
        // Загружаем из файла
        String filePath = STORAGE_DIR + "/" + windowId + ".json";
        File file = new File(filePath);
        
        if (!file.exists()) {
            logger.debug("No saved state found for window: {}", windowId);
            return null;
        }
        
        try (Reader reader = new FileReader(file)) {
            WindowState state = gson.fromJson(reader, WindowState.class);
            if (state != null) {
                windowStates.put(windowId, state);
                logger.debug("Window state loaded: {} -> {}", windowId, state);
                return state;
            }
        } catch (IOException e) {
            logger.error("Error loading state for window {}: {}", windowId, e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Восстанавливает состояние окна (позиция и размеры)
     * @return true если состояние было применено, false если использованы дефолтные значения
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
     * Сбрасывает состояние окна (удаляет файл)
     */
    public void resetWindowState(String windowId) {
        if (windowId == null || windowId.trim().isEmpty()) {
            logger.error("ERROR: windowId is null or empty");
            return;
        }
        
        String filePath = STORAGE_DIR + "/" + windowId + ".json";
        File file = new File(filePath);
        
        if (file.exists()) {
            if (file.delete()) {
                windowStates.remove(windowId);
                logger.debug("Window state reset: {}", windowId);
            } else {
                logger.error("Failed to delete state file for window: {}", windowId);
            }
        }
    }
    
    /**
     * Проверяет наличие сохраненного состояния
     */
    public boolean hasSavedState(String windowId) {
        if (windowId == null || windowId.trim().isEmpty()) {
            return false;
        }
        
        String filePath = STORAGE_DIR + "/" + windowId + ".json";
        File file = new File(filePath);
        return file.exists();
    }
    
    /**
     * Загружает все сохраненные состояния при старте
     */
    private void loadAllStates() {
        File dir = new File(STORAGE_DIR);
        if (!dir.exists() || !dir.isDirectory()) {
            logger.debug("No window state directory found");
            return;
        }
        
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            logger.debug("No window state files found");
            return;
        }
        
        for (File file : files) {
            String windowId = file.getName().replace(".json", "");
            WindowState state = getWindowState(windowId);
            if (state != null) {
                windowStates.put(windowId, state);
            }
        }
        
        logger.debug("Loaded {} window states", windowStates.size());
    }
    
    private static void createDirectoryIfNotExists() {
        File dir = new File(STORAGE_DIR);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                logger.debug("Created directory: {}", STORAGE_DIR);
            } else {
                logger.error("Failed to create directory: {}", STORAGE_DIR);
            }
        }
    }
}
