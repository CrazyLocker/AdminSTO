package com.autoservice.controllers;

import com.autoservice.*;
import com.autoservice.TestTags;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Тесты для StockPanelController
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag(TestTags.CONTROLLER)
class StockPanelControllerTest extends BaseTest {

    private SparePart testPart;

    @BeforeEach
    void setUp() {
        testPart = new SparePart("Масло моторное", 800, 1200, 10);
    }

    @Test
    @Order(1)
    void testUpdateMinStock() {
        DataStore.load();
        DataStore.addSparePart(testPart);
        DataStore.load();

        SparePart saved = DataStore.getSpareParts().get(0);
        StockPanelController.updateMinStock(saved, 5);
        DataStore.load();

        assertThat(DataStore.getSpareParts().get(0).getMinStock()).isEqualTo(5);
    }

    @Test
    @Order(2)
    void testAddStockIncome() {
        DataStore.load();
        DataStore.addSparePart(testPart);
        DataStore.load();

        SparePart saved = DataStore.getSpareParts().get(0);
        double initialStock = saved.getStock();
        StockPanelController.addStockIncome(saved, 10);
        DataStore.load();

        assertThat(DataStore.getSpareParts().get(0).getStock()).isEqualTo(initialStock + 10);
    }

    @Test
    @Order(3)
    void testAddStockIncomeZeroAmount() {
        DataStore.load();
        DataStore.addSparePart(testPart);
        DataStore.load();

        SparePart saved = DataStore.getSpareParts().get(0);
        double initialStock = saved.getStock();
        StockPanelController.addStockIncome(saved, 0);
        DataStore.load();

        // Приход = 0 не должен менять остаток
        assertThat(DataStore.getSpareParts().get(0).getStock()).isEqualTo(initialStock);
    }

    @Test
    @Order(4)
    void testRefreshTableDoesNotThrow() {
        assertThatCode(() -> StockPanelController.refreshTable()).doesNotThrowAnyException();
    }
}
