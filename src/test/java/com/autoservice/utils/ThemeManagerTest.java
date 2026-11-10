package com.autoservice.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты сопоставления идентификаторов тем и чтения сохранённой темы.
 */
class ThemeManagerTest {

    @TempDir
    Path tempDir;

    private String previousThemeFile;
    private Path themeFile;

    @BeforeEach
    void setUp() {
        previousThemeFile = System.getProperty(ThemePreferences.THEME_FILE_PROPERTY);
        themeFile = tempDir.resolve("config").resolve("theme.txt");
        System.setProperty(ThemePreferences.THEME_FILE_PROPERTY, themeFile.toString());
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
    void themeIdentifiersAreMapped() {
        assertThat(ThemeManager.AppTheme.fromId("primer-light"))
                .isEqualTo(ThemeManager.AppTheme.PRIMER_LIGHT);
        assertThat(ThemeManager.AppTheme.fromId("dracula"))
                .isEqualTo(ThemeManager.AppTheme.DRACULA);
    }

    @Test
    void themeIdentifierIsCaseInsensitive() {
        assertThat(ThemeManager.AppTheme.fromId("PRIMER_DARK"))
                .isEqualTo(ThemeManager.AppTheme.PRIMER_DARK);
        assertThat(ThemeManager.AppTheme.fromId("Nord-Light"))
                .isEqualTo(ThemeManager.AppTheme.NORD_LIGHT);
    }

    @Test
    void unknownThemeIdentifierIsNotMapped() {
        assertThat(ThemeManager.AppTheme.fromId("unknown")).isNull();
        assertThat(ThemeManager.AppTheme.fromId(null)).isNull();
    }

    @Test
    void missingThemeFileUsesPrimerLight() {
        assertThat(ThemeManager.getSavedTheme()).isEqualTo(ThemeManager.AppTheme.PRIMER_LIGHT);
    }

    @Test
    void savedThemeIsReadFromFile() throws Exception {
        Files.createDirectories(themeFile.getParent());
        Files.writeString(themeFile, "cupertino-dark\n", StandardCharsets.UTF_8);

        assertThat(ThemeManager.getSavedTheme()).isEqualTo(ThemeManager.AppTheme.CUPERTINO_DARK);
    }

    @Test
    void unknownSavedThemeFallsBackToPrimerLight() throws Exception {
        Files.createDirectories(themeFile.getParent());
        Files.writeString(themeFile, "does-not-exist\n", StandardCharsets.UTF_8);

        assertThat(ThemeManager.getSavedTheme()).isEqualTo(ThemeManager.AppTheme.PRIMER_LIGHT);
    }

    @Test
    void saveThemeWritesThemeIdentifierToFile() throws Exception {
        ThemeManager.saveTheme(ThemeManager.AppTheme.NORD_DARK);

        assertThat(themeFile).exists();
        assertThat(Files.readString(themeFile, StandardCharsets.UTF_8).trim())
                .isEqualTo("nord-dark");
    }
}
