package com.autoservice;

import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Validators {

    // Допустимые буквы для российского госномера
    private static final String ALLOWED_LETTERS = "АВЕКМНОРСТУХ";

    /**
     * Настройка поля телефона с маской +7
     */
    public static void setupPhoneField(TextField phoneField) {
        if (phoneField.getText().isEmpty()) {
            phoneField.setText("+7");
        }

        phoneField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.startsWith("+7")) {
                phoneField.setText("+7");
                return;
            }

            // Удаляем всё кроме + и цифр
            String cleaned = newValue.replaceAll("[^+0-9]", "");
            if (!cleaned.startsWith("+7")) {
                phoneField.setText("+7");
                return;
            }

            // Ограничиваем длину: +7 + 10 цифр = 12 символов
            if (cleaned.length() > 12) {
                cleaned = cleaned.substring(0, 12);
            }

            if (!cleaned.equals(newValue)) {
                phoneField.setText(cleaned);
            }
        });

        // Запрещаем удаление префикса +7
        phoneField.addEventFilter(javafx.scene.input.KeyEvent.KEY_TYPED, event -> {
            int caretPos = phoneField.getCaretPosition();
            if (caretPos < 2 && event.getCharacter().matches("\\p{Print}")) {
                event.consume();
            }
        });
    }

    /**
     * Настройка поля госномера (только русские буквы и цифры, верхний регистр)
     */
    public static void setupCarNumberField(TextField carNumberField) {
        carNumberField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null) return;

            // Переводим в верхний регистр
            String upper = newValue.toUpperCase();

            // Удаляем недопустимые символы
            StringBuilder filtered = new StringBuilder();
            for (char c : upper.toCharArray()) {
                if (ALLOWED_LETTERS.indexOf(c) >= 0 || Character.isDigit(c)) {
                    filtered.append(c);
                }
            }

            String result = filtered.toString();

            // Ограничиваем длину (максимум 9 символов: А123ВС163)
            if (result.length() > 9) {
                result = result.substring(0, 9);
            }

            if (!result.equals(newValue)) {
                carNumberField.setText(result);
            }
        });
    }

    /**
     * Проверка формата госномера
     */
    public static boolean isValidCarNumber(String number) {
        if (number == null || number.isEmpty()) return false;

        // Паттерн: буква + 3 цифры + 2 буквы + 2-3 цифры
        String pattern = "^[" + ALLOWED_LETTERS + "]\\d{3}[" + ALLOWED_LETTERS + "]{2}\\d{2,3}$";
        return number.matches(pattern);
    }

    /**
     * Проверка формата телефона
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || !phone.startsWith("+7")) return false;
        String digits = phone.substring(2);
        return digits.length() == 10 && digits.matches("\\d+");
    }

    /**
     * Очистка телефона от всех символов кроме цифр
     */
    public static String cleanPhone(String phone) {
        if (phone == null) return "+7";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() >= 11) {
            digits = digits.substring(digits.length() - 10);
        }
        return "+7" + digits;
    }

    /**
     * Приведение госномера к стандартному виду (верхний регистр)
     */
    public static String normalizeCarNumber(String number) {
        if (number == null) return "";
        return number.toUpperCase().replaceAll("[^АВЕКМНОРСТУХ0-9]", "");
    }
}