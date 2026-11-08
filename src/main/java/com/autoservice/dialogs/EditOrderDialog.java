package com.autoservice.dialogs;

import com.autoservice.AppConstants;
import com.autoservice.Client;
import com.autoservice.Service;
import com.autoservice.SparePart;
import com.autoservice.WorkOrder;
import com.autoservice.Appointment;
import com.autoservice.DataStore;
import com.autoservice.controllers.OrderController;
import com.autoservice.services.AutoAddSparePartService;
import com.autoservice.services.WindowStateManager;
import com.autoservice.utils.DialogValidator;
import com.autoservice.utils.ServicePartHelper;
import com.autoservice.utils.StockHelper;
import com.autoservice.utils.ValidationErrorIndicator;
import com.autoservice.utils.ValidationUtils;
import com.autoservice.utils.TooltipHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.autoservice.DateUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Диалог редактирования заказа: услуги, запчасти, пробег и запись в календарь.
 */
public class EditOrderDialog {
    
    private static final Logger logger = LoggerFactory.getLogger(EditOrderDialog.class);

    /**
     * Прежний вид диалога выбора запчастей в редактировании: запчасти не
     * отмечены, список с рамкой, ввод количества не ограничивается остатком.
     */
    private static final ServiceSparePartsDialog.Options SPARE_PARTS_DIALOG_OPTIONS =
            new ServiceSparePartsDialog.Options(false, true, false);

    private static ListView<String> servicesListView;
    private static ListView<String> partsListView;
    private static Label totalLabel;
    private static ComboBox<SparePart> partCombo;
    private static Stage currentStage;
    private static TextField mileageField;
    
    // TIME_SLOTS и MASTERS перенесены в AppConstants
    
    // TIME_SLOTS и MASTERS перенесены в AppConstants
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    // Временные списки для редактирования
    private static final List<String> tempServices = new ArrayList<>();
    private static final List<Double> tempServicePrices = new ArrayList<>();
    private static final List<SparePart> tempParts = new ArrayList<>();
    private static final List<Double> tempPartQuantities = new ArrayList<>();

    /**
     * Открыть диалог редактирования заказа.
     *
     * <p>Закрытые заказы не редактируются — выводится предупреждение.
     * @param order редактируемый заказ
     */
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

        Client orderClient = order.getClient();
        String clientName = orderClient != null ? orderClient.getName() : AppConstants.NO_CLIENT;
        Label infoLabel = new Label("Клиент: " + clientName + " (" + order.getCarModel() + ", " + order.getCarNumber() + ")");
        infoLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        // ==================== ПРОБЕГ ====================
        Label mileageHeader = new Label("ПРОБЕГ");
        mileageHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        Label mileageDisplay = new Label("Текущий пробег: " + order.getMileage() + " км");
        mileageDisplay.setStyle("-fx-font-size: 12px; -fx-padding: 3 0 3 0;");

