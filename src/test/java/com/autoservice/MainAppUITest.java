package com.autoservice;

import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UI-тест главного окна приложения.
 *
 * <p>Проверяет структурные элементы главного окна: наличие вкладок,
 * переключение между ними и видимость ключевых таблиц. Тесты работают
 * в headless-режиме через прямую манипуляцию контролами на JavaFX
 * Application Thread (без эмуляции мыши).</p>
 */
@Tag(TestTags.UI)
@EnabledIfSystemProperty(named = "javafx.headless", matches = "true")
class MainAppUITest extends BaseUITest {

    @BeforeEach
    void setUpStage() {
        createStage();
    }

    @Test
    @DisplayName("Главный TabPane присутствует и содержит 8 вкладок")
    void mainTabPaneHasAllTabs() {
        TabPane tabPane = (TabPane) findNode("mainTabPane");
        assertThat(tabPane.getTabs()).hasSize(8);
    }

    @Test
    @DisplayName("Заголовки вкладок соответствуют ожидаемым")
    void tabTitlesAreCorrect() {
        TabPane tabPane = (TabPane) findNode("mainTabPane");
        assertThat(tabPane.getTabs())
                .extracting(t -> t.getText())
                .containsExactly(
                        "Дашборд", "Клиенты", "Заказы", "Услуги",
                        "Запчасти", "Склад", "Запись", "Настройки"
                );
    }

    @Test
    @DisplayName("Переключение на вкладку 'Клиенты' делает таблицу клиентов видимой")
    void switchingToClientsTabShowsClientTable() {
        switchTab("Клиенты");
        TableView<?> table = getTable("clientTable");
        assertThat(table.isVisible()).isTrue();
    }

    @Test
    @DisplayName("Переключение на вкладку 'Заказы' открывает таблицу заказов")
    void switchingToOrdersTabShowsOrderTable() {
        switchTab("Заказы");
        TableView<?> table = getTable("orderTable");
        assertThat(table.isVisible()).isTrue();
    }

    @Test
    @DisplayName("Кнопки редактирования/удаления клиента изначально отключены")
    void clientEditButtonsDisabledInitially() {
        switchTab("Клиенты");
        assertThat(findNode("editClientBtn").isDisabled()).isTrue();
        assertThat(findNode("deleteClientBtn").isDisabled()).isTrue();
    }

    @Test
    @DisplayName("Переключение вкладок по индексу работает")
    void switchTabByIndex() {
        switchTab(2);
        TabPane tabPane = (TabPane) findNode("mainTabPane");
        assertThat(tabPane.getSelectionModel().getSelectedIndex()).isEqualTo(2);
    }
}
