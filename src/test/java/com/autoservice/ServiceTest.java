package com.autoservice;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Тесты для класса Service
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag(TestTags.UNIT)
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

    @Test
    @Order(7)
    void testServiceWithDuration() {
        Service service = new Service("Замена масла", 1500);
        
        assertThat(service.getDuration()).isEqualTo(60);
        service.setDuration(120);
        assertThat(service.getDuration()).isEqualTo(120);
    }

    @Test
    @Order(8)
    void testServiceWithPartNumber() {
        Service service = new Service("Замена масла", 1500);
        
        assertThat(service.getPartNumber()).isEmpty();
        service.setPartNumber("OF-001");
        assertThat(service.getPartNumber()).isEqualTo("OF-001");
    }

    @Test
    @Order(9)
    void testServiceWithOilData() {
        Service service = new Service("Замена масла", 1500);
        
        assertThat(service.isUsesOil()).isFalse();
        assertThat(service.getOilVolume()).isEqualTo(0);
        
        service.setUsesOil(true);
        service.setOilVolume(5.0);
        
        assertThat(service.isUsesOil()).isTrue();
        assertThat(service.getOilVolume()).isEqualTo(5.0);
    }

    @Test
    @Order(10)
    void testServiceWithSparePart() {
        Service service = new Service("Замена масла", 1500);
        
        assertThat(service.getSparePartName()).isEmpty();
        assertThat(service.getSparePartQuantity()).isEqualTo(0);
        
        service.setSparePartName("Фильтр масляный");
        service.setSparePartQuantity(1);
        
        assertThat(service.getSparePartName()).isEqualTo("Фильтр масляный");
        assertThat(service.getSparePartQuantity()).isEqualTo(1);
    }

    @Test
    @Order(11)
    void testDirtyFlag() {
        Service service = new Service("Замена масла", 1500);
        
        assertThat(service.isDirty()).isTrue();
        
        service.markClean();
        assertThat(service.isDirty()).isFalse();
        
        service.setName("Новое имя");
        assertThat(service.isDirty()).isTrue();
    }

    @Test
    @Order(12)
    void testMarkClean() {
        Service service = new Service("Замена масла", 1500);
        
        service.markClean();
        assertThat(service.isDirty()).isFalse();
        
        service.setPrice(2000);
        assertThat(service.isDirty()).isTrue();
    }

    @Test
    @Order(13)
    void testServiceWithAllFields() {
        Service service = new Service("Замена масла", 1500, 60, "OF-001");
        
        assertThat(service.getName()).isEqualTo("Замена масла");
        assertThat(service.getPrice()).isEqualTo(1500);
        assertThat(service.getDuration()).isEqualTo(60);
        assertThat(service.getPartNumber()).isEqualTo("OF-001");
    }

    @Test
    @Order(14)
    void testServiceConstructorWithAllParams() {
        Service service = new Service("Замена масла", 1500, 60, "OF-001");
        
        service.setOilVolume(5.0);
        service.setUsesOil(true);
        service.setSparePartName("Фильтр");
        service.setSparePartQuantity(1);
        
        assertThat(service.getOilVolume()).isEqualTo(5.0);
        assertThat(service.isUsesOil()).isTrue();
        assertThat(service.getSparePartName()).isEqualTo("Фильтр");
        assertThat(service.getSparePartQuantity()).isEqualTo(1);
    }

    @Test
    @Order(15)
    void testServiceWithNegativePrice() {
        Service service = new Service("Консультация", -100);
        
        assertThat(service.getPrice()).isEqualTo(-100);
    }
}
