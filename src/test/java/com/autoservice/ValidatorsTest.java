package com.autoservice;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Тесты для класса Validators
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag(TestTags.UNIT)
class ValidatorsTest {

    @Test
    @Order(1)
    void testIsValidPhoneValid() {
        assertThat(Validators.isValidPhone("+79001234567")).isTrue();
        assertThat(Validators.isValidPhone("+79112223344")).isTrue();
        assertThat(Validators.isValidPhone("+79998887766")).isTrue();
    }

    @Test
    @Order(2)
    void testIsValidPhoneInvalid() {
        assertThat(Validators.isValidPhone("+89001234567")).isFalse();
        assertThat(Validators.isValidPhone("9001234567")).isFalse();
        assertThat(Validators.isValidPhone("+7900123456")).isFalse();
        assertThat(Validators.isValidPhone("+790012345678")).isFalse();
        assertThat(Validators.isValidPhone("")).isFalse();
        assertThat(Validators.isValidPhone(null)).isFalse();
        assertThat(Validators.isValidPhone("+7abc1234567")).isFalse();
    }

    @Test
    @Order(3)
    void testCleanPhone() {
        assertThat(Validators.cleanPhone("+79001234567")).isEqualTo("+79001234567");
        assertThat(Validators.cleanPhone("89001234567")).isEqualTo("+79001234567");
        assertThat(Validators.cleanPhone("+7 (900) 123-45-67")).isEqualTo("+79001234567");
        assertThat(Validators.cleanPhone("9001234567")).isEqualTo("+79001234567");
        assertThat(Validators.cleanPhone(null)).isEqualTo("+7");
    }

    @Test
    @Order(4)
    void testNormalizeCarNumber() {
        assertThat(Validators.normalizeCarNumber("А123ВС163")).isEqualTo("А123ВС163");
        assertThat(Validators.normalizeCarNumber("а123вс163")).isEqualTo("А123ВС163");
        assertThat(Validators.normalizeCarNumber("А 123 ВС 163")).isEqualTo("А123ВС163");
        assertThat(Validators.normalizeCarNumber("А123ВС")).isEqualTo("А123ВС");
    }

    @Test
    @Order(5)
    void testIsValidCarNumberValid() {
        assertThat(Validators.isValidCarNumber("А123ВС163")).isTrue();
        assertThat(Validators.isValidCarNumber("В456СЕ16")).isTrue();
        assertThat(Validators.isValidCarNumber("Е789КХ197")).isTrue();
        assertThat(Validators.isValidCarNumber("М123НО163")).isTrue();
    }

    @Test
    @Order(6)
    void testIsValidCarNumberInvalid() {
        assertThat(Validators.isValidCarNumber("А123ВС")).isFalse();
        assertThat(Validators.isValidCarNumber("А12BC163")).isFalse();
        assertThat(Validators.isValidCarNumber("I123ВС163")).isFalse();
        assertThat(Validators.isValidCarNumber("А123ВС1634")).isFalse();
        assertThat(Validators.isValidCarNumber("")).isFalse();
        assertThat(Validators.isValidCarNumber(null)).isFalse();
        assertThat(Validators.isValidCarNumber("АБВГ163")).isFalse();
        assertThat(Validators.isValidCarNumber("123АВС163")).isFalse();
    }

    @Test
    @Order(7)
    void testIsValidCarNumberNotAllowedLetters() {
        // Недопустимые буквы (не входят в АВЕКМНОРСТУХ)
        assertThat(Validators.isValidCarNumber("Б123ВС163")).isFalse();
        assertThat(Validators.isValidCarNumber("Г123ВС163")).isFalse();
        assertThat(Validators.isValidCarNumber("Д123ВС163")).isFalse();
        assertThat(Validators.isValidCarNumber("Ж123ВС163")).isFalse();
        assertThat(Validators.isValidCarNumber("З123ВС163")).isFalse();
        assertThat(Validators.isValidCarNumber("Й123ВС163")).isFalse();
    }

