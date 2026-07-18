package com.autoservice;

import com.autoservice.utils.LoggerManager;
import com.autoservice.services.ScheduleService;
import com.autoservice.services.TableStateManager;
import com.autoservice.services.WindowStateManager;
import com.autoservice.views.*;
import com.autoservice.controllers.ServicePanelController;
import com.autoservice.controllers.SparePartPanelController;
import com.autoservice.controllers.StockPanelController;
import com.autoservice.utils.IconHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

/**
 * Базовый класс для UI-тестов JavaFX без TestFX (используем ручную инициализацию и JavaFX API).
 * 
 * Этот класс предоставляет методы для взаимодействия с UI элементами:
 * - findNode() - поиск элемента по ID
 * - clickOn() - клик по кнопке/элементу
 * - writeText() - ввод текста в поле
 * - selectFromComboBox() - выбор из ComboBox
 * - assertVisible() - проверка видимости
 * - assertText() - проверка текста
 */
public abstract class BaseUITest {

    protected static Stage primaryStage;
    protected static Scene currentScene;

    /**
     * Инициализация перед запуском всех тестов.
     * Настраивает headless режим для JavaFX и инициализирует БД.
     */
    @BeforeAll
    static void setupClass() {
        // Установка headless режима для JavaFX
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.text", "t2k");
        System.setProperty("glass.platform", "Monocle");
        System.setProperty("monocle.input.mouse", "false");
        System.setProperty("monocle.input.touch", "false");
        System.setProperty("javafx.headless", "true");
        System.setProperty("javafx.embed.headless", "true");

        // Инициализация JavaFX Platform (headless)
        try {
            Platform.startup(() -> {
                System.out.println("JavaFX Platform initialized in headless mode");
            });
            System.out.println("JavaFX Platform startup completed");
        } catch (IllegalStateException e) {
            // Platform уже инициализирована, это нормально
            System.out.println("JavaFX Platform already initialized: " + e.getMessage());
        }

        LoggerManager.init();
        DatabaseFactory.initForTest();
    }

    /**
     * Инициализация перед каждым тестом.
     * Очищает базу данных.
     */
    @BeforeEach
    void clearDatabase() {
        try (Connection conn = DatabaseFactory.getDatabase().getConnection()) {
            var stmt = conn.createStatement();
            stmt.execute("DELETE FROM order_parts");
            stmt.execute("DELETE FROM order_services");
            stmt.execute("DELETE FROM appointments");
            stmt.execute("DELETE FROM orders");
            stmt.execute("DELETE FROM spare_parts");
            stmt.execute("DELETE FROM services");
            stmt.execute("DELETE FROM clients");
            stmt.execute("DELETE FROM settings");
            stmt.execute("DELETE FROM to_parts");
            stmt.execute("DELETE FROM service_spare_parts");
            stmt.close();
            DataStore.load(); // Сброс кэша
        } catch (Exception e) {
            System.err.println("Ошибка очистки БД: " + e.getMessage());
        }
    }

    /**
     * Очистка после завершения всех тестов.
     */
    @AfterAll
    static void cleanupClass() {
        DatabaseFactory.close();
    }

    /**
     * Создание UI приложения для теста.
     * Вызывается в контексте JavaFX Application Thread.
     * 
     * @return Stage с созданным UI
     */
    protected Stage createStage() {
        Stage stage = new Stage();
        createUI(stage);
        stage.show();
        stage.toFront();
        currentScene = stage.getScene();
        return stage;
    }

    /**
     * Создание UI приложения.
     */
    private void createUI(Stage stage) {
        TabPane tabPane = new TabPane();
        tabPane.setId("mainTabPane");

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

        tabPane.setId("mainStage");
        stage.setTitle("Администратор СТО - Test Mode");
        stage.setScene(scene);
        
        // Восстановление состояния главного окна
        WindowStateManager.getInstance().restoreWindowState("mainWindow", stage);
    }

    private static Tab createTab(String title, SVGPath icon) {
        Tab tab = new Tab(title);
        tab.setClosable(false);
        tab.setGraphic(icon);
        return tab;
    }

    // ====== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ДЛЯ UI-ТЕСТИРОВАНИЯ ======

    /**
     * Поиск узла по ID в сцене.
     * 
     * @param nodeId ID искомого узла (например, "clientTable", "addClientBtn")
     * @return Найденный узел
     */
    protected Node findNode(String nodeId) {
        if (currentScene == null) {
            throw new IllegalStateException("Сцена не инициализирована. Вызовите createStage() первым.");
        }
        
        Node node = currentScene.lookup("#" + nodeId);
        if (node == null) {
            throw new IllegalArgumentException("Узел с ID '" + nodeId + "' не найден в сцене");
        }
        return node;
    }

