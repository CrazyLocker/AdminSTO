package com.autoservice.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Общий каркас JSON-хранилища состояний: кэш в памяти и отдельный JSON-файл на ключ.
 *
 * <p>Наследники задают только каталог хранения и тип значения. Вся работа с кэшем,
 * Gson, файлами и созданием каталога сосредоточена здесь.
 *
 * @param <T> тип сохраняемого значения
 */
public abstract class JsonStateManager<T> {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_EXTENSION = ".json";

    /** Логгер наследника: имя категории соответствует конкретному менеджеру. */
    protected final Logger logger;

    private final String storageDir;
    private final Class<T> valueType;
    private final Map<String, T> cache = new HashMap<>();

    /**
     * Создать менеджер и загрузить все сохранённые значения из каталога.
     *
     * @param storageDir каталог хранения JSON-файлов
     * @param valueType тип сохраняемого значения
     */
    protected JsonStateManager(String storageDir, Class<T> valueType) {
        this.logger = LoggerFactory.getLogger(getClass());
        this.storageDir = storageDir;
        this.valueType = valueType;
        loadAll();
    }

    /**
     * Сохранить значение по ключу в кэш и JSON-файл.
     *
     * @param key ключ (имя файла без расширения)
     * @param value сохраняемое значение
     */
    protected final void save(String key, T value) {
        if (!isValidKey(key)) {
            return;
        }
        if (value == null) {
            logger.debug("Empty value for {}, skipping save", key);
            return;
        }

        cache.put(key, value);

        try {
            createDirectoryIfNotExists();
            try (Writer writer = new FileWriter(filePath(key), StandardCharsets.UTF_8)) {
                GSON.toJson(value, writer);
            }
            logger.debug("State saved: {} -> {}", key, value);
        } catch (IOException e) {
            logger.error("ERROR saving state for {}: {}", key, e.getMessage());
        }
    }

    /**
     * Получить значение по ключу, при необходимости загрузив его из файла.
     *
     * @param key ключ
     * @return сохранённое значение или {@code null}, если оно не найдено
     */
    protected final T get(String key) {
        if (!isValidKey(key)) {
            return null;
        }

        // Сначала проверяем кэш
        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        // Загружаем из файла
        File file = new File(filePath(key));
        if (!file.exists()) {
            logger.debug("No saved state found for: {}", key);
            return null;
        }

        try (Reader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            T value = GSON.fromJson(reader, valueType);
            if (value != null) {
                cache.put(key, value);
                logger.debug("State loaded: {} -> {}", key, value);
                return value;
            }
        } catch (IOException e) {
            logger.error("Error loading state for {}: {}", key, e.getMessage());
        }

        return null;
    }

    /**
     * Удалить сохранённое значение из кэша и его JSON-файл.
     *
     * @param key ключ
     */
    protected final void reset(String key) {
        if (!isValidKey(key)) {
            return;
        }

        File file = new File(filePath(key));
        boolean deleted = !file.exists() || file.delete();
        if (deleted) {
            cache.remove(key);
            logger.debug("State reset: {}", key);
        } else {
            logger.error("Failed to delete state file: {}", file.getPath());
        }
    }

    /**
     * Проверить наличие JSON-файла для ключа.
     *
     * @param key ключ
     * @return {@code true}, если файл состояния существует
     */
    protected final boolean exists(String key) {
        return key != null && !key.trim().isEmpty() && new File(filePath(key)).exists();
    }

    private boolean isValidKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            logger.error("ERROR: key is null or empty");
            return false;
        }
        return true;
    }

    /**
     * Загружает все сохранённые значения каталога в кэш при старте.
     */
    private void loadAll() {
        File dir = new File(storageDir);
        if (!dir.exists() || !dir.isDirectory()) {
            logger.debug("No state directory found: {}", storageDir);
            return;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(FILE_EXTENSION));
        if (files == null || files.length == 0) {
            logger.debug("No state files found in: {}", storageDir);
            return;
        }

        for (File file : files) {
            String key = file.getName().substring(0, file.getName().length() - FILE_EXTENSION.length());
            get(key);
        }

        logger.debug("Loaded {} states from {}", cache.size(), storageDir);
    }

    private String filePath(String key) {
        return storageDir + File.separator + key + FILE_EXTENSION;
    }

    private void createDirectoryIfNotExists() {
        File dir = new File(storageDir);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                logger.debug("Created directory: {}", storageDir);
            } else {
                logger.error("Failed to create directory: {}", storageDir);
            }
        }
    }
}
