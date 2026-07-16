package com.autoservice.dialogs;

import com.autoservice.Client;
import com.autoservice.Service;
import com.autoservice.SparePart;
import com.autoservice.WorkOrder;
import com.autoservice.Appointment;
import com.autoservice.DataStore;
import com.autoservice.controllers.OrderController;
import com.autoservice.services.AutoAddSparePartService;
import com.autoservice.services.WindowStateManager;
import com.autoservice.utils.OilHelper;
import com.autoservice.utils.ValidationErrorIndicator;
import com.autoservice.utils.ValidationUtils;
import com.autoservice.utils.TooltipHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class EditOrderDialog {
    
    private static final Logger logger = LoggerFactory.getLogger(EditOrderDialog.class);
    private static ListView<String> servicesListView;
    private static ListView<String> partsListView;
    private static Label totalLabel;
    private static ComboBox<SparePart> partCombo;
    private static Stage currentStage;
    
    // Временные списки для редактирования
    private static final List<String> tempServices = new ArrayList<>();
    private static final List<Double> tempServicePrices = new ArrayList<>();
    private static final List<SparePart> tempParts = new ArrayList<>();
    private static final List<Double> tempPartQuantities = new ArrayList<>();
    
    // Константы
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String[] TIME_SLOTS = {
        "09:00", "09:30", "10:00", "10:30", "11:00", "11:30",
        "12:00", "12:30", "13:00", "13:30", "14:00", "14:30",
        "15:00", "15:30", "16:00", "16:30", "17:00", "17:30"
    };
    private static final String[] MASTERS = {
        "Иванов Иван", "Петров Петр", "Сидоров Сидор", "Смирнова Анна"
    };

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
        
        // Восстановление состояния диалога
        WindowStateManager.getInstance().restoreWindowState("editOrderDialog", currentStage);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Label infoLabel = new Label("Клиент: " + order.getClient().getName() + " (" + order.getClient().getCarModel() + ", " + order.getClient().getCarNumber() + ")");
        infoLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        // ==================== ЗАПИСЬ В КАЛЕНДАРЬ ====================
        Label appointmentHeader = new Label("ЗАПИСЬ В КАЛЕНДАРЬ");
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

            currentAppointmentInfo.setText("Текущая запись: " + formattedDate + " " + timeStr + ", мастер: " + master + ", услуга: " + service);
            currentAppointmentInfo.setStyle("-fx-text-fill: #2196F3; -fx-font-size: 12px; -fx-padding: 5 0 5 0;");
        } else {
            hasAppointmentCheck.setSelected(false);
            currentAppointmentInfo.setText("Запись в календаре отсутствует");
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
        TooltipHelper.setToolTip(masterCombo, "Выберите мастера сервиса");

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
                currentAppointmentInfo.setText("Запись будет удалена");
                currentAppointmentInfo.setStyle("-fx-text-fill: #f44336; -fx-font-size: 12px; -fx-padding: 5 0 5 0;");
            } else {
                datePicker.setValue(LocalDate.now());
                currentAppointmentInfo.setText("Будет создана новая запись");
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
        Label servicesHeader = new Label("УСЛУГИ");
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

        Button addServiceBtn = new Button("Добавить");
        addServiceBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        Button removeServiceBtn = new Button("Удалить");
        removeServiceBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");
        HBox serviceAddBox = new HBox(10, serviceCombo, addServiceBtn, removeServiceBtn);

        // ==================== ЗАПЧАСТИ ====================
        Label partsHeader = new Label("ЗАПЧАСТИ");
        partsHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        partsListView = new ListView<>();
        partsListView.setPrefHeight(120);

        tempParts.addAll(order.getSpareParts());
        tempPartQuantities.addAll(order.getSparePartQuantities().stream().map(Double::valueOf).collect(java.util.stream.Collectors.toList()));
        for (int i = 0; i < tempParts.size(); i++) {
            SparePart p = tempParts.get(i);
            double q = tempPartQuantities.get(i);
            partsListView.getItems().add((i+1) + ". " + p.getName() + " — " + p.getRetailPrice() + " руб. x " + (int)q + " = " + (p.getRetailPrice() * q) + " руб.");
        }

        partCombo = new ComboBox<>(FXCollections.observableArrayList(DataStore.getSpareParts()));
        partCombo.setPromptText("Выберите запчасть");
        partCombo.setPrefWidth(350);

        Button addPartBtn = new Button("Добавить");
        addPartBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        Button removePartBtn = new Button("Удалить");
        removePartBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");
        HBox partAddBox = new HBox(10, partCombo, addPartBtn, removePartBtn);

        // ==================== ИТОГО ====================
        totalLabel = new Label("ИТОГО: " + calculateTotal() + " руб.");
        totalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2E7D32;");

        // ==================== ЛОГИКА ДОБАВЛЕНИЯ ====================
        addServiceBtn.setOnAction(e -> {
            Service selected = serviceCombo.getValue();
            if (selected == null) {
                showAlert("Выберите услугу");
                return;
            }

            // ====== ВЫБОР ЗАПЧАСТЕЙ ЧЕРЕЗ ЧЕКБОКСЫ ======
            List<AutoAddSparePartService.SparePartWithQuantity> relatedParts = AutoAddSparePartService.getSparePartsByService(selected.getName());
            
            if (!relatedParts.isEmpty()) {
                // Показываем диалог выбора запчастей
                List<AutoAddSparePartService.SparePartWithQuantity> selectedParts = showServiceSparePartsDialog(selected.getName(), relatedParts);
                
                if (selectedParts != null) {
                    // Добавляем выбранные запчасти
                    for (AutoAddSparePartService.SparePartWithQuantity partInfo : selectedParts) {
                        SparePart part = partInfo.getSparePart();
                        double qty = partInfo.getQuantity();
                        
                        if (qty > 0 && qty <= part.getStock()) {
                            tempParts.add(part);
                            tempPartQuantities.add(qty);
                            part.setStock(part.getStock() - qty);
                            partsListView.getItems().add(tempParts.size() + ". " + part.getName() + " — " + part.getRetailPrice() + " руб. x " + (int)qty + " = " + (part.getRetailPrice() * qty) + " руб.");
                        } else {
                            showAlert("Недостаточно запчастей на складе: " + part.getName());
                        }
                    }
                }
            }

            tempServices.add(selected.getName());
            tempServicePrices.add(selected.getPrice());
            servicesListView.getItems().add(tempServices.size() + ". " + selected.getName() + " — " + selected.getPrice() + " руб.");
            serviceCombo.setValue(null);
            updateTotalLabel();
        });

        addPartBtn.setOnAction(e -> {
            SparePart selected = partCombo.getValue();
            if (selected == null) {
                showAlert("Выберите запчасть");
                return;
            }

            // ====== ВАЛИДАЦИЯ ОСТАТКА ======
            double requestedQty = 1.0;
            if (requestedQty > selected.getStock()) {
                showAlert("Недостаточно запчастей на складе: " + selected.getName() + " (в наличии: " + (int)selected.getStock() + ")");
                return;
            }

            tempParts.add(selected);
            tempPartQuantities.add(requestedQty);
            selected.setStock(selected.getStock() - requestedQty);
            partsListView.getItems().add(tempParts.size() + ". " + selected.getName() + " — " + selected.getRetailPrice() + " руб. x " + (int)requestedQty + " = " + selected.getRetailPrice() + " руб.");
            partCombo.setValue(null);
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
                double qty = tempPartQuantities.get(idx);
                part.setStock(part.getStock() + qty);
                tempParts.remove(idx);
                tempPartQuantities.remove(idx);
                partsListView.getItems().clear();
                for (int i = 0; i < tempParts.size(); i++) {
                    SparePart p = tempParts.get(i);
                    double q = tempPartQuantities.get(i);
                    partsListView.getItems().add((i+1) + ". " + p.getName() + " — " + p.getRetailPrice() + " руб. x " + (int)q + " = " + (p.getRetailPrice() * q) + " руб.");
                }
                partCombo.setItems(FXCollections.observableArrayList(DataStore.getSpareParts()));
                updateTotalLabel();
            } else {
                showAlert("Выберите запчасть для удаления");
            }
        });

        // ==================== КНОПКИ СОХРАНЕНИЯ ====================
        Button saveBtn = new Button("Сохранить изменения");
        saveBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        Button cancelBtn = new Button("Отмена");
        cancelBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        HBox btnBox = new HBox(15, saveBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER);

        // Сборка интерфейса
        root.getChildren().addAll(
                infoLabel,
                new Separator(),
                appointmentSection,
                new Separator(),
                servicesHeader, servicesListView, serviceAddBox,
                new Separator(),
                partsHeader, partsListView, partAddBox,
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
        
        currentStage.setOnHiding(e -> {
            WindowStateManager.getInstance().saveWindowState("editOrderDialog", currentStage);
        });

        currentStage.showAndWait();
    }

    private static void saveOrder(WorkOrder order, Appointment existingAppointment,
                                  CheckBox hasAppointmentCheck, DatePicker datePicker,
                                  ComboBox<String> timeCombo, ComboBox<String> masterCombo) {

        // Очистка ошибок валидации
        ValidationErrorIndicator.clearAllErrors(currentStage.getScene().getRoot());
        
        boolean isValid = true;

        // Валидация
        if (tempServices.isEmpty() && tempParts.isEmpty()) {
            showAlert("Должна быть хотя бы одна услуга или запчасть");
            isValid = false;
        }

        // ====== ПОВТОРНАЯ ПРОВЕРКА ОСТАТКА ПЕРЕД СОХРАНЕНИЕМ ======
        boolean hasStockIssue = false;
        for (int i = 0; i < tempParts.size(); i++) {
            SparePart part = tempParts.get(i);
            double qty = tempPartQuantities.get(i);
            int currentStock = (int)DataStore.getSparePartById(part.getId()).getStock();
            
            if (qty > currentStock) {
                hasStockIssue = true;
                showAlert("Недостаточно запчастей на складе: " + part.getName() + " (в наличии: " + currentStock + ")");
            }
        }
        if (hasStockIssue) {
            isValid = false;
        }
        
        if (!isValid) {
            return;
        }

        // Валидация записи в календаре
        if (hasAppointmentCheck.isSelected()) {
            LocalDate selectedDate = datePicker.getValue();
            String selectedTime = timeCombo.getValue();
            String selectedMaster = masterCombo.getValue();

            if (selectedDate == null) {
                ValidationErrorIndicator.showError(datePicker, "Выберите дату записи");
                return;
            }
            if (selectedTime == null || selectedTime.isEmpty()) {
                ValidationErrorIndicator.showError(timeCombo, "Выберите время записи");
                return;
            }
            if (selectedMaster == null || selectedMaster.isEmpty()) {
                ValidationErrorIndicator.showError(masterCombo, "Выберите мастера");
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

        logger.info("=== СОХРАНЕНИЕ ИЗМЕНЕНИЙ ЗАКАЗА ===");
        logger.info("Услуг: {}", tempServices.size());
        logger.info("Запчастей: {}", tempParts.size());

        // ====== ОЧИЩАЕМ СТАРЫЕ УСЛУГИ ======
        while (order.getServices().size() > 0) {
            order.removeService(0);
        }
        for (int i = 0; i < tempServices.size(); i++) {
            order.addService(tempServices.get(i), tempServicePrices.get(i));
        }

        // ====== ОЧИЩАЕМ СТАРЫЕ ЗАПЧАСТИ ======
        while (order.getSpareParts().size() > 0) {
            order.removeSparePart(0);
        }

        // ====== ДОБАВЛЯЕМ РУЧНЫЕ ЗАПЧАСТИ ======
        for (int i = 0; i < tempParts.size(); i++) {
            order.addSparePart(tempParts.get(i), tempPartQuantities.get(i));
        }

        // ====== 🆕 АВТОМАТИЧЕСКИ ДОБАВЛЯЕМ МАСЛО И РАСХОДНИКИ ======
        List<String> serviceNames = new ArrayList<>(tempServices);
        OilHelper.addOilAndPartsToOrder(order, serviceNames);

        // ====== СОХРАНЯЕМ ЗАКАЗ ======
        DataStore.updateOrder(order);

        // ====== ОБРАБОТКА ЗАПИСИ В КАЛЕНДАРЕ ======
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
                existingAppointment.setOrderId(order.getId());
                DataStore.updateAppointment(existingAppointment);
                logger.debug("Запись в календаре обновлена");
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
                logger.debug("Новая запись в календаре создана");
            }
        } else {
            if (existingAppointment != null) {
                DataStore.deleteAppointment(existingAppointment.getId());
                logger.debug("Запись в календаре удалена");
            }
        }

        OrderController.refreshTable();
        currentStage.close();
    }

    private static void cancelChanges() {
        for (int i = 0; i < tempParts.size(); i++) {
            SparePart part = tempParts.get(i);
            double originalQty = tempPartQuantities.get(i);
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
        totalLabel.setText("ИТОГО: " + String.format("%.2f", total) + " руб.");
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

    // ==================== МЕТОД ДИАЛОГА ВЫБОРА ЗАПЧАСТЕЙ ДЛЯ УСЛУГИ ====================

    /**
     * Показывает диалог с чекбоксами для выбора запчастей, связанных с услугой.
     * Возвращает список выбранных запчастей с количеством (или null для отмены).
     */
    private static List<AutoAddSparePartService.SparePartWithQuantity> showServiceSparePartsDialog(String serviceName, List<AutoAddSparePartService.SparePartWithQuantity> relatedParts) {
        Stage stage = new Stage();
        stage.setTitle("Выбор запчастей для услуги: " + serviceName);
        stage.setMinWidth(600);
        stage.setMinHeight(500);
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Label titleLabel = new Label("Выберите запчасти, которые нужно добавить:");
        titleLabel.getStyleClass().add("dialog-title");

        // VBox для чекбоксов
        VBox checkboxesVBox = new VBox(10);
        checkboxesVBox.setPadding(new Insets(10));
        checkboxesVBox.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #cccccc;");
        checkboxesVBox.setPrefHeight(250);

        // Храним состояния чекбоксов
        ObservableMap<AutoAddSparePartService.SparePartWithQuantity, CheckBox> checkBoxMap = FXCollections.observableHashMap();
        
        // Создаём чекбоксы для каждой запчасти
        for (AutoAddSparePartService.SparePartWithQuantity partInfo : relatedParts) {
            SparePart part = partInfo.getSparePart();
            double defaultQty = partInfo.getQuantity();
            
            HBox hBox = new HBox(10);
            hBox.setAlignment(Pos.CENTER_LEFT);
            
            CheckBox checkBox = new CheckBox(part.getName() + " (в наличии: " + (int)part.getStock() + ")");
            checkBox.setSelected(false); // По умолчанию не выбрано
            
            // Поле для ввода количества
            TextField qtyField = new TextField(String.valueOf((int)defaultQty));
            qtyField.setPrefWidth(60);
            qtyField.setDisable(false);
            
            // Кнопка +/- для изменения количества
            HBox qtyControls = new HBox(5);
            Button minusBtn = new Button("-");
            minusBtn.setPrefWidth(25);
            Button plusBtn = new Button("+");
            plusBtn.setPrefWidth(25);
            
            minusBtn.setOnAction(evt -> {
                try {
                    int currentQty = Integer.parseInt(qtyField.getText());
                    if (currentQty > 1) {
                        qtyField.setText(String.valueOf(currentQty - 1));
                    }
                } catch (NumberFormatException ex) {}
            });
            
            plusBtn.setOnAction(evt -> {
                try {
                    int currentQty = Integer.parseInt(qtyField.getText());
                    if (currentQty < (int)part.getStock()) {
                        qtyField.setText(String.valueOf(currentQty + 1));
                    }
                } catch (NumberFormatException ex) {}
            });
            
            qtyControls.getChildren().addAll(minusBtn, plusBtn);
            
            hBox.getChildren().addAll(checkBox, qtyField, qtyControls);
            checkboxesVBox.getChildren().add(hBox);
            
            checkBoxMap.put(partInfo, checkBox);
        }

        // ScrollPane для чекбоксов
        ScrollPane scrollPane = new ScrollPane(checkboxesVBox);
        scrollPane.setFitToWidth(true);

        // Общая стоимость
        Label totalLabel = new Label("Общая стоимость: 0 руб.");
        totalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        // Кнопки
        Button confirmBtn = new Button("Подтвердить");
        confirmBtn.getStyleClass().add("save-button");

        Button skipBtn = new Button("Пропустить");
        skipBtn.getStyleClass().add("cancel-button");

        HBox btnBox = new HBox(15, confirmBtn, skipBtn);
        btnBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(titleLabel, scrollPane, totalLabel, btnBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);

        // Объявляем переменную для результата
        final List<AutoAddSparePartService.SparePartWithQuantity>[] result = new ArrayList[1];

        // Обновление общей стоимости
        Runnable updateTotal = () -> {
            double total = 0;
            int checkedCount = 0;
            for (AutoAddSparePartService.SparePartWithQuantity partInfo : relatedParts) {
                CheckBox checkBox = checkBoxMap.get(partInfo);
                if (checkBox != null && checkBox.isSelected()) {
                    checkedCount++;
                    try {
                        int qty = Integer.parseInt(((HBox)checkboxesVBox.getChildren().get(relatedParts.indexOf(partInfo))).getChildren().get(1).toString().replaceAll(".*text=", "").replaceAll(").*", ""));
                        total += partInfo.getSparePart().getRetailPrice() * qty;
                    } catch (Exception ex) {
                        total += partInfo.getSparePart().getRetailPrice() * partInfo.getQuantity();
                    }
                }
            }
            totalLabel.setText("Общая стоимость: " + String.format("%.2f", total) + " руб. (выбрано: " + checkedCount + ")");
        };

        // Кнопка подтверждения
        confirmBtn.setOnAction(evt -> {
            List<AutoAddSparePartService.SparePartWithQuantity> selectedParts = new ArrayList<>();
            
            for (int i = 0; i < relatedParts.size(); i++) {
                AutoAddSparePartService.SparePartWithQuantity partInfo = relatedParts.get(i);
                HBox hBox = (HBox)checkboxesVBox.getChildren().get(i);
                CheckBox checkBox = checkBoxMap.get(partInfo);
                TextField qtyField = (TextField)hBox.getChildren().get(1);
                
                if (checkBox != null && checkBox.isSelected()) {
                    try {
                        int qty = Integer.parseInt(qtyField.getText());
                        if (qty > 0) {
                            AutoAddSparePartService.SparePartWithQuantity selected = partInfo.copy();
                            selected.setQuantity(qty);
                            selectedParts.add(selected);
                        }
                    } catch (NumberFormatException ex) {
                        // Используем значение по умолчанию
                        selectedParts.add(partInfo.copy());
                    }
                }
            }
            
            if (selectedParts.isEmpty()) {
                showAlert("Выберите хотя бы одну запчасть");
                return;
            }
            
            result[0] = selectedParts;
            stage.close();
        });

        // Кнопка пропуска (добавить все с количеством по умолчанию)
        skipBtn.setOnAction(evt -> {
            List<AutoAddSparePartService.SparePartWithQuantity> allParts = new ArrayList<>();
            for (AutoAddSparePartService.SparePartWithQuantity partInfo : relatedParts) {
                allParts.add(partInfo.copy());
            }
            result[0] = allParts;
            stage.close();
        });

        updateTotal.run();
        stage.showAndWait();

        return result[0]; // Возвращаем результат
    }
}