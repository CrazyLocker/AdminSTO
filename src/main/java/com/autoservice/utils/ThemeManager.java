package com.autoservice.utils;

import atlantafx.base.theme.*;
import com.autoservice.config.SettingsManager;
import javafx.application.Application;
import javafx.scene.Scene;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Менеджер тем AtlantaFX.
 * - Переключает тему в runtime
 * - Сохраняет выбор в JSON через SettingsManager (НЕ в БД)
 * - При старте загружает тему из SettingsManager
 * - После темы подключает theme-fixes.css с резервными определениями
 *   базовых look-up цветов (-color-fg-*)
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

        /**
         * CSS-класс, который помечает корень сцены выбранной темой.
         * Используется в theme-fixes.css для выбора значений нужной палитры.
         */
        public String cssClass() {
            return "theme-" + name().toLowerCase().replace('_', '-');
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private static final String THEME_FIXES = "/theme-fixes.css";
    private static final String GLOBAL_CUSTOM = "/global-custom.css";
    private static final String DASHBOARD_DARK = "/dashboard-custom.css";
    private static final String DASHBOARD_LIGHT = "/dashboard-custom-light.css";

    private static Scene scene;
    private static AppTheme currentTheme = AppTheme.PRIMER_DARK;
    private static final List<AppTheme> THEMES = Arrays.asList(AppTheme.values());

    /** Стили, добавленные самим ThemeManager. Их можно безопасно убрать при смене темы. */
    private static final List<String> managedStylesheets = new ArrayList<>();

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
            String themeCssUrl = resolveThemeStylesheet(instance.getUserAgentStylesheet());

            // 1. Глобальная загрузка темы (user-agent stylesheet).
            //    Применяется ко ВСЕМ окнам: главная сцена, Alert'ы, диалоги.
            Application.setUserAgentStylesheet(themeCssUrl);

            // 2. Author-level загрузка темы и стилей поверх неё для главной Scene.
            //    theme-fixes.css идёт сразу после темы и повторно объявляет
            //    базовые look-up цвета, чтобы они не терялись при переключении.
            applySceneStylesheets(theme, themeCssUrl);

            currentTheme = theme;

            // Сохранить тему в JSON (НЕ в БД)
            SettingsManager.setTheme(theme.name());

        } catch (Exception e) {
            System.err.println("Ошибка загрузки темы " + theme.displayName + ": " + e.getMessage());
            e.printStackTrace();

            // Fallback: стандартная тема JavaFX (Modena)
            try {
                Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA);
                clearThemeClass();

                List<String> target = new ArrayList<>();
                addResource(target, THEME_FIXES);
                addResource(target, GLOBAL_CUSTOM);
                addResource(target, DASHBOARD_DARK);
                replaceManagedStylesheets(target);
                System.err.println("Загружена fallback-тема (Modena)");
            } catch (Exception fallbackErr) {
                System.err.println("Fallback-тема недоступна: " + fallbackErr.getMessage());
            }
        }
    }

    /**
     * Возвращает URL темы, пригодный и для setUserAgentStylesheet, и для Scene.
     * AtlantaFX отдаёт путь вида "/atlantafx/base/theme/primer-dark.css";
     * JavaFX надёжнее работает с уже разрешённым URL, особенно внутри jar.
     */
    private static String resolveThemeStylesheet(String themePath) {
        URL url = ThemeManager.class.getResource(themePath);
        return url != null ? url.toExternalForm() : themePath;
    }

    /**
     * Подключает author-стили к главной сцене в правильном порядке:
     * тема → theme-fixes.css → глобальные и дашбордные стили.
     * Убирает только стили, добавленные предыдущим вызовом, — чужие
     * stylesheets сцены (например, /css/appointment.css) не затрагиваются.
     */
    private static void applySceneStylesheets(AppTheme theme, String themeCssUrl) {
        markTheme(theme);

        List<String> target = new ArrayList<>();
        target.add(themeCssUrl);
        addResource(target, THEME_FIXES);
        addResource(target, GLOBAL_CUSTOM);
        addResource(target, theme.isDark() ? DASHBOARD_DARK : DASHBOARD_LIGHT);

        replaceManagedStylesheets(target);
    }

    /**
     * Заменяет ранее добавленные ThemeManager-ом стили на новый набор.
     *
     * <p>Новые stylesheets сначала добавляются (по позициям старых), и только
     * потом удаляются прежние. Удаление до добавления (например,
     * {@code getStylesheets().clear()}) воспроизводит ошибку JavaFX
     * JDK-8268657: look-up цвета не успевают разрешиться, и в консоли
     * появляется ClassCastException "String cannot be cast to Paint" из
     * правил темы. Такой порядок замены — обходной путь из отчёта JDK-8268657.</p>
     */
    private static void replaceManagedStylesheets(List<String> target) {
        if (scene == null) return;

        List<String> previous = new ArrayList<>(managedStylesheets);

        for (int i = 0; i < target.size(); i++) {
            String externalForm = target.get(i);
            if (scene.getStylesheets().contains(externalForm)) continue;

            // Вставляем новый stylesheet на место соответствующего старого,
            // чтобы сохранить порядок тема → fixes → глобальные стили.
            int index = i < previous.size() ? scene.getStylesheets().indexOf(previous.get(i)) : -1;
            if (index >= 0) {
                scene.getStylesheets().add(index, externalForm);
            } else {
                scene.getStylesheets().add(externalForm);
            }
        }

        List<String> stale = new ArrayList<>(previous);
        stale.removeAll(target);
        scene.getStylesheets().removeAll(stale);

        managedStylesheets.clear();
        managedStylesheets.addAll(target);
    }

    /**
     * Помечает корень сцены CSS-классом темы, чтобы theme-fixes.css
     * подставил значения нужной палитры.
     */
    private static void markTheme(AppTheme theme) {
        if (scene == null || scene.getRoot() == null) return;

        List<String> styleClasses = scene.getRoot().getStyleClass();
        styleClasses.removeIf(styleClass -> styleClass.startsWith("theme-"));
        styleClasses.add(theme.cssClass());
    }

    private static void clearThemeClass() {
        if (scene == null || scene.getRoot() == null) return;
        scene.getRoot().getStyleClass().removeIf(styleClass -> styleClass.startsWith("theme-"));
    }

    private static void addResource(List<String> target, String path) {
        URL url = ThemeManager.class.getResource(path);
        if (url == null) {
            System.err.println("CSS не найден: " + path);
            return;
        }

        String externalForm = url.toExternalForm();
        if (!target.contains(externalForm)) {
            target.add(externalForm);
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
