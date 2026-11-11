package com.autoservice.utils;

import atlantafx.base.theme.CupertinoDark;
import atlantafx.base.theme.CupertinoLight;
import atlantafx.base.theme.Dracula;
import atlantafx.base.theme.NordDark;
import atlantafx.base.theme.NordLight;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import atlantafx.base.theme.Theme;
import javafx.application.Application;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Применяет выбранную тему AtlantaFX только при запуске приложения.
 *
 * <p>Динамическая смена темы во время работы не поддерживается: она приводит
 * к ошибкам JavaFX 21 (например, {@code ClassCastException: String cannot be
 * cast to Paint} при замене набора stylesheets). Выбор пользователя хранится
 * в {@code config/theme.txt} (см. {@link ThemePreferences}), а в
 * {@code App.start()} тема читается и применяется через
 * {@link Application#setUserAgentStylesheet(String)} до создания {@link Scene}.</p>
 */
public final class ThemeManager {

    private static final Logger logger = LoggerFactory.getLogger(ThemeManager.class);

    /**
     * Доступные темы оформления.
     */
    public enum AppTheme {
        PRIMER_DARK("primer-dark", "Primer Dark", PrimerDark.class),
        PRIMER_LIGHT("primer-light", "Primer Light", PrimerLight.class),
        NORD_DARK("nord-dark", "Nord Dark", NordDark.class),
        NORD_LIGHT("nord-light", "Nord Light", NordLight.class),
        CUPERTINO_DARK("cupertino-dark", "Cupertino Dark", CupertinoDark.class),
        CUPERTINO_LIGHT("cupertino-light", "Cupertino Light", CupertinoLight.class),
        DRACULA("dracula", "Dracula", Dracula.class);

        /** Идентификатор темы для {@code config/theme.txt}. */
        public final String id;

        /** Отображаемое название темы. */
        public final String displayName;

        /** Класс темы AtlantaFX. */
        public final Class<? extends Theme> themeClass;

        AppTheme(String id, String displayName, Class<? extends Theme> themeClass) {
            this.id = id;
            this.displayName = displayName;
            this.themeClass = themeClass;
        }

        /**
         * Возвращает тему по идентификатору из файла настроек.
         *
         * @param id идентификатор темы (например, {@code primer-light})
         * @return найденная тема или {@code null}, если идентификатор неизвестен
         */
        public static AppTheme fromId(String id) {
            if (id == null) {
                return null;
            }
            String normalized = id.trim().toLowerCase(Locale.ROOT).replace('_', '-');
            for (AppTheme theme : values()) {
                if (theme.id.equals(normalized)) {
                    return theme;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private static final List<AppTheme> THEMES = Arrays.asList(AppTheme.values());

    private ThemeManager() {
    }

    /**
     * Читает тему из {@code config/theme.txt}.
     *
     * @return сохранённая тема или {@link ThemePreferences#DEFAULT_THEME},
     *         если файл отсутствует либо содержит неизвестный идентификатор
     */
    public static AppTheme getSavedTheme() {
        String saved = ThemePreferences.loadTheme();
        AppTheme theme = AppTheme.fromId(saved);
        if (theme != null) {
            return theme;
        }
        logger.warn("Неизвестная тема '{}' в {} — используется {}",
                saved, ThemePreferences.getThemeFile(), ThemePreferences.DEFAULT_THEME);
        return AppTheme.fromId(ThemePreferences.DEFAULT_THEME);
    }

    /**
     * Сохраняет выбор темы в {@code config/theme.txt}. Интерфейс при этом не
     * меняется: новая тема применится только после перезапуска.
     *
     * @param theme выбранная тема
     */
    public static void saveTheme(AppTheme theme) {
        if (theme == null) {
            return;
        }
        ThemePreferences.saveTheme(theme.id);
    }

    /**
     * Читает тему из {@code config/theme.txt} и применяет её. Вызывается в
     * {@code App.start()} до создания сцены, чтобы всё окно сразу открылось
     * в правильной теме.
     */
    public static void applySavedTheme() {
        applyTheme(getSavedTheme());
    }

    /**
     * Применяет тему через {@link Application#setUserAgentStylesheet(String)}.
     * Вызывается только при старте, до создания сцены.
     *
     * @param theme тема для применения
     */
    private static void applyTheme(AppTheme theme) {
        try {
            Theme instance = theme.themeClass.getDeclaredConstructor().newInstance();
            Application.setUserAgentStylesheet(instance.getUserAgentStylesheet());
            logger.info("Применена тема при запуске: {}", theme.displayName);
        } catch (ReflectiveOperationException e) {
            logger.error("Не удалось применить тему {}: {}",
                    theme.displayName, e.getMessage(), e);
            Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA);
        }
    }

    /**
     * Добавляет кастомные стили приложения к сцене. Вызывается один раз после
     * создания сцены; динамического переключения стилей здесь нет.
     *
     * @param scene главная сцена приложения
     */
    public static void applySceneStyles(Scene scene) {
        if (scene == null) {
            return;
        }
        addStylesheet(scene, "/global-custom.css");
        addStylesheet(scene, "/dashboard-custom.css");
    }

    private static void addStylesheet(Scene scene, String resourcePath) {
        URL url = ThemeManager.class.getResource(resourcePath);
        if (url == null) {
            logger.warn("CSS не найден: {}", resourcePath);
            return;
        }
        String stylesheet = url.toExternalForm();
        if (!scene.getStylesheets().contains(stylesheet)) {
            scene.getStylesheets().add(stylesheet);
        }
    }

    /**
     * Возвращает список доступных тем для выпадающего списка в настройках.
     *
     * @return все поддерживаемые темы
     */
    public static List<AppTheme> getAvailableThemes() {
        return THEMES;
    }
}
