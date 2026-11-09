package com.autoservice.utils;

import atlantafx.base.theme.*;
import com.autoservice.config.SettingsManager;
import javafx.application.Application;
import javafx.application.Platform;
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
 * - После смены темы заставляет JavaFX заново применить CSS ко всему дереву
 *   узлов сцены, чтобы look-up цвета AtlantaFX не остались от прошлой темы
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
     * Стили сцены, которыми управляет менеджер тем. Храним их отдельно, чтобы
     * при смене темы убирать только свои стили и не затирать чужие
     * (например, /css/appointment.css, который добавляет App).
     */
    private static final List<String> managedSceneStylesheets = new ArrayList<>();

    /** Подписчики на смену темы (например, дашборд, пересобирающий палитру). */
    private static final List<Runnable> themeChangeListeners = new ArrayList<>();

    /**
     * Инициализация менеджера тем. Вызвать ПОСЛЕ создания Scene.
     * Загружает тему из SettingsManager (JSON), НЕ из БД.
     */
    public static void init(Scene scene) {
        if (ThemeManager.scene != scene) {
            // Новая сцена (например, в тестах) — прежний набор стилей к ней не относится.
            managedSceneStylesheets.clear();
        }
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
     * Подписаться на смену темы. Подписчик вызывается на JavaFX Application
     * Thread после того, как к корню сцены повторно применён CSS.
     *
     * @param listener действие, выполняемое после смены темы
     */
    public static void addThemeChangeListener(Runnable listener) {
        if (listener != null && !themeChangeListeners.contains(listener)) {
            themeChangeListeners.add(listener);
        }
    }

    /**
     * Применить тему: сохранить в JSON и загрузить CSS.
     */
    private static void applyTheme(AppTheme theme) {
        try {
            Theme instance = theme.themeClass.getDeclaredConstructor().newInstance();
            String themeCssUrl = resolveStylesheet(instance.getUserAgentStylesheet());

            // Сохранить тему в JSON (НЕ в БД)
            SettingsManager.setTheme(theme.name());

            // 1. Глобальная загрузка темы (user-agent stylesheet).
            //    Применяется ко ВСЕМ окнам: главная сцена, Alert'ы, диалоги.
            Application.setUserAgentStylesheet(themeCssUrl);

            // 2. Author-level загрузка для главной Scene + кастомные стили.
            replaceSceneStylesheets(themeCssUrl);

            currentTheme = theme;
            refreshSceneNodeTree();
        } catch (Exception e) {
            System.err.println("Ошибка загрузки темы " + theme.displayName + ": " + e.getMessage());
            e.printStackTrace();

            // Fallback: стандартная тема JavaFX (Modena)
            try {
                Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA);
                replaceSceneStylesheets(null);
                currentTheme = theme;
                refreshSceneNodeTree();
                System.err.println("Загружена fallback-тема (Modena)");
            } catch (Exception fallbackErr) {
                System.err.println("Fallback-тема недоступна: " + fallbackErr.getMessage());
            }
        }
    }

    /**
     * Превращает путь темы из AtlantaFX (например,
     * {@code /atlantafx/base/theme/primer-dark.css}) в реальный URL.
     */
    private static String resolveStylesheet(String rawUrl) {
        if (rawUrl == null) return null;
        URL url = ThemeManager.class.getResource(rawUrl);
        return url != null ? url.toExternalForm() : rawUrl;
    }

    /**
     * Заменяет стили сцены, которыми управляет менеджер тем.
     *
     * <p>Новые стили добавляются до удаления старых: JavaFX 21 разрешает
     * look-up цвета при замене набора stylesheets с задержкой (JDK-8268657),
     * и очистка списка «в один шаг» оставляет часть узлов без цветов —
     * отсюда ClassCastException «String cannot be cast to Paint» и разная
     * тема на разных вкладках.</p>
     */
    private static void replaceSceneStylesheets(String themeCssUrl) {
        if (scene == null) return;

        List<String> next = new ArrayList<>();
        if (themeCssUrl != null) {
            next.add(themeCssUrl);
        }
        addStylesheetIfPresent(next, "/global-custom.css");
        addStylesheetIfPresent(next, "/dashboard-custom.css");

        // add-then-remove вместо clear(): старые стили остаются на месте, пока
        // новые уже загружены, поэтому цвета не «пропадают» между кадрами.
        for (String stylesheet : next) {
            if (!scene.getStylesheets().contains(stylesheet)) {
                scene.getStylesheets().add(stylesheet);
            }
        }
        scene.getStylesheets().removeAll(managedSceneStylesheets);
        managedSceneStylesheets.clear();
        managedSceneStylesheets.addAll(next);
    }

    private static void addStylesheetIfPresent(List<String> target, String resourcePath) {
        URL url = ThemeManager.class.getResource(resourcePath);
        if (url == null) {
            System.err.println("CSS не найден: " + resourcePath);
            return;
        }
        target.add(url.toExternalForm());
    }

    /**
     * Принудительно повторно применяет CSS ко всему дереву узлов сцены и
     * пересчитывает layout. Без этого после смены user-agent stylesheet часть
     * узлов может остаться с разрешёнными цветами прежней темы.
     *
     * <p>Вызывается через {@link Platform#runLater(Runnable)}, чтобы дать
     * JavaFX завершить текущую смену набора stylesheets.</p>
     */
    private static void refreshSceneNodeTree() {
        Platform.runLater(() -> {
            Scene currentScene = scene;
            if (currentScene != null && currentScene.getRoot() != null) {
                currentScene.getRoot().applyCss();
                currentScene.getRoot().layout();
            }
            notifyThemeChanged();
        });
    }

    private static void notifyThemeChanged() {
        for (Runnable listener : new ArrayList<>(themeChangeListeners)) {
            try {
                listener.run();
            } catch (Exception e) {
                System.err.println("Ошибка обработчика смены темы: " + e.getMessage());
            }
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
