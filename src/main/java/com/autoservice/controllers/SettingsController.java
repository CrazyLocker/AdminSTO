package com.autoservice.controllers;

import com.autoservice.DataStore;
import com.autoservice.Service;
import com.autoservice.SparePart;
import com.autoservice.model.ServiceSparePart;
import com.autoservice.model.ServiceSparePartsList;
import com.autoservice.model.ServiceSparePartsListItem;
import com.autoservice.model.ServicePart;
import com.autoservice.model.ToPart;
import com.autoservice.services.BackupService;
import com.autoservice.services.ScheduleService;
import com.autoservice.services.SettingService;
import com.autoservice.utils.LoadingIndicator;
import com.autoservice.views.ServiceSparePartsRow;
import com.autoservice.views.ToPartsRow;
import com.autoservice.views.SettingsView;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Контроллер для управления настройками приложения.
 */
public class SettingsController {

    private static TableView<ServiceSparePart> serviceSparePartsTable;
    private static TableView<ServiceSparePartsList> serviceSparePartsListsTable;
    private static TableView<ToPart> toPartsTable;
    private static TableView<ServiceSparePartsRow> serviceSparePartsRowTable;
    private static TableView<ToPartsRow> toPartsRowTable;
    private static TableView<ServicePart> servicePartsTable;

    // ==================== SERVICE-PART RELATIONSHIPS (NEW STRUCTURE) ====================

    public static void setServiceSparePartsTable(TableView<ServiceSparePart> table) {
        serviceSparePartsTable = table;
        loadServiceSpareParts();
    }

    public static void setServiceSparePartsListsTable(TableView<ServiceSparePartsList> table) {
        serviceSparePartsListsTable = table;
        loadServiceSparePartsLists();
    }

    public static void setServiceSparePartsRowTable(TableView<ServiceSparePartsRow> table) {
        serviceSparePartsRowTable = table;
        loadServiceSparePartsRows();
    }

    public static void setServicePartsTable(TableView<ServicePart> table) {
        servicePartsTable = table;
        loadServiceParts();
    }

    public static TableView<ServiceSparePartsRow> getServiceSparePartsRowTable() {
        return serviceSparePartsRowTable;
    }

    public static void loadServiceSpareParts() {
        if (serviceSparePartsTable == null) return;

        // Получаем все связи
        List<ServiceSparePart> relations = DataStore.getServiceSparePartsByServiceId(-1);
        serviceSparePartsTable.setItems(javafx.collections.FXCollections.observableArrayList(relations));
    }

    public static void loadServiceSparePartsLists() {
        if (serviceSparePartsListsTable == null) return;

        // Получаем все списки
        List<ServiceSparePartsList> lists = DataStore.getServiceSparePartsListsByServiceId(-1);
        
        // Загружаем элементы для каждого списка (только если еще не загружены)
        for (ServiceSparePartsList list : lists) {
            if (list.getItems() == null) {
                List<ServiceSparePartsListItem> items = DataStore.getServiceSparePartsListItems(list.getId());
                list.setItems(items);
            }
        }
        
        // Создаем новый observable список для обновления таблицы
        serviceSparePartsListsTable.setItems(javafx.collections.FXCollections.observableArrayList(lists));
    }

