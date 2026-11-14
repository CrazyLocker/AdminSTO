package com.autoservice;

import com.autoservice.builders.SparePartBuilder;
import com.autoservice.utils.ThemeManager;
import com.autoservice.utils.ThemePreferences;
import com.autoservice.views.StockOperationDialog;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.Border;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;
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
 * Регрессия «окно „Операция со складом“ не соответствует светлой теме».
 *
 * <p>Блок выбора «Приход/Списание» был залит жёстким тёмно-синим цветом
 * {@code #1E2139} с синей обводкой {@code #3B82F6}, поэтому в светлых темах
 * выглядел как чужеродное тёмное пятно. Тест открывает диалог в Primer Light,
 * проверяет, что фон и обводка разрешились из looked-up переменных темы, и
 * сохраняет снимок для PR.</p>
 *
 * <p>Суффикс каталога снимков задаётся переменной окружения
 * {@code EVIDENCE_SUFFIX} ({@code after} по умолчанию), чтобы получить
 * настоящий Before на коде до исправления.</p>
 */
@Tag(TestTags.UI)
@EnabledIfSystemProperty(named = "javafx.headless", matches = "true")
class StockOperationDialogThemeUITest extends BaseUITest {

    private static final Path EVIDENCE_DIR = Path.of(".github", "validation-evidence",
            "bugfix-1", System.getenv().getOrDefault("EVIDENCE_SUFFIX", "after"));

    private static final Color OLD_BOX_BACKGROUND = Color.web("#1E2139");
    private static final Color OLD_BOX_BORDER = Color.web("#3B82F6");

    @TempDir
    Path tempDir;

    private String previousThemeFile;

    @BeforeEach
    void setUpThemeFile() {
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
    @DisplayName("Светлая тема: блок выбора операции использует цвета темы")
    void operationTypeBlockUsesThemeColors() {
        ThemePreferences.saveTheme("primer-light");
        runOnFxThread(ThemeManager::applySavedTheme);
        createStage();

        SparePart part = new SparePartBuilder()
                .withName("Масляный фильтр")
                .withStock(5)
                .withMinStock(2)
                .withUnitType("шт")
                .withLocation("A-01")
                .build();
        DataStore.addSparePart(part);

        runOnFxThread(() -> {
            Platform.runLater(this::captureAndAssert);
            StockOperationDialog.show(part, "ПРИХОД");
        });
    }

    /**
     * Выполняется на FX-потоке, пока диалог открыт во вложенном цикле событий:
     * снимает окно, проверяет разрешённые цвета и закрывает диалог.
     */
    private void captureAndAssert() {
        Stage dialog = findOpenDialog();
        assertThat(dialog)
                .as("Диалог «Операция со складом» открыт")
                .isNotNull();

        Scene scene = dialog.getScene();
        scene.getRoot().applyCss();
        scene.getRoot().layout();
        writeEvidence("primer-light-stock-operation-dialog", scene.snapshot(null));

        try {
            Region typeBox = (Region) scene.getRoot().lookup("#stockOperationTypeBox");
            assertThat(typeBox).as("Блок выбора операции найден").isNotNull();

            Background background = typeBox.getBackground();
            assertThat(background).as("У блока выбора есть фон").isNotNull();
            Color fill = (Color) background.getFills().get(0).getFill();
            assertThat(fill)
                    .as("Фон блока берётся из темы, а не из прежнего тёмно-синего #1E2139")
                    .isNotEqualTo(OLD_BOX_BACKGROUND);
            assertThat(fill.getBrightness())
                    .as("В светлой теме фон блока светлый")
                    .isGreaterThan(0.8);

            Border border = typeBox.getBorder();
            assertThat(border).as("У блока выбора есть обводка").isNotNull();
            assertThat(border.getStrokes()).isNotEmpty();
            Color stroke = (Color) border.getStrokes().get(0).getTopStroke();
            assertThat(stroke)
                    .as("Обводка берётся из темы, а не из прежнего синего #3B82F6")
                    .isNotEqualTo(OLD_BOX_BORDER);
            assertThat(stroke.getBrightness())
                    .as("В светлой теме обводка светлая")
                    .isGreaterThan(0.6);

            Label stockLabel = (Label) scene.getRoot().lookup("#stockOperationStockLabel");
            assertThat(stockLabel).as("Подпись остатка найдена").isNotNull();
            Color textFill = (Color) stockLabel.getTextFill();
            assertThat(textFill)
                    .as("Текст остатка читается на светлом фоне: в светлой теме он тёмный")
                    .isNotNull();
            assertThat(textFill.getBrightness()).isLessThan(0.6);
        } finally {
            dialog.close();
        }
    }

    private Stage findOpenDialog() {
        for (Window window : Window.getWindows()) {
            if (window instanceof Stage stage && stage.isShowing()
                    && stage.getTitle() != null
                    && stage.getTitle().startsWith("Операция со складом")) {
                return stage;
            }
        }
        return null;
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
}
