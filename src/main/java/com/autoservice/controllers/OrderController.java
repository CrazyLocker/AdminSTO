package com.autoservice.controllers;

import com.autoservice.DataStore;
import com.autoservice.WorkOrder;
import com.autoservice.dialogs.CreateOrderDialog;
import com.autoservice.dialogs.EditOrderDialog;
import com.autoservice.dialogs.OrderDetailsDialog;
import com.autoservice.views.OrderView;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableView;

public class OrderController {
    private static TableView<WorkOrder> orderTable;

    public static void setTable(TableView<WorkOrder> table) {
        orderTable = table;
    }

    public static void refreshTable() {
        if (orderTable != null) {
            orderTable.setItems(FXCollections.observableArrayList(DataStore.getOrders()));
            orderTable.refresh();
        }
        OrderView.refreshOrderList();
    }

    public static void createOrder() {
        if (DataStore.getClients().isEmpty()) {
            showAlert("Сначала добавьте хотя бы одного клиента");
            return;
        }
        CreateOrderDialog.show();
    }

    public static void editOrder(WorkOrder order) {
        if (order.getStatus().equals(WorkOrder.STATUS_CLOSED)) {
            showAlert("Нельзя редактировать закрытый заказ");
            return;
        }
        EditOrderDialog.show(order);
    }

    public static void viewOrder(WorkOrder order) {
        OrderDetailsDialog.show(order);
    }

    public static void changeOrderStatus(WorkOrder order, String newStatus) {
        if (newStatus != null && !newStatus.equals(order.getStatus())) {
            order.setStatus(newStatus);
            DataStore.updateOrder(order);
            refreshTable();
        }
    }

    public static void deleteOrder(WorkOrder order) {
        if (order.getStatus().equals(WorkOrder.STATUS_CLOSED)) {
            showAlert("Нельзя удалить закрытый заказ");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Удалить заказ " + order.getId() + "?\nЭто действие нельзя отменить.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Подтверждение удаления");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                DataStore.deleteOrder(order);
                refreshTable();
                showAlert("Заказ " + order.getId() + " удалён", Alert.AlertType.INFORMATION);
            }
        });
    }

    private static void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.showAndWait();
    }

    private static void showAlert(String msg, Alert.AlertType type) {
        Alert alert = new Alert(type, msg, ButtonType.OK);
        alert.showAndWait();
    }
}