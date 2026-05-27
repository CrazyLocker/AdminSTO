package com.autoservice.dialogs;

import com.autoservice.*;
import com.autoservice.controllers.OrderController;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class EditOrderDialog {

    private static final String[] TIME_SLOTS = {
            "08:00", "08:30", "09:00", "09:30", "10:00", "10:30", "11:00", "11:30",
            "12:00", "12:30", "13:00", "13:30", "14:00", "14:30", "15:00", "15:30",
            "16:00", "16:30", "17:00", "17:30", "18:00", "18:30", "19:00", "19:30", "20:00"
    };

    private static final String[] MASTERS = {"Иван", "Петр", "Сергей", "Антон"};
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    // Поля класса для хранения временных данных
    private static List<String> tempServices = new ArrayList<>();
    private static List<Double> tempServicePrices = new ArrayList<>();
    private static List<SparePart> tempParts = new ArrayList<>();
    private static List<Integer> tempPartQuantities = new ArrayList<>();

    // Поля для UI компонентов
    private static ListView<String> servicesListView;
    private static ListView<String> partsListView;
    private static Label totalLabel;
    private static ComboBox<SparePart> partCombo;
    private static Stage currentStage;

    public static void show(WorkOrder order) {
        if (order.getStatus().equals(WorkOrder.STATUS_CLOSED)) {
            showAlert("Нельзя редактировать закрытый заказ");
            return;
        }

        // Очищаем временные списки
        tempServices.clear();
        tempServicePrices.clear();
        tempParts.clear();
        tempPartQuantities.clear();

        currentStage = new Stage();
        currentStage.setTitle("Редактирование заказа " + order.getId());
        currentStage.setMinWidth(750);
        currentStage.setMinHeight(750);
        currentStage.initModality(javafx.stage.Modality.WINDOW_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Label infoLabel = new Label("Клиент: " + order.getClient().getName() + " (" + order.getClient().getCarModel() + ", " + order.getClient().getCarNumber() + ")");
        infoLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        // ==================== ЗАПИСЬ В КАЛЕНДАРЬ ====================
        Label appointmentHeader = new Label("📅 ЗАПИСЬ В КАЛЕНДАРЬ");
        appointmentHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        // Находим существующую запись для этого заказа
        Appointment existingAppointment = null;
        for (Appointment a : DataStore.getAppointments()) {
            if (a.getOrderId() != null && a.getOrderId().equals(order.getId())) {
                existingAppointment = a;
                break;
            }
        }

        // Информация о текущей записи
        Label currentAppointmentInfo = new Label();
        currentAppointmentInfo.setStyle("-fx-text-fill: #2E7D32; -fx-font-size: 12px; -fx-padding: 5 0 5 0;");

        CheckBox hasAppointmentCheck = new CheckBox("Создать/редактировать запись в календаре");

        if (existingAppointment != null) {
            hasAppointmentCheck.setSelected(true);
            String dateStr = existingAppointment.getDate();
            String timeStr = existingAppointment.getTime();
            String master = existingAppointment.getMasterName();
            String service = existingAppointment.getServiceName();

            String formattedDate = dateStr;
            try {
                formattedDate = LocalDate.parse(dateStr).format(DATE_FORMATTER);
            } catch (Exception e) {}

            currentAppointmentInfo.setText("📌 Текущая запись: " + formattedDate + " " + timeStr + ", мастер: " + master + ", услуга: " + service);
            currentAppointmentInfo.setStyle("-fx-text-fill: #2196F3; -fx-font-size: 12px; -fx-padding: 5 0 5 0;");
        } else {
            hasAppointmentCheck.setSelected(false);
            currentAppointmentInfo.setText("⚠ Запись в календаре отсутствует");
            currentAppointmentInfo.setStyle("-fx-text-fill: #FF9800; -fx-font-size: 12px; -fx-padding: 5 0 5 0;");
        }

        DatePicker datePicker = new DatePicker();
        ComboBox<String> timeCombo = new ComboBox<>(FXCollections.observableArrayList(TIME_SLOTS));
        ComboBox<String> masterCombo = new ComboBox<>(FXCollections.observableArrayList(MASTERS));

        if (existingAppointment != null) {
            datePicker.setValue(LocalDate.parse(existingAppointment.getDate()));
            timeCombo.setValue(existingAppointment.getTime());
            masterCombo.setValue(existingAppointment.getMasterName());
        } else {
            datePicker.setValue(LocalDate.now());
            timeCombo.setPromptText("Выберите время");
            masterCombo.setPromptText("Выберите мастера");
        }

        datePicker.setDisable(!hasAppointmentCheck.isSelected());
        timeCombo.setDisable(!hasAppointmentCheck.isSelected());
        masterCombo.setDisable(!hasAppointmentCheck.isSelected());

        hasAppointmentCheck.setOnAction(e -> {
            boolean selected = hasAppointmentCheck.isSelected();
            datePicker.setDisable(!selected);
            timeCombo.setDisable(!selected);
            masterCombo.setDisable(!selected);
            if (!selected) {
                datePicker.setValue(null);
                timeCombo.setValue(null);
                masterCombo.setValue(null);
                currentAppointmentInfo.setText("⚠ Запись будет удалена");
                currentAppointmentInfo.setStyle("-fx-text-fill: #f44336; -fx-font-size: 12px; -fx-padding: 5 0 5 0;");
            } else {
                datePicker.setValue(LocalDate.now());
                currentAppointmentInfo.setText("📌 Будет создана новая запись");
                currentAppointmentInfo.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px; -fx-padding: 5 0 5 0;");
            }
        });

        HBox appointmentBox = new HBox(10,
                new Label("Дата:"), datePicker,
                new Label("Время:"), timeCombo,
                new Label("Мастер:"), masterCombo);
        appointmentBox.setAlignment(Pos.CENTER_LEFT);

        VBox appointmentSection = new VBox(5, appointmentHeader, currentAppointmentInfo, hasAppointmentCheck, appointmentBox);

        // ==================== УСЛУГИ ====================
        Label servicesHeader = new Label("📋 УСЛУГИ");
        servicesHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        servicesListView = new ListView<>();
        servicesListView.setPrefHeight(120);

        tempServices.addAll(order.getServices());
        tempServicePrices.addAll(order.getServicePrices());
        for (int i = 0; i < tempServices.size(); i++) {
            servicesListView.getItems().add((i+1) + ". " + tempServices.get(i) + " — " + tempServicePrices.get(i) + " руб.");
        }

        ComboBox<Service> serviceCombo = new ComboBox<>(FXCollections.observableArrayList(DataStore.getServices()));
        serviceCombo.setPromptText("Выберите услугу");
        serviceCombo.setPrefWidth(350);
        TextField servicePriceField = new TextField();
        servicePriceField.setEditable(false);
        servicePriceField.setPrefWidth(100);

        serviceCombo.setOnAction(e -> {
            Service selected = serviceCombo.getValue();
            if (selected != null) {
                servicePriceField.setText(String.valueOf(selected.getPrice()));
            }
        });

        Button addServiceBtn = new Button("➕ Добавить услугу");
        addServiceBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        HBox serviceAddBox = new HBox(10, serviceCombo, servicePriceField, addServiceBtn);

        // ==================== ЗАПЧАСТИ ====================
        Label partsHeader = new Label("🔧 ЗАПЧАСТИ");
        partsHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        partsListView = new ListView<>();
        partsListView.setPrefHeight(120);

        tempParts.addAll(order.getSpareParts());
        tempPartQuantities.addAll(order.getSparePartQuantities());
        for (int i = 0; i < tempParts.size(); i++) {
            SparePart p = tempParts.get(i);
            int q = tempPartQuantities.get(i);
            partsListView.getItems().add((i+1) + ". " + p.getName() + " — " + p.getRetailPrice() + " руб. x " + q + " = " + (p.getRetailPrice() * q) + " руб.");
        }

        partCombo = new ComboBox<>(FXCollections.observableArrayList(DataStore.getSpareParts()));
        partCombo.setPromptText("Выберите запчасть");
        partCombo.setPrefWidth(350);
        TextField partPriceField = new TextField();
        partPriceField.setEditable(false);
        partPriceField.setPrefWidth(100);
        TextField partStockField = new TextField();
        partStockField.setEditable(false);
        partStockField.setPrefWidth(70);
        TextField partQtyField = new TextField();
        partQtyField.setText("1");
        partQtyField.setPrefWidth(70);

        partCombo.setOnAction(e -> {
            SparePart selected = partCombo.getValue();
            if (selected != null) {
                partPriceField.setText(String.valueOf(selected.getRetailPrice()));
                partStockField.setText(String.valueOf(selected.getStock()));
            }
        });

        Button addPartBtn = new Button("➕ Добавить запчасть");
        addPartBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        HBox partAddBox = new HBox(10, partCombo, partPriceField, new Label("Остаток:"), partStockField,
                new Label("Кол-во:"), partQtyField, addPartBtn);

        // ==================== КНОПКИ УДАЛЕНИЯ ====================
        Button removeServiceBtn = new Button("❌ Удалить выбранную услугу");
        removeServiceBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        Button removePartBtn = new Button("❌ Удалить выбранную запчасть");
        removePartBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");

        // ==================== ИТОГО ====================
        totalLabel = new Label("💰 ИТОГО: " + calculateTotal() + " руб.");
        totalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2E7D32;");

        // ==================== ЛОГИКА ДОБАВЛЕНИЯ ====================
        addServiceBtn.setOnAction(e -> {
            Service selected = serviceCombo.getValue();
            if (selected == null) {
                showAlert("Выберите услугу");
                return;
            }
            tempServices.add(selected.getName());
            tempServicePrices.add(selected.getPrice());
            servicesListView.getItems().add(tempServices.size() + ". " + selected.getName() + " — " + selected.getPrice() + " руб.");
            serviceCombo.setValue(null);
            servicePriceField.clear();
            updateTotalLabel();
        });

        addPartBtn.setOnAction(e -> {
            SparePart selected = partCombo.getValue();
            if (selected == null) {
                showAlert("Выберите запчасть");
                return;
            }
            int qty;
            try {
                qty = Integer.parseInt(partQtyField.getText());
                if (qty <= 0) {
                    showAlert("Количество должно быть положительным");
                    return;
                }
                if (qty > selected.getStock()) {
                    showAlert("Недостаточно запчастей. Доступно: " + selected.getStock());
                    return;
                }
            } catch (NumberFormatException ex) {
                showAlert("Введите корректное количество");
                return;
            }
            tempParts.add(selected);
            tempPartQuantities.add(qty);
            selected.setStock(selected.getStock() - qty);
            partsListView.getItems().add(tempParts.size() + ". " + selected.getName() + " — " + selected.getRetailPrice() + " руб. x " + qty + " = " + (selected.getRetailPrice() * qty) + " руб.");
            partStockField.setText(String.valueOf(selected.getStock()));
            partCombo.setValue(null);
            partPriceField.clear();
            partQtyField.setText("1");
            updateTotalLabel();
        });

        // ==================== ЛОГИКА УДАЛЕНИЯ ====================
        removeServiceBtn.setOnAction(e -> {
            int idx = servicesListView.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < tempServices.size()) {
                tempServices.remove(idx);
                tempServicePrices.remove(idx);
                servicesListView.getItems().clear();
                for (int i = 0; i < tempServices.size(); i++) {
                    servicesListView.getItems().add((i+1) + ". " + tempServices.get(i) + " — " + tempServicePrices.get(i) + " руб.");
                }
                updateTotalLabel();
            } else {
                showAlert("Выберите услугу для удаления");
            }
        });

        removePartBtn.setOnAction(e -> {
            int idx = partsListView.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < tempParts.size()) {
                SparePart part = tempParts.get(idx);
                int qty = tempPartQuantities.get(idx);
                part.setStock(part.getStock() + qty);
                tempParts.remove(idx);
                tempPartQuantities.remove(idx);
                partsListView.getItems().clear();
                for (int i = 0; i < tempParts.size(); i++) {
                    SparePart p = tempParts.get(i);
                    int q = tempPartQuantities.get(i);
                    partsListView.getItems().add((i+1) + ". " + p.getName() + " — " + p.getRetailPrice() + " руб. x " + q + " = " + (p.getRetailPrice() * q) + " руб.");
                }
                partCombo.setItems(FXCollections.observableArrayList(DataStore.getSpareParts()));
                updateTotalLabel();
            } else {
                showAlert("Выберите запчасть для удаления");
            }
        });

        // ==================== КНОПКИ СОХРАНЕНИЯ ====================
        Button saveBtn = new Button("💾 СОХРАНИТЬ ИЗМЕНЕНИЯ");
        saveBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        Button cancelBtn = new Button("❌ ОТМЕНА");
        cancelBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 14px;");
        HBox btnBox = new HBox(15, saveBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER);

        // Сборка интерфейса
        root.getChildren().addAll(
                infoLabel,
                new Separator(),
                appointmentSection,
                new Separator(),
                servicesHeader, servicesListView,
                serviceAddBox, removeServiceBtn,
                new Separator(),
                partsHeader, partsListView,
                partAddBox, removePartBtn,
                new Separator(),
                totalLabel, btnBox
        );

        Scene scene = new Scene(root);
        currentStage.setScene(scene);

        // Сохраняем ссылки на внешние переменные
        Appointment finalExistingAppointment = existingAppointment;

        saveBtn.setOnAction(e -> {
            saveOrder(order, finalExistingAppointment, hasAppointmentCheck, datePicker, timeCombo, masterCombo);
        });

        cancelBtn.setOnAction(e -> {
            cancelChanges();
            currentStage.close();
        });

        currentStage.showAndWait();
    }

    private static void saveOrder(WorkOrder order, Appointment existingAppointment,
                                  CheckBox hasAppointmentCheck, DatePicker datePicker,
                                  ComboBox<String> timeCombo, ComboBox<String> masterCombo) {

        // Валидация
        if (tempServices.isEmpty() && tempParts.isEmpty()) {
            showAlert("Должна быть хотя бы одна услуга или запчасть");
            return;
        }

        // Валидация записи в календаре
        if (hasAppointmentCheck.isSelected()) {
            LocalDate selectedDate = datePicker.getValue();
            String selectedTime = timeCombo.getValue();
            String selectedMaster = masterCombo.getValue();

            if (selectedDate == null) {
                showAlert("Выберите дату записи");
                return;
            }
            if (selectedTime == null || selectedTime.isEmpty()) {
                showAlert("Выберите время записи");
                return;
            }
            if (selectedMaster == null || selectedMaster.isEmpty()) {
                showAlert("Выберите мастера");
                return;
            }

            String dateStr = selectedDate.toString();
            String timeStr = selectedTime;
            String master = selectedMaster;

            // Проверка на занятость времени
            List<Appointment> existing = DataStore.getAppointmentsByDate(dateStr);
            boolean isFree = true;
            int existingId = (existingAppointment != null) ? existingAppointment.getId() : -1;
            for (Appointment a : existing) {
                if (a.getTime().equals(timeStr) && a.getMasterName().equals(master)) {
                    if (existingId == -1 || a.getId() != existingId) {
                        isFree = false;
                        break;
                    }
                }
            }

            if (!isFree) {
                showAlert("Выбранное время уже занято другим клиентом!");
                return;
            }
        }

        System.out.println("=== СОХРАНЕНИЕ ИЗМЕНЕНИЙ ЗАКАЗА ===");
        System.out.println("Услуг: " + tempServices.size());
        System.out.println("Запчастей: " + tempParts.size());

        // Очищаем старые услуги
        while (order.getServices().size() > 0) {
            order.removeService(0);
        }
        for (int i = 0; i < tempServices.size(); i++) {
            order.addService(tempServices.get(i), tempServicePrices.get(i));
        }

        // Очищаем старые запчасти
        while (order.getSpareParts().size() > 0) {
            order.removeSparePart(0);
        }
        for (int i = 0; i < tempParts.size(); i++) {
            order.addSparePart(tempParts.get(i), tempPartQuantities.get(i));
        }

        // Сохраняем заказ
        DataStore.updateOrder(order);

        // Обработка записи в календаре
        if (hasAppointmentCheck.isSelected()) {
            LocalDate selectedDate = datePicker.getValue();
            String selectedTime = timeCombo.getValue();
            String selectedMaster = masterCombo.getValue();
            String serviceName = tempServices.isEmpty() ? "Консультация" : tempServices.get(0);
            String dateStr = selectedDate.toString();
            String timeStr = selectedTime;
            String master = selectedMaster;

            if (existingAppointment != null) {
                // Обновляем существующую запись
                existingAppointment.setDate(dateStr);
                existingAppointment.setTime(timeStr);
                existingAppointment.setMasterName(master);
                existingAppointment.setServiceName(serviceName);
                DataStore.updateAppointment(existingAppointment);
                System.out.println("Запись в календаре обновлена");
            } else {
                // Создаём новую запись
                Appointment newAppointment = new Appointment(
                        order.getClient(),
                        master,
                        serviceName,
                        dateStr,
                        timeStr
                );
                newAppointment.setOrderId(order.getId());
                DataStore.addAppointment(newAppointment);
                System.out.println("Новая запись в календаре создана");
            }
        } else {
            if (existingAppointment != null) {
                DataStore.deleteAppointment(existingAppointment.getId());
                System.out.println("Запись в календаре удалена");
            }
        }

        OrderController.refreshTable();
        currentStage.close();
    }

    private static void cancelChanges() {
        for (int i = 0; i < tempParts.size(); i++) {
            SparePart part = tempParts.get(i);
            int originalQty = tempPartQuantities.get(i);
            for (SparePart original : DataStore.getSpareParts()) {
                if (original.getName().equals(part.getName())) {
                    original.setStock(original.getStock() + originalQty);
                    break;
                }
            }
        }
        currentStage.close();
    }

    private static void updateTotalLabel() {
        double total = 0;
        for (Double price : tempServicePrices) total += price;
        for (int i = 0; i < tempParts.size(); i++) {
            total += tempParts.get(i).getRetailPrice() * tempPartQuantities.get(i);
        }
        totalLabel.setText("💰 ИТОГО: " + String.format("%.2f", total) + " руб.");
    }

    private static double calculateTotal() {
        double total = 0;
        for (Double price : tempServicePrices) total += price;
        for (int i = 0; i < tempParts.size(); i++) {
            total += tempParts.get(i).getRetailPrice() * tempPartQuantities.get(i);
        }
        return total;
    }

    private static void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.showAndWait();
    }
}