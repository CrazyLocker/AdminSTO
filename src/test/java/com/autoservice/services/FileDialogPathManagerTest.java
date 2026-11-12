package com.autoservice.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты FileDialogPathManager: сохранение и сброс последнего каталога диалога.
 */
class FileDialogPathManagerTest {

    private static final String DIALOG_ID = "test-file-dialog";

    private final FileDialogPathManager manager = FileDialogPathManager.getInstance();

    // Менеджер хранит состояние в config/file-paths/*.json, поэтому чистим и до, и после
    // теста: иначе файл, оставшийся от прошлого прогона, ломает ожидание «пусто».
    @BeforeEach
    @AfterEach
    void cleanUp() {
        manager.resetLastPath(DIALOG_ID);
    }

    @Test
    void getInstanceReturnsSameInstance() {
        assertThat(FileDialogPathManager.getInstance()).isSameAs(manager);
    }

    @Test
    void savedPathIsReturnedForDialog() {
        manager.saveLastPath(DIALOG_ID, "D:/экспорт");

        assertThat(manager.getLastPath(DIALOG_ID)).isEqualTo("D:/экспорт");
    }

    @Test
    void nullPathIsNotSaved() {
        manager.saveLastPath(DIALOG_ID, null);

        assertThat(manager.getLastPath(DIALOG_ID)).isNull();
    }

    @Test
    void blankPathIsNotSaved() {
        manager.saveLastPath(DIALOG_ID, "   ");

        assertThat(manager.getLastPath(DIALOG_ID)).isNull();
    }

    @Test
    void unknownDialogHasNoPath() {
        assertThat(manager.getLastPath("never-used-dialog")).isNull();
    }

    @Test
    void resetRemovesSavedPath() {
        manager.saveLastPath(DIALOG_ID, "D:/отчёты");
        assertThat(manager.getLastPath(DIALOG_ID)).isEqualTo("D:/отчёты");

        manager.resetLastPath(DIALOG_ID);

        assertThat(manager.getLastPath(DIALOG_ID)).isNull();
    }
}
