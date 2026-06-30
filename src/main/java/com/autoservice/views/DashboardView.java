package com.autoservice.views;

import com.autoservice.Client;
import com.autoservice.DataStore;
import com.autoservice.SparePart;
import com.autoservice.WorkOrder;
import com.autoservice.dialogs.CreateOrderDialog;
import com.autoservice.dialogs.EditClientDialog;
import com.autoservice.controllers.ClientController;
import com.autoservice.controllers.OrderController;
import com.autoservice.utils.IconHelper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.text.NumberFormat;
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

        // Строка карточек
        HBox cardsRow = new HBox(20);
        cardsRow.setAlignment(Pos.CENTER);
        cardsRow.setPadding(new Insets(0, 0, 20, 0));

        cardsRow.getChildren().addAll(
                createStatCard(IconHelper.assignment(24, "#3498db"), "Заказов",
                        String.valueOf(DataStore.getOrders().size()), "#3498db"),
                createStatCard(IconHelper.people(24, "#2ecc71"), "Клиентов",
                        String.valueOf(DataStore.getClients().size()), "#2ecc71"),
                createStatCard(IconHelper.warning(24, "#e74c3c"), "Остатки",
                        getLowStockCount(), "#e74c3c"),
                createStatCard(IconHelper.report(24, "#f39c12"), "Выручка",
                        getTotalRevenue(), "#f39c12")
        );

        gridPane.add(cardsRow, 0, row);
        row++;

        // Активные заказы
        HBox activeRow = new HBox(20);
        activeRow.setAlignment(Pos.CENTER);
        activeRow.setPadding(new Insets(0, 0, 20, 0));

        activeRow.getChildren().addAll(
                createStatCard(IconHelper.edit(24, "#f1c40f"), "В работе",
                        String.valueOf(getActiveOrdersCount()), "#f1c40f"),
                createStatCard(IconHelper.save(24, "#27ae60"), "Выполнено",
                        String.valueOf(getCompletedOrdersCount()), "#27ae60"),
                createStatCard(IconHelper.event(24, "#9b59b6"), "Записей",
                        String.valueOf(DataStore.getAppointments().size()), "#9b59b6")
        );

        gridPane.add(activeRow, 0, row);
        row++;

        // Быстрые действия
        HBox actionsRow = new HBox(15);
        actionsRow.setAlignment(Pos.CENTER);
        actionsRow.setPadding(new Insets(20, 0, 10, 0));

        actionsRow.getChildren().addAll(
                createActionButton("Новый заказ", "#3498db", IconHelper.add()),
                createActionButton("Новый клиент", "#2ecc71", IconHelper.add()),
                createActionButton("Запись", "#9b59b6", IconHelper.event()),
                createActionButton("Отчёт", "#f39c12", IconHelper.report())
        );

        gridPane.add(actionsRow, 0, row);
        row++;

        // Последние заказы
        VBox recentOrdersBox = createRecentOrdersBox();
        gridPane.add(recentOrdersBox, 0, row);
    }

    // ==================== СОЗДАНИЕ КАРТОЧКИ ====================

    private VBox createStatCard(Node icon, String title, String value, String color) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20, 40, 20, 40));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-radius: 12px;" +
                        "-fx-background-radius: 12px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 5);" +
                        "-fx-border-width: 3px 0 0 0;" +
                        "-fx-border-color: " + color + ";" +
                        "-fx-min-width: 180px;" +
                        "-fx-pref-width: 180px;"
        );

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");
        titleLabel.setWrapText(true);

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        valueLabel.setWrapText(true);

        card.getChildren().addAll(icon, titleLabel, valueLabel);
        return card;
    }

    // ==================== КНОПКИ БЫСТРЫХ ДЕЙСТВИЙ ====================

    private Button createActionButton(String text, String color, Node icon) {
        Button btn = new Button(text);
        btn.setGraphic(icon);
        btn.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 12px 25px;" +
                        "-fx-border-radius: 8px;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8, 0, 0, 3);"
        );

        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: " + darken(color) + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 12px 25px;" +
                        "-fx-border-radius: 8px;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 12, 0, 0, 4);"
        ));

        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 12px 25px;" +
                        "-fx-border-radius: 8px;" +
                        "-fx-background-radius: 8px;" +
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
            System.err.println("Ошибка открытия диалога создания заказа: " + ex.getMessage());
            ex.printStackTrace();
            showErrorAlert("Ошибка", "Не удалось открыть диалог создания заказа: " + ex.getMessage());
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
            System.err.println("Ошибка открытия диалога создания клиента: " + ex.getMessage());
            ex.printStackTrace();
            showErrorAlert("Ошибка", "Не удалось открыть диалог создания клиента: " + ex.getMessage());
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
            System.err.println("Ошибка переключения на вкладку Запись: " + ex.getMessage());
            showErrorAlert("Ошибка", "Не удалось переключиться на вкладку 'Запись'.");
        }
    }

    private void generateReport() {
        try {
            ReportView.show();
        } catch (Exception ex) {
            System.err.println("Ошибка открытия отчёта: " + ex.getMessage());
            ex.printStackTrace();
            showErrorAlert("Ошибка", "Не удалось открыть отчёт: " + ex.getMessage());
        }
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

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

    // ==================== БЛОК ПОСЛЕДНИХ ЗАКАЗОВ ====================

    private VBox createRecentOrdersBox() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(20));
        box.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-radius: 12px;" +
                        "-fx-background-radius: 12px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 5);"
        );

        Label header = new Label("Последние заказы");
        header.setGraphic(IconHelper.assignment(20, "#2c3e50"));
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        box.getChildren().add(header);

        var orders = DataStore.getOrders();
        int limit = Math.min(orders.size(), 5);

        if (orders.isEmpty()) {
            Label empty = new Label("Нет заказов");
            empty.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 14px;");
            box.getChildren().add(empty);
            return box;
        }

        for (int i = 0; i < limit; i++) {
            WorkOrder order = orders.get(i);
            HBox row = new HBox(15);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 0, 8, 0));
            row.setStyle("-fx-border-color: #ecf0f1; -fx-border-width: 0 0 1 0;");

            Label number = new Label(order.getId());
            number.setStyle("-fx-font-weight: bold; -fx-text-fill: #34495e; -fx-min-width: 120px;");

            Label client = new Label(order.getClient().getName() + " " + order.getClient().getLastName());
            client.setStyle("-fx-text-fill: #2c3e50; -fx-min-width: 150px;");

            Label status = new Label(order.getStatus());
            status.setStyle("-fx-text-fill: " + getStatusColor(order.getStatus()) + "; -fx-font-weight: bold;");

            Label total = new Label(currencyFormat.format(order.getTotal()));
            total.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold;");

            row.getChildren().addAll(number, client, status, total);
            box.getChildren().add(row);
        }

        return box;
    }

    private String getStatusColor(String status) {
        return switch (status) {
            case "Черновик" -> "#95a5a6";
            case "В работе" -> "#f39c12";
            case "Выполнен" -> "#27ae60";
            case "Отменён" -> "#e74c3c";
            default -> "#2c3e50";
        };
    }
}