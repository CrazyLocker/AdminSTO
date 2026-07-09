package com.autoservice.controllers;

import com.autoservice.DataStore;
import com.autoservice.Service;
import com.autoservice.SparePart;
import com.autoservice.model.ServiceSparePart;
import com.autoservice.model.ServiceSparePartsList;
import com.autoservice.model.ServiceSparePartsListItem;
import com.autoservice.model.ToPart;
import com.autoservice.views.ServiceSparePartsRow;
import com.autoservice.views.SettingsView;
import javafx.scene.control.TableView;

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

    // ==================== SERVICE-SPARE PART RELATIONSHIPS ====================

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

    private static void loadToRemoveParts() {
        if (toPartsTable == null) return;

        // Получаем все расходники ТО
        List<ToPart> parts = DataStore.getToPartsByCarModel("");
        toPartsTable.setItems(javafx.collections.FXCollections.observableArrayList(parts));
    }

    public static void addToPart(ToPart part) {
        DataStore.addToPart(part);
        loadToRemoveParts();
    }

    public static void updateToPart(ToPart part) {
        DataStore.updateToPart(part);
        loadToRemoveParts();
    }

    public static void deleteToPart(ToPart part) {
        DataStore.deleteToPart(part);
        loadToRemoveParts();
    }

    // ==================== SETTINGS ====================

    public static void showSettings() {
        SettingsView.showSettingsWindow();
    }
}
