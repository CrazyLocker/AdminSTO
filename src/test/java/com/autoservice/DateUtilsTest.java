package com.autoservice;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;

/**
 * Тесты для класса DateUtils
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DateUtilsTest {

    @Test
    @Order(1)
    void testFormatDateWithLocalDate() {
        LocalDate date = LocalDate.of(2024, 2, 20);
        
        String result = DateUtils.formatDate(date);
        
        assertThat(result).contains("2024");
        assertThat(result).contains("февраля");
    }

    @Test
    @Order(2)
    void testFormatDateWithStringDate() {
        String result = DateUtils.formatDate("2024-02-20");
        
        assertThat(result).contains("2024");
        assertThat(result).contains("февраля");
    }

    @Test
    @Order(3)
    void testFormatDateWithNull() {
        String result = DateUtils.formatDate((LocalDate) null);
        
        assertThat(result).isEmpty();
    }

    @Test
    @Order(4)
    void testFormatDateWithEmptyString() {
        String result = DateUtils.formatDate("");
        
        assertThat(result).isEmpty();
    }

    @Test
    @Order(5)
    void testFormatDateWithNullString() {
        String result = DateUtils.formatDate((String) null);
        
        assertThat(result).isEmpty();
    }

    @Test
    @Order(6)
    void testFormatDateWithInvalidString() {
        String result = DateUtils.formatDate("invalid-date");
        
        assertThat(result).isEqualTo("invalid-date");
    }

    @Test
    @Order(7)
    void testParseDateWithValidString() {
        LocalDate result = DateUtils.parseDate("2024-02-20");
        
        assertThat(result).isEqualTo(LocalDate.of(2024, 2, 20));
    }

    @Test
    @Order(8)
    void testParseDateWithNull() {
        LocalDate result = DateUtils.parseDate((String) null);
        
        assertThat(result).isNull();
    }

    @Test
    @Order(9)
    void testParseDateWithEmptyString() {
        LocalDate result = DateUtils.parseDate("");
        
        assertThat(result).isNull();
    }

    @Test
    @Order(10)
    void testParseDateWithInvalidString() {
        LocalDate result = DateUtils.parseDate("not-a-date");
        
        assertThat(result).isNull();
    }

    @Test
    @Order(11)
    void testFormatDateDifferentMonths() {
        String january = DateUtils.formatDate("2024-01-15");
        String december = DateUtils.formatDate("2024-12-25");
        
        assertThat(january).contains("января");
        assertThat(december).contains("декабря");
    }

    @Test
    @Order(12)
    void testFormatDateWithSingleDigitDay() {
        String result = DateUtils.formatDate("2024-02-05");
        
        assertThat(result).contains("5 февраля");
    }
}
