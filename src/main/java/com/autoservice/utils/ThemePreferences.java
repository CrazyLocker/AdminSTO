package com.autoservice.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Хранит выбранную тему оформления в простом текстовом файле
 * {@code config/theme.txt}, чтобы не зависеть от базы данных.
 *
 * <p>Файл содержит идентификатор темы в нижнем регистре через дефис,
 * например {@code primer-light}. Если файл отсутствует, пуст или не может
 * быть прочитан, используется {@link #DEFAULT_THEME}.</p>
 *
 * <p>Тема применяется только при запуске приложения (см.
 * {@link ThemeManager#applySavedTheme()}); изменение файла во время работы
 * не перерисовывает интерфейс.</p>
 */
public final class ThemePreferences {

    private static final Logger logger = LoggerFactory.getLogger(ThemePreferences.class);

    /** Тема по умолчанию, если пользователь ещё ничего не выбрал. */
    public static final String DEFAULT_THEME = "primer-light";

    /** Путь к файлу с выбранной темой по умолчанию. */
    public static final Path DEFAULT_THEME_FILE = Paths.get("config", "theme.txt");

    /**
     * Системное свойство, переопределяющее путь к файлу темы. Используется
     * в тестах, чтобы не менять пользовательский {@code config/theme.txt}.
     */
    public static final String THEME_FILE_PROPERTY = "autoservice.theme.file";

    private ThemePreferences() {
    }

    /**
     * Возвращает путь к файлу с выбранной темой.
     *
     * @return путь к {@code config/theme.txt} либо путь из системного
     *         свойства {@link #THEME_FILE_PROPERTY}
     */
    public static Path getThemeFile() {
        String configured = System.getProperty(THEME_FILE_PROPERTY);
        if (configured == null || configured.isBlank()) {
            return DEFAULT_THEME_FILE;
        }
        return Paths.get(configured.trim());
    }

    /**
     * Читает идентификатор сохранённой темы.
     *
     * @return идентификатор темы или {@link #DEFAULT_THEME}, если файл
     *         отсутствует, пуст или не читается
     */
    public static String loadTheme() {
        return readTheme(getThemeFile());
    }

    /**
     * Сохраняет идентификатор выбранной темы.
     *
     * @param themeId идентификатор темы, например {@code primer-light}
     */
    public static void saveTheme(String themeId) {
        writeTheme(getThemeFile(), themeId);
    }

    /**
     * Читает тему из указанного файла.
     *
     * @param file файл с идентификатором темы
     * @return идентификатор темы или {@link #DEFAULT_THEME} при отсутствии
     *         или пустом содержимом файла
     */
    static String readTheme(Path file) {
        if (file == null || !Files.isRegularFile(file)) {
            return DEFAULT_THEME;
        }
        try {
            String value = Files.readString(file, StandardCharsets.UTF_8).trim();
            return value.isEmpty() ? DEFAULT_THEME : normalize(value);
        } catch (IOException e) {
            logger.warn("Не удалось прочитать тему из {}: {}", file, e.getMessage());
            return DEFAULT_THEME;
        }
    }

    /**
     * Записывает тему в указанный файл, создавая каталог при необходимости.
     *
     * @param file    файл с идентификатором темы
     * @param themeId идентификатор темы
     */
    static void writeTheme(Path file, String themeId) {
        if (file == null) {
            throw new IllegalArgumentException("Не задан файл для сохранения темы");
        }
        String value = normalize(themeId);
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Не задан идентификатор темы");
        }
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, value + System.lineSeparator(), StandardCharsets.UTF_8);
            logger.info("Выбранная тема сохранена в {}: {}", file, value);
        } catch (IOException e) {
            logger.error("Не удалось сохранить тему в {}: {}", file, e.getMessage());
        }
    }

    /**
     * Приводит значение из файла или UI к единому виду: нижний регистр,
     * дефис вместо подчёркивания.
     *
     * @param value исходное значение
     * @return нормализованное значение или пустая строка для {@code null}
     */
    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
