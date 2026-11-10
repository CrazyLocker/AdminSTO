package com.autoservice.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты чтения и сохранения выбранной темы в {@code config/theme.txt}.
 */
class ThemePreferencesTest {

    @TempDir
    Path tempDir;

    @Test
    void missingFileReturnsDefaultTheme() {
        Path file = tempDir.resolve("missing").resolve("theme.txt");

        assertThat(ThemePreferences.readTheme(file)).isEqualTo("primer-light");
    }

    @Test
    void emptyFileReturnsDefaultTheme() throws IOException {
        Path file = tempDir.resolve("theme.txt");
        Files.writeString(file, "   " + System.lineSeparator(), StandardCharsets.UTF_8);

        assertThat(ThemePreferences.readTheme(file)).isEqualTo("primer-light");
    }

    @Test
    void savedThemeIsReadBack() {
        Path file = tempDir.resolve("config").resolve("theme.txt");

        ThemePreferences.writeTheme(file, "nord-dark");

        assertThat(ThemePreferences.readTheme(file)).isEqualTo("nord-dark");
    }

    @Test
    void savedThemeCreatesConfigDirectory() {
        Path file = tempDir.resolve("config").resolve("theme.txt");

        ThemePreferences.writeTheme(file, "dracula");

        assertThat(file).exists();
        assertThat(Files.isDirectory(tempDir.resolve("config"))).isTrue();
    }

    @Test
    void enumStyleIdentifierIsNormalized() {
        assertThat(ThemePreferences.normalize(" PRIMER_LIGHT ")).isEqualTo("primer-light");
    }

    @Test
    void fileWithEnumStyleValueIsNormalized() {
        Path file = tempDir.resolve("theme.txt");

        ThemePreferences.writeTheme(file, "CUPERTINO_DARK");

        assertThat(ThemePreferences.readTheme(file)).isEqualTo("cupertino-dark");
    }

    @Test
    void defaultThemeFileIsConfigThemeTxt() {
        String previous = System.getProperty(ThemePreferences.THEME_FILE_PROPERTY);
        System.clearProperty(ThemePreferences.THEME_FILE_PROPERTY);
        try {
            assertThat(ThemePreferences.getThemeFile())
                    .isEqualTo(Path.of("config", "theme.txt"));
        } finally {
            if (previous != null) {
                System.setProperty(ThemePreferences.THEME_FILE_PROPERTY, previous);
            }
        }
    }

    @Test
    void themeFileCanBeOverriddenWithSystemProperty() {
        String previous = System.getProperty(ThemePreferences.THEME_FILE_PROPERTY);
        Path custom = tempDir.resolve("custom-theme.txt");
        System.setProperty(ThemePreferences.THEME_FILE_PROPERTY, custom.toString());
        try {
            assertThat(ThemePreferences.getThemeFile()).isEqualTo(custom);
        } finally {
            if (previous == null) {
                System.clearProperty(ThemePreferences.THEME_FILE_PROPERTY);
            } else {
                System.setProperty(ThemePreferences.THEME_FILE_PROPERTY, previous);
            }
        }
    }
}