        TextField mileageFieldLocal = new TextField(String.valueOf(order.getMileage()));
        mileageFieldLocal.setPrefWidth(150);
        mileageFieldLocal.setTextFormatter(new TextFormatter<>(change -> {
            String text = change.getText();
            if (text.isEmpty()) return change;
            if (!text.matches("[0-9]+")) return null;
            String newText = change.getControlNewText();
            if (newText.length() > 6) return null;
            return change;
        }));
        mileageField = mileageFieldLocal;

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
                LocalDate date = DateUtils.parseDate(dateStr);
                if (date != null) {
                    formattedDate = date.format(DATE_FORMATTER);
                }
            } catch (Exception e) { logger.error("Error in edit order", e); }

            currentAppointmentInfo.setText("Текущая запись: " + formattedDate + " " + timeStr + ", мастер: " + master + ", услуга: " + service);
            currentAppointmentInfo.setStyle("-fx-text-fill: #2196F3; -fx-font-size: 12px; -fx-padding: 5 0 5 0;");
        } else {
            hasAppointmentCheck.setSelected(false);
            currentAppointmentInfo.setText("Запись в календаре отсутствует");
            currentAppointmentInfo.setStyle("-fx-text-fill: #FF9800; -fx-font-size: 12px; -fx-padding: 5 0 5 0;");
        }

        DatePicker datePicker = new DatePicker();
        ComboBox<String> timeCombo = new ComboBox<>(FXCollections.observableArrayList(AppConstants.TIME_SLOTS));
        ComboBox<String> masterCombo = new ComboBox<>(FXCollections.observableArrayList(AppConstants.MASTERS));

        if (existingAppointment != null) {
            LocalDate apptDate = DateUtils.parseDate(existingAppointment.getDate());
            if (apptDate != null) {
                datePicker.setValue(apptDate);
            } else {
                datePicker.setValue(LocalDate.now());
            }
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
        // ID запчастей теперь сохраняется в БД (spare_part_id в order_parts) — костыль поиска по имени не нужен
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
                List<AutoAddSparePartService.SparePartWithQuantity> selectedParts =
                        ServiceSparePartsDialog.show(selected.getName(), relatedParts, SPARE_PARTS_DIALOG_OPTIONS);
                
                if (selectedParts != null) {
                    // Добавляем выбранные запчасти
                    for (AutoAddSparePartService.SparePartWithQuantity partInfo : selectedParts) {
                        SparePart part = partInfo.getSparePart();
                        double qty = partInfo.getQuantity();
                        
                        if (StockHelper.reserve(part, qty)) {
                            tempParts.add(part);
                            tempPartQuantities.add(qty);
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
                StockHelper.restore(part, qty);
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
                mileageHeader, mileageDisplay, mileageField,
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
        if (!validateInput(existingAppointment, hasAppointmentCheck, datePicker, timeCombo, masterCombo)) {
            return;
        }

        logger.info("=== СОХРАНЕНИЕ ИЗМЕНЕНИЙ ЗАКАЗА ===");
        logger.info("Услуг: {}", tempServices.size());
        logger.info("Запчастей: {}", tempParts.size());

        applyOrderChanges(order);
        persistOrder(order);
        createOrUpdateAppointment(order, existingAppointment, hasAppointmentCheck,
                datePicker, timeCombo, masterCombo);

        OrderController.refreshTable();
        currentStage.close();
    }

    /**
     * Проверить введённые данные и выбранное время записи.
     *
     * <p>При ошибке показывает сообщение и возвращает {@code false}. Проверка
     * остатков повторяется перед сохранением, потому что остатки могли
     * измениться, пока диалог был открыт.</p>
     *
     * @return {@code true}, если можно сохранять заказ
     */
    private static boolean validateInput(Appointment existingAppointment,
                                         CheckBox hasAppointmentCheck, DatePicker datePicker,
                                         ComboBox<String> timeCombo, ComboBox<String> masterCombo) {
        ValidationErrorIndicator.clearAllErrors(currentStage.getScene().getRoot());

        DialogValidator validator = new DialogValidator();

        if (tempServices.isEmpty() && tempParts.isEmpty()) {
            validator.add("Должна быть хотя бы одна услуга или запчасть");
        }

        // ====== ПОВТОРНАЯ ПРОВЕРКА ОСТАТКА ПЕРЕД СОХРАНЕНИЕМ ======
        for (int i = 0; i < tempParts.size(); i++) {
            SparePart part = tempParts.get(i);
            double qty = tempPartQuantities.get(i);
            SparePart currentPart = DataStore.getSparePartById(part.getId());
            if (currentPart == null) {
                validator.add("Запчасть не найдена в базе: " + part.getName());
                continue;
            }
            int currentStock = (int) currentPart.getStock();
            if (qty > currentStock) {
                validator.add("Недостаточно запчастей на складе: " + part.getName()
                        + " (в наличии: " + currentStock + ")");
            }
        }

        // ====== ПРОВЕРКА ПРОБЕГА ======
        ValidationUtils.ValidationError mileageError =
                ValidationUtils.validateIntegerText(mileageField.getText(), "Пробег", false);
        if (mileageError != null) {
            ValidationErrorIndicator.showError(mileageField, mileageError.getErrorMessage());
            validator.add(mileageError);
        }

        if (hasAppointmentCheck.isSelected()) {
            if (datePicker.getValue() == null) {
                ValidationErrorIndicator.showError(datePicker, "Выберите дату записи");
                validator.add("Выберите дату записи");
            }
            if (timeCombo.getValue() == null || timeCombo.getValue().isEmpty()) {
                ValidationErrorIndicator.showError(timeCombo, "Выберите время записи");
                validator.add("Выберите время записи");
            }
            if (masterCombo.getValue() == null || masterCombo.getValue().isEmpty()) {
                ValidationErrorIndicator.showError(masterCombo, "Выберите мастера");
                validator.add("Выберите мастера");
            }
        }

        if (validator.hasErrors()) {
            showAlert(validator.getMessage());
            return false;
        }

        if (hasAppointmentCheck.isSelected()) {
            return validateAppointmentSlot(existingAppointment, datePicker, timeCombo, masterCombo);
        }
        return true;
    }

    /**
     * Проверить, что выбранное время записи свободно и подтверждён выходной день.
     *
     * @return {@code true}, если запись можно создавать или обновлять
     */
    private static boolean validateAppointmentSlot(Appointment existingAppointment,
                                                   DatePicker datePicker,
                                                   ComboBox<String> timeCombo,
                                                   ComboBox<String> masterCombo) {
        LocalDate selectedDate = datePicker.getValue();
        String timeStr = timeCombo.getValue();
        String master = masterCombo.getValue();
        String dateStr = DateUtils.formatDateForDB(selectedDate);

        // Проверка на занятость времени
        List<Appointment> existing = DataStore.getAppointmentsByDate(dateStr);
        int existingId = (existingAppointment != null) ? existingAppointment.getId() : -1;
        for (Appointment appointment : existing) {
            if (appointment.getTime().equals(timeStr)
                    && appointment.getMasterName().equals(master)
                    && (existingId == -1 || appointment.getId() != existingId)) {
                showAlert("Выбранное время уже занято другим клиентом!");
                return false;
            }
        }

        // ====== ПРОВЕРКА ВЫХОДНЫХ ДНЕЙ ======
        if (!DateUtils.isWeekend(selectedDate)) {
            return true;
        }

        Alert weekendAlert = new Alert(Alert.AlertType.CONFIRMATION);
        weekendAlert.setTitle("Подтверждение записи");
        weekendAlert.setHeaderText("Выбран выходной день!");
        weekendAlert.setContentText("Запись в выходной день ("
                + selectedDate.format(DateTimeFormatter.ofPattern("EEEE",
                        new java.util.Locale.Builder().setLanguage("ru").build()))
                + ") может быть ограничена.\n\nПродолжить?");
        weekendAlert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        return weekendAlert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    /**
     * Применить отредактированные услуги, запчасти и пробег к заказу.
     *
     * <p>Остатки запчастей списываются и восстанавливаются при добавлении и
     * удалении в диалоге, поэтому перед сохранением достаточно пересобрать
     * состав заказа из временных списков.</p>
     */
    private static void applyOrderChanges(WorkOrder order) {
        // ====== ОЧИЩАЕМ СТАРЫЕ УСЛУГИ ======
        while (!order.getServices().isEmpty()) {
            order.removeService(0);
        }
        for (int i = 0; i < tempServices.size(); i++) {
            String serviceName = tempServices.get(i);
            Service service = DataStore.getServiceByName(serviceName);
            int serviceId = (service != null) ? service.getId() : 0;
            order.addService(serviceId, serviceName, tempServicePrices.get(i));
        }

        // ====== ОЧИЩАЕМ СТАРЫЕ ЗАПЧАСТИ И ДОБАВЛЯЕМ ТЕКУЩИЕ ======
        while (!order.getSpareParts().isEmpty()) {
            order.removeSparePart(0);
        }
        for (int i = 0; i < tempParts.size(); i++) {
            order.addSparePart(tempParts.get(i), tempPartQuantities.get(i));
        }

        // ====== СОХРАНЯЕМ ПРОБЕГ ======
        String mileageText = mileageField.getText().trim();
        int mileage = mileageText.isEmpty() ? 0 : Integer.parseInt(mileageText);
        order.setMileage(mileage);
    }

    /**
     * Сохранить изменения заказа в базе данных.
     */
    private static void persistOrder(WorkOrder order) {
        DataStore.updateOrder(order);
    }

    /**
     * Создать, обновить или удалить связанную запись в календаре.
     */
    private static void createOrUpdateAppointment(WorkOrder order, Appointment existingAppointment,
                                                  CheckBox hasAppointmentCheck, DatePicker datePicker,
                                                  ComboBox<String> timeCombo,
                                                  ComboBox<String> masterCombo) {
        if (!hasAppointmentCheck.isSelected()) {
            if (existingAppointment != null) {
                DataStore.deleteAppointment(existingAppointment.getId());
                logger.debug("Запись в календаре удалена");
            }
            return;
        }

        String serviceName = tempServices.isEmpty() ? "Консультация" : tempServices.get(0);
        String dateStr = DateUtils.formatDateForDB(datePicker.getValue());
        String timeStr = timeCombo.getValue();
        String master = masterCombo.getValue();

        // Находим ID услуги для записи в календарь
        Service appointmentService = DataStore.getServiceByName(serviceName);
        int appointmentServiceId = (appointmentService != null) ? appointmentService.getId() : 0;

        if (existingAppointment != null) {
            existingAppointment.setDate(dateStr);
            existingAppointment.setTime(timeStr);
            existingAppointment.setMasterName(master);
            existingAppointment.setServiceName(serviceName);
            existingAppointment.setServiceId(appointmentServiceId);
            existingAppointment.setOrderId(order.getId());
            DataStore.updateAppointment(existingAppointment);
            logger.debug("Запись в календаре обновлена");
            return;
        }

        Appointment newAppointment = new Appointment(
                order.getClient(),
                master,
                serviceName,
                dateStr,
                timeStr
        );
        newAppointment.setServiceId(appointmentServiceId);
        newAppointment.setOrderId(order.getId());
        DataStore.addAppointment(newAppointment);
        logger.debug("Новая запись в календаре создана");
    }

    private static void cancelChanges() {
        for (int i = 0; i < tempParts.size(); i++) {
            StockHelper.restore(tempParts.get(i), tempPartQuantities.get(i));
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

}
