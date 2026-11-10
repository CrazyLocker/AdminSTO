package com.autoservice.utils;

import atlantafx.base.theme.*;
import com.autoservice.config.SettingsManager;
import javafx.application.Application;
import javafx.scene.Scene;

import java.util.Arrays;
import java.util.List;

/**
 * Менеджер тем AtlantaFX.
 * - Переключает тему в runtime
 * - Сохраняет выбор в JSON через SettingsManager (НЕ в БД)
 * - При старте загружает тему из SettingsManager
 */
public class ThemeManager {

    public enum AppTheme {
        PRIMER_DARK("Primer Dark", PrimerDark.class),
        PRIMER_LIGHT("Primer Light", PrimerLight.class),
        NORD_DARK("Nord Dark", NordDark.class),
        NORD_LIGHT("Nord Light", NordLight.class),
        CUPERTINO_DARK("Cupertino Dark", CupertinoDark.class),
        CUPERTINO_LIGHT("Cupertino Light", CupertinoLight.class),
        DRACULA("Dracula", Dracula.class);

        public final String displayName;
        public final Class<? extends Theme> themeClass;

        AppTheme(String displayName, Class<? extends Theme> themeClass) {
            this.displayName = displayName;
            this.themeClass = themeClass;
        }

        public boolean isDark() {
            return this == PRIMER_DARK || this == NORD_DARK
                    || this == CUPERTINO_DARK || this == DRACULA;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private static Scene scene;
    private static AppTheme currentTheme = AppTheme.PRIMER_DARK;
    private static final List<AppTheme> THEMES = Arrays.asList(AppTheme.values());

    /**
     * Инициализация менеджера тем. Вызвать ПОСЛЕ создания Scene.
     * Загружает тему из SettingsManager (JSON), НЕ из БД.
     */
    public static void init(Scene scene) {
        ThemeManager.scene = scene;

        // Загрузить тему из JSON-конфига
        AppTheme themeToApply = currentTheme;
        try {
            String saved = SettingsManager.getTheme();
            themeToApply = AppTheme.valueOf(saved);
        } catch (Exception e) {
            System.err.println("Тема '" + SettingsManager.getTheme() + "' не найдена, используется PRIMER_DARK");
        }

        applyTheme(themeToApply);
    }

    /**
     * Переключить тему (вызывается из UI).
     */
    public static void setTheme(AppTheme theme) {
        if (scene == null || theme == currentTheme) return;
        applyTheme(theme);
    }

    /**
     * Применить тему: сохранить в JSON и загрузить CSS.
     */
    private static void applyTheme(AppTheme theme) {
        try {
            Theme instance = theme.themeClass.getDeclaredConstructor().newInstance();
            String themeCssUrl = instance.getUserAgentStylesheet();
            currentTheme = theme;

            // Сохранить тему в JSON (НЕ в БД)
            SettingsManager.setTheme(theme.name());

            // 1. Глобальная загрузка темы (user-agent stylesheet).
            //    Применяется ко ВСЕМ окнам: главная сцена, Alert'ы, диалоги.
            Application.setUserAgentStylesheet(themeCssUrl);

            // 2. Author-level загрузка для главной Scene.
            scene.getStylesheets().clear();
            scene.getStylesheets().add(themeCssUrl);

            // 3. Кастомные стили поверх темы
            loadCSS("/global-custom.css");
            String dashboardCss = currentTheme.isDark()
                    ? "/dashboard-custom.css"
                    : "/dashboard-custom-light.css";
            loadCSS(dashboardCss);

        } catch (Exception e) {
            System.err.println("Ошибка загрузки темы " + theme.displayName + ": " + e.getMessage());
            e.printStackTrace();

            // Fallback: стандартная тема JavaFX (Modena)
            try {
                Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA);
                scene.getStylesheets().clear();
                loadCSS("/global-custom.css");
                loadCSS("/dashboard-custom.css");
                System.err.println("Загружена fallback-тема (Modena)");
            } catch (Exception fallbackErr) {
                System.err.println("Fallback-тема недоступна: " + fallbackErr.getMessage());
            }
        }
    }

    private static void loadCSS(String path) {
        java.net.URL url = ThemeManager.class.getResource(path);
        if (url != null) {
            String externalForm = url.toExternalForm();
            if (!scene.getStylesheets().contains(externalForm)) {
                scene.getStylesheets().add(externalForm);
            }
        } else {
            System.err.println("CSS не найден: " + path);
        }
    }

    public static AppTheme getCurrentTheme() {
        return currentTheme;
    }

    public static List<AppTheme> getAvailableThemes() {
        return THEMES;
    }

    public static Scene getScene() {
        return scene;
    }
}