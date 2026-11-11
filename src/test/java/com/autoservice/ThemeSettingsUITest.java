package com.autoservice;

import com.autoservice.utils.ThemeManager;
import com.autoservice.utils.ThemePreferences;
import javafx.application.Application;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
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
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UI-тест вкладки «Настройки → Внешний вид».
 *
 * <p>Проверяет заявленный сценарий: тема читается из {@code config/theme.txt}
 * только при запуске, выбор в ComboBox лишь сохраняется в файл, а рядом
 * выводится подсказка о перезапуске. Динамического переключения темы больше
 * нет, поэтому после выбора другой темы пользовательский agent stylesheet не
 * меняется.</p>
 */
@Tag(TestTags.UI)
@EnabledIfSystemProperty(named = "javafx.headless", matches = "true")
class ThemeSettingsUITest extends BaseUITest {

    private static final String RESTART_HINT = "Для применения темы перезапустите приложение";
    private static final Path EVIDENCE_DIR =
            Path.of("build", "ui-test-evidence", "theme-startup");

    @TempDir
    Path tempDir;

    private String previousThemeFile;

    @BeforeEach
    void setUpStage() {
        previousThemeFile = System.getProperty(ThemePreferences.THEME_FILE_PROPERTY);
        System.setProperty(ThemePreferences.THEME_FILE_PROPERTY,
                tempDir.resolve("config").resolve("theme.txt").toString());
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
    @DisplayName("При старте применяется тема из config/theme.txt, в настройках есть подсказка")
    void startupAppliesThemeFromFileAndShowsRestartHint() {
        ThemePreferences.saveTheme("dracula");

        launchApp();

        assertThat(Application.getUserAgentStylesheet())
                .as("Тема из config/theme.txt должна применяться при старте")
                .contains("dracula.css");

        ComboBox<ThemeManager.AppTheme> combo = themeCombo();
        assertThat(combo.getValue()).isEqualTo(ThemeManager.AppTheme.DRACULA);
        assertThat(restartHint().getText()).isEqualTo(RESTART_HINT);

        writeEvidence("after-settings-dracula");
    }

    @Test
    @DisplayName("Сохранённая тёмная тема применяется при старте")
    void startupAppliesSavedDarkTheme() {
        ThemePreferences.saveTheme("primer-dark");

        launchApp();

        assertThat(Application.getUserAgentStylesheet()).contains("primer-dark.css");
        assertThat(themeCombo().getValue()).isEqualTo(ThemeManager.AppTheme.PRIMER_DARK);

        writeEvidence("after-settings-primer-dark");
    }

    @Test
    @DisplayName("По умолчанию используется primer-light, выбор только сохраняется в файл")
    void settingsSaveThemeWithoutDynamicSwitching() {
        launchApp();

        ComboBox<ThemeManager.AppTheme> combo = themeCombo();
        assertThat(combo.getValue()).isEqualTo(ThemeManager.AppTheme.PRIMER_LIGHT);
        assertThat(restartHint().getText()).isEqualTo(RESTART_HINT);

        writeEvidence("after-settings-primer-light");

        String appliedStylesheet = Application.getUserAgentStylesheet();

        runOnFxThread(() -> combo.setValue(ThemeManager.AppTheme.NORD_DARK));
        settle();

        assertThat(ThemePreferences.loadTheme()).isEqualTo("nord-dark");
        assertThat(combo.getValue()).isEqualTo(ThemeManager.AppTheme.NORD_DARK);
        assertThat(Application.getUserAgentStylesheet())
                .as("Выбор темы не должен переключать оформление без перезапуска")
                .isEqualTo(appliedStylesheet);

        writeEvidence("after-settings-selected-nord-dark");
    }

    private void launchApp() {
        runOnFxThread(ThemeManager::applySavedTheme);
        createStage();
        runOnFxThread(() -> ThemeManager.applySceneStyles(currentScene));

        Stage stage = getOnFxThread(() -> (Stage) currentScene.getWindow());
        runOnFxThread(() -> {
            stage.setWidth(1200);
            stage.setHeight(700);
            stage.centerOnScreen();
        });

        runOnFxThread(() -> ((TabPane) findNode("mainTabPane")).getSelectionModel().select(7));
        settle();
    }

    @SuppressWarnings("unchecked")
    private ComboBox<ThemeManager.AppTheme> themeCombo() {
        return (ComboBox<ThemeManager.AppTheme>) findNode("themeCombo");
    }

    private Label restartHint() {
        return (Label) findNode("themeRestartHint");
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

    private void writeEvidence(String fileName) {
        WritableImage image = getOnFxThread(() -> currentScene.getRoot().snapshot(null, null));
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
}
