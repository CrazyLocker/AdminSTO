package com.autoservice.views;

import com.autoservice.Client;
import com.autoservice.DataStore;
import com.autoservice.DateUtils;
import com.autoservice.Validators;
import com.autoservice.WorkOrder;
import com.autoservice.controllers.ClientController;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;

public class ClientView {

    private static TableView<Client> clientTable;
    private static FilteredList<Client> filteredClients;
    private static TextField searchField;
    private static ObservableList<Client> masterData;

    private static Button addBtn;
    private static Button editBtn;

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
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Панель с кнопками и поиском
        HBox topPanel = new HBox(15);
        topPanel.setAlignment(Pos.CENTER_LEFT);
        topPanel.setPadding(new Insets(0, 0, 10, 0));

        addBtn = new Button("Новый клиент");
        addBtn.getStyleClass().add("add-button");
        addBtn.setOnAction(e -> showAddClientDialog());

        editBtn = new Button("Изменить");
        editBtn.getStyleClass().add("edit-button");
        editBtn.setDisable(true);
        editBtn.setOnAction(e -> {
            Client selected = clientTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showEditClientDialog(selected);
            }
        });

        HBox searchBox = createSearchPanel();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topPanel.getChildren().addAll(searchBox, spacer, addBtn, editBtn);

        // Таблица клиентов
        clientTable = createClientTable();
        VBox.setVgrow(clientTable, Priority.ALWAYS);

        mainContainer.getChildren().addAll(titleLabel, topPanel, clientTable);

        refreshClientList();

        return mainContainer;
    }

    private static HBox createSearchPanel() {
        Label searchLabel = new Label("Поиск:");
        searchLabel.setStyle("-fx-font-weight: bold;");

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

        searchField.textProperty().addListener((observable, oldValue, newValue) -> filterClients(newValue));

        Button clearBtn = new Button("Очистить");
        clearBtn.getStyleClass().add("clear-button");
        clearBtn.setOnAction(e -> {
            searchField.clear();
            filterClients("");
        });

        HBox searchBox = new HBox(10, searchLabel, searchField, clearBtn);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        return searchBox;
    }

    private static TableView<Client> createClientTable() {
        TableView<Client> table = new TableView<>();
        table.getStyleClass().add("table-view");

        TableColumn<Client, String> colLastName = new TableColumn<>("Фамилия");
        colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colLastName.setPrefWidth(130);

        TableColumn<Client, String> colName = new TableColumn<>("Имя");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(130);

        TableColumn<Client, String> colPhone = new TableColumn<>("Телефон");
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colPhone.setPrefWidth(140);

        TableColumn<Client, String> colCar = new TableColumn<>("Автомобиль");
        colCar.setCellValueFactory(cellData -> {
            Client client = cellData.getValue();
            String model = client.getCarModel() != null ? client.getCarModel() : "—";
            String number = client.getCarNumber() != null ? client.getCarNumber() : "—";
            return new SimpleStringProperty(model + " (" + number + ")");
        });
        colCar.setPrefWidth(260);

        TableColumn<Client, String> colLastRepair = new TableColumn<>("Последний ремонт");
        colLastRepair.setCellValueFactory(cellData -> {
            String date = cellData.getValue().getLastRepairDate();
            return new SimpleStringProperty(date != null && !date.isEmpty() ? DateUtils.formatDate(date) : "—");
        });
        colLastRepair.setPrefWidth(140);

        table.getColumns().addAll(colLastName, colName, colPhone, colCar, colLastRepair);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            editBtn.setDisable(newVal == null);
        });

        filteredClients = new FilteredList<>(FXCollections.observableArrayList(DataStore.getClients()), p -> true);
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

    private static void showAddClientDialog() {
        Stage stage = new Stage();
        stage.setTitle("Новый клиент");
        stage.setMinWidth(500);
        stage.setMinHeight(450);
        stage.initModality(Modality.WINDOW_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("Новый клиент");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(10));

        TextField lastNameField = new TextField();
        lastNameField.setPromptText("Фамилия");
        lastNameField.setPrefWidth(250);

        TextField nameField = new TextField();
        nameField.setPromptText("Имя");
        nameField.setPrefWidth(250);

        TextField phoneField = new TextField("+7");
        phoneField.setPromptText("Телефон");
        phoneField.setPrefWidth(250);
        Validators.setupPhoneField(phoneField);

        ComboBox<String> carModelCombo = new ComboBox<>();
        carModelCombo.setPromptText("Модель автомобиля");
        carModelCombo.setPrefWidth(250);
        carModelCombo.setItems(FXCollections.observableArrayList(GWM_MODELS));
        carModelCombo.setEditable(true);

        TextField carNumberField = new TextField();
        carNumberField.setPromptText("Госномер");
        carNumberField.setPrefWidth(200);
        Validators.setupCarNumberField(carNumberField);

        grid.add(new Label("Фамилия:"), 0, 0);
        grid.add(lastNameField, 1, 0);
        grid.add(new Label("Имя:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Телефон:"), 0, 2);
        grid.add(phoneField, 1, 2);
        grid.add(new Label("Модель автомобиля:"), 0, 3);
        grid.add(carModelCombo, 1, 3);
        grid.add(new Label("Госномер:"), 0, 4);
        grid.add(carNumberField, 1, 4);

        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER);

        Button saveBtn = new Button("Сохранить");
        saveBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20;");
        Button cancelBtn = new Button("Отмена");
        cancelBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20;");

        btnBox.getChildren().addAll(saveBtn, cancelBtn);

        root.getChildren().addAll(titleLabel, grid, btnBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);

        saveBtn.setOnAction(e -> {
            if (nameField.getText().trim().isEmpty()) {
                showAlert("Введите имя клиента");
                return;
            }
            if (!Validators.isValidPhone(phoneField.getText())) {
                showAlert("Неверный формат телефона");
                return;
            }
            if (!Validators.isValidCarNumber(carNumberField.getText())) {
                showAlert("Неверный формат госномера");
                return;
            }

            Client client = new Client(
                    nameField.getText().trim(),
                    lastNameField.getText().trim(),
                    phoneField.getText(),
                    carModelCombo.getValue() != null ? carModelCombo.getValue() : carModelCombo.getEditor().getText(),
                    carNumberField.getText().trim().toUpperCase()
            );

            ClientController.addClient(client);
            refreshClientList();
            stage.close();
        });

        cancelBtn.setOnAction(e -> stage.close());

        stage.showAndWait();
    }

    private static void showEditClientDialog(Client client) {
        Stage stage = new Stage();
        stage.setTitle("Редактирование клиента - " + client.getName());
        stage.setMinWidth(700);
        stage.setMinHeight(550);
        stage.initModality(Modality.WINDOW_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("Редактирование клиента");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        TabPane tabPane = new TabPane();

        // Вкладка "Основная информация"
        Tab infoTab = new Tab("Основная информация");
        infoTab.setClosable(false);

        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(15);
        infoGrid.setVgap(12);
        infoGrid.setPadding(new Insets(15));

        TextField lastNameField = new TextField(client.getLastName());
        lastNameField.setPrefWidth(250);

        TextField nameField = new TextField(client.getName());
        nameField.setPrefWidth(250);

        TextField phoneField = new TextField(client.getPhone());
        phoneField.setPrefWidth(250);
        Validators.setupPhoneField(phoneField);

        ComboBox<String> carModelCombo = new ComboBox<>();
        carModelCombo.setPromptText("Модель автомобиля");
        carModelCombo.setPrefWidth(250);
        carModelCombo.setItems(FXCollections.observableArrayList(GWM_MODELS));
        carModelCombo.setEditable(true);
        carModelCombo.setValue(client.getCarModel());

        TextField carNumberField = new TextField(client.getCarNumber());
        carNumberField.setPromptText("Госномер");
        carNumberField.setPrefWidth(200);
        Validators.setupCarNumberField(carNumberField);

        infoGrid.add(new Label("Фамилия:"), 0, 0);
        infoGrid.add(lastNameField, 1, 0);
        infoGrid.add(new Label("Имя:"), 0, 1);
        infoGrid.add(nameField, 1, 1);
        infoGrid.add(new Label("Телефон:"), 0, 2);
        infoGrid.add(phoneField, 1, 2);
        infoGrid.add(new Label("Модель автомобиля:"), 0, 3);
        infoGrid.add(carModelCombo, 1, 3);
        infoGrid.add(new Label("Госномер:"), 0, 4);
        infoGrid.add(carNumberField, 1, 4);

        infoTab.setContent(infoGrid);

        // Вкладка "История заказов"
        Tab historyTab = new Tab("История заказов");
        historyTab.setClosable(false);

        VBox historyBox = new VBox(10);
        historyBox.setPadding(new Insets(15));

        TableView<WorkOrder> ordersTable = new TableView<>();
        ordersTable.setPrefHeight(300);

        TableColumn<WorkOrder, String> colOrderId = new TableColumn<>("№ заказа");
        colOrderId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colOrderId.setPrefWidth(150);

        TableColumn<WorkOrder, String> colDate = new TableColumn<>("Дата");
        colDate.setCellValueFactory(new PropertyValueFactory<>("createdDate"));
        colDate.setPrefWidth(120);

        TableColumn<WorkOrder, String> colStatus = new TableColumn<>("Статус");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setPrefWidth(100);

        TableColumn<WorkOrder, Double> colTotal = new TableColumn<>("Сумма");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setPrefWidth(100);
        colTotal.setCellFactory(col -> new TableCell<WorkOrder, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%,.0f руб.", item));
                setAlignment(Pos.CENTER_RIGHT);
            }
        });

        ordersTable.getColumns().addAll(colOrderId, colDate, colStatus, colTotal);

        ObservableList<WorkOrder> clientOrders = FXCollections.observableArrayList();
        for (WorkOrder order : DataStore.getOrders()) {
            if (order.getClient().getId() == client.getId()) {
                clientOrders.add(order);
            }
        }
        ordersTable.setItems(clientOrders);

        Label ordersLabel = new Label("Всего заказов: " + clientOrders.size());
        ordersLabel.setStyle("-fx-font-weight: bold;");

        historyBox.getChildren().addAll(ordersLabel, ordersTable);
        historyTab.setContent(historyBox);

        tabPane.getTabs().addAll(infoTab, historyTab);

        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER);

        Button saveBtn = new Button("Сохранить");
        saveBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20;");

        Button deleteBtn = new Button("Удалить клиента");
        deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20;");

        Button cancelBtn = new Button("Отмена");
        cancelBtn.setStyle("-fx-background-color: #9e9e9e; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20;");

        btnBox.getChildren().addAll(saveBtn, deleteBtn, cancelBtn);

        root.getChildren().addAll(titleLabel, tabPane, btnBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);

        saveBtn.setOnAction(e -> {
            if (nameField.getText().trim().isEmpty()) {
                showAlert("Введите имя клиента");
                return;
            }
            if (!Validators.isValidPhone(phoneField.getText())) {
                showAlert("Неверный формат телефона");
                return;
            }
            if (!Validators.isValidCarNumber(carNumberField.getText())) {
                showAlert("Неверный формат госномера");
                return;
            }

            client.setName(nameField.getText().trim());
            client.setLastName(lastNameField.getText().trim());
            client.setPhone(phoneField.getText());
            client.setCarModel(carModelCombo.getValue() != null ? carModelCombo.getValue() : carModelCombo.getEditor().getText());
            client.setCarNumber(carNumberField.getText().trim().toUpperCase());

            ClientController.updateClient(client);
            refreshClientList();
            stage.close();
            showAlert("Клиент сохранён", Alert.AlertType.INFORMATION);
        });

        deleteBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Удалить клиента " + client.getName() + "?\nВсе заказы клиента также будут удалены.",
                    ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    DataStore.deleteClient(client);
                    refreshClientList();
                    stage.close();
                    showAlert("Клиент удалён", Alert.AlertType.INFORMATION);
                }
            });
        });

        cancelBtn.setOnAction(e -> stage.close());

        stage.showAndWait();
    }

    public static void refreshClientList() {
        masterData = FXCollections.observableArrayList(DataStore.getClients());
        filteredClients = new FilteredList<>(masterData, p -> true);
        if (clientTable != null) {
            clientTable.setItems(filteredClients);
        }
        if (searchField != null && searchField.getText() != null && !searchField.getText().isEmpty()) {
            filterClients(searchField.getText());
        }
    }

    private static void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.showAndWait();
    }

    private static void showAlert(String msg, Alert.AlertType type) {
        Alert alert = new Alert(type, msg, ButtonType.OK);
        alert.showAndWait();
    }
}