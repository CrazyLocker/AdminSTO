package com.autoservice.views;

import com.autoservice.*;
import com.autoservice.controllers.ClientController;
import com.autoservice.controllers.DictionaryController;
import com.autoservice.controllers.OrderController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class DashboardView {

    public static VBox create() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        vbox.setStyle("-fx-background-color: #f5f5f5;");

        // Заголовок
        Label title = new Label("Дашборд администратора");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #333333;");

        // Верхняя панель с карточками статистики
        HBox statsPanel = createStatsPanel();

        // Секция последних заказов
        VBox recentOrders = createRecentOrdersSection();

        // Секция записей на сегодня
        VBox appointmentsSection = createAppointmentsSection();

        // Секция с низким запасом запчастей
        VBox lowStockSection = createLowStockSection();

        // Кнопка обновления
        Button refreshBtn = new Button("Обновить данные");
        refreshBtn.setPrefWidth(200);
        refreshBtn.setStyle(
            "-fx-background-color: #4CAF50; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 14px; " +
            "-fx-padding: 10 20; " +
            "-fx-background-radius: 5; " +
            "-fx-cursor: hand;"
        );
        refreshBtn.setOnAction(e -> refreshData());

        vbox.getChildren().addAll(title, statsPanel, recentOrders, appointmentsSection, lowStockSection, refreshBtn);
        return vbox;
    }

    private static HBox createStatsPanel() {
        HBox hbox = new HBox(15);
        hbox.setPadding(new Insets(10));

        CardPanel activeOrdersCard = new CardPanel(
            "Активные заказы",
            String.valueOf(DataStore.getActiveOrdersCount()),
            "#2196F3",
            "Заказы в работе, диагностике\nи ожидании запчастей"
        );

        CardPanel clientsCard = new CardPanel(
            "Всего клиентов",
            String.valueOf(DataStore.getClients().size()),
            "#9C27B0",
            "Все клиенты в базе"
        );

        CardPanel ordersCard = new CardPanel(
            "Всего заказов",
            String.valueOf(DataStore.getOrders().size()),
            "#FF9800",
            "Заказы за всё время"
        );

        double activeRevenue = calculateActiveOrdersRevenue();
        CardPanel revenueCard = new CardPanel(
            "В работе (руб.)",
            String.format("%.0f", activeRevenue),
            "#4CAF50",
            "Сумма незакрытых заказов"
        );

        hbox.getChildren().addAll(activeOrdersCard, clientsCard, ordersCard, revenueCard);
        return hbox;
    }

    private static double calculateActiveOrdersRevenue() {
        return DataStore.getOrders().stream()
            .filter(order -> !order.getStatus().equals(WorkOrder.STATUS_CLOSED))
            .mapToDouble(WorkOrder::getTotal)
            .sum();
    }

    private static VBox createRecentOrdersSection() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(15));
        vbox.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        Label title = new Label("Последние заказы");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333;");

        List<WorkOrder> orders = DataStore.getOrders().stream()
            .limit(5)
            .collect(Collectors.toList());

        if (orders.isEmpty()) {
            Label noData = new Label("Заказов нет");
            noData.setStyle("-fx-text-fill: #999; -fx-font-style: italic;");
            vbox.getChildren().add(noData);
        } else {
            for (WorkOrder order : orders) {
                Label orderLabel = new Label(
                    order.getId() + " | " + order.getClient().getName() +
                    " | " + String.format("%.0f", order.getTotal()) + " p. | " + order.getStatus()
                );
                orderLabel.setStyle("-fx-font-size: 13px; -fx-padding: 5; -fx-border-color: #eee; -fx-border-width: 0 0 1 0;");
                vbox.getChildren().add(orderLabel);
            }
        }

        return vbox;
    }

    private static VBox createAppointmentsSection() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(15));
        vbox.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        Label title = new Label("Записи на сегодня");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333;");

        List<Appointment> todayAppointments = DataStore.getAppointmentsByDate(LocalDate.now().toString());

        if (todayAppointments.isEmpty()) {
            Label noData = new Label("Записей на сегодня нет");
            noData.setStyle("-fx-text-fill: #999; -fx-font-style: italic;");
            vbox.getChildren().add(noData);
        } else {
            for (Appointment appt : todayAppointments) {
                Label apptLabel = new Label(
                    appt.getTime() + " | " + appt.getClient().getName() +
                    " | " + appt.getServiceName() + " | " + appt.getMasterName()
                );
                apptLabel.setStyle("-fx-font-size: 13px; -fx-padding: 5; -fx-border-color: #eee; -fx-border-width: 0 0 1 0;");
                vbox.getChildren().add(apptLabel);
            }
        }

        return vbox;
    }

    private static VBox createLowStockSection() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(15));
        vbox.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        Label title = new Label("Запчасти на остатке (< 10 шт.)");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #f44336;");

        List<SparePart> lowStockParts = DataStore.getSpareParts().stream()
            .filter(part -> part.getStock() < 10)
            .collect(Collectors.toList());

        if (lowStockParts.isEmpty()) {
            Label noData = new Label("Запасов достаточно");
            noData.setStyle("-fx-text-fill: #4CAF50; -fx-font-style: italic;");
            vbox.getChildren().add(noData);
        } else {
            for (SparePart part : lowStockParts) {
                Label partLabel = new Label(
                    part.getName() + " — " + part.getStock() + " шт. (цена: " +
                    String.format("%.0f", part.getRetailPrice()) + " p.)"
                );
                partLabel.setStyle("-fx-font-size: 13px; -fx-padding: 5; -fx-border-color: #ffebee; -fx-border-width: 0 0 1 0;");
                vbox.getChildren().add(partLabel);
            }
        }

        return vbox;
    }

    private static void refreshData() {
        DataStore.load();
        ClientController.refreshTable();
        OrderController.refreshTable();
        DictionaryController.refreshAll();
        System.out.println("Дашборд обновлен");
    }

    // Вспомогательный класс для карточек статистики
    static class CardPanel extends VBox {
        CardPanel(String title, String value, String color, String description) {
            setPadding(new Insets(15));
            setAlignment(Pos.CENTER);
            setSpacing(5);
            setMinWidth(200);
            setStyle(
                "-fx-background-color: white; " +
                "-fx-background-radius: 8; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);"
            );

            Label titleLabel = new Label(title);
            titleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #666;");

            Label valueLabel = new Label(value);
            valueLabel.setStyle(
                "-fx-font-size: 28px; -fx-font-weight: bold; " +
                "-fx-text-fill: " + color + ";"
            );

            Label descLabel = new Label(description);
            descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #999;");
            descLabel.setWrapText(true);
            descLabel.setAlignment(Pos.CENTER);

            getChildren().addAll(titleLabel, valueLabel, descLabel);
        }
    }
}
