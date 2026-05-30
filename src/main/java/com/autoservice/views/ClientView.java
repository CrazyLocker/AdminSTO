package com.autoservice.views;

import com.autoservice.Client;
import com.autoservice.DataStore;
import com.autoservice.DateUtils;
import com.autoservice.Validators;
import com.autoservice.controllers.ClientController;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class ClientView {

    private static TableView<Client> clientTable;
    private static FilteredList<Client> filteredClients;
    private static TextField searchField;
    private static ObservableList<Client> masterData;

    private static final List<String> GWM_MODELS = Arrays.asList(
            "Haval Jolion", "Haval F7", "Haval F7x", "Haval Dargo", "Haval Big Dog",
            "Haval H6", "Haval H9", "GWM Poer", "GWM Tank 300", "GWM Tank 500",
            "GMW Wingle 7", "GMW Cannon", "Great Wall Poer"
    );

    public static VBox create() {
        VBox mainContainer = new VBox(15);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setStyle("-fx-background-color: #f5f7fa;");

        Label titleLabel = new Label("Управление клиентами");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 20));
        titleLabel.setStyle("-fx-text-fill: #2c3e50;");

        // Панель поиска
        HBox searchBox = createSearchPanel();

        // Таблица клиентов
        clientTable = createClientTable();

        // Форма добавления
        VBox formPanel = createAddClientForm();

        mainContainer.getChildren().addAll(titleLabel, searchBox, clientTable, formPanel);
        VBox.setVgrow(clientTable, Priority.ALWAYS);

        // Загружаем начальные данные
        refreshClientList();

        return mainContainer;
    }

    private static HBox createSearchPanel() {
        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-font-size: 14px;");

        searchField = new TextField();
        searchField.setPromptText("Поиск по имени, фамилии, телефону...");
        searchField.setPrefWidth(350);
        searchField.setStyle(
                "-fx-padding: 8 12 8 12; " +
                        "-fx-background-radius: 6; " +
                        "-fx-border-radius: 6; " +
                        "-fx-border-color: #e0e0e0; " +
                        "-fx-border-width: 1; " +
                        "-fx-font-size: 12px;"
        );

        // Слушатель для поиска
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterClients(newValue);
        });

        Button clearBtn = new Button("Очистить");
        clearBtn.setStyle(
                "-fx-background-color: #e74c3c; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 11px; " +
                        "-fx-background-radius: 4; " +
                        "-fx-padding: 6 12 6 12;"
        );
        clearBtn.setOnAction(e -> {
            searchField.clear();
            filterClients("");
        });

        HBox searchBox = new HBox(10, searchIcon, searchField, clearBtn);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPadding(new Insets(0, 0, 10, 0));

        return searchBox;
    }

    private static TableView<Client> createClientTable() {
        TableView<Client> table = new TableView<>();
        table.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #e0e0e0; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8; " +
                        "-fx-font-size: 12px;"
        );

        TableColumn<Client, String> colLastName = new TableColumn<>("Фамилия");
        colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colLastName.setPrefWidth(130);
        colLastName.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<Client, String> colName = new TableColumn<>("Имя");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(130);
        colName.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<Client, String> colPhone = new TableColumn<>("Телефон");
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colPhone.setPrefWidth(140);
        colPhone.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<Client, String> colCar = new TableColumn<>("Автомобиль");
        colCar.setCellValueFactory(cellData -> {
            Client client = cellData.getValue();
            String model = client.getCarModel() != null ? client.getCarModel() : "—";
            String number = client.getCarNumber() != null ? client.getCarNumber() : "—";
            return new SimpleStringProperty(model + " (" + number + ")");
        });
        colCar.setPrefWidth(260);
        colCar.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<Client, String> colLastRepair = new TableColumn<>("Последний ремонт");
        colLastRepair.setCellValueFactory(cellData -> {
            String date = cellData.getValue().getLastRepairDate();
            return new SimpleStringProperty(date != null && !date.isEmpty() ? DateUtils.formatDate(date) : "—");
        });
        colLastRepair.setPrefWidth(140);
        colLastRepair.setStyle("-fx-alignment: CENTER;");

        table.getColumns().addAll(colLastName, colName, colPhone, colCar, colLastRepair);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.setRowFactory(tv -> new TableRow<Client>() {
            @Override
            protected void updateItem(Client item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    setStyle(getIndex() % 2 == 0 ?
                            "-fx-background-color: #ffffff;" :
                            "-fx-background-color: #f8f9fa;");
                    setPrefHeight(35);
                }
            }
        });

        table.setStyle(table.getStyle() +
                "-fx-selection-bar: #3498db; " +
                "-fx-selection-bar-non-focused: #2980b9;");

        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Client selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    ClientController.editClient(selected);
                }
            }
        });

        // Получаем данные
        masterData = FXCollections.observableArrayList(DataStore.getClients());
        filteredClients = new FilteredList<>(masterData, p -> true);
        table.setItems(filteredClients);
        ClientController.setTable(table);

        return table;
    }

    private static void filterClients(String filterText) {
        if (filterText == null || filterText.trim().isEmpty()) {
            filteredClients.setPredicate(p -> true);
        } else {
            String lowerFilter = filterText.toLowerCase().trim();
            filteredClients.setPredicate(client -> {
                if (client.getName() != null && client.getName().toLowerCase().contains(lowerFilter)) return true;
                if (client.getLastName() != null && client.getLastName().toLowerCase().contains(lowerFilter)) return true;
                if (client.getPhone() != null && client.getPhone().toLowerCase().contains(lowerFilter)) return true;
                if (client.getCarNumber() != null && client.getCarNumber().toLowerCase().contains(lowerFilter)) return true;
                if (client.getCarModel() != null && client.getCarModel().toLowerCase().contains(lowerFilter)) return true;
                return false;
            });
        }
    }

    private static VBox createAddClientForm() {
        VBox formContainer = new VBox(10);
        formContainer.setPadding(new Insets(12));
        formContainer.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #e0e0e0; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8;"
        );

        Label formTitle = new Label("➕ Добавление нового клиента");
        formTitle.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
        formTitle.setStyle("-fx-text-fill: #2c3e50;");

        GridPane formGrid = new GridPane();
        formGrid.setHgap(12);
        formGrid.setVgap(10);
        formGrid.setAlignment(Pos.CENTER_LEFT);

        // Фамилия
        Label lastNameLabel = new Label("Фамилия");
        lastNameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        TextField lastNameField = new TextField();
        lastNameField.setPromptText("Иванов");
        lastNameField.setPrefWidth(160);
        lastNameField.setStyle(getTextFieldStyle());

        // Имя
        Label nameLabel = new Label("Имя");
        nameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        TextField nameField = new TextField();
        nameField.setPromptText("Иван");
        nameField.setPrefWidth(160);
        nameField.setStyle(getTextFieldStyle());

        // Телефон
        Label phoneLabel = new Label("Телефон");
        phoneLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        TextField phoneField = new TextField("+7");
        phoneField.setPrefWidth(160);
        Validators.setupPhoneField(phoneField);
        phoneField.setStyle(getTextFieldStyle());

        // Модель авто
        Label carModelLabel = new Label("Модель автомобиля");
        carModelLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        ComboBox<String> carModelComboBox = new ComboBox<>();
        carModelComboBox.setPromptText("Выберите модель");
        carModelComboBox.setPrefWidth(200);
        carModelComboBox.setItems(FXCollections.observableArrayList(GWM_MODELS));
        carModelComboBox.setEditable(true);
        carModelComboBox.setStyle(getTextFieldStyle());

        // Госномер
        Label carNumberLabel = new Label("Госномер");
        carNumberLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        TextField carNumberField = new TextField();
        carNumberField.setPromptText("А123ВС163");
        carNumberField.setPrefWidth(140);
        Validators.setupCarNumberField(carNumberField);
        carNumberField.setStyle(getTextFieldStyle());

        Button addBtn = new Button("➕ Добавить клиента");
        addBtn.setStyle(
                "-fx-background-color: #3498db; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 11px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 4; " +
                        "-fx-padding: 6 16 6 16;"
        );

        formGrid.add(lastNameLabel, 0, 0);
        formGrid.add(lastNameField, 1, 0);
        formGrid.add(nameLabel, 2, 0);
        formGrid.add(nameField, 3, 0);
        formGrid.add(phoneLabel, 0, 1);
        formGrid.add(phoneField, 1, 1);
        formGrid.add(carModelLabel, 2, 1);
        formGrid.add(carModelComboBox, 3, 1);
        formGrid.add(carNumberLabel, 0, 2);
        formGrid.add(carNumberField, 1, 2);

        HBox buttonContainer = new HBox(addBtn);
        buttonContainer.setAlignment(Pos.CENTER_RIGHT);
        buttonContainer.setPadding(new Insets(8, 0, 0, 0));

        addBtn.setOnAction(e -> {
            if (nameField.getText().trim().isEmpty()) {
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

            Client c = new Client(
                    nameField.getText().trim(),
                    lastNameField.getText().trim(),
                    phoneField.getText(),
                    carModelComboBox.getValue() != null ? carModelComboBox.getValue() : carModelComboBox.getEditor().getText(),
                    carNumberField.getText()
            );
            ClientController.addClient(c);

            lastNameField.clear();
            nameField.clear();
            phoneField.setText("+7");
            carModelComboBox.getEditor().clear();
            carNumberField.clear();

            refreshClientList();
        });

        formContainer.getChildren().addAll(formTitle, formGrid, buttonContainer);
        return formContainer;
    }

    private static String getTextFieldStyle() {
        return
                "-fx-background-radius: 4; " +
                        "-fx-padding: 6 10 6 10; " +
                        "-fx-border-color: #e0e0e0; " +
                        "-fx-border-radius: 4; " +
                        "-fx-font-size: 11px;";
    }

    public static void refreshClientList() {
        // Обновляем исходные данные
        masterData = FXCollections.observableArrayList(DataStore.getClients());
        filteredClients = new FilteredList<>(masterData, p -> true);
        clientTable.setItems(filteredClients);

        // Применяем текущий фильтр
        if (searchField != null && searchField.getText() != null && !searchField.getText().isEmpty()) {
            filterClients(searchField.getText());
        }
    }

    private static void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.showAndWait();
    }
}