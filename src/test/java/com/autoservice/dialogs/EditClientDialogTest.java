package com.autoservice.dialogs;

import com.autoservice.Client;
import com.autoservice.TestTags;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Тесты для EditClientDialog
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag(TestTags.UI)
class EditClientDialogTest {

    private Client testClient;

    @BeforeEach
    void setUp() {
        testClient = new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");
        testClient.setId(1);
    }

    @Test
    @Order(1)
    void testConstructorWithNewClient() {
        Client newClient = new Client();
        assertThat(newClient.getId()).isEqualTo(-1);
        assertThat(newClient.getName()).isEmpty();
    }

    @Test
    @Order(2)
    void testConstructorWithExistingClient() {
        Client existingClient = new Client(1, "Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163", "");
        assertThat(existingClient.getId()).isEqualTo(1);
        assertThat(existingClient.getName()).isEqualTo("Иван");
        assertThat(existingClient.getLastName()).isEqualTo("Петров");
    }

    @Test
    @Order(3)
    void testValidationValidData() {
        Client client = new Client();
        client.setName("Иван");
        client.setLastName("Петров");
        client.setPhone("+79001234567");
        client.setCarModel("Haval Jolion");
        client.setCarNumber("А123ВС163");

        assertThat(client.getName()).isEqualTo("Иван");
        assertThat(client.getLastName()).isEqualTo("Петров");
        assertThat(client.getPhone()).isEqualTo("+79001234567");
        assertThat(client.getCarModel()).isEqualTo("Haval Jolion");
        assertThat(client.getCarNumber()).isEqualTo("А123ВС163");
    }

    @Test
    @Order(4)
    void testValidationEmptyName() {
        Client client = new Client();
        client.setName("");

        assertThat(client.getName()).isEmpty();
    }

    @Test
    @Order(5)
    void testValidationInvalidPhone() {
        String invalidPhone = "89001234567";
        assertThat(!com.autoservice.Validators.isValidPhone(invalidPhone)).isTrue();
    }

    @Test
    @Order(6)
    void testValidationValidPhone() {
        String validPhone = "+79001234567";
        assertThat(com.autoservice.Validators.isValidPhone(validPhone)).isTrue();
    }

    @Test
    @Order(7)
    void testValidationInvalidCarNumber() {
        String invalidCarNumber = "I123ВС163";
        assertThat(!com.autoservice.Validators.isValidCarNumber(invalidCarNumber)).isTrue();
    }

    @Test
    @Order(8)
    void testValidationValidCarNumber() {
        String validCarNumber = "А123ВС163";
        assertThat(com.autoservice.Validators.isValidCarNumber(validCarNumber)).isTrue();
    }

    @Test
    @Order(9)
    void testPhoneNormalization() {
        String inputPhone = "+7 (900) 123-45-67";
        String expected = "+79001234567";
        String result = com.autoservice.Validators.cleanPhone(inputPhone);
        
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @Order(10)
    void testCarNumberNormalization() {
        String inputCarNumber = "а123вс163";
        String expected = "А123ВС163";
        String result = com.autoservice.Validators.normalizeCarNumber(inputCarNumber);
        
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @Order(11)
    void testClientFullName() {
        Client client = new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");
        String fullName = client.getFullName();
        
        assertThat(fullName).contains("Петров");
        assertThat(fullName).contains("Иван");
    }

    @Test
    @Order(12)
    void testClientToString() {
        Client client = new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");
        String result = client.toString();
        
        assertThat(result).contains("Иван");
        assertThat(result).contains("Петров");
    }

    @Test
    @Order(13)
    void testClientWithEmptyLastName() {
        Client client = new Client("Иван", "", "+79001234567", "Haval Jolion", "А123ВС163");
        
        assertThat(client.getLastName()).isEmpty();
        assertThat(client.getName()).isEqualTo("Иван");
    }

    @Test
    @Order(14)
    void testClientWithEmptyCarModel() {
        Client client = new Client("Иван", "Петров", "+79001234567", "", "А123ВС163");
        
        assertThat(client.getCarModel()).isEmpty();
    }

    @Test
    @Order(15)
    void testClientWithEmptyCarNumber() {
        Client client = new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "");
        
        assertThat(client.getCarNumber()).isEmpty();
    }
}
