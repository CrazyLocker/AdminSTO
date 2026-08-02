package com.autoservice.controllers;

import com.autoservice.DataStore;
import com.autoservice.Service;
import com.autoservice.views.ServicePanel;
import javafx.scene.control.TableView;

public class ServicePanelController {
    private static TableView<Service> table;

    public static void setTable(TableView<Service> table) {
        ServicePanelController.table = table;
    }

    public static void refreshTable() {
        // Не вызываем setItems напрямую — это ломает SortedList
        ServicePanel.refreshTable();
    }

    public static void addService(Service service) {
        DataStore.addService(service);
        refreshTable();
    }

    public static void updateService(Service service) {
        DataStore.updateService(service);
        refreshTable();
    }

    public static void removeService(Service service) {
        // Удаляем все связи service_parts для этой услуги
        DataStore.deleteServicePartsByServiceId(service.getId());
        DataStore.removeService(service);
        refreshTable();
    }
}
