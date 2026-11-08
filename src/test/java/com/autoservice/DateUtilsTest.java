package com.autoservice;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Тесты для класса DateUtils
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag(TestTags.UNIT)
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
    @Order(13)
    void testParseDateTimeStrictWithColon() {
        LocalDateTime result = DateUtils.parseDateTime("18/07/2026 12:30", true);

        assertThat(result).isEqualTo(LocalDateTime.of(2026, 7, 18, 12, 30));
    }

    @Test
    @Order(14)
    void testParseDateTimeStrictWithDash() {
        LocalDateTime result = DateUtils.parseDateTime("18/07/2026 12-30", true);

        assertThat(result).isEqualTo(LocalDateTime.of(2026, 7, 18, 12, 30));
    }

    @Test
    @Order(15)
    void testParseDateTimeStrictRejectsNonDatabaseFormats() {
        assertThat(DateUtils.parseDateTime("2026-07-18 12:30", true)).isNull();
        assertThat(DateUtils.parseDateTime("18 июля 2026 г. в 12-30", true)).isNull();
    }

    @Test
    @Order(16)
    void testParseDateTimeStrictRejectsOutOfRangeTime() {
        assertThat(DateUtils.parseDateTime("18/07/2026 24:00", true)).isNull();
        assertThat(DateUtils.parseDateTime("18/07/2026 12:60", true)).isNull();
    }

    @Test
    @Order(17)
    void testParseDateTimeLenientSupportsIsoDate() {
        LocalDateTime result = DateUtils.parseDateTime("2026-07-18 12-30", false);

        assertThat(result).isEqualTo(LocalDateTime.of(2026, 7, 18, 12, 30));
    }

    @Test
    @Order(18)
    void testParseDateTimeLenientSupportsRussianFormat() {
        LocalDateTime result = DateUtils.parseDateTime("18 июля 2026 г. в 12-30", false);

        assertThat(result).isEqualTo(LocalDateTime.of(2026, 7, 18, 12, 30));
    }

    @Test
    @Order(19)
    void testParseDateTimeReturnsNullForInvalidInput() {
        assertThat(DateUtils.parseDateTime(null, true)).isNull();
        assertThat(DateUtils.parseDateTime("", false)).isNull();
        assertThat(DateUtils.parseDateTime("not-a-date", true)).isNull();
        assertThat(DateUtils.parseDateTime("not-a-date", false)).isNull();
    }
}
