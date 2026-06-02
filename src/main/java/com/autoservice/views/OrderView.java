package com.autoservice.views;

import com.autoservice.DataStore;
import com.autoservice.WorkOrder;
import com.autoservice.controllers.OrderController;
import com.autoservice.dialogs.PrintOrderDialog;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class OrderView {

    private static TableView<WorkOrder> orderTable;
    private static TextField searchField;
    private static Label resultLabel;

    private static Button viewBtn, editBtn, deleteBtn, printBtn, createOrderBtn;

    public static VBox create() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #f5f7fa;");

        HBox topPanel = new HBox(15);
        topPanel.setAlignment(Pos.CENTER_LEFT);
        topPanel.setPadding(new Insets(10));
        topPanel.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");

        HBox searchBox = createSearchPanel();

        viewBtn = createActionButton("Просмотр", "#3498db");
        editBtn = createActionButton("Изменить", "#f39c12");
        deleteBtn = createActionButton("Удалить", "#e74c3c");
        printBtn = createActionButton("Печать", "#9b59b6");
        createOrderBtn = createActionButton("Новый", "#2ecc71");

        viewBtn.setOnAction(e -> onView());
        editBtn.setOnAction(e -> onEdit());
        deleteBtn.setOnAction(e -> onDelete());
        printBtn.setOnAction(e -> onPrint());
        createOrderBtn.setOnAction(e -> OrderController.createOrder());

        viewBtn.setDisable(true);
        editBtn.setDisable(true);
        deleteBtn.setDisable(true);
        printBtn.setDisable(true);

        Label separator = new Label("|");
        separator.setStyle("-fx-text-fill: #bdc3c7; -fx-font-size: 18px; -fx-padding: 0 5 0 5;");

        topPanel.getChildren().addAll(searchBox, separator, viewBtn, editBtn, deleteBtn, printBtn, createOrderBtn);
        HBox.setHgrow(searchBox, Priority.ALWAYS);

        // СОЗДАЕМ ТАБЛИЦУ
        orderTable = new TableView<>();
        setupTableColumns();

        // ЗАГРУЖАЕМ ДАННЫЕ
        refreshTable(DataStore.getOrders());

        orderTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            viewBtn.setDisable(newVal == null);
            editBtn.setDisable(newVal == null);
            deleteBtn.setDisable(newVal == null);
            printBtn.setDisable(newVal == null);
        });

        root.getChildren().addAll(topPanel, orderTable);
        VBox.setVgrow(orderTable, Priority.ALWAYS);

        return root;
    }

    private static void setupTableColumns() {
        orderTable.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 12px;");

        TableColumn<WorkOrder, String> colId = new TableColumn<>("№ заказа");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(160);

        TableColumn<WorkOrder, String> colClient = new TableColumn<>("Клиент");
        colClient.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getClient().getName()));
        colClient.setPrefWidth(150);

        TableColumn<WorkOrder, String> colCar = new TableColumn<>("Автомобиль");
        colCar.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getClient().getCarModel() + " (" + cellData.getValue().getClient().getCarNumber() + ")"));
        colCar.setPrefWidth(200);

        TableColumn<WorkOrder, String> colServices = new TableColumn<>("Услуги");
        colServices.setCellValueFactory(cellData -> {
            WorkOrder order = cellData.getValue();
            return new SimpleStringProperty(String.join(", ", order.getServices()));
        });
        colServices.setPrefWidth(250);

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

        TableColumn<WorkOrder, String> colStatus = new TableColumn<>("Статус");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setPrefWidth(120);
        colStatus.setCellFactory(col -> new TableCell<WorkOrder, String>() {
            private final ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(WorkOrder.getAllStatuses()));
            {
                comboBox.setOnAction(e -> {
                    WorkOrder order = getTableView().getItems().get(getIndex());
                    if (order != null) OrderController.changeOrderStatus(order, comboBox.getValue());
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    comboBox.setValue(item);
                    setGraphic(comboBox);
                    setText(null);
                }
            }
        });

        orderTable.getColumns().addAll(colId, colClient, colCar, colServices, colTotal, colStatus);
        orderTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        orderTable.setRowFactory(tv -> {
            TableRow<WorkOrder> row = new TableRow<WorkOrder>() {
                @Override
                protected void updateItem(WorkOrder item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setStyle("");
                    } else {
                        setStyle(getIndex() % 2 == 0 ? "-fx-background-color: #ffffff;" : "-fx-background-color: #f8f9fa;");
                        setPrefHeight(40);
                    }
                }
            };
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1 && row.isEmpty()) {
                    orderTable.getSelectionModel().clearSelection();
                }
            });
            return row;
        });

        orderTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                WorkOrder selected = orderTable.getSelectionModel().getSelectedItem();
                if (selected != null) OrderController.editOrder(selected);
            }
        });
    }

    private static void refreshTable(List<WorkOrder> orders) {
        ObservableList<WorkOrder> items = FXCollections.observableArrayList(orders);
        orderTable.setItems(items);
        OrderController.setTable(orderTable);
    }

    private static Button createActionButton(String text, String color) {
        Button btn = new Button(text);
        btn.setPrefWidth(100);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 0 8 0; -fx-background-radius: 4;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: derive(" + color + ", -10%); -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 0 8 0; -fx-background-radius: 4;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 0 8 0; -fx-background-radius: 4;"));
        return btn;
    }

    private static HBox createSearchPanel() {
        searchField = new TextField();
        searchField.setPromptText("Введите имя клиента...");
        searchField.setPrefWidth(250);
        // ПОИСК "НА ЛЕТУ" - при каждом изменении текста
        searchField.textProperty().addListener((obs, oldVal, newVal) -> searchOrders());

        Button clearBtn = new Button("Очистить");
        clearBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold;");
        clearBtn.setOnAction(e -> {
            searchField.clear();
            searchOrders();
        });

        resultLabel = new Label();
        resultLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px; -fx-padding: 0 0 0 10;");

        HBox searchBox = new HBox(10, new Label("Поиск:"), searchField, clearBtn, resultLabel);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        return searchBox;
    }

    private static void searchOrders() {
        String filter = searchField.getText();

        if (filter == null || filter.trim().isEmpty()) {
            refreshTable(DataStore.getOrders());
            resultLabel.setText("");
            return;
        }

        String lowerFilter = filter.toLowerCase().trim();
        List<WorkOrder> allOrders = DataStore.getOrders();
        List<WorkOrder> filtered = new ArrayList<>();

        for (WorkOrder order : allOrders) {
            if (order.getClient() != null) {
                String name = order.getClient().getName();
                String phone = order.getClient().getPhone();
                String carNumber = order.getClient().getCarNumber();

                if ((name != null && name.toLowerCase().contains(lowerFilter)) ||
                        (phone != null && phone.toLowerCase().contains(lowerFilter)) ||
                        (carNumber != null && carNumber.toLowerCase().contains(lowerFilter))) {
                    filtered.add(order);
                }
            }
        }

        refreshTable(filtered);
        resultLabel.setText("Найдено: " + filtered.size() + " заказов");
    }

    private static void onView() {
        WorkOrder selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected != null) OrderController.viewOrder(selected);
    }

    private static void onEdit() {
        WorkOrder selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected != null) OrderController.editOrder(selected);
    }

    private static void onDelete() {
        WorkOrder selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected != null) OrderController.deleteOrder(selected);
    }

    private static void onPrint() {
        WorkOrder selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected != null) PrintOrderDialog.show(selected);
    }

    public static void refreshOrderList() {
        if (orderTable != null) {
            refreshTable(DataStore.getOrders());
            resultLabel.setText("");
            searchField.clear();
        }
    }
}