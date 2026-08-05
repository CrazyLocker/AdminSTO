package com.autoservice.utils;

import atlantafx.base.theme.*;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;

import java.util.Arrays;
import java.util.List;

/**
 * Менеджер тем AtlantaFX. Позволяет переключать тему в runtime.
 * Управляет и глобальным, и дашборд-специфичным CSS.
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
     */
    public static void init(Scene scene) {
        ThemeManager.scene = scene;
        reloadCustomCSS();
    }

    /**
     * Установить тему по enum-значению.
     */
    public static void setTheme(AppTheme theme) {
        if (scene == null) return;
        if (theme == currentTheme) return;
        try {
            // 1. Устанавливаем AtlantaFX тему
            Theme instance = theme.themeClass.getDeclaredConstructor().newInstance();
            Application.setUserAgentStylesheet(instance.getUserAgentStylesheet());
            currentTheme = theme;

            // 2. Удаляем ВСЕ кастомные CSS
            scene.getStylesheets().removeIf(s ->
                s.contains("global-custom") || s.contains("dashboard-custom")
            );

            // 3. Подгружаем нужные CSS (тёмные/светлые)
            reloadCustomCSS();

            // 4. Принудительное обновление — пересоздаём список стилей
            //    чтобы JavaFX пересчитал все стили для существующих нод
            ObservableList<String> sheets = FXCollections.observableArrayList(scene.getStylesheets());
            scene.getStylesheets().clear();
            scene.getStylesheets().addAll(sheets);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Ошибка переключения темы: " + e.getMessage());
        }
    }

    /**
     * Загрузить кастомные CSS в зависимости от текущей темы.
     */
    private static void reloadCustomCSS() {
        if (scene == null) return;

        boolean dark = currentTheme.isDark();
        String globalCss = dark ? "/global-custom.css" : "/global-custom-light.css";
        String dashboardCss = dark ? "/dashboard-custom.css" : "/dashboard-custom-light.css";

        loadCSS(globalCss);
        loadCSS(dashboardCss);
    }

    /**
     * Безопасная загрузка CSS-файла из resources.
     */
    private static void loadCSS(String path) {
        java.net.URL url = ThemeManager.class.getResource(path);
        if (url != null) {
            String externalForm = url.toExternalForm();
            // Не добавляем дубль
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
