package com.autoservice.controllers;

import com.autoservice.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DictionaryControllerTest {

    @BeforeAll
    static void setup() {
        Database.initForTest();
    }

    @AfterAll
    static void cleanup() {
        Database.close();
        try {
            new java.io.File("test.db").delete();
        } catch (Exception e) {
            // Ignored
        }
    }

    @BeforeEach
    void clearData() {
        try {
            var stmt = Database.getConnection().createStatement();
            stmt.execute("DELETE FROM appointments");
            stmt.execute("DELETE FROM orders");
            stmt.execute("DELETE FROM spare_parts");
            stmt.execute("DELETE FROM services");
            stmt.execute("DELETE FROM clients");
            stmt.execute("DELETE FROM sqlite_sequence");
            stmt.close();
            DataStore.load();
        } catch (Exception e) {
            System.err.println("Cleanup: " + e.getMessage());
        }
    }

    @Test
    @Order(1)
    void testAddService() {
        Service service = new Service("Test Service 1", 1000);
        DictionaryController.addService(service);
        assertThat(DataStore.getServices()).hasSize(1);
        assertThat(DataStore.getServices().get(0).getName()).isEqualTo("Test Service 1");
    }

    @Test
    @Order(2)
    void testRemoveService() {
        Service service = new Service("Test Service 2", 500);
        DictionaryController.addService(service);
        assertThat(DataStore.getServices()).hasSize(1);
        DictionaryController.removeService(service);
        assertThat(DataStore.getServices()).isEmpty();
    }

    @Test
    @Order(3)
    void testAddSparePart() {
        SparePart part = new SparePart("Test Part 3", 500, 1000, 10);
        DictionaryController.addSparePart(part);
        assertThat(DataStore.getSpareParts()).hasSize(1);
        assertThat(DataStore.getSpareParts().get(0).getName()).isEqualTo("Test Part 3");
    }

    @Test
    @Order(4)
    void testRemoveSparePart() {
        SparePart part = new SparePart("Test Part 4", 300, 600, 5);
        DictionaryController.addSparePart(part);
        assertThat(DataStore.getSpareParts()).hasSize(1);
        SparePart saved = DataStore.getSpareParts().get(0);
        DictionaryController.removeSparePart(saved);
        assertThat(DataStore.getSpareParts()).isEmpty();
    }

    @Test
    @Order(5)
    void testRemoveSpareParts() {
        SparePart p1 = new SparePart("Test Part 5a", 100, 200, 5);
        SparePart p2 = new SparePart("Test Part 5b", 150, 300, 10);
        SparePart p3 = new SparePart("Test Part 5c", 200, 400, 15);
        DictionaryController.addSparePart(p1);
        DictionaryController.addSparePart(p2);
        DictionaryController.addSparePart(p3);
        assertThat(DataStore.getSpareParts()).hasSize(3);
        List<SparePart> toRemove = DataStore.getSpareParts().stream()
            .filter(p -> p.getName().contains("5a") || p.getName().contains("5c"))
            .collect(java.util.stream.Collectors.toList());
        DictionaryController.removeSpareParts(toRemove);
        assertThat(DataStore.getSpareParts()).hasSize(1);
    }

    @Test
    @Order(6)
    void testIncomeSparePart() {
        SparePart part = new SparePart("Test Part 6", 500, 1000, 10);
        DictionaryController.addSparePart(part);
        SparePart saved = DataStore.getSpareParts().get(0);
        int oldStock = saved.getStock();
        DictionaryController.incomeSparePart(saved, 5);
        SparePart updated = DataStore.getSpareParts().get(0);
        assertThat(updated.getStock()).isEqualTo(oldStock + 5);
    }

    @Test
    @Order(7)
    void testRefreshAll() {
        DictionaryController.addService(new Service("Test Service 7", 1000));
        DictionaryController.addSparePart(new SparePart("Test Part 7", 500, 1000, 10));
        assertThat(DataStore.getServices()).hasSize(1);
        assertThat(DataStore.getSpareParts()).hasSize(1);
        DictionaryController.refreshAll();
        assertThat(DataStore.getServices()).hasSize(1);
        assertThat(DataStore.getSpareParts()).hasSize(1);
    }

    @Test
    @Order(8)
    void testRefreshServices() {
        DictionaryController.addService(new Service("Test Service 8", 2000));
        DictionaryController.refreshServices();
        assertThat(DataStore.getServices()).hasSize(1);
    }

    @Test
    @Order(9)
    void testRefreshSpareParts() {
        DictionaryController.addSparePart(new SparePart("Test Part 9", 600, 1200, 20));
        DictionaryController.refreshSpareParts();
        assertThat(DataStore.getSpareParts()).hasSize(1);
    }

    @Test
    @Order(10)
    void testRefreshStock() {
        DictionaryController.addSparePart(new SparePart("Test Part 10", 700, 1400, 30));
        DictionaryController.refreshStock();
        assertThat(DataStore.getSpareParts()).hasSize(1);
    }
}