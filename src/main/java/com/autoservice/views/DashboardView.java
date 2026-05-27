package com.autoservice.views;

import com.autoservice.DataStore;
import com.autoservice.controllers.ClientController;
import com.autoservice.controllers.DictionaryController;
import com.autoservice.controllers.OrderController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class DashboardView {

    public static VBox create() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));
        vbox.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Статистика");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label stat1 = new Label("Активных заказов: " + DataStore.getActiveOrdersCount());
        Label stat2 = new Label("Всего клиентов: " + DataStore.getClients().size());
        Label stat3 = new Label("Всего заказов: " + DataStore.getOrders().size());
        Label stat4 = new Label("Услуг в справочнике: " + DataStore.getServices().size());
        Label stat5 = new Label("Запчастей: " + DataStore.getSpareParts().size());

        Button refreshBtn = new Button("Обновить");
        refreshBtn.setOnAction(e -> {
            stat1.setText("Активных заказов: " + DataStore.getActiveOrdersCount());
            stat2.setText("Всего клиентов: " + DataStore.getClients().size());
            stat3.setText("Всего заказов: " + DataStore.getOrders().size());
            stat4.setText("Услуг в справочнике: " + DataStore.getServices().size());
            stat5.setText("Запчастей: " + DataStore.getSpareParts().size());
            ClientController.refreshTable();
            OrderController.refreshTable();
            DictionaryController.refreshAll();
        });

        vbox.getChildren().addAll(title, stat1, stat2, stat3, stat4, stat5, refreshBtn);
        return vbox;
    }
}