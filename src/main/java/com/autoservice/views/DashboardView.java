package com.autoservice.views;

import com.autoservice.DataStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

public class DashboardView {

    public static VBox create() {
        VBox mainContainer = new VBox(15);
        mainContainer.setPadding(new Insets(15));
        mainContainer.setStyle("-fx-background-color: #f0f2f5;");

        Label titleLabel = new Label("Дашборд");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Accordion accordion = new Accordion();

        TitledPane ordersPane = createOrdersPane();
        TitledPane clientsPane = createClientsPane();

        accordion.getPanes().addAll(ordersPane, clientsPane);
        accordion.setExpandedPane(ordersPane);

        VBox.setVgrow(accordion, Priority.ALWAYS);
        mainContainer.getChildren().addAll(titleLabel, accordion);

        return mainContainer;
    }

    private static TitledPane createOrdersPane() {
        VBox content = new VBox(12);
        content.setPadding(new Insets(12));
        content.setStyle("-fx-background-color: white; -fx-background-radius: 8;");

        Label headerLabel = new Label("📦 Заказы");
        headerLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        HBox statsRow = new HBox(15);
        statsRow.setAlignment(Pos.CENTER);

        VBox totalBox = createStatCard("Всего заказов", String.valueOf(DataStore.getOrders().size()), "#3498db");
        VBox activeBox = createStatCard("Активных заказов", String.valueOf(DataStore.getActiveOrdersCount()), "#2ecc71");

        statsRow.getChildren().addAll(totalBox, activeBox);

        content.getChildren().addAll(headerLabel, statsRow);

        TitledPane pane = new TitledPane("📊 Статистика заказов", content);
        pane.setAnimated(true);
        return pane;
    }

    private static TitledPane createClientsPane() {
        VBox content = new VBox(12);
        content.setPadding(new Insets(12));
        content.setStyle("-fx-background-color: white; -fx-background-radius: 8;");

        Label headerLabel = new Label("👥 Клиенты");
        headerLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        HBox statsRow = new HBox(15);
        statsRow.setAlignment(Pos.CENTER);

        VBox totalBox = createStatCard("Всего клиентов", String.valueOf(DataStore.getClients().size()), "#3498db");

        long withAppointment = DataStore.getAppointments().stream()
                .filter(a -> a.getStatus().equals("ЗАПЛАНИРОВАНО"))
                .count();
        VBox appointmentBox = createStatCard("Клиентов с записью", String.valueOf(withAppointment), "#f39c12");

        statsRow.getChildren().addAll(totalBox, appointmentBox);

        content.getChildren().addAll(headerLabel, statsRow);

        TitledPane pane = new TitledPane("👥 Статистика клиентов", content);
        pane.setAnimated(true);
        return pane;
    }

    private static VBox createStatCard(String title, String value, String color) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(15));
        card.setStyle(
                "-fx-background-color: #f8f9fa; " +
                        "-fx-border-color: #e0e0e0; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8;"
        );
        card.setPrefWidth(180);

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");

        card.getChildren().addAll(valueLabel, titleLabel);
        return card;
    }
}