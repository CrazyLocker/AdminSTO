package com.autoservice.dialogs;

import com.autoservice.AppConstants;
import com.autoservice.Client;
import com.autoservice.Car;
import com.autoservice.Service;
import com.autoservice.SparePart;
import com.autoservice.WorkOrder;
import com.autoservice.Appointment;
import com.autoservice.DataStore;
import com.autoservice.controllers.OrderController;
import com.autoservice.services.AutoAddSparePartService;
import com.autoservice.services.WindowStateManager;
import com.autoservice.utils.ServicePartHelper;
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
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.autoservice.DateUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditOrderDialog {
    
    private static final Logger logger = LoggerFactory.getLogger(EditOrderDialog.class);
    private static ListView<String> servicesListView;
    private static ListView<String> partsListView;
    private static Label totalLabel;
    private static ComboBox<SparePart> partCombo;
    private static ComboBox<Client> clientCombo;
    private static ComboBox<Car> carCombo;
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
        root.setStyle("-fx-font-size: 12px;");

        // ==================== КЛИЕНТ, АВТОМОБИЛЬ, ПРОБЕГ (в одной строке) ====================

        // ComboBox выбора клиента
        clientCombo = new ComboBox<>(FXCollections.observableArrayList(DataStore.getClients()));
        clientCombo.setPromptText("Выберите клиента");
        clientCombo.setPrefWidth(250);
        clientCombo.setCellFactory(lv -> new ListCell<Client>() {
            @Override
            protected void updateItem(Client item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText((item.getLastName() != null && !item.getLastName().isEmpty()
                            ? item.getLastName() + " " + item.getName() : item.getName()));
                }
            }
        });
        clientCombo.setButtonCell(new ListCell<Client>() {
            @Override
            protected void updateItem(Client item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText((item.getLastName() != null && !item.getLastName().isEmpty()
                            ? item.getLastName() + " " + item.getName() : item.getName()));
                }
            }
        });

        // ComboBox выбора автомобиля (зависит от клиента)
        carCombo = new ComboBox<>();
        carCombo.setPromptText("Выберите автомобиль");
        carCombo.setPrefWidth(220);
        carCombo.setCellFactory(lv -> new ListCell<Car>() {
            @Override
            protected void updateItem(Car item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplayName());
                }
            }
        });
        carCombo.setButtonCell(new ListCell<Car>() {
            @Override
            protected void updateItem(Car item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplayName());
                }
            }
        });

        // Пробег — загружается из заказа, компактная ширина
        TextField mileageFieldLocal = new TextField(String.valueOf(order.getMileage()));
        mileageFieldLocal.setPrefWidth(120);
        mileageFieldLocal.setTextFormatter(new TextFormatter<>(change -> {
            String text = change.getText();
            if (text.isEmpty()) return change;
            if (!text.matches("[0-9]+")) return null;
            String newText = change.getControlNewText();
            if (newText.length() > 6) return null;
            return change;
        }));
        mileageField = mileageFieldLocal;

        // Начальное значение клиента — из заказа
        Client orderClient = order.getClient();
        if (orderClient != null) {
            // Находим клиента в списке DataStore по ID (гарантируем актуальный объект)
            Client freshClient = DataStore.getClients().stream()
                    .filter(c -> c.getId() == orderClient.getId())
                    .findFirst().orElse(orderClient);
            clientCombo.setValue(freshClient);

            // Заполняем список авто выбранного клиента
            if (freshClient.getCars() != null && !freshClient.getCars().isEmpty()) {
                carCombo.getItems().setAll(freshClient.getCars());
                carCombo.setDisable(false);
                // Пытаемся выбрать авто из заказа по модели и номеру
                Car matchingCar = null;
                for (Car c : freshClient.getCars()) {
                    if (c.getCarModel() != null && c.getCarModel().equals(order.getCarModel())
                            && c.getCarNumber() != null && c.getCarNumber().equals(order.getCarNumber())) {
                        matchingCar = c;
                        break;
                    }
                }
                if (matchingCar != null) {
                    carCombo.setValue(matchingCar);
                }
            } else {
                carCombo.setDisable(true);
            }
        } else {
            carCombo.setDisable(true);
        }

        // При смене клиента — обновляем список авто
        clientCombo.setOnAction(e -> {
            Client selectedClient = clientCombo.getValue();
            if (selectedClient != null && selectedClient.getCars() != null && !selectedClient.getCars().isEmpty()) {
                carCombo.getItems().setAll(selectedClient.getCars());
                carCombo.setDisable(false);
                carCombo.setValue(null);
            } else {
                carCombo.getItems().clear();
                carCombo.setDisable(true);
                carCombo.setValue(null);
            }
        });

        // Компактная строка: Клиент | Авто | Пробег
        HBox clientCarMileageRow = new HBox(10,
                new Label("Клиент:"), clientCombo,
                new Label("Авто:"), carCombo,
                new Label("Пробег:"), mileageFieldLocal);
        clientCarMileageRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(clientCombo, Priority.ALWAYS);
        HBox.setHgrow(carCombo, Priority.ALWAYS);

        // ==================== ЗАПИСЬ В КАЛЕНДАРЬ ====================
        Label appointmentHeader = new Label("ЗАПИСЬ В КАЛЕНДАРЬ");

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
        } else {
            hasAppointmentCheck.setSelected(false);
            currentAppointmentInfo.setText("Запись в календаре отсутствует");
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
            } else {
                datePicker.setValue(LocalDate.now());
                currentAppointmentInfo.setText("Будет создана новая запись");
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

        servicesListView = new ListView<>();
        servicesListView.setMinHeight(90);
        servicesListView.setPrefHeight(100);
        servicesListView.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(servicesListView, Priority.ALWAYS);

        tempServices.addAll(order.getServices());
        tempServicePrices.addAll(order.getServicePrices());
        for (int i = 0; i < tempServices.size(); i++) {
            servicesListView.getItems().add((i+1) + ". " + tempServices.get(i) + " — " + tempServicePrices.get(i) + " руб.");
        }

        ComboBox<Service> serviceCombo = new ComboBox<>(FXCollections.observableArrayList(DataStore.getServices()));
        serviceCombo.setPromptText("Выберите услугу");
        serviceCombo.setPrefWidth(350);

        Button addServiceBtn = new Button("Добавить");
        Button removeServiceBtn = new Button("Удалить");
        HBox serviceAddBox = new HBox(10, serviceCombo, addServiceBtn, removeServiceBtn);

        // ==================== ЗАПЧАСТИ ====================
        Label partsHeader = new Label("ЗАПЧАСТИ");

        partsListView = new ListView<>();
        partsListView.setMinHeight(90);
        partsListView.setPrefHeight(100);
        partsListView.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(partsListView, Priority.ALWAYS);

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
        Button removePartBtn = new Button("Удалить");
        HBox partAddBox = new HBox(10, partCombo, addPartBtn, removePartBtn);

        // ==================== ИТОГО ====================
        totalLabel = new Label("ИТОГО: " + calculateTotal() + " руб.");

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
                    // Добавляем выбранные запчасти (НЕ списываем сразу — списание при сохранении)
                    for (AutoAddSparePartService.SparePartWithQuantity partInfo : selectedParts) {
                        SparePart part = partInfo.getSparePart();
                        double qty = partInfo.getQuantity();
                        
                        if (qty > 0) {
                            tempParts.add(part);
                            tempPartQuantities.add(qty);
                            partsListView.getItems().add(tempParts.size() + ". " + part.getName() + " — " + part.getRetailPrice() + " руб. x " + (int)qty + " = " + (part.getRetailPrice() * qty) + " руб.");
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

            tempParts.add(selected);
            tempPartQuantities.add(1.0);
            partsListView.getItems().add(tempParts.size() + ". " + selected.getName() + " — " + selected.getRetailPrice() + " руб. x 1 = " + selected.getRetailPrice() + " руб.");
            partCombo.setValue(null);
            updateTotalLabel();
        });

        // ==================== ЛОГИКА УДАЛЕНИЯ ====================
        removeServiceBtn.setOnAction(e -> {
            int idx = servicesListView.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < tempServices.size()) {
                String removedService = tempServices.remove(idx);
                tempServicePrices.remove(idx);
                
                // При удалении услуги удаляем также связанные запчасти из tempParts
                Service removedSvc = DataStore.getServiceByName(removedService);
                if (removedSvc != null) {
                    List<AutoAddSparePartService.SparePartWithQuantity> relatedParts = 
                            AutoAddSparePartService.getSparePartsByService(removedSvc.getName());
                    for (AutoAddSparePartService.SparePartWithQuantity relPart : relatedParts) {
                        SparePart toRemove = relPart.getSparePart();
                        for (int i = tempParts.size() - 1; i >= 0; i--) {
                            if (tempParts.get(i).getId() == toRemove.getId()) {
                                tempParts.remove(i);
                                tempPartQuantities.remove(i);
                            }
                        }
                    }
                }
                
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
        Button cancelBtn = new Button("Отмена");
        HBox btnBox = new HBox(15, saveBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER);

        // Секции услуг и запчастей в отдельных VBox для равного роста
        VBox servicesSection = new VBox(8, servicesHeader, servicesListView, serviceAddBox);
        VBox.setVgrow(servicesListView, Priority.ALWAYS);

        VBox partsSection = new VBox(8, partsHeader, partsListView, partAddBox);
        VBox.setVgrow(partsListView, Priority.ALWAYS);

        // Сборка интерфейса
        root.getChildren().addAll(
                clientCarMileageRow,
                new Separator(),
                appointmentSection,
                new Separator(),
                servicesSection,
                new Separator(),
                partsSection,
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
        
        currentStage.setOnHidden(e -> {
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

        // ====== ПРОВЕРКА ДОСТАТОЧНОСТИ ВСЕХ ЗАПЧАСТЕЙ ======
        // Создаём временный заказ для проверки через DataStore
        WorkOrder tempOrder = new WorkOrder();
        for (int i = 0; i < tempServices.size(); i++) {
            String svcName = tempServices.get(i);
            Service svc = DataStore.getServiceByName(svcName);
            int svcId = (svc != null) ? svc.getId() : 0;
            tempOrder.addService(svcId, svcName, tempServicePrices.get(i));
        }
        for (int i = 0; i < tempParts.size(); i++) {
            tempOrder.addSparePart(tempParts.get(i), tempPartQuantities.get(i));
        }
        
        List<String> missingParts = DataStore.checkSparePartsAvailability(tempOrder);
        if (!missingParts.isEmpty()) {
            StringBuilder msg = new StringBuilder("Недостаточно запчастей на складе:\n\n");
            for (String part : missingParts) {
                msg.append("  • ").append(part).append("\n");
            }
            Alert stockAlert = new Alert(Alert.AlertType.WARNING, msg.toString(), ButtonType.OK);
            stockAlert.setTitle("Недостаточно запчастей");
            stockAlert.showAndWait();
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

            String dateStr = DateUtils.formatDateForDB(selectedDate);
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

            // ====== ПРОВЕРКА ВЫХОДНЫХ ДНЕЙ ======
            if (DateUtils.isWeekend(selectedDate)) {
                Alert weekendAlert = new Alert(Alert.AlertType.CONFIRMATION);
                weekendAlert.setTitle("Подтверждение записи");
                weekendAlert.setHeaderText("Выбран выходной день!");
                weekendAlert.setContentText("Запись в выходной день (" + selectedDate.format(DateTimeFormatter.ofPattern("EEEE", new java.util.Locale.Builder().setLanguage("ru").build())) + ") может быть ограничена.\n\nПродолжить?");
                weekendAlert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
                
                if (weekendAlert.showAndWait().orElse(ButtonType.NO) == ButtonType.NO) {
                    return; // Отмена при нажатии "Нет"
                }
            }
        }

        logger.info("=== СОХРАНЕНИЕ ИЗМЕНЕНИЙ ЗАКАЗА ===");
        logger.info("Услуг: {}", tempServices.size());
        logger.info("Запчастей: {}", tempParts.size());

        // ====== РАСЧЁТ РАЗНИЦЫ ЗАПЧАСТЕЙ ======
        // Корректно обрабатываем 3 случая:
        // 1. Запчасть НОВАЯ (не была в заказе) → списать
        // 2. Запчасть УДАЛЕНА (была в заказе, но нет в temp) → вернуть
        // 3. Запчасть ИЗМЕНЕНА (количество изменилось) → скорректировать разницу
        List<SparePartWithQuantity> addedParts = new ArrayList<>();
        List<SparePartWithQuantity> removedParts = new ArrayList<>();
        
        // Строим карту оригинальных запчастей по ID
        Map<Integer, Double> originalQtyMap = new HashMap<>();
        for (int i = 0; i < order.getSpareParts().size(); i++) {
            SparePart part = order.getSpareParts().get(i);
            double qty = order.getSparePartQuantities().get(i);
            originalQtyMap.put(part.getId(), qty);
        }
        
        // Строим карту новых запчастей по ID
        Map<Integer, Double> newQtyMap = new HashMap<>();
        for (int i = 0; i < tempParts.size(); i++) {
            SparePart part = tempParts.get(i);
            double qty = tempPartQuantities.get(i);
            newQtyMap.put(part.getId(), qty);
        }
        
        // Находим добавленные и изменённые запчасти
        for (Map.Entry<Integer, Double> entry : newQtyMap.entrySet()) {
            int partId = entry.getKey();
            double newQty = entry.getValue();
            
            if (!originalQtyMap.containsKey(partId)) {
                // НОВАЯ запчасть — списать
                SparePart part = tempParts.stream()
                    .filter(p -> p.getId() == partId)
                    .findFirst().orElse(null);
                if (part != null) {
                    addedParts.add(new SparePartWithQuantity(part, newQty));
                }
            } else {
                // Запчасть была в заказе — проверяем изменение количества
                double origQty = originalQtyMap.get(partId);
                double diff = newQty - origQty;
                if (diff > 0) {
                    // Количество увеличилось — списать разницу
                    SparePart part = tempParts.stream()
                        .filter(p -> p.getId() == partId)
                        .findFirst().orElse(null);
                    if (part != null) {
                        addedParts.add(new SparePartWithQuantity(part, diff));
                    }
                } else if (diff < 0) {
                    // Количество уменьшилось — вернуть разницу
                    SparePart part = tempParts.stream()
                        .filter(p -> p.getId() == partId)
                        .findFirst().orElse(null);
                    if (part != null) {
                        removedParts.add(new SparePartWithQuantity(part, Math.abs(diff)));
                    }
                }
                // Если diff == 0 — изменений нет, ничего не делаем
            }
        }
        
        // Находим полностью удалённые запчасти
        for (Map.Entry<Integer, Double> entry : originalQtyMap.entrySet()) {
            int partId = entry.getKey();
            if (!newQtyMap.containsKey(partId)) {
                // Полностью удалена — вернуть весь объём
                SparePart part = order.getSpareParts().stream()
                    .filter(p -> p.getId() == partId)
                    .findFirst().orElse(null);
                if (part != null) {
                    removedParts.add(new SparePartWithQuantity(part, entry.getValue()));
                }
            }
        }
        
        // ====== СОХРАНЯЕМ КЛИЕНТА ======
        Client selectedClient = clientCombo.getValue();
        if (selectedClient != null) {
            order.setClient(selectedClient);
            order.setClientId(selectedClient.getId());
        }

        // ====== СОХРАНЯЕМ АВТОМОБИЛЬ ======
        Car selectedCar = carCombo.getValue();
        if (selectedCar != null) {
            order.setCarModel(selectedCar.getCarModel());
            order.setCarNumber(selectedCar.getCarNumber());
        } else if (selectedClient != null && selectedClient.getCars() != null && !selectedClient.getCars().isEmpty()) {
            order.setCarModel(selectedClient.getCars().get(0).getCarModel());
            order.setCarNumber(selectedClient.getCars().get(0).getCarNumber());
        }

        // ====== ОЧИЩАЕМ СТАРЫЕ УСЛУГИ ======
        while (order.getServices().size() > 0) {
            order.removeService(0);
        }
        for (int i = 0; i < tempServices.size(); i++) {
            String serviceName = tempServices.get(i);
            Service service = DataStore.getServiceByName(serviceName);
            int serviceId = (service != null) ? service.getId() : 0;
            order.addService(serviceId, serviceName, tempServicePrices.get(i));
        }

        // ====== ОЧИЩАЕМ СТАРЫЕ ЗАПЧАСТИ ======
        while (order.getSpareParts().size() > 0) {
            order.removeSparePart(0);
        }

        // ====== ДОБАВЛЯЕМ НОВЫЕ ЗАПЧАСТИ ======
        for (int i = 0; i < tempParts.size(); i++) {
            order.addSparePart(tempParts.get(i), tempPartQuantities.get(i));
        }

        // ====== СОХРАНЯЕМ ПРОБЕГ ======
        String mileageText = mileageField.getText().trim();
        int mileage = mileageText.isEmpty() ? 0 : Integer.parseInt(mileageText);
        order.setMileage(mileage);

        // ====== ОБРАБОТКА ЗАПЧАСТЕЙ (РЕЗЕРВИРОВАНИЕ) ======
        // 1. Снимаем резерв с удалённых запчастей
        for (SparePartWithQuantity spw : removedParts) {
            SparePart part = spw.part;
            double qty = spw.quantity;
            SparePart current = DataStore.getSparePartById(part.getId());
            if (current != null) {
                current.unreserve(qty);
                logger.info("Снят резерв (удаление из заказа): {} -{}", part.getName(), qty);
            }
        }
        
        // 2. Резервируем добавленные запчасти
        for (SparePartWithQuantity spw : addedParts) {
            SparePart part = spw.part;
            double qty = spw.quantity;
            SparePart current = DataStore.getSparePartById(part.getId());
            if (current != null) {
                if (current.reserve(qty)) {
                    logger.info("Резервирование (добавление в заказ): {} +{}", part.getName(), qty);
                } else {
                    logger.warn("Не удалось зарезервировать «{}» для заказа {}: нужно {}, доступно {}", 
                            part.getName(), order.getId(), qty, current.getAvailableStock());
                }
            }
        }
        
        // 3. Проверяем минимальные остатки
        DataStore.checkMinStockLevels();

        // ====== СОХРАНЯЕМ ЗАКАЗ ======
        DataStore.updateOrder(order);

        // ====== ОБРАБОТКА ЗАПИСИ В КАЛЕНДАРЕ ======
        if (hasAppointmentCheck.isSelected()) {
            LocalDate selectedDate = datePicker.getValue();
            String selectedTime = timeCombo.getValue();
            String selectedMaster = masterCombo.getValue();
            String serviceName = tempServices.isEmpty() ? "Консультация" : tempServices.get(0);
            String dateStr = DateUtils.formatDateForDB(selectedDate);
            String timeStr = selectedTime;
            String master = selectedMaster;

            // Находим ID услуги для записи в календарь
            Service appointmentService = DataStore.getServiceByName(serviceName);
            int appointmentServiceId = (appointmentService != null) ? appointmentService.getId() : 0;

            if (existingAppointment != null) {
                // Обновляем существующую запись
                existingAppointment.setDate(dateStr);
                existingAppointment.setTime(timeStr);
                existingAppointment.setMasterName(master);
                existingAppointment.setServiceName(serviceName);
                existingAppointment.setServiceId(appointmentServiceId);
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
                newAppointment.setServiceId(appointmentServiceId);
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

    /**
     * Внутренний класс для хранения запчасти с количеством.
     */
    private static class SparePartWithQuantity {
        SparePart part;
        double quantity;
        
        SparePartWithQuantity(SparePart part, double quantity) {
            this.part = part;
            this.quantity = quantity;
        }
    }

    private static void cancelChanges() {
        // При отмене не нужно ничего возвращать — запчасти не списывались при редактировании.
        // Списание/возврат происходит только при сохранении.
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
        
        WindowStateManager.getInstance().restoreWindowState("editOrderServicePartsDialog", stage);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-font-size: 12px;");

        Label titleLabel = new Label("Выберите запчасти, которые нужно добавить:");

        // VBox для чекбоксов
        VBox checkboxesVBox = new VBox(10);
        checkboxesVBox.setPadding(new Insets(10));
        checkboxesVBox.setPrefHeight(250);

        // Храним состояния чекбоксов
        ObservableMap<AutoAddSparePartService.SparePartWithQuantity, CheckBox> checkBoxMap = FXCollections.observableHashMap();
        
        // Создаём чекбоксы для каждой запчасти
        for (AutoAddSparePartService.SparePartWithQuantity partInfo : relatedParts) {
            SparePart part = partInfo.getSparePart();
            double defaultQty = partInfo.getQuantity();
            
            HBox hBox = new HBox(10);
            hBox.setAlignment(Pos.CENTER_LEFT);
            
            CheckBox checkBox = new CheckBox(part.getName() + " (доступно: " + (int)part.getAvailableStock() + " / всего: " + (int)part.getStock() + ")");
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
                } catch (NumberFormatException ex) { logger.error("Invalid number format", ex); }
            });

            plusBtn.setOnAction(evt -> {
                try {
                    int currentQty = Integer.parseInt(qtyField.getText());
                    if (currentQty < (int)part.getAvailableStock()) {
                        qtyField.setText(String.valueOf(currentQty + 1));
                    }
                } catch (NumberFormatException ex) { logger.error("Invalid number format", ex); }
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

        // Кнопки
        Button confirmBtn = new Button("Подтвердить");

        Button skipBtn = new Button("Пропустить");

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
        stage.setOnHidden(e -> {
            WindowStateManager.getInstance().saveWindowState("editOrderServicePartsDialog", stage);
        });
        stage.showAndWait();

        return result[0]; // Возвращаем результат
    }
}