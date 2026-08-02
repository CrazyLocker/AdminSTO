package com.autoservice;

import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UI-тест вкладки "Клиенты".
 *
 * <p>Проверяет реальное поведение JavaFX-интерфейса: отображение данных в
 * таблице, фильтрацию через поле поиска, активацию кнопок при выборе строки.
 * Взаимодействие выполняется через прямой доступ к контролам на JavaFX
 * Application Thread (надёжно в headless-режиме).</p>
 */
@Tag(TestTags.UI)
@EnabledIfSystemProperty(named = "javafx.headless", matches = "true")
class ClientViewUITest extends BaseUITest {

    @BeforeEach
    void setUpStage() {
        createStage();
        switchTab("Клиенты");
    }

    private void seedClient(String name, String lastName, String phone, String carModel, String carNumber) {
        Client client = new Client(name, lastName, phone, carModel, carNumber);
        com.autoservice.controllers.ClientController.addClient(client);
        waitForFxEvents();
    }

    @Test
    @DisplayName("Таблица клиентов пуста после очистки БД")
    void tableEmptyInitially() {
        TableView<Client> table = getTable("clientTable");
        assertThat(getOnFxThread(() -> table.getItems().size())).isZero();
    }

    @Test
    @DisplayName("Добавление клиента отображает строку в таблице")
    void addedClientAppearsInTable() {
        seedClient("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");

        TableView<Client> table = getTable("clientTable");
        assertThat(getOnFxThread(() -> table.getItems().size())).isEqualTo(1);
    }

    @Test
    @DisplayName("Поиск по имени фильтрует таблицу")
    void searchByNameFiltersTable() {
        seedClient("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");
        seedClient("Сергей", "Смирнов", "+79009876543", "Haval F7", "В777ОР777");

        TableView<Client> table = getTable("clientTable");
        assertThat(getOnFxThread(() -> table.getItems().size())).isEqualTo(2);

        writeText("clientSearchField", "Иван");
        assertThat(getOnFxThread(() -> table.getItems().size())).isEqualTo(1);
    }

    @Test
    @DisplayName("Поиск по телефону фильтрует таблицу")
    void searchByPhoneFiltersTable() {
        seedClient("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");
        seedClient("Сергей", "Смирнов", "+79009876543", "Haval F7", "В777ОР777");

        writeText("clientSearchField", "9876543");
        TableView<Client> table = getTable("clientTable");
        assertThat(getOnFxThread(() -> table.getItems().size())).isEqualTo(1);
    }

    @Test
    @DisplayName("Очистка поиска восстанавливает полный список")
    void clearingSearchRestoresAllRows() {
        seedClient("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");
        seedClient("Сергей", "Смирнов", "+79009876543", "Haval F7", "В777ОР777");

        writeText("clientSearchField", "Иван");
        TableView<Client> table = getTable("clientTable");
        assertThat(getOnFxThread(() -> table.getItems().size())).isEqualTo(1);

        writeText("clientSearchField", "");
        assertThat(getOnFxThread(() -> table.getItems().size())).isEqualTo(2);
    }

    @Test
    @DisplayName("Выбор строки активирует кнопки 'Изменить' и 'Удалить'")
    void selectingRowEnablesEditButtons() {
        seedClient("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");

        assertThat(findNode("editClientBtn").isDisabled()).isTrue();
        assertThat(findNode("deleteClientBtn").isDisabled()).isTrue();

        selectTableRow("clientTable", 0);

        assertThat(findNode("editClientBtn").isDisabled()).isFalse();
        assertThat(findNode("deleteClientBtn").isDisabled()).isFalse();
    }

    @Test
    @DisplayName("Поле поиска присутствует и доступно")
    void searchFieldExists() {
        TextField searchField = (TextField) findNode("clientSearchField");
        assertThat(searchField).isNotNull();
        assertThat(searchField.isDisabled()).isFalse();
    }
}
