package com.autoservice;

import com.autoservice.utils.ThemeManager;
import com.autoservice.utils.ThemePreferences;
import com.autoservice.views.DashboardView;
import javafx.scene.control.TabPane;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UI-тест соответствия дашборда выбранной теме AtlantaFX.
 *
 * <p>Тема применяется только при запуске приложения (см.
 * {@link ThemeManager}), поэтому каждый сценарий сохраняет тему в
 * {@code config/theme.txt}, применяет её до создания сцены и затем снимает
 * дашборд. Тёмные элементы дашборда не должны перекрывать светлую тему
 * {@code primer-light}, а тема {@code dracula} должна оставаться тёмной.</p>
 *
 * <p>Снимки экрана сохраняются в {@code build/ui-test-evidence/dashboard-theme}
 * и служат визуальным подтверждением в PR.</p>
 */
@Tag(TestTags.UI)
@EnabledIfSystemProperty(named = "javafx.headless", matches = "true")
class DashboardThemeUITest extends BaseUITest {

    private static final Path EVIDENCE_DIR =
            Path.of("build", "ui-test-evidence", "dashboard-theme");

    /** Пограничное значение средней яркости, разделяющее светлый и тёмный фон. */
    private static final double LIGHT_BACKGROUND_THRESHOLD = 0.6;
    private static final double DARK_BACKGROUND_THRESHOLD = 0.4;

    @TempDir
    Path tempDir;

    private String previousThemeFile;

    @BeforeEach
    void setUpThemeFile() {
        previousThemeFile = System.getProperty(ThemePreferences.THEME_FILE_PROPERTY);
        System.setProperty(ThemePreferences.THEME_FILE_PROPERTY,
                tempDir.resolve("config").resolve("theme.txt").toString());
        resetDashboardSingleton();
    }

    @AfterEach
    void tearDown() {
        if (previousThemeFile == null) {
            System.clearProperty(ThemePreferences.THEME_FILE_PROPERTY);
        } else {
            System.setProperty(ThemePreferences.THEME_FILE_PROPERTY, previousThemeFile);
        }
    }

    @Test
    @DisplayName("Дашборд светлый при теме primer-light")
    void dashboardIsLightWithPrimerLightTheme() {
        WritableImage image = launchDashboard("primer-light");

        writeEvidence("primer-light-dashboard", image);

        assertThat(averageLuminance(image))
                .as("Средняя яркость дашборда в светлой теме primer-light")
                .isGreaterThan(LIGHT_BACKGROUND_THRESHOLD);
    }

    @Test
    @DisplayName("Дашборд тёмный при теме dracula")
    void dashboardIsDarkWithDraculaTheme() {
        WritableImage image = launchDashboard("dracula");

        writeEvidence("dracula-dashboard", image);

        assertThat(averageLuminance(image))
                .as("Средняя яркость дашборда в тёмной теме dracula")
                .isLessThan(DARK_BACKGROUND_THRESHOLD);
    }

    /**
     * Запускает приложение с указанной темой, открывает вкладку «Дашборд»
     * и возвращает снимок сцены.
     *
     * @param themeId идентификатор темы AtlantaFX
     * @return снимок окна приложения размером 1200×700
     */
    private WritableImage launchDashboard(String themeId) {
        ThemePreferences.saveTheme(themeId);

        runOnFxThread(ThemeManager::applySavedTheme);
        createStage();
        runOnFxThread(() -> ThemeManager.applySceneStyles(currentScene));

        Stage stage = getOnFxThread(() -> (Stage) currentScene.getWindow());
        runOnFxThread(() -> {
            stage.setWidth(1200);
            stage.setHeight(700);
            stage.centerOnScreen();
            ((TabPane) findNode("mainTabPane")).getSelectionModel().select(0);
        });
        settle();

        return getOnFxThread(() -> currentScene.getRoot().snapshot(null, null));
    }

    /**
     * Средняя яркость нижней части снимка (без строки вкладок).
     * Значение 0 — чёрный, 1 — белый.
     */
    private static double averageLuminance(WritableImage image) {
        PixelReader reader = image.getPixelReader();
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        double sum = 0;
        long count = 0;
        for (int y = 70; y < height - 5; y += 3) {
            for (int x = 5; x < width - 5; x += 3) {
                Color color = reader.getColor(x, y);
                sum += 0.2126 * color.getRed()
                        + 0.7152 * color.getGreen()
                        + 0.0722 * color.getBlue();
                count++;
            }
        }
        return count == 0 ? 0 : sum / count;
    }

    private void settle() {
        for (int i = 0; i < 8; i++) {
            waitForFxEvents();
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void writeEvidence(String fileName, WritableImage image) {
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
            ImageIO.write(buffered, "png", EVIDENCE_DIR.resolve(fileName + ".png").toFile());
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось сохранить скриншот " + fileName, e);
        }
    }

    /**
     * Дашборд — синглтон: между тестами возвращаем null, чтобы следующий
     * сценарий построил его заново уже с другой темой.
     */
    private static void resetDashboardSingleton() {
        try {
            Field instance = DashboardView.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Не удалось сбросить синглтон DashboardView", e);
        }
    }
}
