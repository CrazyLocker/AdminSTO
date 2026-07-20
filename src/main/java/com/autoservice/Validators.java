package com.autoservice;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.util.regex.Pattern;

public class Validators {

    // Допустимые буквы для российского госномера
    private static final String ALLOWED_LETTERS = "АВЕКМНОРСТУХ";
    
    // Паттерн для кириллицы
    private static final Pattern CYRILLIC_PATTERN = Pattern.compile("^[\\u0400-\\u04FF]+$");

    /**
     * Настройка поля телефона с маской +7 (не редактируемая)
     * Разрешает ввод только 10 цифр после +7
     */
    public static void setupPhoneField(TextField phoneField) {
        // Инициализируем поле +7 если пустое
        if (phoneField.getText().isEmpty()) {
            phoneField.setText("+7");
        }
        
        // Создаем форматтер для телефона
        // +7 - не редактируемая часть (позиция 0-1)
        // Затем только 10 цифр (позиция 2-11)
        TextFormatter<String> formatter = new TextFormatter<>(change -> {
            // Если это удаление или вставка
            String text = change.getText();
            
            // Разрешаем удаление любых символов
            if (text.isEmpty()) {
                return change;
            }
            
            // Разрешаем только цифры
            if (!text.matches("[0-9]+")) {
                return null; // Отклоняем
            }
            
            // Получаем новую текст
            String newText = change.getControlNewText();
            
            // Проверяем длину - максимум 12 символов (+7 + 10 цифр)
            if (newText.length() > 12) {
                return null;
            }
            
            // Проверяем, что первые 2 символа это +7
            if (newText.length() >= 2 && !newText.startsWith("+7")) {
                // Если пользователь попытался удалить +7, восстанавливаем
                if (change.getRangeStart() < 2) {
                    change.setText("+7");
                    change.setRange(0, newText.length());
                    return change;
                }
                return null;
            }
            
            return change;
        });
        
        phoneField.setTextFormatter(formatter);
        
        // Блокируем позицию каретки на +7 при фокусе
        phoneField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                phoneField.positionCaret(2);
            }
        });
    }

    /**
     * Настройка поля госномера (только русские буквы и цифры, верхний регистр)
     * Запрещает латиницу и специальные символы
     */
    public static void setupCarNumberField(TextField carNumberField) {
        // Создаем форматтер для госномера
        TextFormatter<String> formatter = new TextFormatter<>(change -> {
            String text = change.getText();
            
            // Разрешаем удаление
            if (text.isEmpty()) {
                return change;
            }
            
            // Проверяем каждый символ
            String upper = text.toUpperCase();
            StringBuilder filtered = new StringBuilder();
            
            for (char c : upper.toCharArray()) {
                if (ALLOWED_LETTERS.indexOf(c) >= 0 || Character.isDigit(c)) {
                    filtered.append(c);
                }
            }
            
            // Если ничего не прошло фильтра, отклоняем
            if (filtered.length() == 0) {
                return null;
            }
            
            // Ограничиваем длину (максимум 9 символов)
            String result = filtered.toString();
            if (result.length() > 9) {
                result = result.substring(0, 9);
            }
            
            change.setText(result);
            return change;
        });
        
        carNumberField.setTextFormatter(formatter);
    }

    /**
     * Настройка поля имени (только кириллица)
     */
    public static void setupNameField(TextField nameField) {
        TextFormatter<String> formatter = new TextFormatter<>(change -> {
            String text = change.getText();
            
            // Разрешаем удаление
            if (text.isEmpty()) {
                return change;
            }
            
            // Разрешаем только кириллические символы
            if (text.matches("^[\\u0400-\\u04FF]+$")) {
                return change;
            }
            
            return null; // Отклоняем изменение
        });
        
        nameField.setTextFormatter(formatter);
    }

    /**
     * Настройка поля фамилии (только кириллица)
     */
    public static void setupLastNameField(TextField lastNameField) {
        TextFormatter<String> formatter = new TextFormatter<>(change -> {
            String text = change.getText();
            
            // Разрешаем удаление
            if (text.isEmpty()) {
                return change;
            }
            
            // Разрешаем только кириллические символы
            if (text.matches("^[\\u0400-\\u04FF]+$")) {
                return change;
            }
            
            return null; // Отклоняем изменение
        });
        
        lastNameField.setTextFormatter(formatter);
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