    /**
     * Загружает связи услуг и запчастей в таблицу с одной строкой на услугу.
     * Запчасти объединяются в одну строку через запятую.
     */
    public static void loadServiceSparePartsRows() {
        if (serviceSparePartsRowTable == null) return;

        // Получаем все связи
        List<ServiceSparePart> relations = DataStore.getServiceSparePartsByServiceId(-1);

        // Группируем связи по услугам
        Map<Service, List<ServiceSparePart>> relationsByService = new HashMap<>();
        for (ServiceSparePart relation : relations) {
            Service service = DataStore.getServices().stream()
                    .filter(s -> s.getId() == relation.getServiceId())
                    .findFirst()
                    .orElse(null);
            if (service != null) {
                relationsByService.computeIfAbsent(service, k -> new ArrayList<>()).add(relation);
            }
        }

        // Создаем строки для таблицы
        List<ServiceSparePartsRow> rows = new ArrayList<>();
        for (Map.Entry<Service, List<ServiceSparePart>> entry : relationsByService.entrySet()) {
            Service service = entry.getKey();
            List<ServiceSparePart> serviceRelations = entry.getValue();

            // Формируем список запчастей через запятую
            StringBuilder sparePartsBuilder = new StringBuilder();
            StringBuilder quantityBuilder = new StringBuilder();

            for (int i = 0; i < serviceRelations.size(); i++) {
                ServiceSparePart relation = serviceRelations.get(i);
                SparePart part = DataStore.getSpareParts().stream()
                        .filter(s -> s.getId() == relation.getSparePartId())
                        .findFirst()
                        .orElse(null);

                if (part != null) {
                    if (i > 0) {
                        sparePartsBuilder.append(", ");
                        quantityBuilder.append(", ");
                    }
                    sparePartsBuilder.append(part.getName());
                    quantityBuilder.append(relation.getQuantity()).append(" ").append(relation.getUnitType());
                }
            }

            ServiceSparePartsRow row = new ServiceSparePartsRow(
                    service,
                    sparePartsBuilder.toString(),
                    quantityBuilder.toString()
            );
            rows.add(row);
        }

        // Обновляем таблицу
        serviceSparePartsRowTable.setItems(javafx.collections.FXCollections.observableArrayList(rows));
    }

    public static void addServiceSparePart(ServiceSparePart relation) {
        DataStore.addServiceSparePart(relation);
        loadServiceSparePartsRows();
    }

    public static void deleteServiceSparePart(ServiceSparePart relation) {
        DataStore.deleteServiceSparePart(relation);
        loadServiceSparePartsRows();
    }

    public static void addServiceSparePartsList(ServiceSparePartsList list) {
        DataStore.addServiceSparePartsList(list);
        loadServiceSparePartsLists();
    }

    public static void deleteServiceSparePartsList(ServiceSparePartsList list) {
        DataStore.deleteServiceSparePartsList(list);
        loadServiceSparePartsLists();
    }

    // ==================== TO PARTS ====================

    public static void setToPartsTable(TableView<ToPart> table) {
        toPartsTable = table;
        loadToRemoveParts();
    }

    public static void setToPartsRowTable(TableView<ToPartsRow> table) {
        toPartsRowTable = table;
        loadToRemovePartsRows();
    }

    private static void loadToRemoveParts() {
        if (toPartsTable == null) return;

        // Получаем все расходники ТО
        List<ToPart> parts = DataStore.getToPartsByCarModel("");
        toPartsTable.setItems(javafx.collections.FXCollections.observableArrayList(parts));
    }

    /**
     * Загружает расходники ТО в таблицу с одной строкой на модель авто.
     * Запчасти объединяются в одну строку через запятую.
     */
    private static void loadToRemovePartsRows() {
        if (toPartsRowTable == null) return;

        // Получаем все расходники
        List<ToPart> parts = DataStore.getToPartsByCarModel("");

        // Группируем расходники по моделям авто
        java.util.Map<String, List<ToPart>> partsByModel = new java.util.HashMap<>();
        for (ToPart part : parts) {
            String carModel = part.getCarModel();
            partsByModel.computeIfAbsent(carModel, k -> new java.util.ArrayList<>()).add(part);
        }

        // Создаем строки для таблицы
        java.util.List<ToPartsRow> rows = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, List<ToPart>> entry : partsByModel.entrySet()) {
            String carModel = entry.getKey();
            List<ToPart> modelParts = entry.getValue();

            // Формируем список запчастей через запятую
            StringBuilder sparePartsBuilder = new StringBuilder();
            StringBuilder quantitiesBuilder = new StringBuilder();
            StringBuilder notesBuilder = new StringBuilder();
            ToPart firstPart = modelParts.get(0); // Используем первую запись как основную

            for (int i = 0; i < modelParts.size(); i++) {
                ToPart part = modelParts.get(i);
                SparePart sparePart = DataStore.getSpareParts().stream()
                        .filter(s -> s.getId() == part.getSparePartId())
                        .findFirst()
                        .orElse(null);

                if (sparePart != null) {
                    if (i > 0) {
                        sparePartsBuilder.append(", ");
                        quantitiesBuilder.append(", ");
                    }
                    sparePartsBuilder.append(sparePart.getName());
                    quantitiesBuilder.append(part.getQuantity()).append(" ").append(part.getUnitType());
                }
            }

            ToPartsRow row = new ToPartsRow(
                    firstPart,
                    carModel,
                    sparePartsBuilder.toString(),
                    firstPart.getQuantity(),
                    firstPart.getUnitType(),
                    firstPart.getNote() != null ? firstPart.getNote() : ""
            );
            rows.add(row);
        }

