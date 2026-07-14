package com.autoservice.views;

import com.autoservice.*;
import com.autoservice.controllers.ClientController;
import com.autoservice.controllers.OrderController;
import com.autoservice.controllers.SettingsController;
import com.autoservice.dialogs.CreateOrderDialog;
import com.autoservice.dialogs.EditClientDialog;
import com.autoservice.dialogs.OrderDetailsDialog;
import com.autoservice.utils.IconHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardView extends ScrollPane {

    private static DashboardView instance;
    private final GridPane gridPane;
    private final NumberFormat currencyFormat;
    private Stage primaryStage;

    // ==================== СТАТИЧЕСКИЕ МЕТОДЫ ====================

    public static DashboardView create() {
        if (instance == null) {
            instance = new DashboardView();
        }
        return instance;
    }

    public static void refresh() {
        if (instance != null) {
            instance.doRefresh();
        }
    }

    public static void setStage(Stage stage) {
        if (instance != null) {
            instance.primaryStage = stage;
        }
    }

    // ==================== КОНСТРУКТОР ====================

    private DashboardView() {
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("ru", "RU"));

        gridPane = new GridPane();
        gridPane.setPadding(new Insets(20));
        gridPane.setHgap(20);
        gridPane.setVgap(20);
        gridPane.setAlignment(Pos.TOP_CENTER);

        setContent(gridPane);
        setFitToWidth(true);
        setStyle("-fx-background-color: #f4f6f9;");

        doRefresh();
    }

    // ==================== ОБНОВЛЕНИЕ ====================

    private void doRefresh() {
        gridPane.getChildren().clear();

        int row = 0;

        // ====== ВЕРХНИЕ КАРТОЧКИ (320x140) ======
        HBox cardsRow = new HBox(20);
        cardsRow.setAlignment(Pos.CENTER);
        cardsRow.setPadding(new Insets(0, 0, 20, 0));

        cardsRow.getChildren().addAll(
                createStatCard(IconHelper.assignment(32, "#3498db"), "Заказов",
                        String.valueOf(DataStore.getOrders().size()), "#3498db"),
                createStatCard(IconHelper.people(32, "#2ecc71"), "Клиентов",
                        String.valueOf(DataStore.getClients().size()), "#2ecc71"),
                createStatCard(IconHelper.warning(32, "#e74c3c"), "Остатки",
                        getLowStockCount(), "#e74c3c"),
                createStatCard(IconHelper.report(32, "#f39c12"), "Выручка",
                        getTotalRevenue(), "#f39c12")
        );

        gridPane.add(cardsRow, 0, row);
        row++;

        // ====== КАРТОЧКИ СТАТУСОВ (320x140) ======
        HBox activeRow = new HBox(20);
        activeRow.setAlignment(Pos.CENTER);
        activeRow.setPadding(new Insets(0, 0, 20, 0));

        activeRow.getChildren().addAll(
                createStatCard(IconHelper.settings(32, "#f1c40f"), "В работе",
                        String.valueOf(getActiveOrdersCount()), "#f1c40f"),
                createStatCard(IconHelper.checkCircle(32, "#27ae60"), "Выполнено",
                        String.valueOf(getCompletedOrdersCount()), "#27ae60"),
                createStatCard(IconHelper.event(32, "#9b59b6"), "Записей",
                        String.valueOf(DataStore.getAppointments().size()), "#9b59b6")
        );

        gridPane.add(activeRow, 0, row);
        row++;

        // ====== КНОПКИ БЫСТРЫХ ДЕЙСТВИЙ (190x40) ======
        HBox actionsRow = new HBox(15);
        actionsRow.setAlignment(Pos.CENTER);
        actionsRow.setPadding(new Insets(20, 0, 10, 0));

        actionsRow.getChildren().addAll(
                createActionButton("Новый заказ", "#3498db"),
                createActionButton("Новый клиент", "#2ecc71"),
                createActionButton("Запись", "#9b59b6"),
                createActionButton("Отчёт", "#f39c12"),
                createActionButton("Настройки", "#95a5a6")
        );

        gridPane.add(actionsRow, 0, row);
        row++;

        // ====== ЗАПИСИ НА ТЕКУЩУЮ НЕДЕЛЮ ======
        VBox appointmentsBox = createAppointmentsWeekBox();
        gridPane.add(appointmentsBox, 0, row);
    }

    // ==================== СОЗДАНИЕ КАРТОЧКИ (320x140) ====================

    private VBox createStatCard(Node icon, String title, String value, String color) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(320, 140);
        card.setMinSize(320, 140);
        card.setMaxSize(320, 140);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-radius: 12px;" +
                        "-fx-background-radius: 12px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 5);" +
                        "-fx-border-width: 4px 0 0 0;" +
                        "-fx-border-color: " + color + ";"
        );

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");
        titleLabel.setWrapText(true);

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        valueLabel.setWrapText(true);

        card.getChildren().addAll(icon, titleLabel, valueLabel);
        return card;
    }

    // ==================== КНОПКИ БЫСТРЫХ ДЕЙСТВИЙ (190x40) ====================

    private Button createActionButton(String text, String color) {
        Button btn = new Button(text);
        btn.setPrefSize(190, 40);
        btn.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-border-radius: 8px;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8, 0, 0, 3);"
        );

        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: " + darken(color) + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-border-radius: 8px;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 12, 0, 0, 4);"
        ));

        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-border-radius: 8px;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8, 0, 0, 3);"
        ));

        btn.setOnAction(e -> {
            String btnText = text;
            if (btnText.contains("Новый заказ")) {
                openCreateOrderDialog();
            } else if (btnText.contains("Новый клиент")) {
                openEditClientDialog();
            } else if (btnText.contains("Запись")) {
                openAppointmentView();
            } else if (btnText.contains("Отчёт")) {
                generateReport();
            } else if (btnText.contains("Настройки")) {
                openSettings();
            }
        });

        return btn;
    }

    private String darken(String color) {
        return switch (color) {
            case "#3498db" -> "#2980b9";
            case "#2ecc71" -> "#27ae60";
            case "#9b59b6" -> "#8e44ad";
            case "#f39c12" -> "#e67e22";
            case "#f1c40f" -> "#d4ac0d";
            case "#27ae60" -> "#1e8449";
            case "#e74c3c" -> "#c0392b";
            default -> color;
        };
    }

    // ==================== ДЕЙСТВИЯ КНОПОК ====================

    private void openCreateOrderDialog() {
        try {
            CreateOrderDialog.show();
            DataStore.load();
            refresh();
            OrderController.refreshTable();
        } catch (Exception ex) {
            showErrorAlert("Ошибка", "Не удалось открыть диалог создания заказа");
        }
    }

    private void openEditClientDialog() {
        try {
            Client emptyClient = new Client(-1, "", "", "", "", "", "");
            EditClientDialog.show(emptyClient);
            DataStore.load();
            refresh();
            ClientController.refreshTable();
        } catch (Exception ex) {
            showErrorAlert("Ошибка", "Не удалось открыть диалог создания клиента");
        }
    }

    private void openAppointmentView() {
        try {
            Node parent = this;
            while (parent != null && !(parent instanceof TabPane)) {
                parent = parent.getParent();
            }
            if (parent instanceof TabPane tabPane) {
                for (Tab tab : tabPane.getTabs()) {
                    if ("Запись".equals(tab.getText())) {
                        tabPane.getSelectionModel().select(tab);
                        return;
                    }
                }
            }
            showInfoAlert("Переключение", "Вкладка 'Запись' не найдена.");
        } catch (Exception ex) {
            showErrorAlert("Ошибка", "Не удалось переключиться на вкладку 'Запись'.");
        }
    }

    private void generateReport() {
        try {
            ReportView.show();
        } catch (Exception ex) {
            showErrorAlert("Ошибка", "Не удалось открыть отчёт");
        }
    }

    private void openSettings() {
        try {
            SettingsController.showSettings();
        } catch (Exception ex) {
            ex.printStackTrace();
            showErrorAlert("Ошибка", "Не удалось открыть настройки: " + ex.getMessage());
        }
    }

    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ==================== СТАТИСТИКА ====================

    private String getLowStockCount() {
        int count = 0;
        for (SparePart part : DataStore.getSpareParts()) {
            if (part.getStock() < part.getMinStock()) {
                count++;
            }
        }
        return String.valueOf(count);
    }

    private String getTotalRevenue() {
        double total = 0;
        for (WorkOrder order : DataStore.getOrders()) {
            String status = order.getStatus();
            if (!"Отменён".equals(status)) {
                total += order.getTotal();
            }
        }
        return currencyFormat.format(total);
    }

    private int getActiveOrdersCount() {
        int count = 0;
        for (WorkOrder order : DataStore.getOrders()) {
            if ("В работе".equals(order.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private int getCompletedOrdersCount() {
        int count = 0;
        for (WorkOrder order : DataStore.getOrders()) {
            if ("Выполнен".equals(order.getStatus())) {
                count++;
            }
        }
        return count;
    }

// ==================== ЗАПИСИ НА ТЕКУЩУЮ НЕДЕЛЮ ====================

    private VBox createAppointmentsWeekBox() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(20));
        box.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-radius: 12px;" +
                        "-fx-background-radius: 12px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 5);"
        );

        Label header = new Label("Записи на текущую неделю");
        header.setGraphic(IconHelper.event(20, "#2c3e50"));
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-graphic-text-gap: 8;");
        box.getChildren().add(header);

        // Получаем записи на текущую неделю
        List<Appointment> weekAppointments = getWeekAppointments();

        if (weekAppointments.isEmpty()) {
            Label empty = new Label("Нет записей на текущую неделю");
            empty.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 14px;");
            box.getChildren().add(empty);
            return box;
        }

        // Создаём таблицу
        TableView<AppointmentRow> table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setPrefHeight(300);

        // Отключаем автоматическое добавление колонок
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // ====== КОЛОНКА 1: ЗАКАЗ ======
        TableColumn<AppointmentRow, String> colOrder = new TableColumn<>("Заказ");
        colOrder.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        colOrder.setPrefWidth(150);

        // ====== КОЛОНКА 2: КЛИЕНТ ======
        TableColumn<AppointmentRow, String> colClient = new TableColumn<>("Клиент");
        colClient.setCellValueFactory(new PropertyValueFactory<>("clientName"));
        colClient.setPrefWidth(180);

        // ====== КОЛОНКА 3: АВТОМОБИЛЬ ======
        TableColumn<AppointmentRow, String> colCar = new TableColumn<>("Автомобиль");
        colCar.setCellValueFactory(new PropertyValueFactory<>("carModel"));
        colCar.setPrefWidth(180);

        // ====== КОЛОНКА 4: УСЛУГА ======
        TableColumn<AppointmentRow, String> colService = new TableColumn<>("Услуга");
        colService.setCellValueFactory(new PropertyValueFactory<>("serviceName"));
        colService.setPrefWidth(200);

        // ====== КОЛОНКА 5: ДАТА И ВРЕМЯ ======
        TableColumn<AppointmentRow, String> colDateTime = new TableColumn<>("Дата и время");
        colDateTime.setCellValueFactory(new PropertyValueFactory<>("dateTime"));
        colDateTime.setPrefWidth(180);

        // Добавляем только 5 колонок
        table.getColumns().addAll(colOrder, colClient, colCar, colService, colDateTime);

        // Заполняем данные
        ObservableList<AppointmentRow> items = FXCollections.observableArrayList();
        for (Appointment a : weekAppointments) {
            Client client = a.getClient();
            String orderId = a.getOrderId() != null ? a.getOrderId() : "—";
            String clientName = (client.getLastName() != null && !client.getLastName().isEmpty())
                    ? client.getLastName() + " " + client.getName()
                    : client.getName();
            String carModel = client.getCarModel() != null ? client.getCarModel() : "—";
            String serviceName = a.getServiceName() != null ? a.getServiceName() : "—";
            String dateTime = DateUtils.formatDate(a.getDate()) + " " + a.getTime();

            // Пытаемся найти заказ для открытия
            WorkOrder linkedOrder = null;
            if (a.getOrderId() != null && !a.getOrderId().isEmpty()) {
                for (WorkOrder order : DataStore.getOrders()) {
                    if (a.getOrderId().equals(order.getId())) {
                        linkedOrder = order;
                        break;
                    }
                }
            }

            AppointmentRow row = new AppointmentRow(
                    orderId,
                    clientName,
                    carModel,
                    serviceName,
                    dateTime,
                    linkedOrder
            );
            items.add(row);
        }

        table.setItems(items);

        // Клик по строке — открываем заказ
        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                AppointmentRow selected = table.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getLinkedOrder() != null) {
                    OrderDetailsDialog.show(selected.getLinkedOrder());
                } else if (selected != null) {
                    showInfoAlert("Информация", "Для этой записи нет связанного заказа");
                }
            }
        });

        // Добавляем подсказку
        Label hint = new Label("💡 Двойной клик по строке — просмотр заказа");
        hint.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d; -fx-padding: 8 0 0 0;");

        box.getChildren().addAll(table, hint);
        return box;
    }

    /**
     * Возвращает список записей на текущую неделю (с понедельника по воскресенье)
     * Только активные записи (не закрытые и не выполненные)
     */
    private List<Appointment> getWeekAppointments() {
        List<Appointment> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // Определяем понедельник текущей недели
        LocalDate monday = today;
        while (monday.getDayOfWeek().getValue() != 1) {
            monday = monday.minusDays(1);
        }
        // Воскресенье
        LocalDate sunday = monday.plusDays(6);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Appointment a : DataStore.getAppointments()) {
            try {
                LocalDate appointmentDate = LocalDate.parse(a.getDate(), formatter);

                // Проверяем, что запись в пределах недели
                if (appointmentDate.isBefore(monday) || appointmentDate.isAfter(sunday)) {
                    continue;
                }

                // Проверяем, что запись активна (не закрыта и не выполнена)
                String status = a.getStatus();
                if (status == null) {
                    continue;
                }

                // Исключаем закрытые и выполненные записи
                if (status.equals("Выполнено") || status.equals("Закрыт")) {
                    continue;
                }

                result.add(a);

            } catch (Exception ignored) {
                // Если дата не парсится — пропускаем
            }
        }

        // Сортируем по дате и времени
        result.sort((a1, a2) -> {
            int dateCompare = a1.getDate().compareTo(a2.getDate());
            if (dateCompare != 0) return dateCompare;
            return a1.getTime().compareTo(a2.getTime());
        });

        return result;
    }

    // ==================== ВНУТРЕННИЙ КЛАСС ДЛЯ ТАБЛИЦЫ ====================

    public static class AppointmentRow {
        private final String orderId;
        private final String clientName;
        private final String carModel;
        private final String serviceName;
        private final String dateTime;
        private final WorkOrder linkedOrder;

        public AppointmentRow(String orderId, String clientName, String carModel,
                              String serviceName, String dateTime, WorkOrder linkedOrder) {
            this.orderId = orderId;
            this.clientName = clientName;
            this.carModel = carModel;
            this.serviceName = serviceName;
            this.dateTime = dateTime;
            this.linkedOrder = linkedOrder;
        }

        public String getOrderId() { return orderId; }
        public String getClientName() { return clientName; }
        public String getCarModel() { return carModel; }
        public String getServiceName() { return serviceName; }
        public String getDateTime() { return dateTime; }
        public WorkOrder getLinkedOrder() { return linkedOrder; }
    }
}