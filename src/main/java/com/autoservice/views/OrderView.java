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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OrderView {

    private static TableView<WorkOrder> orderTable;
    private static TextField searchField;
    private static Label resultLabel;

    // Поля фильтров
    private static ComboBox<String> statusFilterCombo;
    private static DatePicker dateFromPicker;
    private static DatePicker dateToPicker;
    private static TextField minTotalField;
    private static TextField maxTotalField;
    private static ToggleButton advancedToggleBtn;
    private static VBox advancedFilterPanel;
    private static Button resetFiltersBtn;

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

        // ========== ПАНЕЛЬ РАСШИРЕННЫХ ФИЛЬТРОВ ==========
        advancedToggleBtn = new ToggleButton("▼ Расширенный фильтр");
        advancedToggleBtn.setStyle("-fx-background-color: #ecf0f1; -fx-font-size: 11px; -fx-padding: 5 10 5 10; -fx-margin: 5 0 0 0;");
        advancedToggleBtn.setSelected(false);

        advancedFilterPanel = createAdvancedFilterPanel();
        advancedFilterPanel.setVisible(false);
        advancedFilterPanel.setManaged(false);

        advancedToggleBtn.setOnAction(e -> {
            boolean show = advancedToggleBtn.isSelected();
            advancedFilterPanel.setVisible(show);
            advancedFilterPanel.setManaged(show);
            advancedToggleBtn.setText(show ? "▲ Расширенный фильтр" : "▼ Расширенный фильтр");
        });

        // ========== ТАБЛИЦА ==========
        orderTable = new TableView<>();
        setupTableColumns();

        // Загружаем данные
        applyFilters();

        orderTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            viewBtn.setDisable(newVal == null);
            editBtn.setDisable(newVal == null);
            deleteBtn.setDisable(newVal == null);
            printBtn.setDisable(newVal == null);
        });

        root.getChildren().addAll(topPanel, advancedToggleBtn, advancedFilterPanel, orderTable);
        VBox.setVgrow(orderTable, Priority.ALWAYS);

        return root;
    }

    private static VBox createAdvancedFilterPanel() {
        VBox filterBox = new VBox(10);
        filterBox.setPadding(new Insets(10));
        filterBox.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-background-radius: 5;");

        Label titleLabel = new Label("Фильтры");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        // Строка 1: Статус
        HBox row1 = new HBox(15);
        row1.setAlignment(Pos.CENTER_LEFT);

        Label statusLabel = new Label("Статус:");
        statusLabel.setStyle("-fx-font-weight: bold;");
        statusFilterCombo = new ComboBox<>();
        statusFilterCombo.getItems().addAll("Все", "Новый", "В работе", "Закрыт");
        statusFilterCombo.setValue("Все");
        statusFilterCombo.setPrefWidth(120);
        statusFilterCombo.setOnAction(e -> applyFilters());

        row1.getChildren().addAll(statusLabel, statusFilterCombo);

        // Строка 2: Дата от и до - УВЕЛИЧЕННЫЙ РАЗМЕР
        HBox row2 = new HBox(15);
        row2.setAlignment(Pos.CENTER_LEFT);

        Label dateFromLabel = new Label("Дата от:");
        dateFromLabel.setStyle("-fx-font-weight: bold;");
        dateFromPicker = new DatePicker();
        dateFromPicker.setPromptText("дд.мм.гггг");
        dateFromPicker.setPrefWidth(180);
        dateFromPicker.setStyle("-fx-font-size: 13px; -fx-padding: 6;");
        dateFromPicker.setOnAction(e -> applyFilters());

        Label dateToLabel = new Label("Дата до:");
        dateToLabel.setStyle("-fx-font-weight: bold;");
        dateToPicker = new DatePicker();
        dateToPicker.setPromptText("дд.мм.гггг");
        dateToPicker.setPrefWidth(180);
        dateToPicker.setStyle("-fx-font-size: 13px; -fx-padding: 6;");
        dateToPicker.setOnAction(e -> applyFilters());

        row2.getChildren().addAll(dateFromLabel, dateFromPicker, dateToLabel, dateToPicker);

        // Строка 3: Сумма от и до
        HBox row3 = new HBox(15);
        row3.setAlignment(Pos.CENTER_LEFT);

        Label minTotalLabel = new Label("Сумма от:");
        minTotalLabel.setStyle("-fx-font-weight: bold;");
        minTotalField = new TextField();
        minTotalField.setPromptText("0");
        minTotalField.setPrefWidth(100);
        minTotalField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        Label maxTotalLabel = new Label("Сумма до:");
        maxTotalLabel.setStyle("-fx-font-weight: bold;");
        maxTotalField = new TextField();
        maxTotalField.setPromptText("100000");
        maxTotalField.setPrefWidth(100);
        maxTotalField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        // Кнопка сброса
        resetFiltersBtn = new Button("Сбросить фильтры");
        resetFiltersBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5 15 5 15;");
        resetFiltersBtn.setOnAction(e -> resetFilters());

        row3.getChildren().addAll(minTotalLabel, minTotalField, maxTotalLabel, maxTotalField, resetFiltersBtn);

        filterBox.getChildren().addAll(titleLabel, row1, row2, row3);
        return filterBox;
    }

    private static void resetFilters() {
        statusFilterCombo.setValue("Все");
        dateFromPicker.setValue(null);
        dateToPicker.setValue(null);
        minTotalField.clear();
        maxTotalField.clear();
        applyFilters();
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
        colStatus.setPrefWidth(100);
        colStatus.setCellFactory(col -> new TableCell<WorkOrder, String>() {
            private final ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList("Новый", "В работе", "Закрыт"));
            {
                comboBox.setOnAction(e -> {
                    WorkOrder order = getTableView().getItems().get(getIndex());
                    if (order != null) {
                        String newStatus = comboBox.getValue();
                        order.setStatus(newStatus);
                        OrderController.changeOrderStatus(order, newStatus);
                        applyFilters(); // Обновляем фильтр после изменения статуса
                    }
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
        searchField.setPromptText("Поиск по имени, телефону или номеру авто...");
        searchField.setPrefWidth(300);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        Button clearBtn = new Button("Очистить");
        clearBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold;");
        clearBtn.setOnAction(e -> {
            searchField.clear();
            applyFilters();
        });

        resultLabel = new Label();
        resultLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px; -fx-padding: 0 0 0 10;");

        HBox searchBox = new HBox(10, new Label("Поиск:"), searchField, clearBtn, resultLabel);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        return searchBox;
    }

    private static String normalizeStatus(String status) {
        if (status == null) return null;

        // Приводим к единому формату
        if (status.equalsIgnoreCase("Новый") || status.equals("НОВЫЙ") || status.contains("Нов")) {
            return "Новый";
        }
        if (status.equalsIgnoreCase("В работе") || status.equals("В РАБОТЕ") || status.contains("работе")) {
            return "В работе";
        }
        if (status.equalsIgnoreCase("Закрыт") || status.equals("ЗАКРЫТ") || status.equals("Завершён") ||
                status.equals("ЗАВЕРШЁН") || status.contains("Закр") || status.contains("Завер")) {
            return "Закрыт";
        }
        return status;
    }

    private static void applyFilters() {
        List<WorkOrder> allOrders = DataStore.getOrders();
        List<WorkOrder> filtered = new ArrayList<>();

        String searchText = searchField.getText();
        String statusFilter = statusFilterCombo.getValue();
        LocalDate fromDate = dateFromPicker.getValue();
        LocalDate toDate = dateToPicker.getValue();
        String minTotalText = minTotalField.getText();
        String maxTotalText = maxTotalField.getText();

        for (WorkOrder order : allOrders) {
            // Поиск по тексту
            if (searchText != null && !searchText.trim().isEmpty()) {
                String lowerFilter = searchText.toLowerCase().trim();
                if (order.getClient() != null) {
                    String name = order.getClient().getName();
                    String phone = order.getClient().getPhone();
                    String carNumber = order.getClient().getCarNumber();

                    boolean match = (name != null && name.toLowerCase().contains(lowerFilter)) ||
                            (phone != null && phone.toLowerCase().contains(lowerFilter)) ||
                            (carNumber != null && carNumber.toLowerCase().contains(lowerFilter));
                    if (!match) continue;
                } else {
                    continue;
                }
            }

            // Фильтр по статусу (с нормализацией)
            if (statusFilter != null && !statusFilter.equals("Все")) {
                String orderStatus = order.getStatus();
                String normalizedOrderStatus = normalizeStatus(orderStatus);

                if (!statusFilter.equals(normalizedOrderStatus)) {
                    continue;
                }
            }

            // Фильтр по дате от
            if (fromDate != null) {
                LocalDate orderDate = parseDate(order.getCreatedDate());
                if (orderDate == null || orderDate.isBefore(fromDate)) {
                    continue;
                }
            }

            // Фильтр по дате до
            if (toDate != null) {
                LocalDate orderDate = parseDate(order.getCreatedDate());
                if (orderDate == null || orderDate.isAfter(toDate)) {
                    continue;
                }
            }

            // Фильтр по минимальной сумме
            if (minTotalText != null && !minTotalText.trim().isEmpty()) {
                try {
                    double minTotal = Double.parseDouble(minTotalText.trim());
                    if (order.getTotal() < minTotal) continue;
                } catch (NumberFormatException ignored) {}
            }

            // Фильтр по максимальной сумме
            if (maxTotalText != null && !maxTotalText.trim().isEmpty()) {
                try {
                    double maxTotal = Double.parseDouble(maxTotalText.trim());
                    if (order.getTotal() > maxTotal) continue;
                } catch (NumberFormatException ignored) {}
            }

            filtered.add(order);
        }

        refreshTable(filtered);
        resultLabel.setText("Найдено: " + filtered.size() + " из " + allOrders.size() + " заказов");
    }

    private static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            String datePart = dateStr.split(" ")[0];
            return LocalDate.parse(datePart);
        } catch (Exception e) {
            return null;
        }
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
            applyFilters();
        }
    }
}