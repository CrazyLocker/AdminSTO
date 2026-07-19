package com.autoservice;

import com.autoservice.utils.LoggerManager;
import com.autoservice.services.WindowStateManager;
import com.autoservice.views.*;
import com.autoservice.utils.IconHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
            // Очищаем все таблицы по отдельности: часть таблиц (settings,
            // to_parts, service_spare_parts) может отсутствовать в тестовой
            // H2-схеме — игнорируем ошибки отсутствующих таблиц.
            for (String table : new String[]{
                    "order_parts", "order_services", "appointments", "orders",
                    "spare_parts", "services", "clients", "settings",
                    "to_parts", "service_spare_parts"
            }) {
                try {
                    stmt.execute("DELETE FROM " + table);
                } catch (SQLException ignored) {
                    // таблица отсутствует в тестовой схеме — это нормально
                }
            }
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
     *
     * <p>Создание узлов JavaFX должно происходить на JavaFX Application Thread,
     * поэтому вся работа со Stage/Scene выполняется синхронно через
     * {@link #runOnFxThread(Runnable)}.</p>
     *
     * @return Stage с созданным UI
     */
    protected Stage createStage() {
        AtomicReference<Stage> ref = new AtomicReference<>();
        runOnFxThread(() -> {
            Stage stage = new Stage();
            createUI(stage);
            stage.show();
            stage.toFront();
            currentScene = stage.getScene();
            ref.set(stage);
        });
        return ref.get();
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

        Scene scene = new Scene(tabPane, 800, 600);
        scene.getStylesheets().add(
                App.class.getResource("/styles.css").toExternalForm()
        );

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
     * В headless-режиме координатная эмуляция мыши ненадёжна (узлы не имеют
     * реальных экранных координат), поэтому клик эмулируется через прямой
     * вызов {@link Button#fire()} на JavaFX Application Thread. Для узлов,
     * не являющихся Button, просто запрашивается фокус.
     *
     * @param nodeId ID узла (кнопки, таблицы, поля и т.д.)
     */
    protected void clickOn(String nodeId) {
        Node node = findNode(nodeId);
        runOnFxThread(() -> {
            if (node instanceof Button) {
                ((Button) node).fire();
            } else {
                node.requestFocus();
            }
        });
    }

    /**
     * Ввод текста в TextField по ID.
     *
     * @param fieldId ID текстового поля
     * @param text Текст для ввода
     */
    protected void writeText(String fieldId, String text) {
        TextField field = (TextField) findNode(fieldId);
        runOnFxThread(() -> {
            field.requestFocus();
            field.clear();
            field.setText(text);
        });
    }

    /**
     * Очистка текста в TextField и ввод нового текста.
     *
     * @param fieldId ID текстового поля
     * @param text Новый текст
     */
    protected void clearAndType(String fieldId, String text) {
        writeText(fieldId, text);
    }

    /**
     * Выбор элемента из ComboBox по ID и тексту элемента.
     *
     * @param comboBoxId ID ComboBox
     * @param itemText Текст элемента для выбора
     */
    protected void selectFromComboBox(String comboBoxId, String itemText) {
        ComboBox<String> comboBox = (ComboBox<String>) findNode(comboBoxId);
        runOnFxThread(() -> comboBox.setValue(itemText));
    }

    /**
     * Нажатие кнопки Enter в поле ввода.
     *
     * @param fieldId ID текстового поля
     */
    protected void pressEnter(String fieldId) {
        TextField field = (TextField) findNode(fieldId);
        runOnFxThread(() -> field.fireEvent(new KeyEvent(
                KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, false, false, false, false
        )));
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
     *
     * @deprecated В headless-режиме координатная эмуляция мыши ненадёжна;
     *             используйте {@link #clickOn(String)} (через fire()).
     */
    @Deprecated
    protected javafx.geometry.Point2D getNodeCenter(Node node) {
        double x = node.getBoundsInParent().getMinX() + node.getBoundsInParent().getWidth() / 2;
        double y = node.getBoundsInParent().getMinY() + node.getBoundsInParent().getHeight() / 2;
        return new javafx.geometry.Point2D(x, y);
    }

    /**
     * Синхронно выполняет действие на JavaFX Application Thread и дожидается
     * его завершения. Все операции с UI-узлами должны идти через этот метод:
     * он гарантирует, что к моменту возврата управление изменённое состояние
     * уже применилось, и последующие assert'ы видят актуальные данные.
     *
     * @param action действие, выполняемое на FX-потоке
     */
    protected void runOnFxThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
            waitForFxEvents();
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                latch.countDown();
            }
        });
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Таймаут ожидания выполнения на FX-потоке");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        waitForFxEvents();
    }

    /**
     * Синхронно вычисляет значение на JavaFX Application Thread.
     *
     * @param supplier поставщик значения
     * @param <T> тип значения
     * @return вычисленное значение
     */
    protected <T> T getOnFxThread(Supplier<T> supplier) {
        if (Platform.isFxApplicationThread()) {
            return supplier.get();
        }
        AtomicReference<T> ref = new AtomicReference<>();
        runOnFxThread(() -> ref.set(supplier.get()));
        return ref.get();
    }

    /**
     * Ожидание завершения всех запланированных задач в Platform (layout/pulse).
     * Позволяет таблицам и bindings пересчитаться после изменения данных.
     */
    protected void waitForFxEvents() {
        // Два прохода: первый запускает отложенные layout-задачи, второй
        // дожидается их выполнения — этого достаточно для пересчёта bindings.
        for (int i = 0; i < 2; i++) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(latch::countDown);
            try {
                if (!latch.await(2, TimeUnit.SECONDS)) {
                    System.err.println("Таймаут ожидания Platform (проход " + (i + 1) + ")");
                    return;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * Получение TableView по ID.
     *
     * @param tableId ID таблицы
     * @param <T> тип элементов таблицы
     * @return таблица
     */
    @SuppressWarnings("unchecked")
    protected <T> TableView<T> getTable(String tableId) {
        return (TableView<T>) findNode(tableId);
    }

    /**
     * Выбор строки в TableView по индексу и прокрутка к ней.
     *
     * @param tableId ID таблицы
     * @param index индекс строки
     * @param <T> тип элементов таблицы
     */
    protected <T> void selectTableRow(String tableId, int index) {
        TableView<T> table = getTable(tableId);
        runOnFxThread(() -> {
            table.getSelectionModel().select(index);
            table.scrollTo(index);
        });
    }

    /**
     * Переключение вкладки в главном TabPane по индексу.
     *
     * @param tabIndex индекс вкладки
     */
    protected void switchTab(int tabIndex) {
        TabPane tabPane = (TabPane) findNode("mainTabPane");
        runOnFxThread(() -> tabPane.getSelectionModel().select(tabIndex));
    }

    /**
     * Переключение вкладки в главном TabPane по заголовку.
     *
     * @param title заголовок вкладки
     */
    protected void switchTab(String title) {
        TabPane tabPane = (TabPane) findNode("mainTabPane");
        runOnFxThread(() -> {
            for (Tab tab : tabPane.getTabs()) {
                if (tab.getText() != null && tab.getText().equals(title)) {
                    tabPane.getSelectionModel().select(tab);
                    return;
                }
            }
        });
    }
}
