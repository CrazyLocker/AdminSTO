package com.autoservice.views;

import com.autoservice.Client;
import com.autoservice.DataStore;
import com.autoservice.DateUtils;
import com.autoservice.Validators;
import com.autoservice.controllers.ClientController;
import com.autoservice.dialogs.EditClientDialog;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.List;

public class ClientView {

    private static TableView<Client> clientTable;
    private static FilteredList<Client> filteredClients;
    private static TextField searchField;
    private static ObservableList<Client> masterData;

    private static Button addBtn;
    private static Button editBtn;
    private static Button deleteBtn;

    private static final List<String> GWM_MODELS = Arrays.asList(
            "Haval Jolion", "Haval F7", "Haval F7x", "Haval Dargo", "Haval Big Dog",
            "Haval H6", "Haval H9", "GWM Poer", "GWM Tank 300", "GWM Tank 500",
            "GWM Wingle 7", "GWM Cannon", "Great Wall Poer"
    );

    public static VBox create() {
        VBox mainContainer = new VBox(15);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setStyle("-fx-background-color: #f5f7fa;");

        Label titleLabel = new Label("Управление клиентами");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        HBox topPanel = new HBox(15);
        topPanel.setAlignment(Pos.CENTER_LEFT);
        topPanel.setPadding(new Insets(0, 0, 10, 0));

        // ====== КНОПКИ БЕЗ ИКОНОК ======
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

        deleteBtn = new Button("Удалить");
        deleteBtn.getStyleClass().add("delete-button");
        deleteBtn.setDisable(true);
        deleteBtn.setOnAction(e -> {
            Client selected = clientTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                DataStore.deleteClient(selected);
                refreshClientList();
            }
        });

        HBox searchBox = createSearchPanel();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topPanel.getChildren().addAll(searchBox, spacer, addBtn, editBtn, deleteBtn);

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
        searchField.getStyleClass().add("search-field");

        searchField.textProperty().addListener((observable, oldValue, newValue) -> filterClients(newValue));

        Button clearBtn = new Button("✕");
        clearBtn.setStyle(
                "-fx-background-color: #dc3545;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 4 8 4 8;" +
                        "-fx-background-radius: 4;"
        );
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
            deleteBtn.setDisable(newVal == null);
        });

        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Client selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    showEditClientDialog(selected);
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
                return client.getCarModel() != null && client.getCarModel().toLowerCase().contains(lowerFilter);
            });
        }
    }

    private static void showAddClientDialog() {
        Client emptyClient = new Client(-1, "", "", "", "", "", "");
        EditClientDialog.show(emptyClient);
        refreshClientList();
    }

    private static void showEditClientDialog(Client client) {
        EditClientDialog.show(client);
        refreshClientList();
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
}