package com.autoservice.utils;

import com.autoservice.TestTags;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты для форматтеров значений.
 */
@Tag(TestTags.UNIT)
class FormattersTest {

    @Test
    void cleanPhoneKeepsElevenDigitNumber() {
        assertThat(Formatters.cleanPhone("+79001234567")).isEqualTo("+79001234567");
        assertThat(Formatters.cleanPhone("89001234567")).isEqualTo("+79001234567");
        assertThat(Formatters.cleanPhone("+7 (900) 123-45-67")).isEqualTo("+79001234567");
        assertThat(Formatters.cleanPhone("9001234567")).isEqualTo("+79001234567");
    }

    @Test
    void cleanPhoneTrimsToLastTenDigits() {
        String result = Formatters.cleanPhone("+79001234567890123");

        assertThat(result).startsWith("+7");
        assertThat(result).hasSize(12);
        assertThat(result).isEqualTo("+74567890123");
    }

    @Test
    void cleanPhoneReturnsPlusSevenForNull() {
        assertThat(Formatters.cleanPhone(null)).isEqualTo("+7");
    }

    @Test
    void cleanPhoneRemovesSpacesAndHyphens() {
        assertThat(Formatters.cleanPhone("+7-900-123-45-67")).isEqualTo("+79001234567");
        assertThat(Formatters.cleanPhone("+7  900  123  45  67")).isEqualTo("+79001234567");
    }

    @Test
    void normalizeCarNumberUppercasesAndRemovesSeparators() {
        assertThat(Formatters.normalizeCarNumber("А123ВС163")).isEqualTo("А123ВС163");
        assertThat(Formatters.normalizeCarNumber("а123вс163")).isEqualTo("А123ВС163");
        assertThat(Formatters.normalizeCarNumber("А 123 ВС 163")).isEqualTo("А123ВС163");
        assertThat(Formatters.normalizeCarNumber("А-123.ВС_163")).isEqualTo("А123ВС163");
        assertThat(Formatters.normalizeCarNumber("А-123-ВС-163")).isEqualTo("А123ВС163");
        assertThat(Formatters.normalizeCarNumber("А   123   ВС   163")).isEqualTo("А123ВС163");
    }

    @Test
    void normalizeCarNumberKeepsShortNumber() {
        assertThat(Formatters.normalizeCarNumber("А123ВС")).isEqualTo("А123ВС");
    }

    @Test
    void normalizeCarNumberReturnsEmptyForNull() {
        assertThat(Formatters.normalizeCarNumber(null)).isEmpty();
    }

    @Test
    void formatPhoneForDisplayBuildsReadableNumber() {
        assertThat(Formatters.formatPhoneForDisplay("+79001234567")).isEqualTo("+7 (900) 123-45-67");
        assertThat(Formatters.formatPhoneForDisplay("89001234567")).isEqualTo("+7 (900) 123-45-67");
        assertThat(Formatters.formatPhoneForDisplay("+7 (900) 123-45-67")).isEqualTo("+7 (900) 123-45-67");
    }

    @Test
    void formatPhoneForDisplayReturnsEmptyForNullOrEmpty() {
        assertThat(Formatters.formatPhoneForDisplay(null)).isEmpty();
        assertThat(Formatters.formatPhoneForDisplay("")).isEmpty();
    }

    @Test
    void formatPhoneForDisplayReturnsUnexpectedValueAsIs() {
        assertThat(Formatters.formatPhoneForDisplay("123")).isEqualTo("123");
    }
}
