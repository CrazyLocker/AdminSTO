package com.autoservice;

import com.autoservice.config.SettingsManager;
import com.autoservice.utils.ExceptionHandler;
import com.autoservice.utils.LoggerManager;
import com.autoservice.utils.ThemeManager;
import com.autoservice.services.ScheduleService;
import com.autoservice.services.TableStateManager;
import com.autoservice.services.WindowStateManager;
import com.autoservice.views.*;
import com.autoservice.controllers.AppointmentController;
import com.autoservice.controllers.ServicePanelController;
import com.autoservice.controllers.SparePartPanelController;
import com.autoservice.controllers.StockPanelController;
import com.autoservice.utils.IconHelper;
import com.autoservice.utils.LoadingIndicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private static AppointmentController appointmentController;

    @Override
    public void start(Stage primaryStage) {
        SettingsManager.load();
        logger.info("Запуск приложения Администратор СТО");

        try {
            Database.init();
            logger.info("База данных инициализирована");

            LoadingIndicator.show();
            new Thread(() -> {
                DataStore.load();
                Platform.runLater(() -> {
                    LoadingIndicator.hide();
                    logger.info("Данные загружены");
                });
            }).start();

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

        // ===== ЗАГРУЗКА AppointmentView через FXML =====
        appointmentTab.setContent(createAppointmentView());

        tabPane.getTabs().addAll(dashTab, clientTab, orderTab, servicesTab, sparePartsTab, stockTab, appointmentTab, settingsTab);

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, tab) -> {
            logger.info("🔄 Переключение вкладки: {} -> {}", oldTab != null ? oldTab.getText() : "null", tab != null ? tab.getText() : "null");
            if (tab == dashTab) {
                Platform.runLater(() -> DashboardView.refresh());
            } else if (tab == appointmentTab) {
                Platform.runLater(() -> {
                    if (appointmentController != null) {
                        appointmentController.refresh();
                    }
                });
            }
        });

        Scene scene = new Scene(tabPane, 1500, 1000);
        ThemeManager.init(scene);
        // Подключение стилей для календаря
        scene.getStylesheets().add(getClass().getResource("/css/appointment.css").toExternalForm());
        primaryStage.setTitle("Администратор СТО");
        primaryStage.setScene(scene);
        WindowStateManager.getInstance().restoreWindowState("mainWindow", primaryStage);

        primaryStage.setOnCloseRequest(e -> {
            logger.info("Закрытие приложения");
            WindowStateManager.getInstance().saveWindowState("mainWindow", primaryStage);
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
     * Создаёт представление AppointmentView через FXML.
     */
    private VBox createAppointmentView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AppointmentView.fxml"));
            VBox root = loader.load();
            appointmentController = loader.getController();
            logger.info("✅ AppointmentView загружен через FXML");
            return root;
        } catch (IOException e) {
            logger.error("❌ Ошибка загрузки AppointmentView.fxml", e);
            // Показываем сообщение об ошибке
            VBox errorBox = new VBox();
            errorBox.setAlignment(javafx.geometry.Pos.CENTER);
            Label errorLabel = new Label("❌ Ошибка загрузки календаря");
            errorLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 16px; -fx-font-weight: bold;");
            Label detailLabel = new Label(e.getMessage());
            detailLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
            errorBox.getChildren().addAll(errorLabel, detailLabel);
            return errorBox;
        }
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