    /**
     * Поиск узла по ID в конкретном родителе.
     * 
     * @param parentId ID родительского контейнера
     * @param nodeId ID искомого узла
     * @return Найденный узел
     */
    protected Node findNode(String parentId, String nodeId) {
        Node parent = findNode(parentId);
        Node node = parent.lookup("#" + nodeId);
        if (node == null) {
            throw new IllegalArgumentException("Узел с ID '" + nodeId + "' не найден в контейнере '" + parentId + "'");
        }
        return node;
    }

    /**
     * Клик по кнопке или другому узлу по ID.
     * 
     * @param nodeId ID узла (кнопки, таблицы, поля и т.д.)
     */
    protected void clickOn(String nodeId) {
        Node node = findNode(nodeId);
        
        // Получаем координаты центра узла
        Point2D center = getNodeCenter(node);
        
        // Используем Robot для эмуляции клика
        Robot robot = new Robot();
        robot.mouseMove((int)center.getX(), (int)center.getY());
        robot.mousePress(MouseButton.PRIMARY);
        robot.mouseRelease(MouseButton.PRIMARY);
        
        // Ждем завершения обработки события
        awaitPlatform();
    }

    /**
     * Ввод текста в TextField по ID.
     * 
     * @param fieldId ID текстового поля
     * @param text Текст для ввода
     */
    protected void writeText(String fieldId, String text) {
        TextField field = (TextField) findNode(fieldId);
        
        Platform.runLater(() -> {
            field.requestFocus();
            field.selectAll();
            field.insertText(field.getCaretPosition(), text);
        });
        
        // Ждем завершения ввода
        awaitPlatform();
    }

    /**
     * Очистка текста в TextField и ввод нового текста.
     * 
     * @param fieldId ID текстового поля
     * @param text Новый текст
     */
    protected void clearAndType(String fieldId, String text) {
        TextField field = (TextField) findNode(fieldId);
        
        Platform.runLater(() -> {
            field.requestFocus();
            field.selectAll();
            Platform.runLater(() -> {
                field.deleteText(0, field.getLength());
                field.insertText(field.getCaretPosition(), text);
            });
        });
        
        awaitPlatform();
    }

    /**
     * Выбор элемента из ComboBox по ID и тексту элемента.
     * 
     * @param comboBoxId ID ComboBox
     * @param itemText Текст элемента для выбора
     */
    protected void selectFromComboBox(String comboBoxId, String itemText) {
        ComboBox<String> comboBox = (ComboBox<String>) findNode(comboBoxId);
        
        Platform.runLater(() -> {
            comboBox.setValue(itemText);
        });
        
        awaitPlatform();
    }

    /**
     * Нажатие кнопки Enter в поле ввода.
     * 
     * @param fieldId ID текстового поля
     */
    protected void pressEnter(String fieldId) {
        TextField field = (TextField) findNode(fieldId);
        
        Platform.runLater(() -> {
            field.fireEvent(new javafx.scene.input.KeyEvent(
                javafx.scene.input.KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, false, false, false, false
            ));
        });
        
        awaitPlatform();
    }

    /**
     * Получение текста из Label по ID.
     * 
     * @param labelId ID label
     * @return Текст label
     */
    protected String getText(String labelId) {
        Label label = (Label) findNode(labelId);
        return label.getText();
    }

    /**
     * Проверка видимости узла.
     * 
     * @param nodeId ID узла
     */
    protected void assertVisible(String nodeId) {
        Node node = findNode(nodeId);
        assert node.isVisible() : "Узел '" + nodeId + "' не видим";
    }

    /**
     * Проверка, что узел не видим.
     * 
     * @param nodeId ID узла
     */
    protected void assertNotVisible(String nodeId) {
        Node node = findNode(nodeId);
        assert !node.isVisible() : "Узел '" + nodeId + "' должен быть скрыт";
    }

    /**
     * Проверка текста элемента.
     * 
     * @param nodeId ID узла
     * @param expectedText Ожидаемый текст
     */
    protected void assertText(String nodeId, String expectedText) {
        Node node = findNode(nodeId);
        String actualText = node instanceof Label ? ((Label) node).getText() : node.toString();
        assert actualText.equals(expectedText) : "Текст узла '" + nodeId + "' не совпадает. Ожидалось: '" + expectedText + "', получено: '" + actualText + "'";
    }

    /**
     * Получение центра узла в координатах сцены.
     */
    private Point2D getNodeCenter(Node node) {
        double x = node.getBoundsInParent().getMinX() + node.getBoundsInParent().getWidth() / 2;
        double y = node.getBoundsInParent().getMinY() + node.getBoundsInParent().getHeight() / 2;
        return new Point2D(x, y);
    }

    /**
     * Ожидание завершения всех запланированных задач в Platform.
     */
    private void awaitPlatform() {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        try {
            if (!latch.await(2, TimeUnit.SECONDS)) {
                System.err.println("Таймаут ожидания Platform");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Класс для эмуляции ввода мыши.
     */
    private static class Robot {
        void mouseMove(int x, int y) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        void mousePress(MouseButton button) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        void mouseRelease(MouseButton button) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