    @Test
    @Order(8)
    void testIsValidCarNumberValidLetters() {
        // Допустимые буквы для российских госномеров: АВЕКМНОРСТУХ
        assertThat(Validators.isValidCarNumber("А123ВС163")).isTrue();
        assertThat(Validators.isValidCarNumber("В123ВС163")).isTrue();
        assertThat(Validators.isValidCarNumber("Е123ВС163")).isTrue();
        assertThat(Validators.isValidCarNumber("К123ВС163")).isTrue();
        assertThat(Validators.isValidCarNumber("М123ВС163")).isTrue();
        assertThat(Validators.isValidCarNumber("Н123ВС163")).isTrue();
        assertThat(Validators.isValidCarNumber("О123ВС163")).isTrue();
        assertThat(Validators.isValidCarNumber("Р123ВС163")).isTrue();
        assertThat(Validators.isValidCarNumber("С123ВС163")).isTrue();
        assertThat(Validators.isValidCarNumber("Т123ВС163")).isTrue();
        assertThat(Validators.isValidCarNumber("У123ВС163")).isTrue();
        assertThat(Validators.isValidCarNumber("Х123ВС163")).isTrue();
    }

    @Test
    @Order(9)
    void testCleanPhoneWithExtraDigits() {
        String longPhone = "+79001234567890123";
        String result = Validators.cleanPhone(longPhone);
        
        assertThat(result).startsWith("+7");
        assertThat(result.length()).isEqualTo(12);
    }

    @Test
    @Order(10)
    void testNormalizeCarNumberWithInvalidChars() {
        String result = Validators.normalizeCarNumber("А-123.ВС_163");
        
        assertThat(result).isEqualTo("А123ВС163");
    }

    @Test
    @Order(11)
    void testIsValidPhoneEdgeCases() {
        assertThat(Validators.isValidPhone("+7900123456")).isFalse();
        assertThat(Validators.isValidPhone("+790012345678")).isFalse();
        assertThat(Validators.isValidPhone("+79001234567890")).isFalse();
    }

    @Test
    @Order(12)
    void testCleanPhoneWithSpaces() {
        String result = Validators.cleanPhone("+7 (900) 123-45-67");
        assertThat(result).isEqualTo("+79001234567");
    }

    @Test
    @Order(13)
    void testNormalizeCarNumberEdgeCases() {
        assertThat(Validators.normalizeCarNumber("А 123 ВС 163")).isEqualTo("А123ВС163");
        assertThat(Validators.normalizeCarNumber("А-123-ВС-163")).isEqualTo("А123ВС163");
    }

    @Test
    @Order(14)
    void testIsValidCarNumberEdgeCases() {
        assertThat(Validators.isValidCarNumber("А123ВС")).isFalse();
        assertThat(Validators.isValidCarNumber("А123ВС1634")).isFalse();
        assertThat(Validators.isValidCarNumber("АА123ВС163")).isFalse();
    }

    @Test
    @Order(15)
    void testCleanPhoneWithHyphens() {
        String result = Validators.cleanPhone("+7-900-123-45-67");
        assertThat(result).isEqualTo("+79001234567");
    }

    @Test
    @Order(16)
    void testNormalizeCarNumberWithMixedCase() {
        String result = Validators.normalizeCarNumber("а123вс163");
        assertThat(result).isEqualTo("А123ВС163");
    }

    @Test
    @Order(17)
    void testIsValidCarNumberWithAllValidLetters() {
        assertThat(Validators.isValidCarNumber("А123ВС163")).isTrue();
        assertThat(Validators.isValidCarNumber("В123ВС163")).isTrue();
        assertThat(Validators.isValidCarNumber("Е123ВС163")).isTrue();
        assertThat(Validators.isValidCarNumber("К123ВС163")).isTrue();
        assertThat(Validators.isValidCarNumber("М123ВС163")).isTrue();
        assertThat(Validators.isValidCarNumber("Н123ВС163")).isTrue();
        assertThat(Validators.isValidCarNumber("О123ВС163")).isTrue();
        assertThat(Validators.isValidCarNumber("Р123ВС163")).isTrue();
        assertThat(Validators.isValidCarNumber("С123ВС163")).isTrue();
        assertThat(Validators.isValidCarNumber("Т123ВС163")).isTrue();
        assertThat(Validators.isValidCarNumber("У123ВС163")).isTrue();
        assertThat(Validators.isValidCarNumber("Х123ВС163")).isTrue();
    }

    @Test
    @Order(18)
    void testCleanPhoneWithMultipleSpaces() {
        String result = Validators.cleanPhone("+7  900  123  45  67");
        assertThat(result).isEqualTo("+79001234567");
    }

    @Test
    @Order(19)
    void testNormalizeCarNumberWithExtraSpaces() {
        String result = Validators.normalizeCarNumber("А   123   ВС   163");
        assertThat(result).isEqualTo("А123ВС163");
    }

    @Test
    @Order(20)
    void testIsValidCarNumberWithSingleDigit() {
        assertThat(Validators.isValidCarNumber("А1ВС163")).isFalse();
    }
}
