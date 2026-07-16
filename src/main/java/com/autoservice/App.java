package com.autoservice;

import com.autoservice.utils.ExceptionHandler;
import com.autoservice.utils.LoggerManager;
import com.autoservice.services.ScheduleService;
import com.autoservice.services.TableStateManager;
import com.autoservice.services.WindowStateManager;
import com.autoservice.views.*;
import com.autoservice.controllers.ServicePanelController;
import com.autoservice.controllers.SparePartPanelController;
import com.autoservice.controllers.StockPanelController;
import com.autoservice.utils.IconHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

public class App extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    @Override
    public void start(Stage primaryStage) {
        // Инициализация логгирования
        LoggerManager.init();
        logger.info("Запуск приложения Администратор СТО");
        
        try {
            Database.init();
            logger.info("База данных инициализирована");
            DataStore.load();
            logger.info("Данные загружены");
            
            // Инициализация ScheduleService и проверка авто-бэкапа
            ScheduleService.init();
            ScheduleService.checkAndRunBackupOnStartup();
        } catch (Exception e) {
            logger.error("Ошибка при инициализации", e);
            String friendlyMessage = ExceptionHandler.getFriendlyMessage(e);
            logger.error("Пользовательское сообщение: {}", friendlyMessage);
        }

        TabPane tabPane = new TabPane();

        Tab dashTab = createTab("Дашборд", IconHelper.dashboard());
        Tab clientTab = createTab("Клиенты", IconHelper.people());
        Tab orderTab = createTab("Заказы", IconHelper.assignment());
        Tab servicesTab = createTab("Услуги", IconHelper.book());
        Tab sparePartsTab = createTab("Запчасти", IconHelper.inventory());
        Tab stockTab = createTab("Склад", IconHelper.box());
        Tab appointmentTab = createTab("Запись", IconHelper.event());
        Tab settingsTab = createTab("Настройки", IconHelper.settings());

        dashTab.setContent(DashboardView.create());
        clientTab.setContent(ClientView.create());
        orderTab.setContent(OrderView.create());
        servicesTab.setContent(ServicePanel.create());
        sparePartsTab.setContent(SparePartPanel.create());
        stockTab.setContent(StockPanel.create());
        settingsTab.setContent(SettingsView.create());
        appointmentTab.setContent(AppointmentView.create());

        tabPane.getTabs().addAll(dashTab, clientTab, orderTab, servicesTab, sparePartsTab, stockTab, appointmentTab, settingsTab);

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, tab) -> {
            if (tab == dashTab) {
                DashboardView.refresh();
            }
        });

        Scene scene = new Scene(tabPane, 1500, 1000);
        scene.getStylesheets().add(
                App.class.getResource("/styles.css").toExternalForm()
        );

        primaryStage.setTitle("Администратор СТО");
        primaryStage.setScene(scene);
        
        // Восстановление состояния главного окна
        WindowStateManager.getInstance().restoreWindowState("mainWindow", primaryStage);

        primaryStage.setOnCloseRequest(e -> {
            logger.info("Закрытие приложения");
            
            // Сохранение состояния главного окна
            WindowStateManager.getInstance().saveWindowState("mainWindow", primaryStage);
            
            // Сохранение состояний всех таблиц (синхронно, до System.exit)
            TableStateManager.saveTableState(ClientView.getTable(), "clientTable");
            TableStateManager.saveTableState(OrderView.getTable(), "orderTable");
            TableStateManager.saveTableState(ServicePanel.getTable(), "servicesTable");
            TableStateManager.saveTableState(SparePartPanel.getTable(), "sparePartsTable");
            TableStateManager.saveTableState(StockPanel.getTable(), "stockTable");
            TableStateManager.saveTableState(SettingsView.getSettingsTable(), "settingsTable");
            TableStateManager.saveTableState(SettingsView.getServiceSparePartsTable(), "serviceSparePartsTable");
            TableStateManager.saveTableState(SettingsView.getToPartsTable(), "toPartsTable");
            
            DataStore.save();
            Database.close();
            ScheduleService.shutdown();
            logger.info("Приложение закрыто");
            Platform.exit();
            System.exit(0);
        });

        primaryStage.show();

        logger.info("Приложение запущено успешно");
        
        com.autoservice.controllers.ClientController.refreshTable();
        com.autoservice.controllers.OrderController.refreshTable();
        ServicePanelController.refreshTable();
        SparePartPanelController.refreshTable();
        StockPanelController.refreshTable();
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
