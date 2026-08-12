package com.autoservice;

import com.autoservice.config.SettingsManager;
import com.autoservice.utils.ExceptionHandler;
import com.autoservice.utils.LoggerManager;
import com.autoservice.utils.ThemeManager;
import com.autoservice.services.ScheduleService;
import com.autoservice.services.TableStateManager;
import com.autoservice.services.WindowStateManager;
import com.autoservice.views.*;
import com.autoservice.controllers.ServicePanelController;
import com.autoservice.controllers.SparePartPanelController;
import com.autoservice.controllers.StockPanelController;
import com.autoservice.utils.IconHelper;
import com.autoservice.utils.LoadingIndicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

/**
 * Точка входа в приложение «Администратор СТО» (JavaFX).
 * 
 * Ответственность: инициализация инфраструктуры приложения (база данных,
 * логгирование, планировщик бэкапов, тема оформления), создание главного
 * окна с набором вкладок и управление жизненным циклом приложения
 * (корректное сохранение данных и состояния при закрытии).
 * 
 * Зависимости: JavaFX, Database, DataStore, ScheduleService, ThemeManager,
 * WindowStateManager, TableStateManager, все View-классы (ClientView,
 * OrderView, ServicePanel, SparePartPanel, StockPanel, SettingsView,
 * AppointmentView) и их контроллеры.
 * 
 * Особенности: загрузка данных выполняется в фоновом потоке с индикатором
 * LoadingIndicator; состояние главного окна и всех таблиц сохраняется при
 * закрытии приложения.
 * 
 * @author AdminSTO Team
 * @since 1.0
 * @see Database
 * @see DataStore
 * @see ScheduleService
 * @see DashboardView
 */
public class App extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    /**
     * Точка входа JavaFX-приложения. Выполняет инициализацию стилей,
     * логгирования, базы данных и фоновую загрузку данных, затем строит
     * главное окно с вкладками и настраивает обработчики закрытия.
     * 
     * @param primaryStage главная сцена приложения, предоставляемая JavaFX
     */
    @Override
    public void start(Stage primaryStage) {
        // 1. Загрузка настроек из JSON (независимо от БД)
        SettingsManager.load();
        logger.info("Запуск приложения Администратор СТО");

        try {
            // 2. Инициализация БД (бизнес-данные)
            Database.init();
            logger.info("База данных инициализирована");

            // 3. Загрузка данных в кэш (в фоновом потоке)
            LoadingIndicator.show();
            new Thread(() -> {
                DataStore.load();
                Platform.runLater(() -> {
                    LoadingIndicator.hide();
                    logger.info("Данные загружены");
                });
            }).start();
            
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
            logger.info("🔄 Переключение вкладки: {} -> {}", oldTab != null ? oldTab.getText() : "null", tab != null ? tab.getText() : "null");
            if (tab == dashTab) {
                // Отложенный refresh — не блокирует переключение вкладки
                Platform.runLater(() -> DashboardView.refresh());
            } else if (tab == appointmentTab) {
                // Отложенный refresh — не блокирует переключение вкладки
                Platform.runLater(() -> AppointmentView.refresh());
            }
        });

        Scene scene = new Scene(tabPane, 1500, 1000);

        // Инициализация менеджера тем + загрузка глобального CSS
        ThemeManager.init(scene);

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
            if (SettingsView.getServiceSparePartsTable() != null) {
                TableStateManager.saveTableState(SettingsView.getServiceSparePartsTable(), "serviceSparePartsTable");
            }
            TableStateManager.saveTableState(SettingsView.getToPartsTable(), "toPartsTable");
            
            // Остановить планировщик до закрытия БД, чтобы избежать конфликтов
            // при выполнении фоновых бэкапов
            ScheduleService.shutdown();
            DataStore.save();
            Database.close();
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

    /**
     * Создаёт не закрываемую вкладку с заданным заголовком и иконкой.
     * 
     * @param title текст заголовка вкладки
     * @param icon  SVG-иконка, отображаемая рядом с заголовком
     * @return настроенная вкладка {@link Tab}
     */
    private static Tab createTab(String title, SVGPath icon) {
        Tab tab = new Tab(title);
        tab.setClosable(false);
        tab.setGraphic(icon);
        return tab;
    }

    /**
     * Основной метод запуска приложения. Делегирует управление
     * методу {@code launch} базового класса {@link Application}.
     * 
     * @param args аргументы командной строки (не используются)
     */
    public static void main(String[] args) {
        launch(args);
    }
}