        // Обновляем таблицу
        toPartsRowTable.setItems(javafx.collections.FXCollections.observableArrayList(rows));
    }

    public static void addToPart(ToPart part) {
        DataStore.addToPart(part);
        loadToRemoveParts();
        loadToRemovePartsRows();
    }

    public static void updateToPart(ToPart part) {
        DataStore.updateToPart(part);
        loadToRemoveParts();
        loadToRemovePartsRows();
    }

    public static void deleteToPart(ToPart part) {
        DataStore.deleteToPart(part);
        loadToRemoveParts();
        loadToRemovePartsRows();
    }

    // ==================== SERVICE-PART RELATIONSHIPS (NEW STRUCTURE) ====================

    public static List<ServicePart> getAllServiceParts() {
        return DataStore.getAllServiceParts();
    }

    public static void addServicePart(ServicePart part) {
        DataStore.addServicePart(part);
        loadServiceParts();
    }

    public static void updateServicePart(ServicePart part) {
        DataStore.updateServicePart(part);
        loadServiceParts();
    }

    public static void deleteServicePart(ServicePart part) {
        DataStore.deleteServicePart(part);
        loadServiceParts();
    }

    public static void loadServiceParts() {
        if (servicePartsTable == null) return;
        List<ServicePart> parts = DataStore.getAllServiceParts();
        servicePartsTable.setItems(javafx.collections.FXCollections.observableArrayList(parts));
    }

    // ==================== SERVICE-SPARE PART DUPLICATES ====================

    public static void deleteDuplicateServiceSpareParts() {
        DataStore.deleteDuplicateServiceSpareParts();
        loadServiceSparePartsRows();
        loadServiceSpareParts();
    }

    // ==================== SETTINGS ====================

    public static void showSettings() {
        SettingsView.showSettingsWindow();
    }

    // ==================== BACKUP SETTINGS ====================

    public static void loadBackupSettings() {
        // Настройки загружаются автоматически в ScheduleService
    }

    public static void saveBackupSettings(boolean enabled, String time, int retention) {
        // Сохраняем настройки в базу данных
        SettingService.setAutoAddSparePartsEnabled(enabled);
        SettingService.setSparePartConfirmationRequired(false); // для совместимости
        
        // Сохраняем время и количество копий в настройки
        SettingService.saveBackupSettings(enabled, time, retention);
        
        ScheduleService.saveSettings(enabled, time, retention);
    }

    public static List<String> listAvailableBackups() {
        return BackupService.getAvailableBackups();
    }

    public static boolean performManualBackup() {
        String path = BackupService.createBackup();
        return path != null;
    }

    public static boolean performRestoreBackup(String backupPath) {
        return BackupService.restoreBackup(backupPath);
    }

    public static boolean deleteBackup(String backupPath) {
        try {
            File file = new File(backupPath);
            if (file.exists()) {
                return file.delete();
            }
            return false;
        } catch (Exception e) {
            com.autoservice.utils.ExceptionHandler.handleServiceError(e, "Удаление бэкапа");
            return false;
        }
    }

    public static String getLastBackupTime() {
        return BackupService.getLastBackupTime();
    }

    public static int getBackupCount() {
        return BackupService.getBackupCount();
    }
}
