package com.autoservice;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Тесты для класса Client
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClientTest {

    @Test
    @Order(1)
    void testClientConstructorBasic() {
        Client client = new Client("Иван", "+79001234567", "Haval Jolion", "А123ВС163");
        
        assertThat(client.getName()).isEqualTo("Иван");
        assertThat(client.getPhone()).isEqualTo("+79001234567");
        assertThat(client.getCarModel()).isEqualTo("Haval Jolion");
        assertThat(client.getCarNumber()).isEqualTo("А123ВС163");
        assertThat(client.getId()).isEqualTo(-1);
        assertThat(client.getLastName()).isEmpty();
        assertThat(client.getLastRepairDate()).isEmpty();
    }

    @Test
    @Order(2)
    void testClientConstructorWithLastName() {
        Client client = new Client("Иван", "Петров", "+79001234567", "Haval F7", "В456СЕ163");
        
        assertThat(client.getName()).isEqualTo("Иван");
        assertThat(client.getLastName()).isEqualTo("Петров");
        assertThat(client.getPhone()).isEqualTo("+79001234567");
        assertThat(client.getCarModel()).isEqualTo("Haval F7");
        assertThat(client.getCarNumber()).isEqualTo("В456СЕ163");
    }

    @Test
    @Order(3)
    void testClientConstructorWithId() {
        Client client = new Client(1, "Иван", "+79001234567", "Haval F5", "С789ЕЕ163");
        
        assertThat(client.getId()).isEqualTo(1);
        assertThat(client.getName()).isEqualTo("Иван");
    }

    @Test
    @Order(4)
    void testClientConstructorWithAllFields() {
        Client client = new Client(1, "Иван", "Петров", "+79001234567", "Haval Dargo", "М123НО163", "2024-01-15");
        
        assertThat(client.getId()).isEqualTo(1);
        assertThat(client.getName()).isEqualTo("Иван");
        assertThat(client.getLastName()).isEqualTo("Петров");
        assertThat(client.getPhone()).isEqualTo("+79001234567");
        assertThat(client.getCarModel()).isEqualTo("Haval Dargo");
        assertThat(client.getCarNumber()).isEqualTo("М123НО163");
        assertThat(client.getLastRepairDate()).isEqualTo("2024-01-15");
    }

    @Test
    @Order(5)
    void testClientSetters() {
        Client client = new Client("Иван", "+79001234567", "Haval Jolion", "А123ВС163");
        
        client.setId(5);
        client.setLastName("Иванов");
        client.setPhone("+79112223344");
        client.setCarModel("Haval Big Dog");
        client.setCarNumber("Е456КХ163");
        client.setLastRepairDate("2024-06-20");
        
        assertThat(client.getId()).isEqualTo(5);
        assertThat(client.getLastName()).isEqualTo("Иванов");
        assertThat(client.getPhone()).isEqualTo("+79112223344");
        assertThat(client.getCarModel()).isEqualTo("Haval Big Dog");
        assertThat(client.getCarNumber()).isEqualTo("Е456КХ163");
        assertThat(client.getLastRepairDate()).isEqualTo("2024-06-20");
    }

    @Test
    @Order(6)
    void testClientToString() {
        Client client = new Client("Иван", "+79001234567", "Haval Jolion", "А123ВС163");
        
        String result = client.toString();
        assertThat(result).contains("Иван");
        assertThat(result).contains("А123ВС163");
    }

    @Test
    @Order(7)
    void testClientEqualsAndHashCode() {
        Client client1 = new Client("Иван", "+79001234567", "Haval Jolion", "А123ВС163");
        Client client2 = new Client("Иван", "+79001234567", "Haval F7", "В456СЕ163");
        Client client3 = new Client("Петр", "+79001234567", "Haval Jolion", "А123ВС163");
        
        assertThat(client1).isEqualTo(client2);
        assertThat(client1).isNotEqualTo(client3);
        assertThat(client1.hashCode()).isEqualTo(client2.hashCode());
    }

    @Test
    @Order(8)
    void testClientWithNullLastRepairDate() {
        Client client = new Client(1, "Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163", null);
        
        assertThat(client.getLastRepairDate()).isEmpty();
    }
}
