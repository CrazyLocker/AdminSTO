package com.autoservice.services;

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
 * Управление путями для импорт/экспорт файлов: сохранение и восстановление последнего каталога.
 */
public class FileDialogPathManager {
    private static final Logger logger = LoggerFactory.getLogger(FileDialogPathManager.class);
    
    private static final String STORAGE_DIR = "config/file-paths";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    private static FileDialogPathManager instance;
    private Map<String, String> lastPaths = new HashMap<>();
    
    private FileDialogPathManager() {
        loadAllPaths();
    }
    
    public static synchronized FileDialogPathManager getInstance() {
        if (instance == null) {
            instance = new FileDialogPathManager();
        }
        return instance;
    }
    
    /**
     * Сохраняет последний путь для конкретного окна
     */
    public void saveLastPath(String dialogId, String path) {
        if (dialogId == null || dialogId.trim().isEmpty()) {
            logger.error("ERROR: dialogId is null or empty");
            return;
        }
        
        if (path == null || path.trim().isEmpty()) {
            logger.debug("Empty path for {}, skipping save", dialogId);
            return;
        }
        
        lastPaths.put(dialogId, path);
        
        try {
            createDirectoryIfNotExists();
            String filePath = STORAGE_DIR + "/" + dialogId + ".json";
            try (Writer writer = new FileWriter(filePath)) {
                gson.toJson(path, writer);
            }
            logger.debug("Last path saved: {} -> {}", dialogId, path);
        } catch (IOException e) {
            logger.error("ERROR saving path for {}: {}", dialogId, e.getMessage());
        }
    }
    
    /**
     * Загружает последний путь для конкретного окна
     * @return путь или null если не найден
     */
    public String getLastPath(String dialogId) {
        if (dialogId == null || dialogId.trim().isEmpty()) {
            logger.error("ERROR: dialogId is null or empty");
            return null;
        }
        
        // Сначала проверяем кэш
        if (lastPaths.containsKey(dialogId)) {
            return lastPaths.get(dialogId);
        }
        
        // Загружаем из файла
        String filePath = STORAGE_DIR + "/" + dialogId + ".json";
        File file = new File(filePath);
        
        if (!file.exists()) {
            logger.debug("No saved path found for dialog: {}", dialogId);
            return null;
        }
        
        try (Reader reader = new FileReader(file)) {
            String path = gson.fromJson(reader, String.class);
            if (path != null && !path.trim().isEmpty()) {
                lastPaths.put(dialogId, path);
                logger.debug("Last path loaded: {} -> {}", dialogId, path);
                return path;
            }
        } catch (IOException e) {
            logger.error("Error loading path for {}: {}", dialogId, e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Удаляет сохраненный путь для конкретного окна
     */
    public void resetLastPath(String dialogId) {
        if (dialogId == null || dialogId.trim().isEmpty()) {
            logger.error("ERROR: dialogId is null or empty");
            return;
        }
        
        String filePath = STORAGE_DIR + "/" + dialogId + ".json";
        File file = new File(filePath);
        
        if (file.exists()) {
            if (file.delete()) {
                lastPaths.remove(dialogId);
                logger.debug("Last path reset: {}", dialogId);
            } else {
                logger.error("Failed to delete path file for: {}", dialogId);
            }
        }
    }
    
    /**
     * Загружает все сохраненные пути при старте
     */
    private void loadAllPaths() {
        File dir = new File(STORAGE_DIR);
        if (!dir.exists() || !dir.isDirectory()) {
            logger.debug("No file paths directory found");
            return;
        }
        
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            logger.debug("No file path files found");
            return;
        }
        
        for (File file : files) {
            String dialogId = file.getName().replace(".json", "");
            String path = getLastPath(dialogId);
            if (path != null) {
                lastPaths.put(dialogId, path);
            }
        }
        
        logger.debug("Loaded {} file paths", lastPaths.size());
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
