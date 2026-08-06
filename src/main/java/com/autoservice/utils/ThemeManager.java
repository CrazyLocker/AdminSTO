package com.autoservice.utils;

import atlantafx.base.theme.*;
import com.autoservice.services.SettingService;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;

import java.util.Arrays;
import java.util.List;

/**
 * Менеджер тем AtlantaFX.
 * - Переключает тему в runtime
 * - Сохраняет выбор в БД через SettingService
 * - При старте загружает сохранённую тему
 */
public class ThemeManager {

    private static final String SETTING_KEY = "app_theme";

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
     * Инициализация менеджера тем. Вызвать ПОСЛЕ создания Scene и инициализации БД.
     * Загружает сохранённую тему из настроек.
     */
    public static void init(Scene scene) {
        ThemeManager.scene = scene;

        // Попробовать загрузить сохранённую тему
        AppTheme themeToApply = currentTheme;
        try {
            String saved = SettingService.getSettingValue(SETTING_KEY);
            if (saved != null) {
                themeToApply = AppTheme.valueOf(saved);
            }
        } catch (Exception ignored) {}

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
     * Применить тему: переключить AtlantaFX, перезагрузить CSS, сохранить в БД.
     */
    private static void applyTheme(AppTheme theme) {
        try {
            Theme instance = theme.themeClass.getDeclaredConstructor().newInstance();
            Application.setUserAgentStylesheet(instance.getUserAgentStylesheet());
            currentTheme = theme;

            // Сохранить в БД
            try {
                SettingService.setSettingValue(SETTING_KEY, theme.name());
            } catch (Exception ignored) {}

            // Удалить старые кастомные CSS
            scene.getStylesheets().removeIf(s ->
                    s.contains("global-custom") || s.contains("dashboard-custom")
            );

            // Загрузить нужные CSS
            loadCSS("/global-custom.css");
            String dashboardCss = currentTheme.isDark()
                    ? "/dashboard-custom.css"
                    : "/dashboard-custom-light.css";
            loadCSS(dashboardCss);

            // Принудительное обновление сцены
            ObservableList<String> sheets = FXCollections.observableArrayList(scene.getStylesheets());
            scene.getStylesheets().clear();
            scene.getStylesheets().addAll(sheets);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Ошибка переключения темы: " + e.getMessage());
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