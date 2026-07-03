package com.autoservice.controllers;

import com.autoservice.DataStore;
import com.autoservice.SparePart;
import com.autoservice.Service;
import javafx.scene.control.TableView;

import java.util.List;

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
            servicesTable.setItems(javafx.collections.FXCollections.observableArrayList(DataStore.getServices()));
        }
    }

    public static void refreshSpareParts() {
        if (sparePartsTable != null) {
            sparePartsTable.setItems(javafx.collections.FXCollections.observableArrayList(DataStore.getSpareParts()));
        }
    }

    public static void refreshStock() {
        if (stockTable != null) {
            stockTable.setItems(javafx.collections.FXCollections.observableArrayList(DataStore.getSpareParts()));
        }
    }

    public static void addSparePart(SparePart part) {
        DataStore.addSparePart(part);
        refreshAll();
    }

    public static void removeSpareParts(List<SparePart> parts) {
        for (SparePart part : parts) {
            DataStore.removeSparePart(part);
        }
        refreshAll();
    }

    public static void addService(Service service) {
        DataStore.addService(service);
        refreshServices();
    }

    public static void removeService(Service service) {
        DataStore.removeService(service);
        refreshServices();
    }
}