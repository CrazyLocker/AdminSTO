package com.autoservice.dialogs;

import com.autoservice.*;
import com.autoservice.controllers.ServicePanelController;
import com.autoservice.controllers.SparePartPanelController;
import com.autoservice.controllers.StockPanelController;
import com.autoservice.controllers.OrderController;
import com.autoservice.services.AutoAddSparePartService;
import com.autoservice.utils.ServicePartHelper;
import com.autoservice.utils.DialogValidator;
import com.autoservice.utils.ValidationErrorIndicator;
import com.autoservice.utils.ValidationUtils;
import com.autoservice.utils.TooltipHelper;
import com.autoservice.utils.IconHelper;
import com.autoservice.utils.StockHelper;
import com.autoservice.services.WindowStateManager;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Диалог создания нового заказа: выбор клиента и автомобиля,
 * услуг, запчастей, мастера и времени записи.
 */
public class CreateOrderDialog {

    /**
     * Прежний вид диалога выбора запчастей: все запчасти отмечены, список без
     * рамки, ввод количества ограничивается остатком на складе.
     */
    private static final ServiceSparePartsDialog.Options SPARE_PARTS_DIALOG_OPTIONS =
            new ServiceSparePartsDialog.Options(true, false, true);

    // TIME_SLOTS и MASTERS перенесены в AppConstants

