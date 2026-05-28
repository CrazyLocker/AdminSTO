package com.autoservice.views;

import com.autoservice.Client;
import com.autoservice.DataStore;
import com.autoservice.DateUtils;
import com.autoservice.Validators;
import com.autoservice.controllers.ClientController;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.collections.transformation.FilteredList;

public class ClientView {

    private static TableView<Client> clientTable;
    private static FilteredList<Client> filteredClients;
    private static TextField searchField;

    public static VBox create() {
        clientTable = new TableView<>();

        TableColumn<Client, String> colName = new TableColumn<>("Имя");
        colName.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getName()));
        colName.setPrefWidth(180);

        TableColumn<Client, String> colPhone = new TableColumn<>("Телефон");
        colPhone.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPhone()));
        colPhone.setPrefWidth(150);

        TableColumn<Client, String> colCar = new TableColumn<>("Авто");
        colCar.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCarModel() + " (" + cellData.getValue().getCarNumber() + ")"));
        colCar.setPrefWidth(250);

        TableColumn<Client, String> colLastRepair = new TableColumn<>("Последний ремонт");
        colLastRepair.setCellValueFactory(cellData ->
                new SimpleStringProperty(DateUtils.formatDate(cellData.getValue().getLastRepairDate())));
        colLastRepair.setPrefWidth(150);

        clientTable.getColumns().addAll(colName, colPhone, colCar, colLastRepair);
        clientTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(clientTable, Priority.ALWAYS);

        clientTable.setRowFactory(tv -> new TableRow<Client>() {
            @Override
            protected void updateItem(Client item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    setStyle(getIndex() % 2 == 0 ? "-fx-background-color: #f5f5f5;" : "-fx-background-color: white;");
                }
            }
        });

        clientTable.setStyle("-fx-selection-bar: #3399ff; -fx-selection-bar-text: white;");

        clientTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Client selected = clientTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    ClientController.editClient(selected);
                }
            }
        });

        // Настройка фильтрованного списка
        filteredClients = new FilteredList<>(FXCollections.observableArrayList(DataStore.getClients()), p -> true);
        clientTable.setItems(filteredClients);
        ClientController.setTable(clientTable);

        // Панель поиска
        Label searchLabel = new Label("🔍 Поиск:");
        searchField = new TextField();
        searchField.setPromptText("Имя или телефон...");
        searchField.setPrefWidth(250);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyClientFilter(newVal));

        Button clearBtn = new Button("✖ Очистить");
        clearBtn.setOnAction(e -> {
            searchField.clear();
            applyClientFilter("");
        });

        HBox searchBox = new HBox(10, searchLabel, searchField, clearBtn);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPadding(new Insets(0, 0, 10, 0));

        // Форма добавления клиента
        TextField nameField = new TextField();
        nameField.setPromptText("Имя");
        nameField.setPrefWidth(120);

        TextField phoneField = new TextField("+7");
        phoneField.setPrefWidth(120);
        Validators.setupPhoneField(phoneField);

        TextField carModelField = new TextField();
        carModelField.setPromptText("Модель");
        carModelField.setPrefWidth(130);

        TextField carNumberField = new TextField();
        carNumberField.setPromptText("Госномер");
        carNumberField.setPrefWidth(100);
        Validators.setupCarNumberField(carNumberField);

        Button addBtn = new Button("➕ Добавить клиента");
        addBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");

        HBox formRow = new HBox(10);
        formRow.setAlignment(Pos.CENTER_LEFT);
        formRow.getChildren().addAll(
                new Label("Имя:"), nameField,
                new Label("Тел.:"), phoneField,
                new Label("Модель:"), carModelField,
                new Label("Госномер:"), carNumberField,
                addBtn
        );
        formRow.setPadding(new Insets(10, 0, 10, 0));

        addBtn.setOnAction(e -> {
            if (nameField.getText().isEmpty()) {
                showAlert("Введите имя клиента");
                return;
            }
            if (!Validators.isValidPhone(phoneField.getText())) {
                showAlert("Неверный формат телефона. Должно быть +7XXXXXXXXXX (10 цифр)");
                return;
            }
            if (!Validators.isValidCarNumber(carNumberField.getText())) {
                showAlert("Неверный формат госномера.\nПримеры: А123ВС163, А123ВС16");
                return;
            }

            Client c = new Client(nameField.getText(), phoneField.getText(),
                    carModelField.getText(), carNumberField.getText());
            ClientController.addClient(c);
            nameField.clear();
            phoneField.setText("+7");
            carModelField.clear();
            carNumberField.clear();
            refreshClientList();
        });

        VBox vbox = new VBox(5);
        vbox.setPadding(new Insets(10));
        vbox.getChildren().addAll(searchBox, clientTable, formRow);
        VBox.setVgrow(clientTable, Priority.ALWAYS);
        return vbox;
    }

    private static void applyClientFilter(String filter) {
        if (filter == null || filter.trim().isEmpty()) {
            filteredClients.setPredicate(p -> true);
        } else {
            String lowerFilter = filter.toLowerCase().trim();
            filteredClients.setPredicate(client ->
                    client.getName().toLowerCase().contains(lowerFilter) ||
                            client.getPhone().toLowerCase().contains(lowerFilter) ||
                            client.getCarNumber().toLowerCase().contains(lowerFilter)
            );
        }
    }

    public static void refreshClientList() {
        filteredClients = new FilteredList<>(FXCollections.observableArrayList(DataStore.getClients()), p -> true);
        clientTable.setItems(filteredClients);
        applyClientFilter(searchField != null ? searchField.getText() : "");
    }

    private static void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.showAndWait();
    }
}