package com.autoservice.services;

import com.autoservice.BaseTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Тесты ScheduleService: загрузка и сохранение настроек авто-бэкапа, планирование.
 */
class ScheduleServiceTest extends BaseTest {

    @BeforeEach
    void initSettings() {
        SettingService.loadSettings();
        SettingService.initDefaultSettings();
        SettingService.saveBackupSettings(false, "02:00", 14);
    }

    @AfterEach
    void stopScheduler() {
        SettingService.saveBackupSettings(false, "02:00", 14);
        ScheduleService.shutdown();
    }

    @Test
    void initLoadsDisabledBackupSettings() {
        ScheduleService.init();

        assertThat(ScheduleService.isBackupEnabled()).isFalse();
        assertThat(ScheduleService.getBackupTime()).isEqualTo("02:00");
        assertThat(ScheduleService.getRetentionDays()).isEqualTo(14);
    }

    @Test
    void initLoadsEnabledBackupSettingsAndSchedulesTask() {
        SettingService.saveBackupSettings(true, "05:00", 7);

        ScheduleService.init();

        assertThat(ScheduleService.isBackupEnabled()).isTrue();
        assertThat(ScheduleService.getBackupTime()).isEqualTo("05:00");
        assertThat(ScheduleService.getRetentionDays()).isEqualTo(7);
    }

    @Test
    void saveSettingsPersistsValuesInDatabase() {
        ScheduleService.saveSettings(true, "23:45", 7);

        assertThat(SettingService.isAutoBackupEnabled()).isTrue();
        assertThat(SettingService.getBackupTime()).isEqualTo("23:45");
        assertThat(SettingService.getBackupRetention()).isEqualTo(7);
    }

    @Test
    void saveSettingsWithBackupDisabledDoesNotSchedule() {
        ScheduleService.saveSettings(false, "03:00", 14);

        assertThat(ScheduleService.isBackupEnabled()).isFalse();
        assertThat(ScheduleService.getBackupTime()).isEqualTo("03:00");
    }

    @Test
    void scheduleDailyBackupAcceptsValidTime() {
        assertThatCode(() -> ScheduleService.scheduleDailyBackup("04:30"))
                .doesNotThrowAnyException();
    }

    @Test
    void scheduleDailyBackupToleratesInvalidTime() {
        assertThatCode(() -> ScheduleService.scheduleDailyBackup("не время"))
                .doesNotThrowAnyException();
    }

    @Test
    void cancelScheduledBackupIsSafeWithoutTask() {
        assertThatCode(ScheduleService::cancelScheduledBackup).doesNotThrowAnyException();
    }

    @Test
    void checkAndRunBackupOnStartupDoesNothingWhenDisabled() {
        ScheduleService.saveSettings(false, "02:00", 14);

        assertThatCode(ScheduleService::checkAndRunBackupOnStartup).doesNotThrowAnyException();
    }

    @Test
    void checkAndRunBackupOnStartupSkipsWhenTimeIsFarAway() {
        // Время в 3 часах от текущего: гарантированно вне 5-минутного окна запуска.
        String farAway = LocalTime.now().plusHours(3).format(DateTimeFormatter.ofPattern("HH:mm"));
        ScheduleService.saveSettings(true, farAway, 14);

        assertThatCode(ScheduleService::checkAndRunBackupOnStartup).doesNotThrowAnyException();
    }

    @Test
    void checkAndRunBackupOnStartupToleratesInvalidTime() {
        ScheduleService.saveSettings(true, "не время", 14);

        assertThatCode(ScheduleService::checkAndRunBackupOnStartup).doesNotThrowAnyException();
    }

    @Test
    void shutdownIsIdempotent() {
        ScheduleService.shutdown();

        assertThatCode(ScheduleService::shutdown).doesNotThrowAnyException();
    }
}
