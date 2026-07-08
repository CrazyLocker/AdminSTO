package com.autoservice.controllers;

import com.autoservice.Appointment;
import com.autoservice.DataStore;
import com.autoservice.SparePart;
import com.autoservice.WorkOrder;
import com.autoservice.dialogs.CreateOrderDialog;
import com.autoservice.dialogs.EditOrderDialog;
import com.autoservice.dialogs.OrderDetailsDialog;
import com.autoservice.views.AppointmentView;
import com.autoservice.views.DashboardView;
import com.autoservice.views.OrderView;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableView;

public class OrderController {
    private static TableView<WorkOrder> orderTable;

    public static void setTable(TableView<WorkOrder> table) {
        orderTable = table;
    }

    public static void refreshTable() {
        OrderView.refreshOrderList();
        AppointmentView.refresh();
        DashboardView.refresh();
    }

    public static void createOrder() {
        if (DataStore.getClients().isEmpty()) {
            showAlert("Сначала добавьте хотя бы одного клиента");
            return;
        }
        CreateOrderDialog.show();
        refreshTable();
        DashboardView.refresh();
    }

    public static void editOrder(WorkOrder order) {
        if (order == null) {
            showAlert("Выберите заказ для редактирования");
            return;
        }
        if ("Закрыт".equals(order.getStatus())) {
            showAlert("Нельзя редактировать закрытый заказ");
            return;
        }
        EditOrderDialog.show(order);
        refreshTable();
    }

    public static void viewOrder(WorkOrder order) {
        if (order == null) {
            showAlert("Выберите заказ для просмотра");
            return;
        }
        OrderDetailsDialog.show(order);
    }

    public static void changeOrderStatus(WorkOrder order, String newStatus) {
        if (order == null) return;
        if (newStatus != null && !newStatus.equals(order.getStatus())) {
            order.setStatus(newStatus);
            DataStore.updateOrder(order);
            refreshTable();
        }
    }

    public static void deleteOrder(WorkOrder order) {
        if (order == null) {
            showAlert("Выберите заказ для удаления");
            return;
        }
        if ("Закрыт".equals(order.getStatus())) {
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
                returnSparePartsToStock(order);
                deleteAssociatedAppointment(order);
                DataStore.deleteOrder(order);
                refreshTable();
                showAlert("Заказ " + order.getId() + " удалён", Alert.AlertType.INFORMATION);
            }
        });
    }

    private static void returnSparePartsToStock(WorkOrder order) {
        for (int i = 0; i < order.getSpareParts().size(); i++) {
            SparePart part = order.getSpareParts().get(i);
            double quantity = order.getSparePartQuantities().get(i);

            // Используем double для расчёта нового остатка
            double newStock = part.getStock() + quantity;
            DataStore.updateSparePartStock(part, newStock);
            System.out.println("Возвращено на склад: " + part.getName() + " +" + quantity);
        }
    }

    private static void deleteAssociatedAppointment(WorkOrder order) {
        String orderId = order.getId();
        for (Appointment a : DataStore.getAppointments()) {
            if (orderId != null && orderId.equals(a.getOrderId())) {
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