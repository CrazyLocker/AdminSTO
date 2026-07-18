package com.autoservice.views;

import com.autoservice.DataStore;
import com.autoservice.DateUtils;
import com.autoservice.WorkOrder;
import com.autoservice.Appointment;
import com.autoservice.controllers.OrderController;
import com.autoservice.dialogs.PrintOrderDialog;
import com.autoservice.services.TableStateManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderView {

    private static TableView<WorkOrder> orderTable;
    private static TextField searchField;
    private static Label resultLabel;

    // Getter для получения таблицы извне
    public static TableView<WorkOrder> getTable() {
        return orderTable;
    }

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

    private static final Logger logger = LoggerFactory.getLogger(OrderView.class);

    public static VBox create() {
        VBox root = new VBox(10);
        root.getStyleClass().add("main-container");

        HBox topPanel = new HBox(15);
        topPanel.setAlignment(Pos.CENTER_LEFT);
        topPanel.setPadding(new Insets(10));
        topPanel.getStyleClass().add("top-panel");

        HBox searchBox = createSearchPanel();

        // ====== КНОПКИ БЕЗ ИКОНОК ======
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

        topPanel.getChildren().addAll(searchBox, createSeparator(), editBtn, deleteBtn, printBtn, createOrderBtn);
        HBox.setHgrow(searchBox, Priority.ALWAYS);

        // ========== ПАНЕЛЬ РАСШИРЕННЫХ ФИЛЬТРОВ ==========
        advancedToggleBtn = new ToggleButton("Расширенный фильтр");
        advancedToggleBtn.getStyleClass().add("toggle-button");
        advancedToggleBtn.setSelected(false);

        advancedFilterPanel = createAdvancedFilterPanel();
        advancedFilterPanel.setVisible(false);
        advancedFilterPanel.setManaged(false);

        advancedToggleBtn.setOnAction(e -> {
            boolean show = advancedToggleBtn.isSelected();
            advancedFilterPanel.setVisible(show);
            advancedFilterPanel.setManaged(show);
            advancedToggleBtn.setText(show ? "Скрыть фильтры" : "Расширенный фильтр");
        });

        // ========== ТАБЛИЦА ==========
        orderTable = new TableView<>();
        orderTable.getStyleClass().add("table-view");
        orderTable.setId("orderTable");
        setupTableColumns();

        sortedData = new SortedList<>(masterData);
        sortedData.comparatorProperty().bind(orderTable.comparatorProperty());
        orderTable.setItems(sortedData);

        refreshOrderList();

        orderTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            editBtn.setDisable(newVal == null);
            deleteBtn.setDisable(newVal == null);
            printBtn.setDisable(newVal == null);
        });

        // ========== ГОЯЧИЕ КЛАВИШИ ==========
        orderTable.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.isControlDown() && e.getCode() == javafx.scene.input.KeyCode.S) {
                e.consume();
                onEdit();
            } else if (e.isControlDown() && e.getCode() == KeyCode.N) {
                e.consume();
                OrderController.createOrder();
            } else if (e.getCode() == javafx.scene.input.KeyCode.DELETE) {
                e.consume();
                onDelete();
            } else if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                e.consume();
                searchField.clear();
            }
        });

        root.getChildren().addAll(topPanel, advancedToggleBtn, advancedFilterPanel, orderTable);
        VBox.setVgrow(orderTable, Priority.ALWAYS);

        // Загружаем состояние таблицы ПОСЛЕ отрисовки
        Platform.runLater(() -> {
            if (orderTable != null) {
                TableStateManager.loadTableState(orderTable, "orderTable");
            }
        });

        return root;
    }

    // Вспомогательный метод для создания разделителя
    private static Label createSeparator() {
        Label separator = new Label("|");
        separator.getStyleClass().add("separator");
        return separator;
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
        statusFilterCombo.getItems().addAll("Все", WorkOrder.STATUS_NEW, WorkOrder.STATUS_IN_PROGRESS, WorkOrder.STATUS_CLOSED);
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

        resetFiltersBtn = new Button("Сбросить");
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
        TableColumn<WorkOrder, String> colId = new TableColumn<>("№ заказа");
        colId.setId("colOrderId");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(160);
        colId.setSortable(true);
        
        // Пользовательский компаратор для сортировки по порядковому номеру (4 последних цифры)
        colId.setComparator((id1, id2) -> {
            // Если один из ID пустой, он будет в конце
            if (id1 == null && id2 == null) return 0;
            if (id1 == null) return 1;
            if (id2 == null) return -1;
            
            if (id1.isEmpty() && id2.isEmpty()) return 0;
            if (id1.isEmpty()) return 1;
            if (id2.isEmpty()) return -1;
            
            // Извлекаем порядковый номер (4 последних цифры)
            try {
                String[] parts1 = id1.split("-");
                String[] parts2 = id2.split("-");
                
                if (parts1.length >= 2 && parts2.length >= 2) {
                    // Берём последнюю часть как порядковый номер
                    int num1 = Integer.parseInt(parts1[parts1.length - 1]);
                    int num2 = Integer.parseInt(parts2[parts2.length - 1]);
                    return Integer.compare(num1, num2);
                }
            } catch (Exception e) {
                logger.error("Ошибка парсинга ID для сортировки", e);
            }
            
            // Fallback: лексикографическая сортировка
            return id1.compareTo(id2);
        });

        TableColumn<WorkOrder, String> colClient = new TableColumn<>("Клиент");
        colClient.setId("colClient");
        colClient.setCellValueFactory(cellData -> {
            WorkOrder order = cellData.getValue();
            String lastName = order.getClient().getLastName();
            String firstName = order.getClient().getName();
            return new SimpleStringProperty((lastName != null && !lastName.isEmpty()) ? lastName + " " + firstName : firstName);
        });
        colClient.setPrefWidth(180);
        colClient.setSortable(true);

        TableColumn<WorkOrder, String> colCar = new TableColumn<>("Автомобиль");
        colCar.setId("colCar");
        colCar.setCellValueFactory(cellData -> {
            String model = cellData.getValue().getClient().getCarModel();
            String number = cellData.getValue().getClient().getCarNumber();
            return new SimpleStringProperty(model + " (" + number + ")");
        });
        colCar.setPrefWidth(200);
        colCar.setSortable(true);

        TableColumn<WorkOrder, String> colServices = new TableColumn<>("Услуги");
        colServices.setId("colServices");
        colServices.setCellValueFactory(cellData -> {
            WorkOrder order = cellData.getValue();
            return new SimpleStringProperty(String.join(", ", order.getServices()));
        });
        colServices.setPrefWidth(250);
        colServices.setSortable(true);

        TableColumn<WorkOrder, String> colAppointment = new TableColumn<>("Запись");
        colAppointment.setId("colAppointment");
        colAppointment.setCellValueFactory(cellData -> {
            WorkOrder order = cellData.getValue();
            String orderId = order.getId();
            Appointment appointment = DataStore.getAppointmentByOrderId(orderId);
            if (appointment != null && appointment.getDate() != null && !appointment.getDate().isEmpty()) {
                // Выводим данные из БД в исходном формате: "dd/MM/yyyy HH:mm"
                return new SimpleStringProperty(appointment.getDate() + " " + appointment.getTime());
            }
            return new SimpleStringProperty("");
        });
        colAppointment.setPrefWidth(160);
        colAppointment.setSortable(true);
        // Сортировка по дате и времени (формат из БД: dd/MM/yyyy HH:mm)
        colAppointment.setComparator((s1, s2) -> {
            // Пустые значения в конец
            if (s1 == null || s1.isEmpty()) return s2 == null || s2.isEmpty() ? 0 : 1;
            if (s2 == null || s2.isEmpty()) return -1;
            
            // Извлекаем LocalDateTime из строки "dd/MM/yyyy HH:mm"
            java.time.LocalDateTime dt1 = parseAppointmentDateTime(s1);
            java.time.LocalDateTime dt2 = parseAppointmentDateTime(s2);
            
            if (dt1 != null && dt2 != null) {
                return dt1.compareTo(dt2);
            }
            
            // Fallback: строковое сравнение
            return s1.compareTo(s2);
        });

        TableColumn<WorkOrder, Double> colTotal = new TableColumn<>("Сумма");
        colTotal.setId("colTotal");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setPrefWidth(120);
        colTotal.setSortable(true);
        colTotal.setCellFactory(col -> new TableCell<WorkOrder, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(String.format("%,.0f руб.", item));
            }
        });

        TableColumn<WorkOrder, String> colStatus = new TableColumn<>("Статус");
        colStatus.setId("colStatus");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setPrefWidth(120);
        colStatus.setSortable(true);
        colStatus.setCellFactory(col -> new TableCell<WorkOrder, String>() {
            private final ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList("Новый", "В работе", "Закрыт"));
            {
                comboBox.setOnAction(e -> {
                    WorkOrder order = getTableView().getItems().get(getIndex());
                    if (order != null) {
                        order.setStatus(comboBox.getValue());
                        OrderController.changeOrderStatus(order, comboBox.getValue());
                    }
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); }
                else { comboBox.setValue(item); setGraphic(comboBox); setText(null); }
            }
        });

        orderTable.getColumns().addAll(colId, colClient, colCar, colServices, colAppointment, colTotal, colStatus);
        // Отключаем CONSTRAINED_RESIZE_POLICY — позволяет сохранять ширину колонок
        orderTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

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
        if (status.equalsIgnoreCase("Новый") || status.contains("Нов")) return "Новый";
        if (status.equalsIgnoreCase("В работе") || status.contains("работе")) return "В работе";
        if (status.equalsIgnoreCase("Закрыт") || status.equals("Завершён") || status.contains("Закр") || status.contains("Завер")) return "Закрыт";
        return status;
    }
    
    /**
     * Извлекает дату из номера заказа и преобразует её в формат для сортировки.
     * Формат заказа: ZAK-dd-MM-yy-0001
     * Выход: dd-MM-yyyy (подходит для строковой сортировки)
     */
    private static String extractSortDate(String[] parts) {
        if (parts.length < 5) return "";
        
        String day = parts[1]; // dd
        String month = parts[2]; // MM
        String yearSuffix = parts[3]; // yy
        
        // Преобразуем год в 4-значный формат
        int year;
        if (yearSuffix.length() == 2) {
            year = 2000 + Integer.parseInt(yearSuffix);
        } else if (yearSuffix.length() == 4) {
            year = Integer.parseInt(yearSuffix);
        } else {
            return "";
        }
        
        return String.format("%s-%s-%04d", day, month, year);
    }

    private static void applyFilters() {
        if (orderTable == null) return;

        List<WorkOrder> allOrders = DataStore.getOrders();
        List<WorkOrder> filtered = new ArrayList<>();

        String searchText = searchField != null ? searchField.getText() : "";
        String statusFilter = statusFilterCombo != null ? statusFilterCombo.getValue() : "Все";
        LocalDate fromDate = dateFromPicker != null ? dateFromPicker.getValue() : null;
        LocalDate toDate = dateToPicker != null ? dateToPicker.getValue() : null;
        String minTotalText = minTotalField != null ? minTotalField.getText() : "";
        String maxTotalText = maxTotalField != null ? maxTotalField.getText() : "";

        for (WorkOrder order : allOrders) {
            if (searchText != null && !searchText.trim().isEmpty()) {
                String lowerFilter = searchText.toLowerCase().trim();
                if (order.getClient() != null) {
                    boolean match = (order.getClient().getName() != null && order.getClient().getName().toLowerCase().contains(lowerFilter)) ||
                            (order.getClient().getLastName() != null && order.getClient().getLastName().toLowerCase().contains(lowerFilter)) ||
                            (order.getClient().getPhone() != null && order.getClient().getPhone().toLowerCase().contains(lowerFilter)) ||
                            (order.getClient().getCarNumber() != null && order.getClient().getCarNumber().toLowerCase().contains(lowerFilter));
                    if (!match) continue;
                } else continue;
            }

            if (statusFilter != null && !statusFilter.equals("Все")) {
                String normalizedOrderStatus = normalizeStatus(order.getStatus());
                if (!statusFilter.equals(normalizedOrderStatus)) continue;
            }

            if (fromDate != null) {
                LocalDate orderDate = parseDate(order.getCreatedDate());
                if (orderDate == null || orderDate.isBefore(fromDate)) continue;
            }

            if (toDate != null) {
                LocalDate orderDate = parseDate(order.getCreatedDate());
                if (orderDate == null || orderDate.isAfter(toDate)) continue;
            }

            if (minTotalText != null && !minTotalText.trim().isEmpty()) {
                try { if (order.getTotal() < Double.parseDouble(minTotalText.trim())) continue; } catch (NumberFormatException ignored) {}
            }

            if (maxTotalText != null && !maxTotalText.trim().isEmpty()) {
                try { if (order.getTotal() > Double.parseDouble(maxTotalText.trim())) continue; } catch (NumberFormatException ignored) {}
            }

            filtered.add(order);
        }

        masterData.clear();
        masterData.addAll(filtered);
        orderTable.refresh();

        if (resultLabel != null) {
            resultLabel.setText("Найдено: " + filtered.size() + " из " + allOrders.size() + " заказов");
        }
    }

    private static LocalDate parseDate(String dateStr) {
        return DateUtils.parseDate(dateStr);
    }

    /**
     * Парсит строку формата "18 июля 2026" в LocalDate для сортировки
     */
    private static java.time.LocalDate parseDateString(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            // Формат: "18 июля 2026"
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", new java.util.Locale("ru"));
            return java.time.LocalDate.parse(dateStr, formatter);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Извлекает LocalDateTime из строки с датой и временем.
     * Поддерживает несколько форматов:
     * 1. "18 июля 2026 г. в 12-00" (русский формат)
     * 2. "2026-07-18 12-00" (ISO формат даты)
     * 3. "18/07/2026 12-00" (dd/MM/yyyy формат даты)
     * Время может быть с дефисом (-) или двоеточием (:)
     */
    private static java.time.LocalDateTime extractLocalDateTime(String s) {
        if (s == null || s.isEmpty()) return null;
        
        try {
            java.time.LocalDate date;
            String timeStr;
            
            // Проверяем русский формат: "18 июля 2026 г. в 12-00"
            int idx = s.indexOf(" г. в ");
            if (idx > 0) {
                String dateStr = s.substring(0, idx).trim();
                timeStr = s.substring(idx + 5).trim();
                java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", new java.util.Locale("ru"));
                date = java.time.LocalDate.parse(dateStr, dateFormatter);
            } else {
                // Формат БД: "2026-07-18 12-00" или "18/07/2026 12-00"
                // Ищем пробел, разделяющий дату и время
                int spaceIdx = s.lastIndexOf(" ");
                if (spaceIdx > 0) {
                    String dateStr = s.substring(0, spaceIdx).trim();
                    timeStr = s.substring(spaceIdx + 1).trim();
                    
                    // Парсим дату в разных форматах
                    if (dateStr.contains("-") && !dateStr.contains("/")) {
                        // ISO формат: yyyy-MM-dd
                        date = java.time.LocalDate.parse(dateStr);
                    } else if (dateStr.contains("/")) {
                        // dd/MM/yyyy формат
                        java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                        date = java.time.LocalDate.parse(dateStr, dateFormatter);
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            }
            
            // Парсим время - поддерживаем как дефис (-), так и двоеточие (:)
            String[] timeParts;
            if (timeStr.contains("-")) {
                timeParts = timeStr.split("-");
            } else {
                timeParts = timeStr.split(":");
            }
            
            if (timeParts.length >= 2) {
                return date.atTime(Integer.parseInt(timeParts[0]), Integer.parseInt(timeParts[1]));
            }
            
        } catch (Exception e) {
            // Логируем ошибку для отладки
            logger.debug("Ошибка парсинга даты-времени из строки '{}': {}", s, e.getMessage());
        }
        return null;
    }

    /**
     * Парсит дату и время из строки формата "dd/MM/yyyy HH:mm" (из БД).
     * Поддерживает как дефис (-), так и двоеточие (:) в формате времени.
     * Использует lastIndexOf для поиска последнего пробела между датой и временем.
     */
    private static java.time.LocalDateTime parseAppointmentDateTime(String s) {
        if (s == null || s.isEmpty()) return null;
        
        try {
            // Ищем последний пробел, разделяющий дату и время
            int spaceIdx = s.lastIndexOf(" ");
            if (spaceIdx > 0 && spaceIdx < s.length() - 1) {
                String dateStr = s.substring(0, spaceIdx).trim();
                String timeStr = s.substring(spaceIdx + 1).trim();
                
                // Валидация: дата должна быть в формате dd/MM/yyyy (минимум 10 символов)
                if (dateStr.length() < 10) {
                    logger.debug("Некорректная дата: {} (слишком короткая)", dateStr);
                    return null;
                }
                
                // Проверяем, что дата в формате dd/MM/yyyy (содержит два слеша)
                int slashCount = 0;
                for (char c : dateStr.toCharArray()) {
                    if (c == '/') slashCount++;
                }
                if (slashCount != 2) {
                    logger.debug("Некорректный формат даты: {} (ожидается dd/MM/yyyy)", dateStr);
                    return null;
                }
                
                // Парсим дату в формате dd/MM/yyyy
                java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                java.time.LocalDate date = java.time.LocalDate.parse(dateStr, dateFormatter);
                
                // Валидация времени (должно быть не короче 4 символов, например 12:00)
                if (timeStr.length() < 4) {
                    logger.debug("Некорректное время: {} (слишком короткое)", timeStr);
                    return null;
                }
                
                // Парсим время - поддерживаем как дефис (-), так и двоеточие (:)
                String[] timeParts;
                if (timeStr.contains("-")) {
                    timeParts = timeStr.split("-");
                } else {
                    timeParts = timeStr.split(":");
                }
                
                if (timeParts.length >= 2) {
                    int hour = Integer.parseInt(timeParts[0]);
                    int minute = Integer.parseInt(timeParts[1]);
                    
                    // Валидация: час от 0 до 23, минута от 0 до 59
                    if (hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59) {
                        return date.atTime(hour, minute);
                    } else {
                        logger.debug("Некорректное время: {} (ожидается 00:00-23:59)", timeStr);
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Ошибка парсинга даты-времени записи из строки '{}': {}", s, e.getMessage());
        }
        return null;
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