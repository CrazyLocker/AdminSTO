package com.autoservice.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Утилита для валидации данных.
 * Проверяет обязательные поля, форматы и допустимые значения.
 */
public class ValidationUtils {
    
    // Паттерны для валидации
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+7\\d{10}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PLATE_PATTERN = Pattern.compile("^[АВЕКМНОРСТУХ]\\d{3}[АВЕКМНОРСТУХ]{2}\\d{2,3}$");
    
    /**
     * Класс для представления ошибки валидации.
     */
    public static class ValidationError {
        private final String fieldName;
        private final String errorMessage;
        private final String fieldId;
        
        public ValidationError(String fieldName, String errorMessage) {
            this.fieldName = fieldName;
            this.errorMessage = errorMessage;
            this.fieldId = null;
        }
        
        public ValidationError(String fieldName, String errorMessage, String fieldId) {
            this.fieldName = fieldName;
            this.errorMessage = errorMessage;
            this.fieldId = fieldId;
        }
        
        public String getFieldName() { return fieldName; }
        public String getErrorMessage() { return errorMessage; }
        public String getFieldId() { return fieldId; }
    }
    
    /**
     * Проверить, что строка не пустая и не состоит только из пробелов.
     * @param value проверяемое значение
     * @param fieldName название поля для сообщения об ошибке
     * @return true если валидно
     */
    public static boolean isNotBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        return true;
    }
    
    /**
     * Проверить целое число на положительное значение.
     * @param value проверяемое значение
     * @param fieldName название поля
     * @return true если value > 0
     */
    public static boolean isPositiveInteger(Integer value, String fieldName) {
        if (value == null) {
            return false;
        }
        return value > 0;
    }
    
    /**
     * Проверить целое число на неотрицательное значение.
     * @param value проверяемое значение
     * @param fieldName название поля
     * @return true если value >= 0
     */
    public static boolean isNonNegativeInteger(Integer value, String fieldName) {
        if (value == null) {
            return false;
        }
        return value >= 0;
    }
    
    /**
     * Проверить вещественное число на неотрицательное значение.
     * @param value проверяемое значение
     * @param fieldName название поля
     * @return true если value >= 0
     */
    public static boolean isNonNegativeDouble(Double value, String fieldName) {
        if (value == null) {
            return false;
        }
        return value >= 0;
    }
    
    /**
     * Проверить номер телефона.
     * @param phone номер телефона
     * @return true если валиден (формат: +7XXXXXXXXXX)
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        return phone.trim().matches("^\\+7\\d{10}$");
    }
    
    /**
     * Проверить email.
     * @param email email адрес
     * @return true если валиден
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }
    
    /**
     * Проверить номер автомобиля (формат для РФ).
     * @param number номер автомобиля
     * @return true если валиден (формат: А123ВС777 или А123ВС163)
     */
    public static boolean isValidCarNumber(String number) {
        if (number == null || number.trim().isEmpty()) {
            return false;
        }
        return number.trim().matches("^[АВЕКМНОРСТУХ]\\d{3}[АВЕКМНОРСТУХ]{2}\\d{2,3}$");
    }
    
    /**
     * Проверить список значений на допустимое значение.
     * @param value проверяемое значение
     * @param validValues допустимые значения
     * @param fieldName название поля
     * @return true если значение в списке допустимых
     */
    public static boolean isValidEnum(String value, List<String> validValues, String fieldName) {
        if (value == null) {
            return false;
        }
        return validValues.contains(value);
    }
    
    /**
     * Валидировать обязательное текстовое поле.
     * @param value значение
     * @param fieldName название поля
     * @return ValidationError если невалидно, null если валидно
     */
    public static ValidationError validateRequiredText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            return new ValidationError(fieldName, fieldName + " не может быть пустым");
        }
        return null;
    }
    
    /**
     * Валидировать текстовое поле с максимальной длиной.
     * @param value значение
     * @param fieldName название поля
     * @param maxLength максимальная длина
     * @return ValidationError если невалидно, null если валидно
     */
    public static ValidationError validateTextWithMaxLength(String value, String fieldName, int maxLength) {
        ValidationError error = validateRequiredText(value, fieldName);
        if (error != null) {
            return error;
        }
        if (value.length() > maxLength) {
            return new ValidationError(fieldName, fieldName + " не может быть длиннее " + maxLength + " символов");
        }
        return null;
    }
    
    /**
     * Валидировать положительное целое число.
     * @param value значение
     * @param fieldName название поля
     * @return ValidationError если невалидно, null если валидно
     */
    public static ValidationError validatePositiveInteger(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            return new ValidationError(fieldName, fieldName + " должно быть положительным числом");
        }
        return null;
    }
    
    /**
     * Валидировать неотрицательное целое число.
     * @param value значение
     * @param fieldName название поля
     * @return ValidationError если невалидно, null если валидно
     */
    public static ValidationError validateNonNegativeInteger(Integer value, String fieldName) {
        if (value == null || value < 0) {
            return new ValidationError(fieldName, fieldName + " должно быть неотрицательным числом");
        }
        return null;
    }
    
    /**
     * Валидировать положительное вещественное число.
     * @param value значение
     * @param fieldName название поля
     * @return ValidationError если невалидно, null если валидно
     */
    public static ValidationError validatePositiveDouble(Double value, String fieldName) {
        if (value == null || value <= 0) {
            return new ValidationError(fieldName, fieldName + " должно быть положительным числом");
        }
        return null;
    }
    
    /**
     * Валидировать неотрицательное вещественное число.
     * @param value значение
     * @param fieldName название поля
     * @return ValidationError если невалидно, null если валидно
     */
    public static ValidationError validateNonNegativeDouble(Double value, String fieldName) {
        if (value == null || value < 0) {
            return new ValidationError(fieldName, fieldName + " должно быть неотрицательным числом");
        }
        return null;
    }
    
    /**
     * Валидировать номер телефона.
     * @param phone номер телефона
     * @return ValidationError если невалидно, null если валидно
     */
    public static ValidationError validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return new ValidationError("Телефон", "Телефон не может быть пустым");
        }
        if (!isValidPhone(phone.trim())) {
            return new ValidationError("Телефон", "Неверный формат телефона (должен содержать 10-15 цифр)");
        }
        return null;
    }
    
    /**
     * Валидировать email.
     * @param email email адрес
     * @return ValidationError если невалидно, null если валидно
     */
    public static ValidationError validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null; // email не обязателен
        }
        if (!isValidEmail(email.trim())) {
            return new ValidationError("Email", "Неверный формат email");
        }
        return null;
    }
    
    /**
     * Валидировать номер автомобиля.
     * @param number номер автомобиля
     * @return ValidationError если невалидно, null если валидно
     */
    public static ValidationError validateCarNumber(String number) {
        if (number == null || number.trim().isEmpty()) {
            return new ValidationError("Номер авто", "Номер автомобиля не может быть пустым");
        }
        if (!isValidCarNumber(number.trim())) {
            return new ValidationError("Номер авто", "Неверный формат номера (например, А123ВС777)");
        }
        return null;
    }
    
    /**
     * Валидировать единицу измерения.
     * @param unitType единица измерения
     * @return ValidationError если невалидно, null если валидно
     */
    public static ValidationError validateUnitType(String unitType) {
        List<String> validUnits = List.of("шт", "л", "компл");
        if (!validUnits.contains(unitType)) {
            return new ValidationError("Ед. изм", "Единица измерения должна быть: шт, л или компл");
        }
        return null;
    }
}
