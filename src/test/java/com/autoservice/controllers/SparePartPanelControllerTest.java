package com.autoservice.controllers;

import com.autoservice.*;
import com.autoservice.TestTags;
import org.junit.jupiter.api.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

/**
 * Тесты для SparePartPanelController.
 * 
 * <p>Проверяют базовую функциональность управления запчастями: добавление,
 * обновление, удаление запчастей, обновление таблицы. Тесты не зависят
 * от JavaFX и могут запускаться без инициализации UI.</p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag(TestTags.CONTROLLER)
class SparePartPanelControllerTest extends BaseTest {

    private SparePart testPart;

    @BeforeEach
    void setUp() {
        testPart = new SparePart("Масло моторное", 800, 1200, 10);
    }

    @Test
    @Order(1)
    void testAddSparePart() {
        DataStore.load();
        SparePartPanelController.addSparePart(testPart);
        DataStore.load();

        assertThat(DataStore.getSpareParts()).isNotEmpty();
        assertThat(DataStore.getSpareParts().get(0).getName()).isEqualTo("Масло моторное");
    }

    @Test
    @Order(2)
    void testUpdateSparePart() {
        DataStore.load();
        SparePartPanelController.addSparePart(testPart);
        DataStore.load();

        SparePart saved = DataStore.getSpareParts().get(0);
        saved.setStock(20);
        SparePartPanelController.updateSparePart(saved);
        DataStore.load();

        assertThat(DataStore.getSpareParts().get(0).getStock()).isEqualTo(20);
    }

    @Test
    @Order(3)
    void testRemoveSpareParts() {
        DataStore.load();
        SparePartPanelController.addSparePart(testPart);
        DataStore.load();

        assertThat(DataStore.getSpareParts()).hasSize(1);

        SparePart saved = DataStore.getSpareParts().get(0);
        SparePartPanelController.removeSpareParts(List.of(saved));
        DataStore.load();

        assertThat(DataStore.getSpareParts()).isEmpty();
    }

    @Test
    @Order(4)
    void testRefreshTableDoesNotThrow() {
        assertThatCode(() -> SparePartPanelController.refreshTable()).doesNotThrowAnyException();
    }
}
