package com.autoservice.controllers;

import com.autoservice.*;
import com.autoservice.dialogs.CreateOrderDialog;
import com.autoservice.dialogs.EditOrderDialog;
import com.autoservice.dialogs.OrderDetailsDialog;
import com.autoservice.views.AppointmentView;
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
        AppointmentView.refresh();
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
                "Удалить заказ " + order.getId() + "?\n\n" +
                        "Будут выполнены следующие действия:\n" +
                        "• Удаление заказа\n" +
                        "• Возврат запчастей на склад\n" +
                        "• Удаление связанной записи в календаре\n\n" +
                        "Это действие нельзя отменить.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Подтверждение удаления");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                // 1. Возвращаем запчасти на склад
                returnSparePartsToStock(order);

                // 2. Удаляем связанную запись в календаре
                deleteAssociatedAppointment(order);

                // 3. Удаляем заказ
                DataStore.deleteOrder(order);
                refreshTable();
                showAlert("Заказ " + order.getId() + " удалён", Alert.AlertType.INFORMATION);
            }
        });
    }

    private static void returnSparePartsToStock(WorkOrder order) {
        for (int i = 0; i < order.getSpareParts().size(); i++) {
            SparePart part = order.getSpareParts().get(i);
            int quantity = order.getSparePartQuantities().get(i);
            int newStock = part.getStock() + quantity;
            DataStore.updateSparePartStock(part, newStock);
            System.out.println("Возвращено на склад: " + part.getName() + " +" + quantity);
        }
    }

    private static void deleteAssociatedAppointment(WorkOrder order) {
        String orderId = order.getId();
        for (Appointment a : DataStore.getAppointments()) {
            if (orderId.equals(a.getOrderId())) {
                DataStore.deleteAppointment(a.getId());
                System.out.println("Удалена запись в календаре для заказа " + orderId);
                break;
            }
        }
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