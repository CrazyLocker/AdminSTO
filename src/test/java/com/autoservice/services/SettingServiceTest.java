package com.autoservice.services;

import com.autoservice.BaseTest;
import com.autoservice.DataStore;
import com.autoservice.model.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты SettingService: настройки по умолчанию, чтение и запись значений.
 */
class SettingServiceTest extends BaseTest {

    @BeforeEach
    void initSettings() {
        SettingService.loadSettings();
        SettingService.initDefaultSettings();
    }

    @Test
    void initDefaultSettingsCreatesAllDefaults() {
        assertThat(SettingService.getAllSettings()).hasSize(9);
        assertThat(SettingService.getSettingValue("interface_language")).isEqualTo("ru");
        assertThat(SettingService.getSettingValue("date_format")).isEqualTo("dd/MM/yyyy");
        assertThat(SettingService.getSettingValue("backup_time")).isEqualTo("02:00");
        assertThat(SettingService.getSettingValue("backup_retention")).isEqualTo("14");
    }

    @Test
    void initDefaultSettingsIsIdempotent() {
        SettingService.initDefaultSettings();
        SettingService.initDefaultSettings();

        assertThat(SettingService.getAllSettings()).hasSize(9);
    }

    @Test
    void getSettingReturnsNullForUnknownKey() {
        assertThat(SettingService.getSetting("unknown_key")).isNull();
        assertThat(SettingService.getSettingValue("unknown_key")).isNull();
    }

    @Test
    void setSettingValueUpdatesExistingSetting() {
        SettingService.setSettingValue("interface_language", "en");

        assertThat(SettingService.getSettingValue("interface_language")).isEqualTo("en");
        assertThat(SettingService.getAllSettings()).hasSize(9);
    }

    @Test
    void setSettingValueCreatesMissingSetting() {
        SettingService.setSettingValue("custom_key", "custom_value");

        Setting setting = SettingService.getSetting("custom_key");
        assertThat(setting).isNotNull();
        assertThat(setting.getValue()).isEqualTo("custom_value");
    }

    @Test
    void deleteSettingRemovesKey() {
        SettingService.deleteSetting("date_format");

        assertThat(SettingService.getSettingValue("date_format")).isNull();
        assertThat(SettingService.getAllSettings()).hasSize(8);
    }

    @Test
    void deleteSettingIgnoresUnknownKey() {
        SettingService.deleteSetting("unknown_key");

        assertThat(SettingService.getAllSettings()).hasSize(9);
    }

    @Test
    void loadSettingsReadsSettingsFromDatabase() {
        Setting fresh = new Setting("from_db", "value", "описание");
        DataStore.addSetting(fresh);
        DataStore.load();

        SettingService.loadSettings();

        assertThat(SettingService.getSettingValue("from_db")).isEqualTo("value");
    }

    // ==================== ЧАСТО ИСПОЛЬЗУЕМЫЕ НАСТРОЙКИ ====================

    @Test
    void autoAddSparePartsFlagReflectsStoredValue() {
        assertThat(SettingService.isAutoAddSparePartsEnabled()).isTrue();

        SettingService.setAutoAddSparePartsEnabled(false);

        assertThat(SettingService.isAutoAddSparePartsEnabled()).isFalse();
    }

    @Test
    void sparePartConfirmationFlagReflectsStoredValue() {
        assertThat(SettingService.isSparePartConfirmationRequired()).isTrue();

        SettingService.setSparePartConfirmationRequired(false);

        assertThat(SettingService.isSparePartConfirmationRequired()).isFalse();
    }

    @Test
    void interfaceLanguageDefaultsToRussianAndCanBeChanged() {
        assertThat(SettingService.getInterfaceLanguage()).isEqualTo("ru");

        SettingService.setInterfaceLanguage("en");

        assertThat(SettingService.getInterfaceLanguage()).isEqualTo("en");
    }

    @Test
    void interfaceLanguageFallsBackToRussianWithoutSetting() {
        SettingService.deleteSetting("interface_language");

        assertThat(SettingService.getInterfaceLanguage()).isEqualTo("ru");
    }

    @Test
    void reportsPathDefaultsAndCanBeChanged() {
        assertThat(SettingService.getReportsPath()).isEqualTo("reports");

        SettingService.setReportsPath("D:/отчёты");

        assertThat(SettingService.getReportsPath()).isEqualTo("D:/отчёты");
    }

    @Test
    void dateFormatDefaultsAndCanBeChanged() {
        assertThat(SettingService.getDateFormat()).isEqualTo("dd/MM/yyyy");

        SettingService.setDateFormat("yyyy-MM-dd");

        assertThat(SettingService.getDateFormat()).isEqualTo("yyyy-MM-dd");
    }

    // ==================== БЭКАПЫ ====================

    @Test
    void autoBackupFlagReflectsStoredValue() {
        assertThat(SettingService.isAutoBackupEnabled()).isTrue();

        SettingService.setAutoBackupEnabled(false);

        assertThat(SettingService.isAutoBackupEnabled()).isFalse();
    }

    @Test
    void backupTimeDefaultsAndCanBeChanged() {
        assertThat(SettingService.getBackupTime()).isEqualTo("02:00");

        SettingService.setBackupTime("23:30");

        assertThat(SettingService.getBackupTime()).isEqualTo("23:30");
    }

    @Test
    void backupRetentionDefaultsAndCanBeChanged() {
        assertThat(SettingService.getBackupRetention()).isEqualTo(14);

        SettingService.setBackupRetention(7);

        assertThat(SettingService.getBackupRetention()).isEqualTo(7);
    }

    @Test
    void backupRetentionFallsBackToDefaultForNonNumericValue() {
        SettingService.setSettingValue("backup_retention", "не число");

        assertThat(SettingService.getBackupRetention()).isEqualTo(14);
    }

    @Test
    void backupRetentionFallsBackToDefaultWithoutSetting() {
        SettingService.deleteSetting("backup_retention");

        assertThat(SettingService.getBackupRetention()).isEqualTo(14);
    }

    @Test
    void saveBackupSettingsWritesAllThreeValues() {
        SettingService.saveBackupSettings(false, "04:15", 10);

        assertThat(SettingService.isAutoBackupEnabled()).isFalse();
        assertThat(SettingService.getBackupTime()).isEqualTo("04:15");
        assertThat(SettingService.getBackupRetention()).isEqualTo(10);
    }
}
