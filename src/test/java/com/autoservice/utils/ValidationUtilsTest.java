package com.autoservice.utils;

import com.autoservice.TestTags;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-тесты для логики валидации, используемой диалогами.
 */
@Tag(TestTags.UNIT)
class ValidationUtilsTest {

    @Test
    void validateRequiredTextRejectsNullOrBlankValues() {
        assertThat(ValidationUtils.validateRequiredText(null, "Имя")).isNotNull();
        assertThat(ValidationUtils.validateRequiredText("", "Имя")).isNotNull();
        assertThat(ValidationUtils.validateRequiredText("   ", "Имя")).isNotNull();
    }

    @Test
    void validateRequiredTextAcceptsNonBlankValue() {
        assertThat(ValidationUtils.validateRequiredText("Иван", "Имя")).isNull();
    }

    @Test
    void validateIntegerTextAcceptsOnlyDigits() {
        assertThat(ValidationUtils.validateIntegerText("15000", "Пробег", true)).isNull();
        assertThat(ValidationUtils.validateIntegerText("0", "Пробег", true)).isNull();
        assertThat(ValidationUtils.validateIntegerText("  42  ", "Пробег", true)).isNull();
    }

    @Test
    void validateIntegerTextRejectsLettersNegativeAndFractionalValues() {
        assertThat(ValidationUtils.validateIntegerText("abc", "Пробег", true).getErrorMessage())
                .contains("только цифры");
        assertThat(ValidationUtils.validateIntegerText("12abc", "Пробег", true)).isNotNull();
        assertThat(ValidationUtils.validateIntegerText("-5", "Пробег", true)).isNotNull();
        assertThat(ValidationUtils.validateIntegerText("1.5", "Пробег", true)).isNotNull();
        assertThat(ValidationUtils.validateIntegerText("+5", "Пробег", true)).isNotNull();
    }

    @Test
    void validateIntegerTextHandlesEmptyValuesDependingOnRequiredFlag() {
        assertThat(ValidationUtils.validateIntegerText(null, "Пробег", false)).isNull();
        assertThat(ValidationUtils.validateIntegerText("", "Пробег", false)).isNull();
        assertThat(ValidationUtils.validateIntegerText("   ", "Пробег", false)).isNull();
        assertThat(ValidationUtils.validateIntegerText("", "Пробег", true).getErrorMessage())
                .contains("обязательно");
    }

    @Test
    void validateIntegerTextRejectsOverflow() {
        ValidationUtils.ValidationError error =
                ValidationUtils.validateIntegerText("99999999999", "Пробег", true);
        assertThat(error).isNotNull();
        assertThat(error.getErrorMessage()).contains("слишком велико");
    }

    @Test
    void validatePositiveIntegerTextRejectsZeroAndNegativeValues() {
        assertThat(ValidationUtils.validatePositiveIntegerText("1", "Количество")).isNull();
        assertThat(ValidationUtils.validatePositiveIntegerText("10", "Количество")).isNull();
        assertThat(ValidationUtils.validatePositiveIntegerText("0", "Количество").getErrorMessage())
                .contains("больше нуля");
        assertThat(ValidationUtils.validatePositiveIntegerText("-3", "Количество")).isNotNull();
        assertThat(ValidationUtils.validatePositiveIntegerText("abc", "Количество")).isNotNull();
        assertThat(ValidationUtils.validatePositiveIntegerText("", "Количество")).isNotNull();
    }

    @Test
    void validateNonNegativeDecimalTextAcceptsPrices() {
        assertThat(ValidationUtils.validateNonNegativeDecimalText("0", "Цена", true)).isNull();
        assertThat(ValidationUtils.validateNonNegativeDecimalText("1500", "Цена", true)).isNull();
        assertThat(ValidationUtils.validateNonNegativeDecimalText("1500.50", "Цена", true)).isNull();
        assertThat(ValidationUtils.validateNonNegativeDecimalText("1500,50", "Цена", true)).isNull();
    }

    @Test
    void validateNonNegativeDecimalTextRejectsLettersAndNegativeValues() {
        assertThat(ValidationUtils.validateNonNegativeDecimalText("abc", "Цена", true)).isNotNull();
        assertThat(ValidationUtils.validateNonNegativeDecimalText("-1", "Цена", true)).isNotNull();
        assertThat(ValidationUtils.validateNonNegativeDecimalText("10 рублей", "Цена", true)).isNotNull();
        assertThat(ValidationUtils.validateNonNegativeDecimalText("1.2.3", "Цена", true)).isNotNull();
    }

    @Test
    void validateNonNegativeDecimalTextHandlesEmptyValuesDependingOnRequiredFlag() {
        assertThat(ValidationUtils.validateNonNegativeDecimalText(null, "Цена", false)).isNull();
        assertThat(ValidationUtils.validateNonNegativeDecimalText("", "Цена", false)).isNull();
        assertThat(ValidationUtils.validateNonNegativeDecimalText("", "Цена", true)).isNotNull();
    }

    @Test
    void validatePositiveDecimalTextRejectsZeroValue() {
        assertThat(ValidationUtils.validatePositiveDecimalText("10.5", "Цена")).isNull();
        assertThat(ValidationUtils.validatePositiveDecimalText("0", "Цена").getErrorMessage())
                .contains("больше нуля");
        assertThat(ValidationUtils.validatePositiveDecimalText("0.0", "Цена")).isNotNull();
        assertThat(ValidationUtils.validatePositiveDecimalText("abc", "Цена")).isNotNull();
    }