    /**
     * Асинхронное создание заказа (для тестирования).
     * Возвращает CompletableFuture, который завершается при создании заказа.
     * Результат содержит созданный WorkOrder.
     * @return future, завершающийся результатом диалога
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
                    setText(name);
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
                    setText(name);
                }
            }
        });
        TooltipHelper.setToolTip(clientCombo, "Выберите клиента из списка");

        // ============================================================
        // 1.1. АВТОМОБИЛЬ
        // ============================================================
        Label carLabel = new Label("Автомобиль:");
        carLabel.setStyle("-fx-font-weight: bold;");

        ComboBox<Car> carCombo = new ComboBox<>();
        carCombo.setPromptText("Выберите автомобиль");
        carCombo.setPrefWidth(350);
        carCombo.setDisable(true);

        clientCombo.setOnAction(e -> {
            Client selectedClient = clientCombo.getValue();
            if (selectedClient != null && selectedClient.getCars() != null && !selectedClient.getCars().isEmpty()) {
                carCombo.getItems().setAll(selectedClient.getCars());
                carCombo.setPromptText("Выберите автомобиль");
                carCombo.setDisable(false);
                carCombo.setValue(null);
            } else {
                carCombo.getItems().clear();
                carCombo.setDisable(true);
                carCombo.setValue(null);
            }
        });

        TooltipHelper.setToolTip(carCombo, "Выберите автомобиль клиента");

        // ============================================================
        // 1.5. ПРОБЕГ
        // ============================================================
        Label mileageLabel = new Label("Пробег (км):");
        mileageLabel.setStyle("-fx-font-weight: bold;");

        TextField mileageField = new TextField();
        mileageField.setPrefWidth(150);
        mileageField.setTextFormatter(new TextFormatter<>(change -> {
            String text = change.getText();
            if (text.isEmpty()) return change;
            if (!text.matches("[0-9]+")) return null;
            String newText = change.getControlNewText();
            if (newText.length() > 6) return null;
            return change;
        }));

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

                // ====== ВЫБОР ЗАПЧАСТЕЙ ЧЕРЕЗ ДИАЛОГ ======
                List<AutoAddSparePartService.SparePartWithQuantity> relatedParts = AutoAddSparePartService.getSparePartsByService(svc.getName());
                
                if (!relatedParts.isEmpty()) {
                    List<AutoAddSparePartService.SparePartWithQuantity> selectedParts =
                            ServiceSparePartsDialog.show(svc.getName(), relatedParts, SPARE_PARTS_DIALOG_OPTIONS);
                    
                    if (selectedParts != null) {
                        for (AutoAddSparePartService.SparePartWithQuantity partInfo : selectedParts) {
                            SparePart part = partInfo.getSparePart();
                            double qty = partInfo.getQuantity();
                            
                        if (StockHelper.reserve(part, qty)) {
                            partsListView.getItems().add(part.getName() + " x" + (int)qty);
                            tempParts.add(part);
                            tempPartQuantities.add(qty);
                            updateTotal.run();
                        } else {
                                showAlert("Недостаточно запчастей на складе: " + part.getName());
                            }
                        }
                    }
                }
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
                carLabel, carCombo,
                mileageLabel, mileageField,
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

            DialogValidator validator = new DialogValidator();

            Client selectedClient = clientCombo.getValue();
            if (selectedClient == null) {
                ValidationErrorIndicator.showError(clientCombo, "Выберите клиента");
                validator.add("Выберите клиента");
            }

            if (tempServices.isEmpty() && tempParts.isEmpty()) {
                validator.add("Добавьте хотя бы одну услугу или запчасть");
            }

            if (createAppointmentCheck.isSelected()) {
                if (datePicker.getValue() == null) {
                    ValidationErrorIndicator.showError(datePicker, "Выберите дату записи");
                    validator.add("Выберите дату записи");
                }
                if (timeCombo.getValue() == null) {
                    ValidationErrorIndicator.showError(timeCombo, "Выберите время записи");
                    validator.add("Выберите время записи");
                }
                if (masterCombo.getValue() == null) {
                    ValidationErrorIndicator.showError(masterCombo, "Выберите мастера");
                    validator.add("Выберите мастера");
                }
            }

            ValidationUtils.ValidationError mileageError =
                    ValidationUtils.validateIntegerText(mileageField.getText(), "Пробег", false);
            if (mileageError != null) {
                ValidationErrorIndicator.showError(mileageField, mileageError.getErrorMessage());
                validator.add(mileageError);
            }

            // ====== ПОВТОРНАЯ ПРОВЕРКА ОСТАТКА ПЕРЕД СОХРАНЕНИЕМ ======
            for (int i = 0; i < tempParts.size(); i++) {
                SparePart part = tempParts.get(i);
                double qty = tempPartQuantities.get(i);
                SparePart stockPart = DataStore.getSparePartById(part.getId());
                if (stockPart == null) {
                    validator.add("Запчасть не найдена в базе: " + part.getName());
                    continue;
                }
                int currentStock = (int) stockPart.getStock();
                if (qty > currentStock) {
                    validator.add("Недостаточно запчастей на складе: " + part.getName()
                            + " (в наличии: " + currentStock + ")");
                }
            }

            if (validator.hasErrors()) {
                showAlert(validator.getMessage());
                return;
            }

            // ====== ПРОВЕРКА ВЫХОДНЫХ ДНЕЙ ======
            if (createAppointmentCheck.isSelected() && DateUtils.isWeekend(datePicker.getValue())) {
                Alert weekendAlert = new Alert(Alert.AlertType.CONFIRMATION);
                weekendAlert.setTitle("Подтверждение записи");
                weekendAlert.setHeaderText("Выбран выходной день!");
                weekendAlert.setContentText("Запись в выходной день (" + datePicker.getValue().format(java.time.format.DateTimeFormatter.ofPattern("EEEE", new java.util.Locale.Builder().setLanguage("ru").build())) + ") может быть ограничена.\n\nПродолжить?");
                weekendAlert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

                if (weekendAlert.showAndWait().orElse(ButtonType.NO) == ButtonType.NO) {
                    return; // Отмена при нажатии "Нет"
                }
            }

            // ====== СОЗДАЁМ ЗАКАЗ ======
            WorkOrder order = new WorkOrder();
            order.setClient(selectedClient);
            order.setClientId(selectedClient.getId());
            
            // Если выбран автомобиль, используем его, иначе — первый из списка клиента
            Car selectedCar = carCombo.getValue();
            if (selectedCar != null) {
                order.setCarModel(selectedCar.getCarModel());
                order.setCarNumber(selectedCar.getCarNumber());
            } else if (selectedClient.getCars() != null && !selectedClient.getCars().isEmpty()) {
                order.setCarModel(selectedClient.getCars().get(0).getCarModel());
                order.setCarNumber(selectedClient.getCars().get(0).getCarNumber());
            }
            
            order.setStatus(WorkOrder.STATUS_NEW);

            // ====== УСТАНАВЛИВАЕМ ПРОБЕГ ======
            String mileageText = mileageField.getText().trim();
            if (!mileageText.isEmpty()) {
                try {
                    order.setMileage(Integer.parseInt(mileageText));
                } catch (NumberFormatException ex) {
                    // игнорируем некорректный ввод
                }
            }

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
            }

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
                StockHelper.restore(tempParts.get(i), tempPartQuantities.get(i));
            }
            stage.close();
            future.complete(new DialogResult(DialogResult.Action.CANCEL));
        });

        stage.showAndWait(); // Используем showAndWait вместо show + join()
        return future;
    }

    /**
     * Синхронно открыть диалог создания заказа и дождаться его завершения.
     */
    public static void show() {
        showAsync().join();
    }

    private static void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.showAndWait();
    }

}
