package com.autoservice.dialogs;

import com.autoservice.*;
import com.autoservice.controllers.DictionaryController;
import com.autoservice.controllers.OrderController;
import com.autoservice.utils.IconHelper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.control.Separator;
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
        stage.setMinWidth(700);
        stage.setMinHeight(700);
        stage.initModality(javafx.stage.Modality.WINDOW_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        // ==================== КЛИЕНТ ====================
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
                    String lastName = item.getLastName() != null && !item.getLastName().isEmpty()
                            ? item.getLastName() + " " : "";
                    String carModel = item.getCarModel() != null && !item.getCarModel().isEmpty()
                            ? " — " + item.getCarModel()
                            : "";
                    setText(lastName + item.getName() + carModel);
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
                    String lastName = item.getLastName() != null && !item.getLastName().isEmpty()
                            ? item.getLastName() + " " : "";
                    String carModel = item.getCarModel() != null && !item.getCarModel().isEmpty()
                            ? " — " + item.getCarModel()
                            : "";
                    setText(lastName + item.getName() + carModel);
                }
            }
        });

        // ==================== ЗАПИСЬ ====================
        Label appointmentLabel = new Label("Запись на сервис:");
        appointmentLabel.setStyle("-fx-font-weight: bold;");

        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setPrefWidth(200);

        ComboBox<String> timeCombo = new ComboBox<>(FXCollections.observableArrayList(TIME_SLOTS));
        timeCombo.setPromptText("Выберите время");
        timeCombo.setPrefWidth(100);

        ComboBox<String> masterCombo = new ComboBox<>(FXCollections.observableArrayList(MASTERS));
        masterCombo.setPromptText("Выберите мастера");
        masterCombo.setPrefWidth(150);

        CheckBox createAppointmentCheck = new CheckBox("Создать запись в календаре");
        createAppointmentCheck.setSelected(true);

        HBox appointmentBox = new HBox(10, new Label("Дата:"), datePicker, new Label("Время:"), timeCombo,
                new Label("Мастер:"), masterCombo);
        appointmentBox.setAlignment(Pos.CENTER_LEFT);

        // ==================== УСЛУГИ ====================
        Label servicesHeader = new Label("Услуги:");
        servicesHeader.setStyle("-fx-font-weight: bold;");

        ListView<String> servicesListView = new ListView<>();
        servicesListView.setPrefHeight(120);

        ComboBox<Service> serviceCombo = new ComboBox<>(FXCollections.observableArrayList(DataStore.getServices()));
        serviceCombo.setPromptText("Выберите услугу");
        serviceCombo.setPrefWidth(300);

        TextField servicePriceField = new TextField();
        servicePriceField.setEditable(false);
        servicePriceField.setPrefWidth(100);

        serviceCombo.setOnAction(e -> {
            Service selected = serviceCombo.getValue();
            if (selected != null) {
                servicePriceField.setText(String.valueOf(selected.getPrice()));
            }
        });

        Button addServiceBtn = new Button("Добавить услугу");
        addServiceBtn.setGraphic(IconHelper.add());
        addServiceBtn.getStyleClass().add("add-button");

        // ==================== ЗАПЧАСТИ ====================
        Label partsHeader = new Label("Запчасти:");
        partsHeader.setStyle("-fx-font-weight: bold;");

        ListView<String> partsListView = new ListView<>();
        partsListView.setPrefHeight(120);

        ComboBox<SparePart> partCombo = new ComboBox<>(FXCollections.observableArrayList(DataStore.getSpareParts()));
        partCombo.setPromptText("Выберите запчасть");
        partCombo.setPrefWidth(300);

        TextField partPriceField = new TextField();
        partPriceField.setEditable(false);
        partPriceField.setPrefWidth(100);

        TextField partStockField = new TextField();
        partStockField.setEditable(false);
        partStockField.setPrefWidth(60);

        TextField partQtyField = new TextField();
        partQtyField.setText("1");
        partQtyField.setPrefWidth(60);

        partCombo.setOnAction(e -> {
            SparePart selected = partCombo.getValue();
            if (selected != null) {
                partPriceField.setText(String.valueOf(selected.getRetailPrice()));
                partStockField.setText(String.valueOf(selected.getStock()));
            }
        });

        Button addPartBtn = new Button("Добавить запчасть");
        addPartBtn.setGraphic(IconHelper.add());
        addPartBtn.getStyleClass().add("add-button");

        // Временные списки
        List<String> tempServices = new ArrayList<>();
        List<Double> tempServicePrices = new ArrayList<>();
        List<SparePart> tempParts = new ArrayList<>();
        List<Integer> tempPartQuantities = new ArrayList<>();

        // ==================== КНОПКИ УДАЛЕНИЯ ====================
        Button removeServiceBtn = new Button("Удалить выбранную");
        removeServiceBtn.setGraphic(IconHelper.delete());
        removeServiceBtn.getStyleClass().add("delete-button");

        Button removePartBtn = new Button("Удалить выбранную");
        removePartBtn.setGraphic(IconHelper.delete());
        removePartBtn.getStyleClass().add("delete-button");

        // ==================== ИТОГО ====================
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
            updateTotal.run();
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
            updateTotal.run();
        });

        removeServiceBtn.setOnAction(e -> {
            int idx = servicesListView.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < tempServices.size()) {
                tempServices.remove(idx);
                tempServicePrices.remove(idx);
                servicesListView.getItems().clear();
                for (int i = 0; i < tempServices.size(); i++) {
                    servicesListView.getItems().add((i+1) + ". " + tempServices.get(i) + " — " + tempServicePrices.get(i) + " руб.");
                }
                updateTotal.run();
            } else {
                showAlert("Выберите услугу");
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
                updateTotal.run();
            } else {
                showAlert("Выберите запчасть");
            }
        });

        // ==================== КНОПКИ СОХРАНЕНИЯ ====================
        Button saveBtn = new Button("Создать заказ");
        saveBtn.setGraphic(IconHelper.save());
        saveBtn.getStyleClass().add("save-button");

        Button cancelBtn = new Button("Отмена");
        cancelBtn.setGraphic(IconHelper.cancel());
        cancelBtn.getStyleClass().add("cancel-button");

        HBox btnBox = new HBox(15, saveBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER);

        // Сборка интерфейса
        HBox serviceAddBox = new HBox(10, serviceCombo, servicePriceField, addServiceBtn);
        HBox partAddBox = new HBox(10, partCombo, partPriceField, new Label("Остаток:"), partStockField,
                new Label("Кол-во:"), partQtyField, addPartBtn);

        root.getChildren().addAll(
                new Label("Клиент:"), clientCombo,
                new Separator(),
                appointmentLabel, appointmentBox, createAppointmentCheck,
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
        scene.getStylesheets().add(
                CreateOrderDialog.class.getResource("/styles.css").toExternalForm()
        );
        stage.setScene(scene);

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

            WorkOrder order = new WorkOrder(clientCombo.getValue());
            order.setStatus("Новый");

            for (int i = 0; i < tempServices.size(); i++) {
                order.addService(tempServices.get(i), tempServicePrices.get(i));
            }
            for (int i = 0; i < tempParts.size(); i++) {
                order.addSparePart(tempParts.get(i), tempPartQuantities.get(i));
                DataStore.updateSparePartStock(tempParts.get(i), tempParts.get(i).getStock());
            }
            DataStore.addOrder(order);

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
}