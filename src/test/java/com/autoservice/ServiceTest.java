package com.autoservice;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Тесты для класса Service
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ServiceTest {

    @Test
    @Order(1)
    void testServiceConstructor() {
        Service service = new Service("Замена масла", 1500);
        
        assertThat(service.getName()).isEqualTo("Замена масла");
        assertThat(service.getPrice()).isEqualTo(1500);
    }

    @Test
    @Order(2)
    void testServiceSetters() {
        Service service = new Service("Замена масла", 1500);
        
        service.setName("Замена масла и фильтра");
        service.setPrice(2000);
        
        assertThat(service.getName()).isEqualTo("Замена масла и фильтра");
        assertThat(service.getPrice()).isEqualTo(2000);
    }

    @Test
    @Order(3)
    void testServiceToString() {
        Service service = new Service("Диагностика", 500);
        
        String result = service.toString();
        assertThat(result).contains("Диагностика");
        assertThat(result).contains("500");
    }

    @Test
    @Order(4)
    void testServiceWithZeroPrice() {
        Service service = new Service("Консультация", 0);
        
        assertThat(service.getPrice()).isEqualTo(0);
        assertThat(service.getName()).isEqualTo("Консультация");
    }

    @Test
    @Order(5)
    void testServiceWithHighPrice() {
        Service service = new Service("Капитальный ремонт ДВС", 50000);
        
        assertThat(service.getPrice()).isEqualTo(50000);
    }

    @Test
    @Order(6)
    void testServiceWithSpecialCharacters() {
        Service service = new Service("Замена масла (5л) + фильтр", 1500);
        
        assertThat(service.getName()).isEqualTo("Замена масла (5л) + фильтр");
    }
}
