package com.autoservice.dialogs;

import com.autoservice.*;
import com.autoservice.controllers.DictionaryController;
import com.autoservice.controllers.OrderController;
import com.autoservice.services.AutoAddSparePartService;
import com.autoservice.services.SettingService;
import com.autoservice.utils.OilHelper;
import com.autoservice.utils.IconHelper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CreateOrderDialog {

    private static final String[] TIME_SLOTS = {
            "08:00", "08:30", "09:00", "09:30", "10:00", "10:30", "11:00", "11:30",
            "12:00", "12:30", "13:00", "13:30", "14:00", "14:30", "15:00", "15:30",
            "16:00", "16:30", "17:00", "17:30", "18:00", "18:30", "19:00", "19:30", "20:00"
    };

    private static final String[] MASTERS = {"Иван", "Петр", "Сергей", "Антон"};

    public static void show() {
        Stage stage = new Stage();
        stage.setTitle("Новый заказ");
        stage.setMinWidth(650);
        stage.setMinHeight(650);
        stage.initModality(javafx.stage.Modality.WINDOW_MODAL);

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

        // ============================================================
        // 2. ЗАПИСЬ
        // ============================================================
        Label appointmentLabel = new Label("Запись на сервис:");
        appointmentLabel.setStyle("-fx-font-weight: bold;");

        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setPrefWidth(150);

        ComboBox<String> timeCombo = new ComboBox<>(FXCollections.observableArrayList(TIME_SLOTS));
        timeCombo.setPromptText("Время");
        timeCombo.setPrefWidth(80);

        ComboBox<String> masterCombo = new ComboBox<>(FXCollections.observableArrayList(MASTERS));
        masterCombo.setPromptText("Мастер");
        masterCombo.setPrefWidth(120);

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
        Label servicesHeader = new Label("Услуги:");
        servicesHeader.setStyle("-fx-font-weight: bold;");

        ListView<String> servicesListView = new ListView<>();
        servicesListView.setPrefHeight(100);

        ComboBox<Service> serviceCombo = new ComboBox<>(FXCollections.observableArrayList(DataStore.getServices()));
        serviceCombo.setPromptText("Выберите услугу");
        serviceCombo.setPrefWidth(250);

        TextField servicePriceField = new TextField();
        servicePriceField.setEditable(false);
        servicePriceField.setPrefWidth(80);
        servicePriceField.setPromptText("Цена");

        serviceCombo.setOnAction(e -> {
            Service selected = serviceCombo.getValue();
            if (selected != null) {
                servicePriceField.setText(String.valueOf(selected.getPrice()));
            }
        });

        Button addServiceBtn = new Button("+ Добавить");

        HBox serviceAddBox = new HBox(8, serviceCombo, servicePriceField, addServiceBtn);
        serviceAddBox.setAlignment(Pos.CENTER_LEFT);

        Button removeServiceBtn = new Button("Удалить выбранную");

        // ============================================================
        // 4. ЗАПЧАСТИ
        // ============================================================
        Label partsHeader = new Label("Запчасти:");
        partsHeader.setStyle("-fx-font-weight: bold;");

        ListView<String> partsListView = new ListView<>();
        partsListView.setPrefHeight(100);

        ComboBox<SparePart> partCombo = new ComboBox<>(FXCollections.observableArrayList(DataStore.getSpareParts()));
        partCombo.setPromptText("Выберите запчасть");
        partCombo.setPrefWidth(200);

        TextField partPriceField = new TextField();
        partPriceField.setEditable(false);
        partPriceField.setPrefWidth(80);
        partPriceField.setPromptText("Цена");

        TextField partStockField = new TextField();
        partStockField.setEditable(false);
        partStockField.setPrefWidth(50);
        partStockField.setPromptText("Ост.");

        TextField partQtyField = new TextField("1");
        partQtyField.setPrefWidth(50);

        partCombo.setOnAction(e -> {
            SparePart selected = partCombo.getValue();
            if (selected != null) {
                partPriceField.setText(String.valueOf(selected.getRetailPrice()));
                partStockField.setText(String.valueOf(selected.getStock()));
            }
        });

        Button addPartBtn = new Button("+ Добавить");

        HBox partAddBox = new HBox(8, partCombo, partPriceField, new Label("Ост:"), partStockField,
                new Label("Кол:"), partQtyField, addPartBtn);
        partAddBox.setAlignment(Pos.CENTER_LEFT);

        Button removePartBtn = new Button("Удалить выбранную");

        // ============================================================
        // 5. ВРЕМЕННЫЕ СПИСКИ
        // ============================================================
        List<String> tempServices = new ArrayList<>();
        List<Double> tempServicePrices = new ArrayList<>();
        List<SparePart> tempParts = new ArrayList<>();
        List<Integer> tempPartQuantities = new ArrayList<>();

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
            totalLabel.setText("Итого: " + String.format("%.2f", total) + " руб.");
        };

        // ============================================================
        // 7. ЛОГИКА ДОБАВЛЕНИЯ/УДАЛЕНИЯ
        // ============================================================
        addServiceBtn.setOnAction(e -> {
            Service selected = serviceCombo.getValue();
            if (selected == null) {
                showAlert("Выберите услугу");
                return;
            }

            // ====== АВТОМАТИЧЕСКОЕ ДОБАВЛЕНИЕ ЗАПЧАСТЕЙ ======
            if (SettingService.isAutoAddSparePartsEnabled()) {
                AutoAddSparePartService.SparePartWithQuantity partInfo = showAutoAddPartsDialog(selected.getName());
                if (partInfo != null) {
                    SparePart selectedPart = partInfo.getSparePart();
                    int qty = partInfo.getQuantity();

                    if (qty > 0 && qty <= selectedPart.getStock()) {
                        tempParts.add(selectedPart);
                        tempPartQuantities.add(qty);
                        selectedPart.setStock(selectedPart.getStock() - qty);
                        partsListView.getItems().add((tempParts.size()) + ". " + selectedPart.getName() + " x" + qty + " = " + (selectedPart.getRetailPrice() * qty) + " руб.");
                        partStockField.setText(String.valueOf(selectedPart.getStock()));
                        updateTotal.run();
                    } else {
                        showAlert("Недостаточно запчастей на складе");
                    }
                }
            }

            tempServices.add(selected.getName());
            tempServicePrices.add(selected.getPrice());
            servicesListView.getItems().add((tempServices.size()) + ". " + selected.getName() + " — " + selected.getPrice() + " руб.");
            serviceCombo.setValue(null);
            servicePriceField.clear();
            updateTotal.run();
        });

        removeServiceBtn.setOnAction(e -> {
            int idx = servicesListView.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < tempServices.size()) {
                tempServices.remove(idx);
                tempServicePrices.remove(idx);
                servicesListView.getItems().clear();
                for (int i = 0; i < tempServices.size(); i++) {
                    servicesListView.getItems().add((i + 1) + ". " + tempServices.get(i) + " — " + tempServicePrices.get(i) + " руб.");
                }
                updateTotal.run();
            } else {
                showAlert("Выберите услугу");
            }
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
            partsListView.getItems().add((tempParts.size()) + ". " + selected.getName() + " x" + qty + " = " + (selected.getRetailPrice() * qty) + " руб.");
            partStockField.setText(String.valueOf(selected.getStock()));
            partCombo.setValue(null);
            partPriceField.clear();
            partQtyField.setText("1");
            updateTotal.run();
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
                    partsListView.getItems().add((i + 1) + ". " + p.getName() + " x" + q + " = " + (p.getRetailPrice() * q) + " руб.");
                }
                partCombo.setItems(FXCollections.observableArrayList(DataStore.getSpareParts()));
                updateTotal.run();
            } else {
                showAlert("Выберите запчасть");
            }
        });

        // ============================================================
        // 8. КНОПКИ СОХРАНЕНИЯ
        // ============================================================
        Button saveBtn = new Button("Создать заказ");
        Button cancelBtn = new Button("Отмена");

        HBox btnBox = new HBox(15, saveBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER);

        // ============================================================
        // 9. СБОРКА ИНТЕРФЕЙСА
        // ============================================================
        root.getChildren().addAll(
                clientLabel, clientCombo,
                new Separator(),
                appointmentLabel, appointmentBox, createAppointmentCheck,
                new Separator(),
                servicesHeader, servicesListView, serviceAddBox, removeServiceBtn,
                new Separator(),
                partsHeader, partsListView, partAddBox, removePartBtn,
                new Separator(),
                totalLabel, btnBox
        );

        Scene scene = new Scene(root);
        stage.setScene(scene);

        // ============================================================
        // 10. ДЕЙСТВИЯ
        // ============================================================
        saveBtn.setOnAction(e -> {
            if (clientCombo.getValue() == null) {
                showAlert("Выберите клиента");
                return;
            }
            if (tempServices.isEmpty() && tempParts.isEmpty()) {
                showAlert("Добавьте услугу или запчасть");
                return;
            }

            if (createAppointmentCheck.isSelected()) {
                if (datePicker.getValue() == null) {
                    showAlert("Выберите дату записи");
                    return;
                }
                if (timeCombo.getValue() == null) {
                    showAlert("Выберите время записи");
                    return;
                }
                if (masterCombo.getValue() == null) {
                    showAlert("Выберите мастера");
                    return;
                }
            }

            // ====== СОЗДАЁМ ЗАКАЗ ======
            WorkOrder order = new WorkOrder(clientCombo.getValue());
            order.setStatus("Новый");

            // ====== ДОБАВЛЯЕМ УСЛУГИ ======
            for (int i = 0; i < tempServices.size(); i++) {
                order.addService(tempServices.get(i), tempServicePrices.get(i));
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
                String dateStr = datePicker.getValue().toString();
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
                    Appointment appointment = new Appointment(
                            clientCombo.getValue(),
                            master,
                            serviceName,
                            dateStr,
                            timeStr
                    );
                    appointment.setOrderId(order.getId());
                    DataStore.addAppointment(appointment);
                } else {
                    showAlert("Выбранное время уже занято! Заказ создан, но запись не добавлена.");
                }
            }

            OrderController.refreshTable();
            DictionaryController.refreshAll();
            stage.close();
        });

        cancelBtn.setOnAction(e -> {
            for (int i = 0; i < tempParts.size(); i++) {
                tempParts.get(i).setStock(tempParts.get(i).getStock() + tempPartQuantities.get(i));
            }
            stage.close();
        });

        stage.showAndWait();
    }

    private static void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.showAndWait();
    }

    // ==================== МЕТОД ДИАЛОГА АВТОМАТИЧЕСКОГО ДОБАВЛЕНИЯ ЗАПЧАСТЕЙ ====================

    /**
     * Показывает диалог с информацией о связанных запчастях для услуги.
     * Возвращает выбранную запчасть с количеством (или null для отмены).
     */
    private static AutoAddSparePartService.SparePartWithQuantity showAutoAddPartsDialog(String serviceName) {
        List<AutoAddSparePartService.SparePartWithQuantity> parts = AutoAddSparePartService.getSparePartsByService(serviceName);

        if (parts.isEmpty()) {
            return null; // Нет связанных запчастей
        }

        // Если не требуется подтверждение - добавляем сразу
        if (!SettingService.isSparePartConfirmationRequired()) {
            // Берем первую запчасть
            return parts.get(0);
        }

        Stage stage = new Stage();
        stage.setTitle("Связанные запчасти");
        stage.setMinWidth(500);
        stage.setMinHeight(400);
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Label titleLabel = new Label("Следующие запчасти будут добавлены автоматически:");
        titleLabel.getStyleClass().add("dialog-title");

        // Таблица запчастей
        TableView<AutoAddSparePartService.SparePartWithQuantity> table = new TableView<>();
        table.setItems(javafx.collections.FXCollections.observableArrayList(parts));
        table.setPrefHeight(200);

        TableColumn<AutoAddSparePartService.SparePartWithQuantity, String> colName = new TableColumn<>("Название");
        colName.setCellValueFactory(cell -> javafx.beans.binding.Bindings.createObjectBinding(() -> cell.getValue().getSparePart().getName()));
        colName.setPrefWidth(200);

        TableColumn<AutoAddSparePartService.SparePartWithQuantity, Integer> colQty = new TableColumn<>("Количество");
        colQty.setCellValueFactory(cell -> javafx.beans.binding.Bindings.createObjectBinding(() -> cell.getValue().getQuantity()));
        colQty.setPrefWidth(80);

        TableColumn<AutoAddSparePartService.SparePartWithQuantity, String> colUnitType = new TableColumn<>("Ед. изм.");
        colUnitType.setCellValueFactory(cell -> javafx.beans.binding.Bindings.createObjectBinding(() -> cell.getValue().getUnitType()));
        colUnitType.setPrefWidth(80);

        TableColumn<AutoAddSparePartService.SparePartWithQuantity, Double> colPrice = new TableColumn<>("Цена");
        colPrice.setCellValueFactory(cell -> javafx.beans.binding.Bindings.createObjectBinding(() -> cell.getValue().getSparePart().getRetailPrice()));
        colPrice.setPrefWidth(80);

        table.getColumns().addAll(colName, colQty, colUnitType, colPrice);

        // Общая стоимость
        Label totalLabel = new Label("Общая стоимость: 0 руб.");
        totalLabel.setStyle("-fx-font-weight: bold;");

        // Выбор количества
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        TextField qtyField = new TextField();
        qtyField.setPrefWidth(80);

        grid.add(new Label("Количество:"), 0, 0);
        grid.add(qtyField, 1, 0);

        // Кнопки
        Button confirmBtn = new Button("Добавить");
        confirmBtn.getStyleClass().add("save-button");

        Button skipBtn = new Button("Пропустить");
        skipBtn.getStyleClass().add("cancel-button");

        HBox btnBox = new HBox(15, confirmBtn, skipBtn);
        btnBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(titleLabel, table, totalLabel, grid, btnBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);

        // Объявляем переменную для результата
        final AutoAddSparePartService.SparePartWithQuantity[] result = {null};

        // Обновление общей стоимости
        Runnable updateTotal = () -> {
            double total = 0;
            try {
                int qty = Integer.parseInt(qtyField.getText());
                if (!parts.isEmpty()) {
                    total += parts.get(0).getSparePart().getRetailPrice() * qty;
                }
            } catch (NumberFormatException ex) {
                if (!parts.isEmpty()) {
                    total += parts.get(0).getSparePart().getRetailPrice() * parts.get(0).getQuantity();
                }
            }
            totalLabel.setText("Общая стоимость: " + String.format("%.2f", total) + " руб.");
        };

        qtyField.textProperty().addListener((obs, oldVal, newVal) -> updateTotal.run());

        // Кнопка подтверждения
        confirmBtn.setOnAction(e -> {
            try {
                int qty = Integer.parseInt(qtyField.getText());
                if (qty <= 0) {
                    showAlert("Количество должно быть положительным");
                    return;
                }

                // Берем первую запчасть из списка
                if (!parts.isEmpty()) {
                    result[0] = parts.get(0).copy();
                    result[0].setQuantity(qty);
                    stage.close();
                }
            } catch (NumberFormatException ex) {
                showAlert("Введите корректное количество");
            }
        });

        // Кнопка пропуска
        skipBtn.setOnAction(e -> stage.close());

        updateTotal.run();
        stage.showAndWait();

        return result[0]; // Возвращаем результат
    }
}