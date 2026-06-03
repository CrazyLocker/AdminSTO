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

import java.util.Arrays;
import java.util.List;

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
        mainContainer.getStyleClass().add("main-container");

        Label titleLabel = new Label("Управление клиентами");
        titleLabel.getStyleClass().add("section-title");

        HBox searchBox = createSearchPanel();

        clientTable = createClientTable();

        VBox formPanel = createAddClientForm();

        mainContainer.getChildren().addAll(titleLabel, searchBox, clientTable, formPanel);
        VBox.setVgrow(clientTable, Priority.ALWAYS);

        refreshClientList();

        return mainContainer;
    }

    private static HBox createSearchPanel() {
        searchField = new TextField();
        searchField.setPromptText("Поиск по имени, фамилии, телефону...");
        searchField.setPrefWidth(350);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterClients(newVal));

        Button clearBtn = new Button("Очистить");
        clearBtn.getStyleClass().add("clear-button");
        clearBtn.setOnAction(e -> {
            searchField.clear();
            filterClients("");
        });

        HBox searchBox = new HBox(10, new Label("🔍"), searchField, clearBtn);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPadding(new Insets(0, 0, 10, 0));

        return searchBox;
    }

    private static TableView<Client> createClientTable() {
        TableView<Client> table = new TableView<>();

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

        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Client selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    ClientController.editClient(selected);
                }
            }
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

    private static VBox createAddClientForm() {
        VBox formContainer = new VBox(10);
        formContainer.getStyleClass().add("card");

        Label formTitle = new Label("Добавление нового клиента");
        formTitle.getStyleClass().add("form-title");

        GridPane formGrid = new GridPane();
        formGrid.setHgap(12);
        formGrid.setVgap(10);
        formGrid.setAlignment(Pos.CENTER_LEFT);

        // Фамилия
        Label lastNameLabel = new Label("Фамилия");
        TextField lastNameField = new TextField();
        lastNameField.setPromptText("Иванов");
        lastNameField.setPrefWidth(160);

        // Имя
        Label nameLabel = new Label("Имя");
        TextField nameField = new TextField();
        nameField.setPromptText("Иван");
        nameField.setPrefWidth(160);

        // Телефон
        Label phoneLabel = new Label("Телефон");
        TextField phoneField = new TextField("+7");
        phoneField.setPrefWidth(160);
        Validators.setupPhoneField(phoneField);

        // Модель авто
        Label carModelLabel = new Label("Модель автомобиля");
        ComboBox<String> carModelComboBox = new ComboBox<>();
        carModelComboBox.setPromptText("Выберите модель");
        carModelComboBox.setPrefWidth(200);
        carModelComboBox.setItems(FXCollections.observableArrayList(GWM_MODELS));
        carModelComboBox.setEditable(true);

        // Госномер
        Label carNumberLabel = new Label("Госномер");
        TextField carNumberField = new TextField();
        carNumberField.setPromptText("А123ВС163");
        carNumberField.setPrefWidth(140);
        Validators.setupCarNumberField(carNumberField);

        Button addBtn = new Button("Добавить клиента");
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

        formContainer.getChildren().addAll(formTitle, formGrid, buttonContainer);
        return formContainer;
    }

    public static void refreshClientList() {
        masterData = FXCollections.observableArrayList(DataStore.getClients());
        filteredClients = new FilteredList<>(masterData, p -> true);
        clientTable.setItems(filteredClients);

        if (searchField != null && searchField.getText() != null && !searchField.getText().isEmpty()) {
            filterClients(searchField.getText());
        }
    }

    private static void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.showAndWait();
    }
}