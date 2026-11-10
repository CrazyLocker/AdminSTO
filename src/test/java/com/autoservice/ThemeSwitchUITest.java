package com.autoservice;

import com.autoservice.utils.ThemeManager;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TabPane;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UI-тест переключения тем.
 *
 * <p>Проверяет главную жалобу: после смены темы дашборд и настройки должны
 * выглядеть в одной теме. Для каждой темы снимаются скриншоты дашборда и
 * вкладки «Настройки», вычисляется средний цвет фона, и тест требует, чтобы
 * средняя яркость отличалась незначительно (нет контрастного «светлая тема
 * на одной вкладке, тёмная на другой»). Скриншоты сохраняются в
 * {@code build/ui-test-evidence/theme-switch}.</p>
 */
@Tag(TestTags.UI)
@EnabledIfSystemProperty(named = "javafx.headless", matches = "true")
class ThemeSwitchUITest extends BaseUITest {

    /** Допустимое расхождение средней яркости между вкладками. */
    private static final double MAX_LUMINANCE_DELTA = 0.12;

    /** Допустимое расхождение среднего значения отдельного канала RGB. */
    private static final double MAX_CHANNEL_DELTA = 0.15;

    /** Вкладки, которые обязаны следовать выбранной теме (без «Записи»). */
    private static final int[] THEMED_TABS = {0, 1, 2, 7};

    private static final Path EVIDENCE_DIR = Path.of("build", "ui-test-evidence", "theme-switch");

    private ThemeManager.AppTheme originalTheme;

    @BeforeEach
    void setUpStage() {
        createStage();
        runOnFxThread(() -> {
            primaryStage.setWidth(1200);
            primaryStage.setHeight(700);
            primaryStage.centerOnScreen();
            ThemeManager.init(currentScene);
        });
        originalTheme = ThemeManager.getCurrentTheme();
        settle();
    }

    @AfterEach
    void restoreTheme() {
        if (originalTheme != null) {
            runOnFxThread(() -> ThemeManager.setTheme(originalTheme));
            settle();
        }
    }

    @Test
    @DisplayName("Дашборд и настройки используют одну тему при переключении всех тем")
    void dashboardAndSettingsUseSameTheme() {
        for (ThemeManager.AppTheme theme : ThemeManager.getAvailableThemes()) {
            switchThemeFromSettings(theme);

            runOnFxThread(() -> ((TabPane) findNode("mainTabPane")).getSelectionModel().select(0));
            settle();
            double[] dashboard = snapshotAndSave(theme, "dashboard");

            runOnFxThread(() -> ((TabPane) findNode("mainTabPane")).getSelectionModel().select(7));
            settle();
            double[] settings = snapshotAndSave(theme, "settings");

            assertSingleTheme(theme, dashboard, settings);
        }
    }

    @Test
    @DisplayName("Основные вкладки используют одинаковый фон в тёмной и светлой темах")
    void mainTabsShareOneBackground() {
        for (ThemeManager.AppTheme theme : new ThemeManager.AppTheme[]{
                ThemeManager.AppTheme.PRIMER_DARK,
                ThemeManager.AppTheme.PRIMER_LIGHT
        }) {
            switchThemeFromSettings(theme);
            double[] dashboard = averageColor(selectAndSnapshot(0));

            for (int tabIndex : THEMED_TABS) {
                if (tabIndex == 0) {
                    continue;
                }
                double[] tab = averageColor(selectAndSnapshot(tabIndex));
                assertThat(Math.abs(luminance(dashboard) - luminance(tab)))
                        .as("Средняя яркость вкладки %d в теме %s",
                                tabIndex, theme.displayName)
                        .isLessThanOrEqualTo(MAX_LUMINANCE_DELTA);
            }

            // Возвращаемся на дашборд, чтобы следующая тема переключалась с него.
            runOnFxThread(() -> ((TabPane) findNode("mainTabPane")).getSelectionModel().select(0));
            settle();
        }
    }

