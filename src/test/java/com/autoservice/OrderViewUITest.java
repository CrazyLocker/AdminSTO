package com.autoservice;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderViewUITest extends BaseUITest {

    @BeforeEach
    void setUp() {
        createStage();
    }

    @Test
    void testOrderTableIsEmptyInitially() {
        runOnFxThread(() -> {
            TableView<WorkOrder> table = getTable("orderTable");
            assertThat(table.getItems()).isEmpty();
        });
    }

    @Test
    void testOrderTableShowsOrders() {
        runOnFxThread(() -> {
            Client client = new Client(-1, "Тестовый", "Клиент", "89001234567", "Toyota Camry", "А123ВВ777", "");
            DataStore.addClient(client);

            WorkOrder order = new WorkOrder(client);
            order.setStatus(WorkOrder.STATUS_NEW);
            order.addService(0, "Замена масла", 500.0);
            DataStore.addOrder(order);
        });

        waitForFxEvents();
        switchTab("Заказы");
        waitForFxEvents();

        runOnFxThread(() -> {
            TableView<WorkOrder> table = getTable("orderTable");
            assertThat(table.getItems()).hasSize(1);
            WorkOrder firstOrder = table.getItems().get(0);
            assertThat(firstOrder.getClient().getLastName()).isEqualTo("Клиент");
            assertThat(firstOrder.getServices()).contains("Замена масла");
        });
    }

    @Test
    void testMultipleOrdersInTable() {
        runOnFxThread(() -> {
            for (int i = 0; i < 3; i++) {
                Client client = new Client(-1, "Клиент" + i, "Фамилия" + i, "8900" + (1000000 + i), "Toyota", "A" + i + i + i + "ВВ777", "");
                DataStore.addClient(client);

                WorkOrder order = new WorkOrder(client);
                order.setStatus(i % 2 == 0 ? WorkOrder.STATUS_NEW : WorkOrder.STATUS_IN_PROGRESS);
                order.addService(0, "Услуга " + i, 1000.0 * (i + 1));
                DataStore.addOrder(order);
            }
        });

        waitForFxEvents();
        switchTab("Заказы");
        waitForFxEvents();

        runOnFxThread(() -> {
            TableView<WorkOrder> table = getTable("orderTable");
            assertThat(table.getItems()).hasSize(3);
        });
    }

    @Test
    void testSearchByClientName() {
        runOnFxThread(() -> {
            Client client1 = new Client(-1, "Иванов", "Иван", "89001111111", "Toyota", "А111ВВ777", "");
            Client client2 = new Client(-1, "Петров", "Петр", "89002222222", "Honda", "А222ВВ777", "");
            DataStore.addClient(client1);
            DataStore.addClient(client2);

            WorkOrder order1 = new WorkOrder(client1);
            order1.setStatus(WorkOrder.STATUS_NEW);
            order1.addService(0, "Замена масла", 500.0);
            DataStore.addOrder(order1);

            WorkOrder order2 = new WorkOrder(client2);
            order2.setStatus(WorkOrder.STATUS_CLOSED);
            order2.addService(0, "Диагностика", 1500.0);
            DataStore.addOrder(order2);
        });

        waitForFxEvents();
        switchTab("Заказы");
        waitForFxEvents();

        writeText("searchField", "Иванов");
        waitForFxEvents();

        runOnFxThread(() -> {
            TableView<WorkOrder> table = getTable("orderTable");
            assertThat(table.getItems()).hasSize(1);
            assertThat(table.getItems().get(0).getClient().getLastName()).isEqualTo("Иванов");
        });
    }

    @Test
    void testSearchByCarNumber() {
        runOnFxThread(() -> {
            Client client = new Client(-1, "Тест", "Клиент", "89001234567", "Toyota", "А777ВВ777", "");
            DataStore.addClient(client);

            WorkOrder order = new WorkOrder(client);
            order.setStatus(WorkOrder.STATUS_NEW);
            order.addService(0, "Замена масла", 500.0);
            DataStore.addOrder(order);
        });

        waitForFxEvents();
        switchTab("Заказы");
        waitForFxEvents();

        writeText("searchField", "А777ВВ777");
        waitForFxEvents();

        runOnFxThread(() -> {
            TableView<WorkOrder> table = getTable("orderTable");
            assertThat(table.getItems()).hasSize(1);
            assertThat(table.getItems().get(0).getClient().getCarNumber()).isEqualTo("А777ВВ777");
        });
    }

    @Test
    void testSearchNotFound() {
        runOnFxThread(() -> {
            Client client = new Client(-1, "Тест", "Клиент", "89001234567", "Toyota", "А123ВВ777", "");
            DataStore.addClient(client);

            WorkOrder order = new WorkOrder(client);
            order.setStatus(WorkOrder.STATUS_NEW);
            DataStore.addOrder(order);
        });

        waitForFxEvents();
        switchTab("Заказы");
        waitForFxEvents();

        writeText("searchField", "Несуществующий");
        waitForFxEvents();

        runOnFxThread(() -> {
            TableView<WorkOrder> table = getTable("orderTable");
            assertThat(table.getItems()).isEmpty();
        });
    }

    @Test
    void testStatusFilterNew() {
        runOnFxThread(() -> {
            Client client1 = new Client(-1, "Клиент1", "Тест", "89001111111", "Toyota", "А111ВВ777", "");
            Client client2 = new Client(-1, "Клиент2", "Тест", "89002222222", "Honda", "А222ВВ777", "");
            DataStore.addClient(client1);
            DataStore.addClient(client2);

            WorkOrder order1 = new WorkOrder(client1);
            order1.setStatus(WorkOrder.STATUS_NEW);
            DataStore.addOrder(order1);

            WorkOrder order2 = new WorkOrder(client2);
            order2.setStatus(WorkOrder.STATUS_CLOSED);
            DataStore.addOrder(order2);
        });

        waitForFxEvents();
        switchTab("Заказы");
        waitForFxEvents();

        selectFromComboBox("statusFilterCombo", WorkOrder.STATUS_NEW);
        waitForFxEvents();

        runOnFxThread(() -> {
            TableView<WorkOrder> table = getTable("orderTable");
            assertThat(table.getItems()).hasSize(1);
            assertThat(table.getItems().get(0).getStatus()).isEqualTo(WorkOrder.STATUS_NEW);
        });
    }

    @Test
    void testStatusFilterClosed() {
        runOnFxThread(() -> {
            Client client = new Client(-1, "Клиент", "Тест", "89001234567", "Toyota", "А123ВВ777", "");
            DataStore.addClient(client);

            WorkOrder order = new WorkOrder(client);
            order.setStatus(WorkOrder.STATUS_CLOSED);
            DataStore.addOrder(order);
        });

        waitForFxEvents();
        switchTab("Заказы");
        waitForFxEvents();

        selectFromComboBox("statusFilterCombo", WorkOrder.STATUS_CLOSED);
        waitForFxEvents();

        runOnFxThread(() -> {
            TableView<WorkOrder> table = getTable("orderTable");
            assertThat(table.getItems()).hasSize(1);
            assertThat(table.getItems().get(0).getStatus()).isEqualTo(WorkOrder.STATUS_CLOSED);
        });
    }

    @Test
    void testStatusFilterAll() {
        runOnFxThread(() -> {
            for (int i = 0; i < 3; i++) {
                Client client = new Client(-1, "Клиент" + i, "Тест", "8900" + (1000000 + i), "Toyota", "A" + i + i + i + "ВВ777", "");
                DataStore.addClient(client);

                WorkOrder order = new WorkOrder(client);
                order.setStatus(i == 0 ? WorkOrder.STATUS_NEW : (i == 1 ? WorkOrder.STATUS_IN_PROGRESS : WorkOrder.STATUS_CLOSED));
                DataStore.addOrder(order);
            }
        });

        waitForFxEvents();
        switchTab("Заказы");
        waitForFxEvents();

        selectFromComboBox("statusFilterCombo", "Все");
        waitForFxEvents();

        runOnFxThread(() -> {
            TableView<WorkOrder> table = getTable("orderTable");
            assertThat(table.getItems()).hasSize(3);
        });
    }

    @Test
    void testMinTotalFilter() {
        runOnFxThread(() -> {
            Client client = new Client(-1, "Клиент", "Тест", "89001234567", "Toyota", "А123ВВ777", "");
            DataStore.addClient(client);

            WorkOrder order = new WorkOrder(client);
            order.setStatus(WorkOrder.STATUS_NEW);
            order.addService(0, "Услуга", 5000.0);
            DataStore.addOrder(order);
        });

        waitForFxEvents();
        switchTab("Заказы");
        waitForFxEvents();

        runOnFxThread(() -> {
            TextField minTotalField = (TextField) findNode("minTotalField");
            minTotalField.clear();
            minTotalField.setText("4000");
        });
        waitForFxEvents();

        runOnFxThread(() -> {
            TableView<WorkOrder> table = getTable("orderTable");
            assertThat(table.getItems()).hasSize(1);
            assertThat(table.getItems().get(0).getTotal()).isGreaterThanOrEqualTo(4000);
        });
    }

    @Test
    void testMaxTotalFilter() {
        runOnFxThread(() -> {
            Client client = new Client(-1, "Клиент", "Тест", "89001234567", "Toyota", "А123ВВ777", "");
            DataStore.addClient(client);

            WorkOrder order = new WorkOrder(client);
            order.setStatus(WorkOrder.STATUS_NEW);
            order.addService(0, "Услуга", 1000.0);
            DataStore.addOrder(order);
        });

        waitForFxEvents();
        switchTab("Заказы");
        waitForFxEvents();

        runOnFxThread(() -> {
            TextField maxTotalField = (TextField) findNode("maxTotalField");
            maxTotalField.clear();
            maxTotalField.setText("2000");
        });
        waitForFxEvents();

        runOnFxThread(() -> {
            TableView<WorkOrder> table = getTable("orderTable");
            assertThat(table.getItems()).hasSize(1);
            assertThat(table.getItems().get(0).getTotal()).isLessThanOrEqualTo(2000);
        });
    }

    @Test
    void testTotalRangeFilter() {
        runOnFxThread(() -> {
            Client client = new Client(-1, "Клиент", "Тест", "89001234567", "Toyota", "А123ВВ777", "");
            DataStore.addClient(client);

            WorkOrder order = new WorkOrder(client);
            order.setStatus(WorkOrder.STATUS_NEW);
            order.addService(0, "Услуга", 3500.0);
            DataStore.addOrder(order);
        });

        waitForFxEvents();
        switchTab("Заказы");
        waitForFxEvents();

        runOnFxThread(() -> {
            TextField minField = (TextField) findNode("minTotalField");
            TextField maxField = (TextField) findNode("maxTotalField");
            minField.clear();
            minField.setText("3000");
            maxField.clear();
            maxField.setText("4000");
        });
        waitForFxEvents();

        runOnFxThread(() -> {
            TableView<WorkOrder> table = getTable("orderTable");
            assertThat(table.getItems()).hasSize(1);
            assertThat(table.getItems().get(0).getTotal()).isBetween(3000.0, 4000.0);
        });
    }

    @Test
    void testAdvancedFilterToggle() {
        switchTab("Заказы");
        waitForFxEvents();

        runOnFxThread(() -> {
            javafx.scene.layout.VBox advancedPanel = (javafx.scene.layout.VBox) findNode("advancedFilterPanel");
            assertThat(advancedPanel.isVisible()).isFalse();
        });

        clickOn("advancedToggleBtn");
        waitForFxEvents();

        runOnFxThread(() -> {
            javafx.scene.layout.VBox advancedPanel = (javafx.scene.layout.VBox) findNode("advancedFilterPanel");
            assertThat(advancedPanel.isVisible()).isTrue();
        });

        clickOn("advancedToggleBtn");
        waitForFxEvents();

        runOnFxThread(() -> {
            javafx.scene.layout.VBox advancedPanel = (javafx.scene.layout.VBox) findNode("advancedFilterPanel");
            assertThat(advancedPanel.isVisible()).isFalse();
        });
    }

    @Test
    void testEditBtnDisabledWhenNoSelection() {
        switchTab("Заказы");
        waitForFxEvents();

        runOnFxThread(() -> {
            Button editBtn = (Button) findNode("editBtn");
            assertThat(editBtn.isDisable()).isTrue();
        });
    }

    @Test
    void testEditBtnEnabledWhenRowSelected() {
        runOnFxThread(() -> {
            Client client = new Client(-1, "Клиент", "Тест", "89001234567", "Toyota", "А123ВВ777", "");
            DataStore.addClient(client);

            WorkOrder order = new WorkOrder(client);
            order.setStatus(WorkOrder.STATUS_NEW);
            DataStore.addOrder(order);
        });

        waitForFxEvents();
        switchTab("Заказы");
        waitForFxEvents();

        selectTableRow("orderTable", 0);
        waitForFxEvents();

        runOnFxThread(() -> {
            Button editBtn = (Button) findNode("editBtn");
            assertThat(editBtn.isDisable()).isFalse();
        });
    }

    @Test
    void testDeleteBtnDisabledWhenNoSelection() {
        switchTab("Заказы");
        waitForFxEvents();

        runOnFxThread(() -> {
            Button deleteBtn = (Button) findNode("deleteBtn");
            assertThat(deleteBtn.isDisable()).isTrue();
        });
    }

    @Test
    void testPrintBtnDisabledWhenNoSelection() {
        switchTab("Заказы");
        waitForFxEvents();

        runOnFxThread(() -> {
            Button printBtn = (Button) findNode("printBtn");
            assertThat(printBtn.isDisable()).isTrue();
        });
    }

    @Test
    void testCreateOrderBtnVisible() {
        switchTab("Заказы");
        waitForFxEvents();

        runOnFxThread(() -> {
            Button createBtn = (Button) findNode("createOrderBtn");
            assertThat(createBtn.isVisible()).isTrue();
            assertThat(createBtn.isDisable()).isFalse();
        });
    }

    @Test
    void testResetFilters() {
        runOnFxThread(() -> {
            Client client = new Client(-1, "Клиент", "Тест", "89001234567", "Toyota", "А123ВВ777", "");
            DataStore.addClient(client);

            WorkOrder order = new WorkOrder(client);
            order.setStatus(WorkOrder.STATUS_NEW);
            order.addService(0, "Услуга", 5000.0);
            DataStore.addOrder(order);
        });

        waitForFxEvents();
        switchTab("Заказы");
        waitForFxEvents();

        runOnFxThread(() -> {
            TextField maxField = (TextField) findNode("maxTotalField");
            maxField.clear();
            maxField.setText("1000");
        });
        waitForFxEvents();

        runOnFxThread(() -> {
            TableView<WorkOrder> table = getTable("orderTable");
            assertThat(table.getItems()).isEmpty();
        });

        clickOn("resetFiltersBtn");
        waitForFxEvents();

        runOnFxThread(() -> {
            TableView<WorkOrder> table = getTable("orderTable");
            assertThat(table.getItems()).hasSize(1);
        });
    }
}
