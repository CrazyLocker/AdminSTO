package com.autoservice.views;

import com.autoservice.DataStore;
import com.autoservice.WorkOrder;
import com.autoservice.controllers.OrderController;
import com.autoservice.dialogs.PrintOrderDialog;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class OrderView {

    private static TableView<WorkOrder> orderTable;
    private static FilteredList<WorkOrder> filteredOrders;
    private static TextField searchField;
    private static ComboBox<String> searchTypeCombo;

    // Кнопки действий
    private static Button viewBtn;
    private static Button editBtn;
    private static Button deleteBtn;
    private static Button printBtn;
    private static Button createOrderBtn;

    public static VBox create() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #f5f7fa;");

        // ========== ВЕРХНЯЯ ПАНЕЛЬ С КНОПКАМИ ==========
        HBox topPanel = new HBox(15);
        topPanel.setAlignment(Pos.CENTER_LEFT);
        topPanel.setPadding(new Insets(10));
        topPanel.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");

        // Панель поиска
        HBox searchBox = createSearchPanel();

        // Кнопки действий
        viewBtn = createActionButton("Просмотр", "#3498db");
        editBtn = createActionButton("Изменить", "#f39c12");
        deleteBtn = createActionButton("Удалить", "#e74c3c");
        printBtn = createActionButton("Печать", "#9b59b6");
        createOrderBtn = createActionButton("Новый", "#2ecc71");

        // Назначаем действия кнопкам
        viewBtn.setOnAction(e -> onView());
        editBtn.setOnAction(e -> onEdit());
        deleteBtn.setOnAction(e -> onDelete());
        printBtn.setOnAction(e -> onPrint());
        createOrderBtn.setOnAction(e -> OrderController.createOrder());

        // Изначально кнопки (кроме "Новый") неактивны
        viewBtn.setDisable(true);
        editBtn.setDisable(true);
        deleteBtn.setDisable(true);
        printBtn.setDisable(true);

        // Разделитель
        Label separator = new Label("|");
        separator.setStyle("-fx-text-fill: #bdc3c7; -fx-font-size: 18px; -fx-padding: 0 5 0 5;");

        topPanel.getChildren().addAll(searchBox, separator, viewBtn, editBtn, deleteBtn, printBtn, createOrderBtn);
        HBox.setHgrow(searchBox, Priority.ALWAYS);

        // ========== ТАБЛИЦА ЗАКАЗОВ ==========
        orderTable = createOrderTable();

        // Обработчик выделения строки
        orderTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean isSelected = newVal != null;
            viewBtn.setDisable(!isSelected);
            editBtn.setDisable(!isSelected);
            deleteBtn.setDisable(!isSelected);
            printBtn.setDisable(!isSelected);
        });

        // Снятие выделения при клике на пустую область таблицы
        orderTable.setRowFactory(tv -> {
            TableRow<WorkOrder> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1) {
                    if (row.isEmpty()) {
                        // Клик по пустой строке - снимаем выделение
                        orderTable.getSelectionModel().clearSelection();
                    }
                }
            });
            return row;
        });

        root.getChildren().addAll(topPanel, orderTable);
        VBox.setVgrow(orderTable, Priority.ALWAYS);

        return root;
    }

    private static Button createActionButton(String text, String color) {
        Button btn = new Button(text);
        btn.setPrefWidth(100);
        btn.setStyle(
                "-fx-background-color: " + color + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 12px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 8 0 8 0; " +
                        "-fx-background-radius: 4;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: derive(" + color + ", -10%); " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 12px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 8 0 8 0; " +
                        "-fx-background-radius: 4;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: " + color + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 12px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 8 0 8 0; " +
                        "-fx-background-radius: 4;"
        ));
        return btn;
    }

    private static HBox createSearchPanel() {
        Label searchLabel = new Label("Поиск:");
        searchLabel.setStyle("-fx-font-weight: bold;");

        searchField = new TextField();
        searchField.setPromptText("Введите текст для поиска...");
        searchField.setPrefWidth(250);
        searchField.setStyle("-fx-padding: 8; -fx-font-size: 12px; -fx-background-radius: 4; -fx-border-radius: 4; -fx-border-color: #e0e0e0;");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyOrderFilter(newVal));

        searchTypeCombo = new ComboBox<>();
        searchTypeCombo.getItems().addAll("По клиенту", "По номеру заказа (полный)", "По последним 4 цифрам номера");
        searchTypeCombo.setValue("По клиенту");
        searchTypeCombo.setPrefWidth(200);
        searchTypeCombo.setStyle("-fx-padding: 6; -fx-font-size: 12px; -fx-background-radius: 4; -fx-border-radius: 4; -fx-border-color: #e0e0e0;");
        searchTypeCombo.setOnAction(e -> applyOrderFilter(searchField.getText()));

        Button clearBtn = new Button("Очистить");
        clearBtn.setStyle(
                "-fx-background-color: #95a5a6; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 11px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 6 12 6 12; " +
                        "-fx-background-radius: 4;"
        );
        clearBtn.setOnAction(e -> {
            searchField.clear();
            applyOrderFilter("");
        });

        HBox searchBox = new HBox(10, searchLabel, searchField, searchTypeCombo, clearBtn);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        return searchBox;
    }

    private static TableView<WorkOrder> createOrderTable() {
        TableView<WorkOrder> table = new TableView<>();
        table.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #e0e0e0; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8; " +
                        "-fx-font-size: 12px;"
        );

        TableColumn<WorkOrder, String> colId = new TableColumn<>("№ заказа");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(160);
        colId.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<WorkOrder, String> colClient = new TableColumn<>("Клиент");
        colClient.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getClient().getName()));
        colClient.setPrefWidth(180);
        colClient.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<WorkOrder, String> colCar = new TableColumn<>("Автомобиль");
        colCar.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getClient().getCarModel() + " (" + cellData.getValue().getClient().getCarNumber() + ")"));
        colCar.setPrefWidth(220);
        colCar.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<WorkOrder, String> colServices = new TableColumn<>("Услуги");
        colServices.setCellValueFactory(cellData -> {
            WorkOrder order = cellData.getValue();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < order.getServices().size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(order.getServices().get(i));
            }
            return new SimpleStringProperty(sb.toString());
        });
        colServices.setPrefWidth(250);
        colServices.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<WorkOrder, Double> colTotal = new TableColumn<>("Сумма");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setPrefWidth(120);
        colTotal.setStyle("-fx-alignment: CENTER-RIGHT;");
        colTotal.setCellFactory(col -> new TableCell<WorkOrder, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%,.0f руб.", item));
                }
                setAlignment(Pos.CENTER_RIGHT);
            }
        });

        TableColumn<WorkOrder, String> colStatus = new TableColumn<>("Статус");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setPrefWidth(150);
        colStatus.setStyle("-fx-alignment: CENTER;");
        colStatus.setCellFactory(col -> new TableCell<WorkOrder, String>() {
            private final ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(WorkOrder.getAllStatuses()));
            {
                comboBox.setOnAction(e -> {
                    WorkOrder order = getTableView().getItems().get(getIndex());
                    OrderController.changeOrderStatus(order, comboBox.getValue());
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

        table.getColumns().addAll(colId, colClient, colCar, colServices, colTotal, colStatus);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Чередование цветов строк
        table.setRowFactory(tv -> {
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

            // Снятие выделения при клике на пустую строку
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1 && row.isEmpty()) {
                    table.getSelectionModel().clearSelection();
                }
            });

            return row;
        });

        table.setStyle(table.getStyle() +
                "-fx-selection-bar: #3498db; " +
                "-fx-selection-bar-text: white;");

        // Настройка фильтрованного списка
        filteredOrders = new FilteredList<>(FXCollections.observableArrayList(DataStore.getOrders()), p -> true);
        table.setItems(filteredOrders);
        OrderController.setTable(table);

        // Двойной клик для редактирования
        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                WorkOrder selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    OrderController.editOrder(selected);
                }
            }
        });

        return table;
    }

    private static void applyOrderFilter(String filter) {
        if (filter == null || filter.trim().isEmpty()) {
            filteredOrders.setPredicate(p -> true);
        } else {
            String lowerFilter = filter.toLowerCase().trim();
            String searchType = searchTypeCombo.getValue();

            filteredOrders.setPredicate(order -> {
                if (searchType.equals("По номеру заказа (полный)")) {
                    return order.getId() != null && order.getId().toLowerCase().contains(lowerFilter);
                } else if (searchType.equals("По последним 4 цифрам номера")) {
                    if (order.getId() == null) return false;
                    String id = order.getId();
                    if (id.length() >= 4) {
                        String last4 = id.substring(id.length() - 4);
                        return last4.contains(lowerFilter);
                    }
                    return false;
                } else { // По клиенту
                    return order.getClient().getName().toLowerCase().contains(lowerFilter) ||
                            order.getClient().getPhone().toLowerCase().contains(lowerFilter) ||
                            order.getClient().getCarNumber().toLowerCase().contains(lowerFilter);
                }
            });
        }
    }

    // Методы для обработки действий
    private static void onView() {
        WorkOrder selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            OrderController.viewOrder(selected);
        }
    }

    private static void onEdit() {
        WorkOrder selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            OrderController.editOrder(selected);
        }
    }

    private static void onDelete() {
        WorkOrder selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            OrderController.deleteOrder(selected);
        }
    }

    private static void onPrint() {
        WorkOrder selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            PrintOrderDialog.show(selected);
        }
    }

    public static void refreshOrderList() {
        if (orderTable != null) {
            filteredOrders = new FilteredList<>(FXCollections.observableArrayList(DataStore.getOrders()), p -> true);
            orderTable.setItems(filteredOrders);
            if (searchField != null) {
                applyOrderFilter(searchField.getText());
            }
        }
    }
}