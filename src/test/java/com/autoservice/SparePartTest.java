package com.autoservice;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Тесты для класса SparePart
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag(TestTags.UNIT)
class SparePartTest {

    private SparePart testPart;

    @BeforeEach
    void setUp() {
        testPart = new SparePart("Масло моторное", 800, 1200, 20);
        testPart.setId(1);
    }

    @Test
    @Order(1)
    void testConstructor() {
        SparePart part = new SparePart("Тест", 100, 200, 10);
        
        assertThat(part.getName()).isEqualTo("Тест");
        assertThat(part.getPurchasePrice()).isEqualTo(100);
        assertThat(part.getRetailPrice()).isEqualTo(200);
        assertThat(part.getStock()).isEqualTo(10);
    }

    @Test
    @Order(2)
    void testGettersAndSetters() {
        SparePart part = new SparePart();
        
        part.setName("Масло");
        part.setPurchasePrice(800);
        part.setRetailPrice(1200);
        part.setStock(20);
        
        assertThat(part.getName()).isEqualTo("Масло");
        assertThat(part.getPurchasePrice()).isEqualTo(800);
        assertThat(part.getRetailPrice()).isEqualTo(1200);
        assertThat(part.getStock()).isEqualTo(20);
    }

    @Test
    @Order(3)
    void testToString() {
        SparePart part = new SparePart("Масло", 800, 1200, 20);
        String result = part.toString();
        
        assertThat(result).contains("Масло");
    }

    @Test
    @Order(4)
    void testDirtyFlag() {
        SparePart part = new SparePart();
        part.setName("Масло");
        part.setPurchasePrice(800);
        part.setRetailPrice(1200);
        part.setStock(20);
        
        // Конструктор пустой запчасти устанавливает dirty=true
        assertThat(part.isDirty()).isTrue();
        part.markClean();
        assertThat(part.isDirty()).isFalse();
        
        part.setStock(15);
        assertThat(part.isDirty()).isTrue();
    }

    @Test
    @Order(5)
    void testMarkClean() {
        SparePart part = new SparePart("Масло", 800, 1200, 20);
        part.markClean();
        
        assertThat(part.isDirty()).isFalse();
    }

    @Test
    @Order(6)
    void testZeroStock() {
        SparePart part = new SparePart("Масло", 800, 1200, 0);
        
        assertThat(part.getStock()).isEqualTo(0);
    }

    @Test
    @Order(7)
    void testNegativeStock() {
        SparePart part = new SparePart("Масло", 800, 1200, -5);
        
        assertThat(part.getStock()).isEqualTo(-5);
    }

    @Test
    @Order(8)
    void testHighStock() {
        SparePart part = new SparePart("Масло", 800, 1200, 1000);
        
        assertThat(part.getStock()).isEqualTo(1000);
    }

    @Test
    @Order(9)
    void testZeroPrice() {
        SparePart part = new SparePart("Масло", 0, 0, 10);
        
        assertThat(part.getPurchasePrice()).isEqualTo(0);
        assertThat(part.getRetailPrice()).isEqualTo(0);
    }

    @Test
    @Order(10)
    void testNegativePrice() {
        SparePart part = new SparePart("Масло", -100, -200, 10);
        
        assertThat(part.getPurchasePrice()).isEqualTo(-100);
        assertThat(part.getRetailPrice()).isEqualTo(-200);
    }

    @Test
    @Order(11)
    void testPartWithPartNumber() {
        SparePart part = new SparePart("Масло", 800, 1200, 20);
        part.setPartNumber("OF-001");
        
        assertThat(part.getPartNumber()).isEqualTo("OF-001");
    }

    @Test
    @Order(12)
    void testPartWithManufacturer() {
        SparePart part = new SparePart("Масло", 800, 1200, 20);
        part.setManufacturer("Shell");
        
        assertThat(part.getManufacturer()).isEqualTo("Shell");
    }

    @Test
    @Order(13)
    void testPartWithCompatibleModels() {
        SparePart part = new SparePart("Масло", 800, 1200, 20);
        part.setCompatibleModels("Haval F7, Haval Jolion");
        
        assertThat(part.getCompatibleModels()).isEqualTo("Haval F7, Haval Jolion");
    }

    @Test
    @Order(14)
    void testPartWithLocation() {
        SparePart part = new SparePart("Масло", 800, 1200, 20);
        part.setLocation("Склад А");
        
        assertThat(part.getLocation()).isEqualTo("Склад А");
    }

    @Test
    @Order(15)
    void testPartWithAllFields() {
        SparePart part = new SparePart(1, 0, "Масло", "OF-001", "Shell", "Haval F7", 800, 1200, 20, 5, "шт", "Склад А");
        
        assertThat(part.getId()).isEqualTo(1);
        assertThat(part.getName()).isEqualTo("Масло");
        assertThat(part.getPartNumber()).isEqualTo("OF-001");
        assertThat(part.getManufacturer()).isEqualTo("Shell");
        assertThat(part.getCompatibleModels()).isEqualTo("Haval F7");
        assertThat(part.getPurchasePrice()).isEqualTo(800);
        assertThat(part.getRetailPrice()).isEqualTo(1200);
        assertThat(part.getStock()).isEqualTo(20);
        assertThat(part.getLocation()).isEqualTo("Склад А");
    }
}
