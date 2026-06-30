package com.autoservice.views;

import com.autoservice.DataStore;
import com.autoservice.WorkOrder;
import com.autoservice.controllers.OrderController;
import com.autoservice.dialogs.PrintOrderDialog;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
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

    private static Button editBtn, deleteBtn, printBtn, createOrderBtn;

    // Единый источник данных
    private static ObservableList<WorkOrder> masterData = FXCollections.observableArrayList();
    private static SortedList<WorkOrder> sortedData;

    // Флаг для отслеживания инициализации
    private static boolean isInitialized = false;

    public static VBox create() {
        VBox root = new VBox(10);
        root.getStyleClass().add("main-container");

        HBox topPanel = new HBox(15);
        topPanel.setAlignment(Pos.CENTER_LEFT);
        topPanel.setPadding(new Insets(10));
        topPanel.getStyleClass().add("top-panel");

        HBox searchBox = createSearchPanel();

        editBtn = createActionButton("Изменить");
        deleteBtn = createActionButton("Удалить");
        printBtn = createActionButton("Печать");
        createOrderBtn = createActionButton("Новый");

        editBtn.setStyle("-fx-background-color: #f39c12;");
        deleteBtn.setStyle("-fx-background-color: #e74c3c;");
        printBtn.setStyle("-fx-background-color: #9b59b6;");
        createOrderBtn.setStyle("-fx-background-color: #2ecc71;");

        editBtn.setOnAction(e -> onEdit());
        deleteBtn.setOnAction(e -> onDelete());
        printBtn.setOnAction(e -> onPrint());
        createOrderBtn.setOnAction(e -> OrderController.createOrder());

        editBtn.setDisable(true);
        deleteBtn.setDisable(true);
        printBtn.setDisable(true);

        Label separator = new Label("|");
        separator.getStyleClass().add("separator");

        topPanel.getChildren().addAll(searchBox, separator, editBtn, deleteBtn, printBtn, createOrderBtn);
        HBox.setHgrow(searchBox, Priority.ALWAYS);

        // ========== ПАНЕЛЬ РАСШИРЕННЫХ ФИЛЬТРОВ ==========
        advancedToggleBtn = new ToggleButton("▼ Расширенный фильтр");
        advancedToggleBtn.getStyleClass().add("toggle-button");
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
        orderTable.getStyleClass().add("table-view");
        setupTableColumns();

        // Настраиваем SortedList
        sortedData = new SortedList<>(masterData);
        sortedData.comparatorProperty().bind(orderTable.comparatorProperty());
        orderTable.setItems(sortedData);

        // Загружаем данные
        refreshOrderList();
        isInitialized = true;

        orderTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
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
        filterBox.getStyleClass().add("filter-panel");

        Label titleLabel = new Label("Фильтры");
        titleLabel.getStyleClass().add("filter-title");

        // Строка 1: Статус
        HBox row1 = new HBox(15);
        row1.setAlignment(Pos.CENTER_LEFT);

        Label statusLabel = new Label("Статус:");
        statusLabel.getStyleClass().add("filter-label");
        statusFilterCombo = new ComboBox<>();
        statusFilterCombo.getItems().addAll("Все", "Новый", "В работе", "Закрыт");
        statusFilterCombo.setValue("Все");
        statusFilterCombo.setPrefWidth(120);
        statusFilterCombo.setOnAction(e -> applyFilters());

        row1.getChildren().addAll(statusLabel, statusFilterCombo);

        // Строка 2: Дата от и до
        HBox row2 = new HBox(15);
        row2.setAlignment(Pos.CENTER_LEFT);

        Label dateFromLabel = new Label("Дата от:");
        dateFromLabel.getStyleClass().add("filter-label");
        dateFromPicker = new DatePicker();
        dateFromPicker.setPromptText("дд.мм.гггг");
        dateFromPicker.setPrefWidth(180);
        dateFromPicker.setOnAction(e -> applyFilters());

        Label dateToLabel = new Label("Дата до:");
        dateToLabel.getStyleClass().add("filter-label");
        dateToPicker = new DatePicker();
        dateToPicker.setPromptText("дд.мм.гггг");
        dateToPicker.setPrefWidth(180);
        dateToPicker.setOnAction(e -> applyFilters());

        row2.getChildren().addAll(dateFromLabel, dateFromPicker, dateToLabel, dateToPicker);

        // Строка 3: Сумма от и до
        HBox row3 = new HBox(15);
        row3.setAlignment(Pos.CENTER_LEFT);

        Label minTotalLabel = new Label("Сумма от:");
        minTotalLabel.getStyleClass().add("filter-label");
        minTotalField = new TextField();
        minTotalField.setPromptText("0");
        minTotalField.setPrefWidth(100);
        minTotalField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        Label maxTotalLabel = new Label("Сумма до:");
        maxTotalLabel.getStyleClass().add("filter-label");
        maxTotalField = new TextField();
        maxTotalField.setPromptText("100000");
        maxTotalField.setPrefWidth(100);
        maxTotalField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        resetFiltersBtn = new Button("Сбросить фильтры");
        resetFiltersBtn.getStyleClass().add("reset-button");
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
        // ====== № ЗАКАЗА ======
        TableColumn<WorkOrder, String> colId = new TableColumn<>("№ заказа");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(160);
        colId.setSortable(true);

        // ====== КЛИЕНТ (ФАМИЛИЯ + ИМЯ) ======
        TableColumn<WorkOrder, String> colClient = new TableColumn<>("Клиент");
        colClient.setCellValueFactory(cellData -> {
            WorkOrder order = cellData.getValue();
            String lastName = order.getClient().getLastName();
            String firstName = order.getClient().getName();
            String fullName = (lastName != null && !lastName.isEmpty())
                    ? lastName + " " + firstName
                    : firstName;
            return new SimpleStringProperty(fullName);
        });
        colClient.setPrefWidth(180);
        colClient.setSortable(true);

        // ====== АВТОМОБИЛЬ ======
        TableColumn<WorkOrder, String> colCar = new TableColumn<>("Автомобиль");
        colCar.setCellValueFactory(cellData -> {
            String model = cellData.getValue().getClient().getCarModel();
            String number = cellData.getValue().getClient().getCarNumber();
            return new SimpleStringProperty(model + " (" + number + ")");
        });
        colCar.setPrefWidth(200);
        colCar.setSortable(true);

        // ====== УСЛУГИ ======
        TableColumn<WorkOrder, String> colServices = new TableColumn<>("Услуги");
        colServices.setCellValueFactory(cellData -> {
            WorkOrder order = cellData.getValue();
            String services = String.join(", ", order.getServices());
            return new SimpleStringProperty(services.isEmpty() ? "—" : services);
        });
        colServices.setPrefWidth(250);
        colServices.setSortable(true);

        // ====== СУММА ======
        TableColumn<WorkOrder, Double> colTotal = new TableColumn<>("Сумма");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setPrefWidth(120);
        colTotal.setSortable(true);
        colTotal.setCellFactory(col -> new TableCell<WorkOrder, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%,.0f руб.", item));
                    setAlignment(Pos.CENTER_RIGHT);
                }
            }
        });

        // ====== СТАТУС ======
        TableColumn<WorkOrder, String> colStatus = new TableColumn<>("Статус");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setPrefWidth(120);
        colStatus.setSortable(true);
        colStatus.setCellFactory(col -> new TableCell<WorkOrder, String>() {
            private final ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList("Новый", "В работе", "Закрыт"));
            {
                comboBox.setOnAction(e -> {
                    WorkOrder order = getTableView().getItems().get(getIndex());
                    if (order != null) {
                        String newStatus = comboBox.getValue();
                        order.setStatus(newStatus);
                        OrderController.changeOrderStatus(order, newStatus);
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

        // ====== ДВОЙНОЙ КЛИК ======
        orderTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                WorkOrder selected = orderTable.getSelectionModel().getSelectedItem();
                if (selected != null) OrderController.editOrder(selected);
            }
        });
    }

    private static Button createActionButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(100);
        btn.getStyleClass().add("action-button");
        return btn;
    }

    private static HBox createSearchPanel() {
        searchField = new TextField();
        searchField.setPromptText("Поиск по имени, телефону или номеру авто...");
        searchField.setPrefWidth(300);
        searchField.getStyleClass().add("search-field");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        Button clearBtn = new Button("Очистить");
        clearBtn.getStyleClass().add("clear-button");
        clearBtn.setOnAction(e -> {
            searchField.clear();
            applyFilters();
        });

        resultLabel = new Label();
        resultLabel.getStyleClass().add("result-label");

        HBox searchBox = new HBox(10, new Label("Поиск:"), searchField, clearBtn, resultLabel);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        return searchBox;
    }

    private static String normalizeStatus(String status) {
        if (status == null) return null;

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
        if (orderTable == null) return;

        // Принудительно загружаем свежие данные из DataStore
        List<WorkOrder> allOrders = DataStore.getOrders();
        List<WorkOrder> filtered = new ArrayList<>();

        String searchText = searchField != null ? searchField.getText() : "";
        String statusFilter = statusFilterCombo != null ? statusFilterCombo.getValue() : "Все";
        LocalDate fromDate = dateFromPicker != null ? dateFromPicker.getValue() : null;
        LocalDate toDate = dateToPicker != null ? dateToPicker.getValue() : null;
        String minTotalText = minTotalField != null ? minTotalField.getText() : "";
        String maxTotalText = maxTotalField != null ? maxTotalField.getText() : "";

        for (WorkOrder order : allOrders) {
            // Поиск по тексту
            if (searchText != null && !searchText.trim().isEmpty()) {
                String lowerFilter = searchText.toLowerCase().trim();
                if (order.getClient() != null) {
                    String lastName = order.getClient().getLastName();
                    String name = order.getClient().getName();
                    String phone = order.getClient().getPhone();
                    String carNumber = order.getClient().getCarNumber();

                    boolean match = (name != null && name.toLowerCase().contains(lowerFilter)) ||
                            (lastName != null && lastName.toLowerCase().contains(lowerFilter)) ||
                            (phone != null && phone.toLowerCase().contains(lowerFilter)) ||
                            (carNumber != null && carNumber.toLowerCase().contains(lowerFilter));
                    if (!match) continue;
                } else {
                    continue;
                }
            }

            // Фильтр по статусу
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

        // ОБНОВЛЯЕМ masterData
        masterData.clear();
        masterData.addAll(filtered);

        // ПРИНУДИТЕЛЬНО ОБНОВЛЯЕМ ТАБЛИЦУ
        orderTable.refresh();

        // Обновляем счётчик
        if (resultLabel != null) {
            resultLabel.setText("Найдено: " + filtered.size() + " из " + allOrders.size() + " заказов");
        }

        System.out.println("OrderView refreshed: " + filtered.size() + " orders shown");
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
            System.out.println("refreshOrderList() called - forcing update");
            applyFilters();
        }
    }
}