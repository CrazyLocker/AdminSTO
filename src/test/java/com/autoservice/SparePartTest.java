package com.autoservice;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Тесты для класса SparePart
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SparePartTest {

    @Test
    @Order(1)
    void testSparePartConstructorSimple() {
        SparePart part = new SparePart("Моторное масло 5W-30", 800, 1200, 10);
        
        assertThat(part.getName()).isEqualTo("Моторное масло 5W-30");
        assertThat(part.getPurchasePrice()).isEqualTo(800);
        assertThat(part.getRetailPrice()).isEqualTo(1200);
        assertThat(part.getStock()).isEqualTo(10);
        assertThat(part.getId()).isEqualTo(-1);
        assertThat(part.getPartNumber()).isEmpty();
        assertThat(part.getManufacturer()).isEmpty();
        assertThat(part.getCompatibleModels()).isEmpty();
        assertThat(part.getMinStock()).isEqualTo(0);
        assertThat(part.getLocation()).isEmpty();
    }

    @Test
    @Order(2)
    void testSparePartConstructorFull() {
        SparePart part = new SparePart(
            1, 0, "Моторное масло 5W-30", "ML-5W30-5L",
            "Shell", "Haval Jolion, Haval F7",
            800, 1200, 10, 3, "Склад А-1"
        );
        
        assertThat(part.getId()).isEqualTo(1);
        assertThat(part.getCategoryId()).isEqualTo(0);
        assertThat(part.getName()).isEqualTo("Моторное масло 5W-30");
        assertThat(part.getPartNumber()).isEqualTo("ML-5W30-5L");
        assertThat(part.getManufacturer()).isEqualTo("Shell");
        assertThat(part.getCompatibleModels()).isEqualTo("Haval Jolion, Haval F7");
        assertThat(part.getPurchasePrice()).isEqualTo(800);
        assertThat(part.getRetailPrice()).isEqualTo(1200);
        assertThat(part.getStock()).isEqualTo(10);
        assertThat(part.getMinStock()).isEqualTo(3);
        assertThat(part.getLocation()).isEqualTo("Склад А-1");
    }

    @Test
    @Order(3)
    void testSparePartSetters() {
        SparePart part = new SparePart("Фильтр масляный", 300, 500, 20);
        
        part.setId(5);
        part.setPartNumber("FL-001");
        part.setManufacturer("Bosch");
        part.setCompatibleModels("Haval Jolion");
        part.setPurchasePrice(350);
        part.setRetailPrice(600);
        part.setStock(25);
        part.setMinStock(5);
        part.setLocation("Склад Б-2");
        
        assertThat(part.getId()).isEqualTo(5);
        assertThat(part.getPartNumber()).isEqualTo("FL-001");
        assertThat(part.getManufacturer()).isEqualTo("Bosch");
        assertThat(part.getCompatibleModels()).isEqualTo("Haval Jolion");
        assertThat(part.getPurchasePrice()).isEqualTo(350);
        assertThat(part.getRetailPrice()).isEqualTo(600);
        assertThat(part.getStock()).isEqualTo(25);
        assertThat(part.getMinStock()).isEqualTo(5);
        assertThat(part.getLocation()).isEqualTo("Склад Б-2");
    }

    @Test
    @Order(4)
    void testSparePartToString() {
        SparePart part = new SparePart("Моторное масло 5W-30", 800, 1200, 10);
        part.setPartNumber("ML-5W30-5L");
        
        String result = part.toString();
        assertThat(result).contains("Моторное масло 5W-30");
        assertThat(result).contains("ML-5W30-5L");
        assertThat(result).contains("1200");
    }

    @Test
    @Order(5)
    void testSparePartWithZeroStock() {
        SparePart part = new SparePart("Дефицитная деталь", 1000, 2000, 0);
        
        assertThat(part.getStock()).isEqualTo(0);
    }

    @Test
    @Order(6)
    void testSparePartWithNegativeMargin() {
        SparePart part = new SparePart("Убыточная запчасть", 1500, 1200, 10);
        
        assertThat(part.getPurchasePrice()).isEqualTo(1500);
        assertThat(part.getRetailPrice()).isEqualTo(1200);
    }

    @Test
    @Order(7)
    void testSparePartWithEmptyFields() {
        SparePart part = new SparePart(
            1, 0, "Запчасть", "", "", "",
            100, 200, 5, 0, ""
        );
        
        assertThat(part.getPartNumber()).isEmpty();
        assertThat(part.getManufacturer()).isEmpty();
        assertThat(part.getCompatibleModels()).isEmpty();
        assertThat(part.getLocation()).isEmpty();
    }
}
