package com.autoservice.services;

import com.autoservice.DataStore;
import com.autoservice.model.Setting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Сервис для управления настройками приложения.
 */
public class SettingService {

    private static Map<String, Setting> settingsCache = null;

    /**
     * Получить все настройки из базы данных и кэшировать их.
     */
    private static void loadSettings() {
        if (settingsCache == null) {
            settingsCache = new HashMap<>();
            List<Setting> settings = DataStore.getAllSettings();
            for (Setting setting : settings) {
                settingsCache.put(setting.getKey(), setting);
            }
        }
    }

    /**
     * Получить настройку по ключу.
     * @param key ключ настройки
     * @return объект Setting или null
     */
    public static Setting getSetting(String key) {
        if (settingsCache == null) {
            loadSettings();
        }
        return settingsCache.get(key);
    }

    /**
     * Получить значение настройки по ключу.
     * @param key ключ настройки
     * @return значение настройки или null
     */
    public static String getSettingValue(String key) {
        Setting setting = getSetting(key);
        return setting != null ? setting.getValue() : null;
    }

    /**
     * Установить значение настройки.
     * @param key ключ настройки
     * @param value новое значение
     */
    public static void setSettingValue(String key, String value) {
        Setting setting = getSetting(key);
        if (setting != null) {
            setting.setValue(value);
            DataStore.updateSetting(setting);
        } else {
            setting = new Setting(key, value, "");
            DataStore.addSetting(setting);
            loadSettings();
        }
    }

    /**
     * Получить все настройки.
     * @return список всех настроек
     */
    public static List<Setting> getAllSettings() {
        if (settingsCache == null) {
            loadSettings();
        }
        return DataStore.getAllSettings();
    }

    /**
     * Удалить настройку по ключу.
     * @param key ключ настройки
     */
    public static void deleteSetting(String key) {
        Setting setting = getSetting(key);
        if (setting != null) {
            DataStore.deleteSetting(setting);
            loadSettings();
        }
    }

    // ==================== ЧАСТО ИСПОЛЬЗУЕМЫЕ НАСТРОЙКИ ====================

    /**
     * Получить настройку: автоматически добавлять запчасти при выборе услуги.
     * @return true, если автоматическое добавление включено
     */
    public static boolean isAutoAddSparePartsEnabled() {
        String value = getSettingValue("auto_add_spare_parts");
        return value != null && value.equalsIgnoreCase("true");
    }

    /**
     * Включить/выключить автоматическое добавление запчастей.
     * @param enabled true для включения
     */
    public static void setAutoAddSparePartsEnabled(boolean enabled) {
        setSettingValue("auto_add_spare_parts", Boolean.toString(enabled));
    }

    /**
     * Получить настройку: требовать подтверждение при добавлении запчастей.
     * @return true, если требуется подтверждение
     */
    public static boolean isSparePartConfirmationRequired() {
        String value = getSettingValue("spare_part_confirmation");
        return value != null && value.equalsIgnoreCase("true");
    }

    /**
     * Включить/выключить подтверждение при добавлении запчастей.
     * @param required true, если требуется подтверждение
     */
    public static void setSparePartConfirmationRequired(boolean required) {
        setSettingValue("spare_part_confirmation", Boolean.toString(required));
    }

    /**
     * Получить настройку: основной язык интерфейса.
     * @return "ru" или "en"
     */
    public static String getInterfaceLanguage() {
        String value = getSettingValue("interface_language");
        return value != null ? value : "ru";
    }

    /**
     * Установить язык интерфейса.
     * @param language "ru" или "en"
     */
    public static void setInterfaceLanguage(String language) {
        setSettingValue("interface_language", language);
    }

    /**
     * Получить настройку: путь к папке с отчетами.
     * @return путь к папке
     */
    public static String getReportsPath() {
        String value = getSettingValue("reports_path");
        return value != null ? value : "reports";
    }

    /**
     * Установить путь к папке с отчетами.
     * @param path путь к папке
     */
    public static void setReportsPath(String path) {
        setSettingValue("reports_path", path);
    }

    /**
     * Получить настройку: формат даты.
     * @return формат даты (по умолчанию "dd/MM/yyyy")
     */
    public static String getDateFormat() {
        String value = getSettingValue("date_format");
        return value != null ? value : "dd/MM/yyyy";
    }

    /**
     * Установить формат даты.
     * @param format формат даты
     */
    public static void setDateFormat(String format) {
        setSettingValue("date_format", format);
    }

    // ==================== НАСТРОЙКИ РЕЗЕРВНОГО КОПИРОВАНИЯ ====================

    /**
     * Получить настройку: включен ли авто-бэкап.
     * @return true, если авто-бэкап включен
     */
    public static boolean isAutoBackupEnabled() {
        String value = getSettingValue("backup_enabled");
        return value != null && value.equalsIgnoreCase("true");
    }

    /**
     * Включить/выключить авто-бэкап.
     * @param enabled true для включения
     */
    public static void setAutoBackupEnabled(boolean enabled) {
        setSettingValue("backup_enabled", Boolean.toString(enabled));
    }

    /**
     * Получить время ежедневного бэкапа.
     * @return время в формате "HH:mm" (например, "02:00")
     */
    public static String getBackupTime() {
        return getSettingValue("backup_time");
    }

    /**
     * Установить время ежедневного бэкапа.
     * @param time время в формате "HH:mm"
     */
    public static void setBackupTime(String time) {
        setSettingValue("backup_time", time);
    }

    /**
     * Получить количество хранимых бэкапов.
     * @return количество копий (по умолчанию 14)
     */
    public static int getBackupRetention() {
        String value = getSettingValue("backup_retention");
        try {
            return value != null ? Integer.parseInt(value) : 14;
        } catch (NumberFormatException e) {
            return 14;
        }
    }

    /**
     * Установить количество хранимых бэкапов.
     * @param retention количество копий (7-14)
     */
    public static void setBackupRetention(int retention) {
        setSettingValue("backup_retention", Integer.toString(retention));
    }

    /**
     * Сохранить все настройки бэкапа.
     * @param enabled включен ли авто-бэкап
     * @param time время бэкапа
     * @param retention количество хранимых копий
     */
    public static void saveBackupSettings(boolean enabled, String time, int retention) {
        setAutoBackupEnabled(enabled);
        setBackupTime(time);
        setBackupRetention(retention);
    }
}
