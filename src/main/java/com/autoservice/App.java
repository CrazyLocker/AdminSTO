package com.autoservice;

import com.autoservice.views.DashboardView;
import com.autoservice.controllers.DictionaryController;
import com.autoservice.views.*;
import com.autoservice.utils.IconHelper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        Database.init();
        DataStore.load();

        TabPane tabPane = new TabPane();

        Tab dashTab = createTab("Дашборд", IconHelper.dashboard());
        Tab clientTab = createTab("Клиенты", IconHelper.people());
        Tab orderTab = createTab("Заказы", IconHelper.assignment());
        Tab dictTab = createTab("Справочники", IconHelper.book());
        Tab appointmentTab = createTab("Запись", IconHelper.event());

        dashTab.setContent(DashboardView.create());
        clientTab.setContent(ClientView.create());
        orderTab.setContent(OrderView.create());
        dictTab.setContent(DictionaryView.create());
        appointmentTab.setContent(AppointmentView.create());

        tabPane.getTabs().addAll(dashTab, clientTab, orderTab, dictTab, appointmentTab);

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == dashTab) {
                DashboardView.refresh();
            }
        });

        Scene scene = new Scene(tabPane, 1500, 1000);
        scene.getStylesheets().add(
                App.class.getResource("/styles.css").toExternalForm()
        );

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

    private static Tab createTab(String title, SVGPath icon) {
        Tab tab = new Tab(title);
        tab.setClosable(false);
        tab.setGraphic(icon);
        return tab;
    }

    public static void main(String[] args) {
        launch(args);
    }
}