    /**
     * Переключает тему, пересобирает дашборд и ждёт завершения отложенных
     * задач JavaFX (applyCss/layout и асинхронный refresh дашборда).
     */
    /**
     * Переключает тему так же, как пользователь — через ComboBox на вкладке
     * «Настройки». Заодно проверяет, что контрол показывает выбранную тему.
     */
    private void switchThemeFromSettings(ThemeManager.AppTheme theme) {
        runOnFxThread(() -> ((TabPane) findNode("mainTabPane")).getSelectionModel().select(7));
        settle();
        @SuppressWarnings("unchecked")
        ComboBox<ThemeManager.AppTheme> combo = (ComboBox<ThemeManager.AppTheme>) findNode("themeCombo");
        runOnFxThread(() -> combo.setValue(theme));
        settle();
        assertThat(getOnFxThread(() -> ThemeManager.getCurrentTheme())).isEqualTo(theme);
    }

    private WritableImage selectAndSnapshot(int tabIndex) {
        runOnFxThread(() -> ((TabPane) findNode("mainTabPane")).getSelectionModel().select(tabIndex));
        settle();
        return getOnFxThread(() -> currentScene.getRoot().snapshot(null, null));
    }

    private double[] snapshotAndSave(ThemeManager.AppTheme theme, String tabName) {
        WritableImage image = getOnFxThread(() -> currentScene.getRoot().snapshot(null, null));
        writeEvidence(image, theme, tabName);
        return averageColor(image);
    }

    private void assertSingleTheme(ThemeManager.AppTheme theme, double[] dashboard, double[] settings) {
        double dashboardLuminance = luminance(dashboard);
        double settingsLuminance = luminance(settings);

        assertThat(Math.abs(dashboardLuminance - settingsLuminance))
                .as("Средняя яркость дашборда и настроек в теме %s (dashboard=%.3f, settings=%.3f)",
                        theme.displayName, dashboardLuminance, settingsLuminance)
                .isLessThanOrEqualTo(MAX_LUMINANCE_DELTA);

        for (int channel = 0; channel < 3; channel++) {
            assertThat(Math.abs(dashboard[channel] - settings[channel]))
                    .as("Канал %d дашборда и настроек в теме %s", channel, theme.displayName)
                    .isLessThanOrEqualTo(MAX_CHANNEL_DELTA);
        }

        // Дополнительно убеждаемся, что обе вкладки либо тёмные, либо светлые.
        assertThat(isDark(dashboardLuminance))
                .as("Дашборд и настройки должны быть одновременно тёмными или светлыми (тема %s)",
                        theme.displayName)
                .isEqualTo(isDark(settingsLuminance));
    }

    private static boolean isDark(double luminance) {
        return luminance < 0.45;
    }

    /**
     * Средний цвет снимка. Полностью прозрачные пиксели (фон окна) не
     * учитываются, чтобы результат не зависел от декораций окна.
     */
    private static double[] averageColor(WritableImage image) {
        PixelReader reader = image.getPixelReader();
        double red = 0;
        double green = 0;
        double blue = 0;
        long count = 0;
        for (int y = 0; y < image.getHeight(); y += 2) {
            for (int x = 0; x < image.getWidth(); x += 2) {
                javafx.scene.paint.Color color = reader.getColor(x, y);
                if (color.getOpacity() < 0.5) {
                    continue;
                }
                red += color.getRed();
                green += color.getGreen();
                blue += color.getBlue();
                count++;
            }
        }
        assertThat(count).as("На снимке не оказалось непрозрачных пикселей").isPositive();
        return new double[]{red / count, green / count, blue / count};
    }

    private static double luminance(double[] rgb) {
        return 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2];
    }

    private void writeEvidence(WritableImage image, ThemeManager.AppTheme theme, String tabName) {
        try {
            Files.createDirectories(EVIDENCE_DIR);
            int width = (int) image.getWidth();
            int height = (int) image.getHeight();
            BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            PixelReader reader = image.getPixelReader();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    buffered.setRGB(x, y, reader.getArgb(x, y));
                }
            }
            ImageIO.write(buffered, "png",
                    EVIDENCE_DIR.resolve(theme.name().toLowerCase() + "-" + tabName + ".png").toFile());
        } catch (IOException e) {
            System.err.println("Не удалось сохранить скриншот темы " + theme.displayName + ": " + e.getMessage());
        }
    }

    private void settle() {
        for (int i = 0; i < 6; i++) {
            waitForFxEvents();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
