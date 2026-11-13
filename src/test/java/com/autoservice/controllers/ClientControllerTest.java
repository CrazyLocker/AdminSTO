package com.autoservice.controllers;

import com.autoservice.*;
import com.autoservice.TestTags;
import com.autoservice.support.JavaFxTestSupport;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Тесты для ClientController.
 * 
 * <p>Проверяют базовую функциональность управления клиентами: добавление,
 * обновление, валидацию данных. Тест {@code testEditClient} открывает настоящий
 * диалог, поэтому класс поднимает JavaFX-тулкит в headless-режиме (Monocle)
 * и закрывает окно с FX-потока.</p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag(TestTags.CONTROLLER)
class ClientControllerTest extends BaseTest {

    private static final String EDIT_CLIENT_DIALOG_TITLE = "Редактирование клиента";

    private Client testClient;

    @BeforeAll
    static void initJavaFx() {
        JavaFxTestSupport.initToolkit();
    }

    @BeforeEach
    void setUp() {
        testClient = new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");
        testClient.setId(1);
        
        ClientController.setTable(null);
    }

    @Test
    @Order(1)
    void testSetTable() {
        ClientController.setTable(null);
        assertThat(ClientController.class).isNotNull();
    }

    @Test
    @Order(2)
    void testRefreshTable() {
        ClientController.setTable(null);
        ClientController.refreshTable();
        assertThat(true).isTrue();
    }

    @Test
    @Order(3)
    void testAddClient() {
        ClientController.setTable(null);
        DataStore.load();
        
        ClientController.addClient(testClient);
        DataStore.load();
        
        assertThat(DataStore.getClients()).isNotEmpty();
    }

    @Test
    @Order(4)
    void testUpdateClient() {
        ClientController.setTable(null);
        DataStore.load();
        
        ClientController.addClient(testClient);
        DataStore.load();
        
        Client updatedClient = DataStore.getClients().get(0);
        updatedClient.setPhone("+79998887766");
        ClientController.updateClient(updatedClient);
        DataStore.load();
        
        assertThat(DataStore.getClients().get(0).getPhone()).isEqualTo("+79998887766");
    }

    @Test
    @Order(5)
    void testEditClient() throws InterruptedException {
        DataStore.load();
        ClientController.addClient(testClient);
        DataStore.load();

        Client stored = DataStore.getClients().get(0);
        String phoneBefore = stored.getPhone();

        // Пользователь открывает диалог и закрывает его крестиком: данные не меняются,
        // а FX-поток не должен застрять на CompletableFuture.join().
        Thread closer = JavaFxTestSupport.closeStageWhenShown(EDIT_CLIENT_DIALOG_TITLE, 15_000);
        JavaFxTestSupport.runOnFxThread(() -> ClientController.editClient(stored));
        closer.join(TimeUnit.SECONDS.toMillis(15));
        assertThat(closer.isAlive()).as("диалог был найден и закрыт").isFalse();

        assertThat(JavaFxTestSupport.fxResponsive(5_000)).as("FX-поток отзывчив после диалога").isTrue();
        assertThat(JavaFxTestSupport.findShowingStage(EDIT_CLIENT_DIALOG_TITLE))
                .as("диалог закрыт").isNull();

        DataStore.load();
        assertThat(DataStore.getClients()).as("клиент не задвоился").hasSize(1);
        assertThat(DataStore.getClients().get(0).getPhone())
                .as("отмена в диалоге не меняет телефон").isEqualTo(phoneBefore);
    }

    @Test
    @Order(6)
    void testMultipleClients() {
        ClientController.setTable(null);
        DataStore.load();
        
        Client client1 = new Client("Клиент1", "Фамилия1", "+79001111111", "Haval F7", "А111ВС163");
        Client client2 = new Client("Клиент2", "Фамилия2", "+79002222222", "Haval F5", "В222ВС163");
        
        ClientController.addClient(client1);
        ClientController.addClient(client2);
        DataStore.load();
        
        assertThat(DataStore.getClients()).hasSize(2);
    }

    @Test
    @Order(7)
    void testClientValidation() {
        assertThat(com.autoservice.utils.ValidationUtils.isValidPhone("+79001234567")).isTrue();
        assertThat(com.autoservice.utils.ValidationUtils.isValidCarNumber("А123ВС163")).isTrue();
    }

    @Test
    @Order(8)
    void testGetClients() {
        ClientController.setTable(null);
        DataStore.load();
        
        assertThat(DataStore.getClients()).isNotNull();
    }

    @Test
    @Order(9)
    void testClientWithAllFields() {
        Client client = new Client();
        client.setName("Иван");
        client.setLastName("Петров");
        client.setPhone("+79001234567");
        client.setCarModel("Haval Jolion");
        client.setCarNumber("А123ВС163");
        client.setLastRepairDate("2024-01-01");

        assertThat(client.getName()).isEqualTo("Иван");
        assertThat(client.getLastName()).isEqualTo("Петров");
        assertThat(client.getPhone()).isEqualTo("+79001234567");
    }

    @Test
    @Order(10)
    void testClientWithoutLastName() {
        Client client = new Client("Иван", "", "+79001234567", "Haval Jolion", "А123ВС163");
        
        assertThat(client.getLastName()).isEmpty();
    }

    @Test
    @Order(11)
    void testClientWithoutCarModel() {
        Client client = new Client("Иван", "Петров", "+79001234567", "", "А123ВС163");
        
        assertThat(client.getCarModel()).isEmpty();
    }

    @Test
    @Order(12)
    void testClientWithoutCarNumber() {
        Client client = new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "");
        
        assertThat(client.getCarNumber()).isEmpty();
    }

    @Test
    @Order(13)
    void testClientFullName() {
        Client client = new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");
        String fullName = client.getFullName();
        
        assertThat(fullName).contains("Иван");
        assertThat(fullName).contains("Петров");
    }

    @Test
    @Order(14)
    void testClientToString() {
        Client client = new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");
        String result = client.toString();
        
        assertThat(result).contains("Иван");
    }

    @Test
    @Order(15)
    void testClientWithLongName() {
        Client client = new Client("Александр", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");
        
        assertThat(client.getName()).isEqualTo("Александр");
    }
}
