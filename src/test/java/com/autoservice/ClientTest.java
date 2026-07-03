package com.autoservice;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Тесты для класса Client
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag(TestTags.UNIT)
class ClientTest {

    private Client testClient;

    @BeforeEach
    void setUp() {
        testClient = new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");
        testClient.setId(1);
    }

    @Test
    @Order(1)
    void testConstructor() {
        Client client = new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");
        
        assertThat(client.getName()).isEqualTo("Иван");
        assertThat(client.getLastName()).isEqualTo("Петров");
        assertThat(client.getPhone()).isEqualTo("+79001234567");
        assertThat(client.getCarModel()).isEqualTo("Haval Jolion");
        assertThat(client.getCarNumber()).isEqualTo("А123ВС163");
    }

    @Test
    @Order(2)
    void testGetFullName() {
        String fullName = testClient.getFullName();
        
        assertThat(fullName).contains("Иван");
        assertThat(fullName).contains("Петров");
    }

    @Test
    @Order(3)
    void testToString() {
        String result = testClient.toString();
        
        assertThat(result).contains("Иван");
        assertThat(result).contains("Петров");
    }

    @Test
    @Order(4)
    void testDirtyFlag() {
        Client client = new Client();
        client.setName("Иван");
        client.setLastName("Петров");
        client.setPhone("+79001234567");
        client.setCarModel("Haval Jolion");
        client.setCarNumber("А123ВС163");
        
        // Конструктор пустого клиента устанавливает dirty=true
        assertThat(client.isDirty()).isTrue();
        client.markClean();
        assertThat(client.isDirty()).isFalse();
        
        client.setName("Сергей");
        assertThat(client.isDirty()).isTrue();
    }

    @Test
    @Order(5)
    void testMarkClean() {
        Client client = new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");
        client.markClean();
        
        assertThat(client.isDirty()).isFalse();
    }

    @Test
    @Order(6)
    void testEmptyLastName() {
        Client client = new Client("Иван", "", "+79001234567", "Haval Jolion", "А123ВС163");
        
        assertThat(client.getLastName()).isEmpty();
    }

    @Test
    @Order(7)
    void testEmptyCarModel() {
        Client client = new Client("Иван", "Петров", "+79001234567", "", "А123ВС163");
        
        assertThat(client.getCarModel()).isEmpty();
    }

    @Test
    @Order(8)
    void testEmptyCarNumber() {
        Client client = new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "");
        
        assertThat(client.getCarNumber()).isEmpty();
    }

    @Test
    @Order(9)
    void testGettersAndSetters() {
        Client client = new Client();
        
        client.setName("Сергей");
        client.setLastName("Сидоров");
        client.setPhone("+79998887766");
        client.setCarModel("Haval F7");
        client.setCarNumber("А456ВС163");
        client.setLastRepairDate("2024-01-01");
        
        assertThat(client.getName()).isEqualTo("Сергей");
        assertThat(client.getLastName()).isEqualTo("Сидоров");
        assertThat(client.getPhone()).isEqualTo("+79998887766");
        assertThat(client.getCarModel()).isEqualTo("Haval F7");
        assertThat(client.getCarNumber()).isEqualTo("А456ВС163");
        assertThat(client.getLastRepairDate()).isEqualTo("2024-01-01");
    }

    @Test
    @Order(10)
    void testClientWithId() {
        Client client = new Client(1, "Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163", "");
        
        assertThat(client.getId()).isEqualTo(1);
        assertThat(client.getName()).isEqualTo("Иван");
    }

    @Test
    @Order(11)
    void testClientWithNullValues() {
        Client client = new Client(null, null, null, null, null);
        
        assertThat(client.getName()).isNull();
        assertThat(client.getLastName()).isNull();
    }

    @Test
    @Order(12)
    void testClientWithLongName() {
        Client client = new Client("Александр", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");
        
        assertThat(client.getName()).isEqualTo("Александр");
    }

    @Test
    @Order(13)
    void testClientWithLongLastName() {
        Client client = new Client("Иван", "Петров-Сидоров", "+79001234567", "Haval Jolion", "А123ВС163");
        
        assertThat(client.getLastName()).isEqualTo("Петров-Сидоров");
    }

    @Test
    @Order(14)
    void testClientWithLongCarModel() {
        Client client = new Client("Иван", "Петров", "+79001234567", "Haval Jolion Elite", "А123ВС163");
        
        assertThat(client.getCarModel()).isEqualTo("Haval Jolion Elite");
    }

    @Test
    @Order(15)
    void testClientWithAllFields() {
        Client client = new Client(1, "Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163", "2024-01-01");
        
        assertThat(client.getId()).isEqualTo(1);
        assertThat(client.getName()).isEqualTo("Иван");
        assertThat(client.getLastName()).isEqualTo("Петров");
        assertThat(client.getPhone()).isEqualTo("+79001234567");
        assertThat(client.getCarModel()).isEqualTo("Haval Jolion");
        assertThat(client.getCarNumber()).isEqualTo("А123ВС163");
        assertThat(client.getLastRepairDate()).isEqualTo("2024-01-01");
    }
}
