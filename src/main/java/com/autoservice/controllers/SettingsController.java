package com.autoservice.controllers;

import com.autoservice.DataStore;
import com.autoservice.model.ServiceSparePart;
import com.autoservice.model.ToPart;
import com.autoservice.views.SettingsView;
import javafx.scene.control.TableView;

import java.util.List;

/**
 * Контроллер для управления настройками приложения.
 */
public class SettingsController {

    private static TableView<ServiceSparePart> serviceSparePartsTable;
    private static TableView<ToPart> toPartsTable;

    // ==================== SERVICE-SPARE PART RELATIONSHIPS ====================

    public static void setServiceSparePartsTable(TableView<ServiceSparePart> table) {
        serviceSparePartsTable = table;
        loadServiceSpareParts();
    }

    public static void loadServiceSpareParts() {
        if (serviceSparePartsTable == null) return;

        // Получаем все связи
        List<ServiceSparePart> relations = DataStore.getServiceSparePartsByServiceId(-1);
        serviceSparePartsTable.setItems(javafx.collections.FXCollections.observableArrayList(relations));
    }

    public static void addServiceSparePart(ServiceSparePart relation) {
        DataStore.addServiceSparePart(relation);
        loadServiceSpareParts();
    }

    public static void deleteServiceSparePart(ServiceSparePart relation) {
        DataStore.deleteServiceSparePart(relation);
        loadServiceSpareParts();
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
