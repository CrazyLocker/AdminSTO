package com.autoservice.controllers;

import com.autoservice.*;
import com.autoservice.TestTags;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Тесты для ServicePanelController
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag(TestTags.CONTROLLER)
class ServicePanelControllerTest extends BaseTest {

    private Service testService;

    @BeforeEach
    void setUp() {
        testService = new Service("Замена масла", 1500);
    }

    @Test
    @Order(1)
    void testAddService() {
        DataStore.load();
        ServicePanelController.addService(testService);
        DataStore.load();

        assertThat(DataStore.getServices()).isNotEmpty();
        assertThat(DataStore.getServices().get(0).getName()).isEqualTo("Замена масла");
    }

    @Test
    @Order(2)
    void testUpdateService() {
        DataStore.load();
        ServicePanelController.addService(testService);
        DataStore.load();

        Service saved = DataStore.getServices().get(0);
        saved.setPrice(2000);
        ServicePanelController.updateService(saved);
        DataStore.load();

        assertThat(DataStore.getServices().get(0).getPrice()).isEqualTo(2000);
    }

    @Test
    @Order(3)
    void testRemoveService() {
        DataStore.load();
        ServicePanelController.addService(testService);
        DataStore.load();

        assertThat(DataStore.getServices()).hasSize(1);

        Service saved = DataStore.getServices().get(0);
        ServicePanelController.removeService(saved);
        DataStore.load();

        assertThat(DataStore.getServices()).isEmpty();
    }

    @Test
    @Order(4)
    void testRefreshTableDoesNotThrow() {
        // refreshTable делегирует в ServicePanel.refreshTable(),
        // который безопасен при table == null
        assertThatCode(() -> ServicePanelController.refreshTable()).doesNotThrowAnyException();
    }
}
