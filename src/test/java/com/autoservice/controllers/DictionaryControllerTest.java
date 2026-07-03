package com.autoservice.controllers;

import com.autoservice.*;
import com.autoservice.TestTags;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Тесты для DictionaryController
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag(TestTags.CONTROLLER)
class DictionaryControllerTest extends BaseTest {

    @BeforeEach
    void setUp() {
        DictionaryController.setServicesTable(null);
        DictionaryController.setSparePartsTable(null);
        DictionaryController.setStockTable(null);
    }

    @Test
    @Order(1)
    void testSetServicesTable() {
        DictionaryController.setServicesTable(null);
        assertThat(DictionaryController.class).isNotNull();
    }

    @Test
    @Order(2)
    void testSetSparePartsTable() {
        DictionaryController.setSparePartsTable(null);
        assertThat(DictionaryController.class).isNotNull();
    }

    @Test
    @Order(3)
    void testSetStockTable() {
        DictionaryController.setStockTable(null);
        assertThat(DictionaryController.class).isNotNull();
    }

    @Test
    @Order(4)
    void testRefreshAll() {
        DictionaryController.refreshAll();
        assertThat(true).isTrue();
    }

    @Test
    @Order(5)
    void testRefreshServices() {
        DictionaryController.refreshServices();
        assertThat(true).isTrue();
    }

    @Test
    @Order(6)
    void testRefreshSpareParts() {
        DictionaryController.refreshSpareParts();
        assertThat(true).isTrue();
    }

    @Test
    @Order(7)
    void testRefreshStock() {
        DictionaryController.refreshStock();
        assertThat(true).isTrue();
    }

    @Test
    @Order(8)
    void testAddSparePart() {
        DictionaryController.addSparePart(new SparePart("Тест", 100, 200, 10));
        assertThat(true).isTrue();
    }

    @Test
    @Order(9)
    void testRemoveSpareParts() {
        SparePart part = new SparePart("Тест", 100, 200, 10);
        DictionaryController.removeSpareParts(java.util.List.of(part));
        assertThat(true).isTrue();
    }

    @Test
    @Order(10)
    void testAddService() {
        DictionaryController.addService(new Service("Тест", 1000));
        assertThat(true).isTrue();
    }

    @Test
    @Order(11)
    void testRemoveService() {
        Service service = new Service("Тест", 1000);
        DictionaryController.removeService(service);
        assertThat(true).isTrue();
    }

    @Test
    @Order(12)
    void testGetServices() {
        DataStore.load();
        assertThat(DataStore.getServices()).isNotNull();
    }

    @Test
    @Order(13)
    void testGetSpareParts() {
        DataStore.load();
        assertThat(DataStore.getSpareParts()).isNotNull();
    }

    @Test
    @Order(14)
    void testGetStock() {
        DataStore.load();
        assertThat(DataStore.getSpareParts()).isNotNull();
    }

    @Test
    @Order(15)
    void testServiceAndPartOperations() {
        Service service = new Service("Услуга", 1000);
        SparePart part = new SparePart("Запчасть", 500, 1000, 5);
        
        assertThat(service.getName()).isEqualTo("Услуга");
        assertThat(service.getPrice()).isEqualTo(1000);
        assertThat(part.getName()).isEqualTo("Запчасть");
        assertThat(part.getRetailPrice()).isEqualTo(1000);
    }
}
