package com.autoservice;

import com.autoservice.controllers.DictionaryController;
import com.autoservice.views.AppointmentView;
import com.autoservice.views.ClientView;
import com.autoservice.views.DashboardView;
import com.autoservice.views.DictionaryView;
import com.autoservice.views.OrderView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        Database.init();
        DataStore.load();

        TabPane tabPane = new TabPane();

        Tab dashTab = new Tab("Дашборд");
        dashTab.setContent(DashboardView.create());
        dashTab.setClosable(false);

        Tab clientTab = new Tab("Клиенты");
        clientTab.setContent(ClientView.create());
        clientTab.setClosable(false);

        Tab orderTab = new Tab("Заказы");
        orderTab.setContent(OrderView.create());
        orderTab.setClosable(false);

        Tab dictTab = new Tab("Справочники");
        dictTab.setContent(DictionaryView.create());
        dictTab.setClosable(false);

        Tab appointmentTab = new Tab("Запись");
        appointmentTab.setContent(AppointmentView.create());
        appointmentTab.setClosable(false);

        tabPane.getTabs().addAll(dashTab, clientTab, orderTab, dictTab, appointmentTab);

        Scene scene = new Scene(tabPane, 1300, 700);
        primaryStage.setTitle("Администратор СТО");
        primaryStage.setScene(scene);

        primaryStage.setOnCloseRequest(e -> {
            DataStore.save();
            Database.close();
            Platform.exit();
            System.exit(0);
        });

        primaryStage.show();

        com.autoservice.controllers.ClientController.refreshTable();
        com.autoservice.controllers.OrderController.refreshTable();
        DictionaryController.refreshAll();
    }

    public static void main(String[] args) {
        launch(args);
    }
}