    @Test
    void dialogValidatorCollectsMessagesAndBuildsSingleAlertText() {
        DialogValidator validator = new DialogValidator();

        assertThat(validator.hasErrors()).isFalse();
        assertThat(validator.getMessage()).isEmpty();

        validator.add("Выберите клиента");
        validator.add((String) null);
        validator.add("   ");
        validator.add(ValidationUtils.validateRequiredText("", "Имя"));

        assertThat(validator.hasErrors()).isTrue();
        assertThat(validator.getErrors()).hasSize(2);
        assertThat(validator.getMessage())
                .contains("Выберите клиента")
                .contains("Имя не может быть пустым");
    }

    @Test
    void dialogValidatorCanBeClearedBetweenSaveAttempts() {
        DialogValidator validator = new DialogValidator();
        validator.add("Ошибка");

        validator.clear();

        assertThat(validator.hasErrors()).isFalse();
        assertThat(validator.getMessage()).isEmpty();
    }

    @Test
    void isValidPhoneAcceptsRussianNumber() {
        assertThat(ValidationUtils.isValidPhone("+79001234567")).isTrue();
        assertThat(ValidationUtils.isValidPhone("+79112223344")).isTrue();
        assertThat(ValidationUtils.isValidPhone("+79998887766")).isTrue();
        assertThat(ValidationUtils.isValidPhone("  +79001234567  ")).isTrue();
    }

    @Test
    void isValidPhoneRejectsWrongFormat() {
        assertThat(ValidationUtils.isValidPhone("+89001234567")).isFalse();
        assertThat(ValidationUtils.isValidPhone("9001234567")).isFalse();
        assertThat(ValidationUtils.isValidPhone("+7900123456")).isFalse();
        assertThat(ValidationUtils.isValidPhone("+790012345678")).isFalse();
        assertThat(ValidationUtils.isValidPhone("+79001234567890")).isFalse();
        assertThat(ValidationUtils.isValidPhone("")).isFalse();
        assertThat(ValidationUtils.isValidPhone("   ")).isFalse();
        assertThat(ValidationUtils.isValidPhone(null)).isFalse();
        assertThat(ValidationUtils.isValidPhone("+7abc1234567")).isFalse();
    }

    @Test
    void isValidCarNumberAcceptsRussianPlate() {
        assertThat(ValidationUtils.isValidCarNumber("А123ВС163")).isTrue();
        assertThat(ValidationUtils.isValidCarNumber("В456СЕ16")).isTrue();
        assertThat(ValidationUtils.isValidCarNumber("Е789КХ197")).isTrue();
        assertThat(ValidationUtils.isValidCarNumber("М123НО163")).isTrue();
        assertThat(ValidationUtils.isValidCarNumber("  А123ВС163  ")).isTrue();
    }

    @Test
    void isValidCarNumberRejectsWrongFormat() {
        assertThat(ValidationUtils.isValidCarNumber("А123ВС")).isFalse();
        assertThat(ValidationUtils.isValidCarNumber("А12BC163")).isFalse();
        assertThat(ValidationUtils.isValidCarNumber("I123ВС163")).isFalse();
        assertThat(ValidationUtils.isValidCarNumber("А123ВС1634")).isFalse();
        assertThat(ValidationUtils.isValidCarNumber("А1ВС163")).isFalse();
        assertThat(ValidationUtils.isValidCarNumber("АА123ВС163")).isFalse();
        assertThat(ValidationUtils.isValidCarNumber("АБВГ163")).isFalse();
        assertThat(ValidationUtils.isValidCarNumber("123АВС163")).isFalse();
        assertThat(ValidationUtils.isValidCarNumber("")).isFalse();
        assertThat(ValidationUtils.isValidCarNumber("   ")).isFalse();
        assertThat(ValidationUtils.isValidCarNumber(null)).isFalse();
    }

    @Test
    void isValidCarNumberAllowsOnlyRussianPlateLetters() {
        for (char letter : "АВЕКМНОРСТУХ".toCharArray()) {
            assertThat(ValidationUtils.isValidCarNumber(letter + "123ВС163")).isTrue();
        }

        for (char letter : "БГДЖЗЙ".toCharArray()) {
            assertThat(ValidationUtils.isValidCarNumber(letter + "123ВС163")).isFalse();
        }
    }

    @Test
    void validatePhoneReportsEmptyAndMalformedValues() {
        assertThat(ValidationUtils.validatePhone(null)).isNotNull();
        assertThat(ValidationUtils.validatePhone("   ")).isNotNull();
        assertThat(ValidationUtils.validatePhone("89001234567")).isNotNull();
        assertThat(ValidationUtils.validatePhone("+79001234567")).isNull();
    }

    @Test
    void validateCarNumberReportsEmptyAndMalformedValues() {
        assertThat(ValidationUtils.validateCarNumber(null)).isNotNull();
        assertThat(ValidationUtils.validateCarNumber("   ")).isNotNull();
        assertThat(ValidationUtils.validateCarNumber("А123ВС")).isNotNull();
        assertThat(ValidationUtils.validateCarNumber("А123ВС163")).isNull();
    }
}
