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
}
