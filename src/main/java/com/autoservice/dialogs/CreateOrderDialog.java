package com.autoservice.dialogs;

import com.autoservice.*;
import com.autoservice.controllers.ServicePanelController;
import com.autoservice.controllers.SparePartPanelController;
import com.autoservice.controllers.StockPanelController;
import com.autoservice.controllers.OrderController;
import com.autoservice.services.AutoAddSparePartService;
import com.autoservice.utils.OilHelper;
import com.autoservice.utils.ValidationErrorIndicator;
import com.autoservice.utils.ValidationUtils;
import com.autoservice.utils.TooltipHelper;
import com.autoservice.utils.IconHelper;
import com.autoservice.services.WindowStateManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class CreateOrderDialog {

    // TIME_SLOTS и MASTERS перенесены в AppConstants

    /**
     * Асинхронное создание заказа (для тестирования).
     * Возвращает CompletableFuture, который завершается при создании заказа.
     * Результат содержит созданный WorkOrder.
     */
    public static CompletableFuture<DialogResult> showAsync() {
        CompletableFuture<DialogResult> future = new CompletableFuture<>();
        
        Stage stage = new Stage();
        stage.setTitle("Новый заказ");
        stage.setMinWidth(650);
        stage.setMinHeight(650);
        stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
        
        // Восстановление состояния диалога
        WindowStateManager.getInstance().restoreWindowState("createOrderDialog", stage);

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));

        // ============================================================
        // 1. КЛИЕНТ
        // ============================================================
        Label clientLabel = new Label("Клиент:");
        clientLabel.setStyle("-fx-font-weight: bold;");

        ComboBox<Client> clientCombo = new ComboBox<>(FXCollections.observableArrayList(DataStore.getClients()));
        clientCombo.setPromptText("Выберите клиента");
        clientCombo.setPrefWidth(350);

        clientCombo.setCellFactory(listView -> new ListCell<Client>() {
            @Override
            protected void updateItem(Client item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String name = item.getLastName() != null && !item.getLastName().isEmpty()
                            ? item.getLastName() + " " + item.getName()
                            : item.getName();
                    String car = item.getCarModel() != null && !item.getCarModel().isEmpty()
                            ? " (" + item.getCarModel() + ")"
                            : "";
                    setText(name + car);
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
                    String name = item.getLastName() != null && !item.getLastName().isEmpty()
                            ? item.getLastName() + " " + item.getName()
                            : item.getName();
                    String car = item.getCarModel() != null && !item.getCarModel().isEmpty()
                            ? " (" + item.getCarModel() + ")"
                            : "";
                    setText(name + car);
                }
            }
        });
        TooltipHelper.setToolTip(clientCombo, "Выберите клиента из списка");

        // ============================================================
        // 2. ЗАПИСЬ
        // ============================================================
        Label appointmentLabel = new Label("Запись на сервис:");
        appointmentLabel.setStyle("-fx-font-weight: bold;");

        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setPrefWidth(150);

        ComboBox<String> timeCombo = new ComboBox<>(FXCollections.observableArrayList(AppConstants.TIME_SLOTS));
        timeCombo.setPromptText("Время");
        timeCombo.setPrefWidth(80);

        ComboBox<String> masterCombo = new ComboBox<>(FXCollections.observableArrayList(AppConstants.MASTERS));
        masterCombo.setPromptText("Мастер");
        masterCombo.setPrefWidth(120);
        TooltipHelper.setToolTip(masterCombo, "Выберите мастера сервиса");

        CheckBox createAppointmentCheck = new CheckBox("Создать запись в календаре");
        createAppointmentCheck.setSelected(true);

        HBox appointmentBox = new HBox(10);
        appointmentBox.setAlignment(Pos.CENTER_LEFT);
        appointmentBox.getChildren().addAll(
                new Label("Дата:"), datePicker,
                new Label("Время:"), timeCombo,
                new Label("Мастер:"), masterCombo
        );

        // ============================================================
        // 3. УСЛУГИ
        // ============================================================
        Label servicesHeader = new Label("УСЛУГИ");
        servicesHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        ListView<String> servicesListView = new ListView<>();
        servicesListView.setPrefHeight(120);

        ComboBox<Service> serviceCombo = new ComboBox<>(FXCollections.observableArrayList(DataStore.getServices()));
        serviceCombo.setPromptText("Выберите услугу");
        serviceCombo.setPrefWidth(350);

        Button addServiceBtn = new Button("Добавить");
        addServiceBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        Button removeServiceBtn = new Button("Удалить");
        removeServiceBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");

        HBox serviceAddBox = new HBox(8, serviceCombo, addServiceBtn, removeServiceBtn);
        serviceAddBox.setAlignment(Pos.CENTER_LEFT);

        // ============================================================
        // 4. ЗАПЧАСТИ
        // ============================================================
        Label partsHeader = new Label("ЗАПЧАСТИ");
        partsHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        ListView<String> partsListView = new ListView<>();
        partsListView.setPrefHeight(120);

        ComboBox<SparePart> partCombo = new ComboBox<>(FXCollections.observableArrayList(DataStore.getSpareParts()));
        partCombo.setPromptText("Выберите запчасть");
        partCombo.setPrefWidth(350);

        Button addPartBtn = new Button("Добавить");
        addPartBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        Button removePartBtn = new Button("Удалить");
        removePartBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");

        HBox partAddBox = new HBox(8, partCombo, addPartBtn, removePartBtn);
        partAddBox.setAlignment(Pos.CENTER_LEFT);

        // ============================================================
        // 5. ВРЕМЕННЫЕ СПИСКИ
        // ============================================================
        List<String> tempServices = new ArrayList<>();
        List<Double> tempServicePrices = new ArrayList<>();
        List<SparePart> tempParts = new ArrayList<>();
        List<Double> tempPartQuantities = new ArrayList<>();

        // ============================================================
        // 6. ИТОГО
        // ============================================================
        Label totalLabel = new Label("Итого: 0 руб.");
        totalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Runnable updateTotal = () -> {
            double total = 0;
            for (Double price : tempServicePrices) total += price;
            for (int i = 0; i < tempParts.size(); i++) {
                total += tempParts.get(i).getRetailPrice() * tempPartQuantities.get(i);
            }
            totalLabel.setText(String.format("Итого: %.0f руб.", total));
        };

        // ============================================================
        // 7. ОБРАБОТЧИКИ
        // ============================================================
        addServiceBtn.setOnAction(e -> {
            Service svc = serviceCombo.getValue();
            if (svc != null) {
                servicesListView.getItems().add(svc.getName());
                tempServices.add(svc.getName());
                tempServicePrices.add(svc.getPrice());
                updateTotal.run();
                serviceCombo.setValue(null);
            }
        });

        removeServiceBtn.setOnAction(e -> {
            int idx = servicesListView.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                servicesListView.getItems().remove(idx);
                tempServices.remove(idx);
                tempServicePrices.remove(idx);
                updateTotal.run();
            }
        });

        addPartBtn.setOnAction(e -> {
            SparePart part = partCombo.getValue();
            if (part != null) {
                // Проверяем, есть ли уже такая запчасть
                boolean found = false;
                for (int i = 0; i < tempParts.size(); i++) {
                    if (tempParts.get(i).getId() == part.getId()) {
                        tempPartQuantities.set(i, tempPartQuantities.get(i) + 1);
                        partsListView.getItems().set(i, part.getName() + " x" + tempPartQuantities.get(i));
                        updateTotal.run();
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    partsListView.getItems().add(part.getName() + " x1");
                    tempParts.add(part);
                    tempPartQuantities.add(1.0);
                    updateTotal.run();
                }
                partCombo.setValue(null);
            }
        });

        removePartBtn.setOnAction(e -> {
            int idx = partsListView.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                SparePart part = tempParts.get(idx);
                double qty = tempPartQuantities.get(idx);
                if (qty > 1) {
                    tempPartQuantities.set(idx, qty - 1);
                    partsListView.getItems().set(idx, part.getName() + " x" + (qty - 1));
                } else {
                    tempParts.remove(idx);
                    tempPartQuantities.remove(idx);
                    partsListView.getItems().remove(idx);
                }
                updateTotal.run();
            }
        });

        // ============================================================
        // 8. КНОПКИ
        // ============================================================
        Button saveBtn = new Button("Создать заказ");
        saveBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        Button cancelBtn = new Button("Отмена");
        cancelBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        HBox btnBox = new HBox(10, saveBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER);

        // ============================================================
        // 9. СБОРКА ИНТЕРФЕЙСА
        // ============================================================
        root.getChildren().addAll(
                clientLabel, clientCombo,
                new Separator(),
                appointmentLabel, appointmentBox, createAppointmentCheck,
                new Separator(),
                servicesHeader, servicesListView, serviceAddBox,
                new Separator(),
                partsHeader, partsListView, partAddBox,
                new Separator(),
                totalLabel, btnBox
        );

        Scene scene = new Scene(root);
        stage.setScene(scene);

        // ============================================================
        // 10. ДЕЙСТВИЯ
        // ============================================================
        saveBtn.setOnAction(e -> {
            // Очистка ошибок валидации
            ValidationErrorIndicator.clearAllErrors(root);
            
            boolean isValid = true;
            
            // Валидация обязательных полей
            if (clientCombo.getValue() == null) {
                ValidationErrorIndicator.showError(clientCombo, "Выберите клиента");
                isValid = false;
            }
            
            if (tempServices.isEmpty() && tempParts.isEmpty()) {
                showAlert("Добавьте услугу или запчасть");
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
                    break;
                }
            }
            
            if (hasStockIssue) {
                return;
            }
            
            if (!isValid) {
                return;
            }

            // ====== ПРОВЕРКА ВЫХОДНЫХ ДНЕЙ ======
            if (createAppointmentCheck.isSelected() && datePicker.getValue() != null) {
                if (DateUtils.isWeekend(datePicker.getValue())) {
                    Alert weekendAlert = new Alert(Alert.AlertType.CONFIRMATION);
                    weekendAlert.setTitle("Подтверждение записи");
                    weekendAlert.setHeaderText("Выбран выходной день!");
                    weekendAlert.setContentText("Запись в выходной день (" + datePicker.getValue().format(java.time.format.DateTimeFormatter.ofPattern("EEEE", new java.util.Locale("ru"))) + ") может быть ограничена.\n\nПродолжить?");
                    weekendAlert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
                    
                    if (weekendAlert.showAndWait().orElse(ButtonType.NO) == ButtonType.NO) {
                        return; // Отмена при нажатии "Нет"
                    }
                }
            }

            if (createAppointmentCheck.isSelected()) {
                if (datePicker.getValue() == null) {
                    ValidationErrorIndicator.showError(datePicker, "Выберите дату записи");
                    return;
                }
                if (timeCombo.getValue() == null) {
                    ValidationErrorIndicator.showError(timeCombo, "Выберите время записи");
                    return;
                }
                if (masterCombo.getValue() == null) {
                    ValidationErrorIndicator.showError(masterCombo, "Выберите мастера");
                    return;
                }
            }

            // ====== СОЗДАЁМ ЗАКАЗ ======
            WorkOrder order = new WorkOrder(clientCombo.getValue());
            order.setStatus(WorkOrder.STATUS_NEW);

            // ====== ДОБАВЛЯЕМ УСЛУГИ ======
            for (int i = 0; i < tempServices.size(); i++) {
                String svcName = tempServices.get(i);
                Service svc = DataStore.getServiceByName(svcName);
                int svcId = (svc != null) ? svc.getId() : 0;
                order.addService(svcId, svcName, tempServicePrices.get(i));
            }

            // ====== ДОБАВЛЯЕМ РУЧНЫЕ ЗАПЧАСТИ ======
            for (int i = 0; i < tempParts.size(); i++) {
                order.addSparePart(tempParts.get(i), tempPartQuantities.get(i));
                DataStore.updateSparePartStock(tempParts.get(i), tempParts.get(i).getStock());
            }

            // ====== 🆕 АВТОМАТИЧЕСКИ ДОБАВЛЯЕМ МАСЛО И РАСХОДНИКИ ======
            List<String> serviceNames = new ArrayList<>(tempServices);
            OilHelper.addOilAndPartsToOrder(order, serviceNames);

            // ====== СОХРАНЯЕМ ЗАКАЗ ======
            DataStore.addOrder(order);

            // ====== ЗАПИСЬ В КАЛЕНДАРЬ ======
            if (createAppointmentCheck.isSelected()) {
                String dateStr = DateUtils.formatDateForDB(datePicker.getValue());
                String timeStr = timeCombo.getValue();
                String master = masterCombo.getValue();
                String serviceName = tempServices.isEmpty() ? "Консультация" : tempServices.get(0);

                List<Appointment> existing = DataStore.getAppointmentsByDate(dateStr);
                boolean isFree = true;
                for (Appointment a : existing) {
                    if (a.getTime().equals(timeStr) && a.getMasterName().equals(master)) {
                        isFree = false;
                        break;
                    }
                }

                if (isFree) {
                    Service apptService = DataStore.getServiceByName(serviceName);
                    int apptServiceId = (apptService != null) ? apptService.getId() : 0;

                    Appointment appointment = new Appointment(
                            clientCombo.getValue(),
                            master,
                            serviceName,
                            dateStr,
                            timeStr
                    );
                    appointment.setServiceId(apptServiceId);
                    appointment.setOrderId(order.getId());
                    DataStore.addAppointment(appointment);
                } else {
                    showAlert("Выбранное время уже занято! Заказ создан, но запись не добавлена.");
                }
            }

            OrderController.refreshTable();
            ServicePanelController.refreshTable();
            SparePartPanelController.refreshTable();
            StockPanelController.refreshTable();
            
            stage.close();
            future.complete(new DialogResult(DialogResult.Action.OK, Map.of("order", order)));
        });

        cancelBtn.setOnAction(e -> {
            // Восстановление остатков запчастей
            for (int i = 0; i < tempParts.size(); i++) {
                tempParts.get(i).setStock(tempParts.get(i).getStock() + tempPartQuantities.get(i));
            }
            stage.close();
            future.complete(new DialogResult(DialogResult.Action.CANCEL));
        });
        
        stage.setOnHiding(e -> {
            // Сохранение состояния диалога при закрытии
            WindowStateManager.getInstance().saveWindowState("createOrderDialog", stage);
            if (!future.isDone()) {
                future.complete(new DialogResult(DialogResult.Action.CANCEL));
            }
        });

        stage.show(); // НЕ showAndWait!
        return future;
    }

    /**
     * Синхронная версия для обратной совместимости.
     */
    public static void show() {
        showAsync().join();
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
            checkBox.setSelected(true); // По умолчанию выбрано
            
            // Поле для ввода количества
            TextField qtyField = new TextField(String.valueOf((int)defaultQty));
            qtyField.setPrefWidth(60);
            qtyField.setDisable(false);
            
            // Валидация ввода количества
            qtyField.textProperty().addListener((obs, oldValue, newValue) -> {
                try {
                    int qty = Integer.parseInt(newValue);
                    int maxQty = (int)part.getStock();
                    if (qty < 1) {
                        qtyField.setText("1");
                    } else if (qty > maxQty) {
                        qtyField.setText(String.valueOf(maxQty));
                    }
                } catch (NumberFormatException ex) {
                    qtyField.setText(String.valueOf(defaultQty));
                }
            });
            
            // Кнопка +/- для изменения количества
            HBox qtyControls = new HBox(5);
            Button minusBtn = new Button("-");
            minusBtn.setPrefWidth(25);
            Button plusBtn = new Button("+");
            plusBtn.setPrefWidth(25);
            
            minusBtn.setOnAction(e -> {
                try {
                    int currentQty = Integer.parseInt(qtyField.getText());
                    if (currentQty > 1) {
                        qtyField.setText(String.valueOf(currentQty - 1));
                    }
                } catch (NumberFormatException ex) {}
            });
            
            plusBtn.setOnAction(e -> {
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
        confirmBtn.setOnAction(e -> {
            List<AutoAddSparePartService.SparePartWithQuantity> selectedParts = new ArrayList<>();
            
            for (int i = 0; i < relatedParts.size(); i++) {
                AutoAddSparePartService.SparePartWithQuantity partInfo = relatedParts.get(i);
                HBox hBox = (HBox)checkboxesVBox.getChildren().get(i);
                CheckBox checkBox = checkBoxMap.get(partInfo);
                TextField qtyField = (TextField)hBox.getChildren().get(1);
                
                if (checkBox != null && checkBox.isSelected()) {
                    try {
                        int qty = Integer.parseInt(qtyField.getText());
                        int maxQty = (int)partInfo.getSparePart().getStock();
                        if (qty < 1) {
                            showAlert("Минимальное количество: 1");
                            return;
                        }
                        if (qty > maxQty) {
                            showAlert("Недостаточно запчастей на складе: " + partInfo.getSparePart().getName() + " (в наличии: " + maxQty + ")");
                            return;
                        }
                        if (qty > 0) {
                            AutoAddSparePartService.SparePartWithQuantity selected = partInfo.copy();
                            selected.setQuantity(qty);
                            selectedParts.add(selected);
                        }
                    } catch (NumberFormatException ex) {
                        showAlert("Некорректное количество");
                        return;
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
        skipBtn.setOnAction(e -> {
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