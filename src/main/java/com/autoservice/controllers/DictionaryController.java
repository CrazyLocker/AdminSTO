package com.autoservice.controllers;

import com.autoservice.DataStore;
import com.autoservice.Service;
import com.autoservice.SparePart;
import com.autoservice.dialogs.StockIncomeDialog;
import javafx.collections.FXCollections;
import javafx.scene.control.TableView;

public class DictionaryController {
    private static TableView<Service> servicesTable;
    private static TableView<SparePart> sparePartsTable;
    private static TableView<SparePart> stockTable;

    public static void setServicesTable(TableView<Service> table) {
        servicesTable = table;
    }

    public static void setSparePartsTable(TableView<SparePart> table) {
        sparePartsTable = table;
    }

    public static void setStockTable(TableView<SparePart> table) {
        stockTable = table;
    }

    public static void refreshAll() {
        refreshServices();
        refreshSpareParts();
        refreshStock();
    }

    public static void refreshServices() {
        if (servicesTable != null) {
            servicesTable.setItems(FXCollections.observableArrayList(DataStore.getServices()));
        }
    }

    public static void refreshSpareParts() {
        if (sparePartsTable != null) {
            sparePartsTable.setItems(FXCollections.observableArrayList(DataStore.getSpareParts()));
        }
    }

    public static void refreshStock() {
        if (stockTable != null) {
            stockTable.setItems(FXCollections.observableArrayList(DataStore.getSpareParts()));
        }
    }

    public static void addService(Service service) {
        DataStore.addService(service);
        refreshServices();
    }

    public static void removeService(Service service) {
        DataStore.removeService(service);
        refreshServices();
    }

    public static void addSparePart(SparePart part) {
        DataStore.addSparePart(part);
        refreshSpareParts();
        refreshStock();
    }

    public static void removeSparePart(SparePart part) {
        DataStore.removeSparePart(part);
        refreshSpareParts();
        refreshStock();
    }

    public static void incomeSparePart(SparePart part, int amount) {
        int newStock = part.getStock() + amount;
        DataStore.updateSparePartStock(part, newStock);
        refreshSpareParts();
        refreshStock();
    }

    public static void showStockIncome(SparePart part) {
        StockIncomeDialog.show(part);
    }
}