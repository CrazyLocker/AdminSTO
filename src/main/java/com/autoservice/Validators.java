package com.autoservice;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.Region;

import java.time.Year;
import java.util.Map;
import java.util.regex.Pattern;

public class Validators {

    // Допустимые буквы для российского госномера
    private static final String ALLOWED_LETTERS = "АВЕКМНОРСТУХ";
    
    // Паттерн для кириллицы
    private static final Pattern CYRILLIC_PATTERN = Pattern.compile("^[\\u0400-\\u04FF]+$");
    
    // Паттерн для имени/фамилии (кириллица, пробел, дефис, апостроф)
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\u0400-\\u04FF\\s'-]+$");
    
    // Паттерн для email
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    
    // Допустимые буквы в VIN (без I, O, Q)
    private static final String VIN_ALLOWED_CHARS = "ABCDEFGHJKLMNPRSTUVWXYZ0123456789";
    
    // Паттерн госномера: А000АА00 или А000АА000
    private static final Pattern CAR_NUMBER_PATTERN = Pattern.compile("^[" + ALLOWED_LETTERS + "]\\d{3}[" + ALLOWED_LETTERS + "]{2}\\d{2,3}$");

    /**
     * Настройка поля телефона с маской +7 (не редактируемая)
     * Разрешает ввод только 10 цифр после +7
     * Спецификация: +7XXXXXXXXXX (11 символов всего)
     * REQ-VD-009: Префикс +7 не редактируется, ввод только цифр
     * REQ-VD-010: Запрет любых символов кроме цифр
     */
    public static void setupPhoneField(TextField phoneField) {
        phoneField.setPromptText("+7 (900) 123-45-67");
        
        // Если поле пустое — инициализируем нулевым номером
        if (phoneField.getText() == null || phoneField.getText().trim().isEmpty()) {
            phoneField.setText("+7");
        }
        
        // Создаем форматтер для телефона
        TextFormatter<String> formatter = new TextFormatter<>(change -> {
            String text = change.getText();
            
            // Разрешаем удаление любых символов
            if (text.isEmpty()) {
                return change;
            }
            
            // REQ-VD-010: Запрет любых символов кроме цифр
            if (!text.matches("[0-9]+")) {
                return null;
            }
            
            // Получаем текст ДО изменения
            String oldText = change.getControlText();
            
            // REQ-VD-009: Блокируем редактирование префикса +7
            // Если пользователь пытается вставить текст в начало (включая удаление +7)
            if (change.getRangeStart() < 2) {
                return null;
            }
            
            // Формируем новый текст
            String newText = oldText.substring(0, change.getRangeStart()) + text + oldText.substring(change.getRangeEnd());
            
            // Максимум 12 символов (+7 + 10 цифр)
            if (newText.length() > 12) {
                return null;
            }
            
            return change;
        });
        
        phoneField.setTextFormatter(formatter);
        
        // Дополнительная защита от вставки нецифровых символов через контекстное меню
        phoneField.setOnKeyTyped(e -> {
            char c = e.getCharacter().charAt(0);
            if (!Character.isDigit(c)) {
                e.consume();
            }
        });
        
        // Защита от вставки через Ctrl+V
        phoneField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\+?7\\d{10}")) {
                phoneField.setText(oldVal);
            }
        });
    }

    /**
     * Настройка поля госномера (только русские буквы и цифры, верхний регистр)
     * Запрещает латиницу и специальные символы
     */
    public static void setupCarNumberField(TextField carNumberField) {
        carNumberField.setPromptText("А000АА00");
        
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
     * Настройка поля имени (только кириллица, пробел, дефис, апостроф)
     */
    public static void setupNameField(TextField nameField) {
        TextFormatter<String> formatter = new TextFormatter<>(change -> {
            String text = change.getText();
            
            // Разрешаем удаление
            if (text.isEmpty()) {
                return change;
            }
            
            // Разрешаем кириллицу, пробел, дефис, апостроф
            if (text.matches("^[\\u0400-\\u04FF\\s'-]+$")) {
                return change;
            }
            
            return null; // Отклоняем изменение
        });
        
        nameField.setTextFormatter(formatter);
    }

    /**
     * Настройка поля фамилии (только кириллица, пробел, дефис, апостроф)
     */
    public static void setupLastNameField(TextField lastNameField) {
        TextFormatter<String> formatter = new TextFormatter<>(change -> {
            String text = change.getText();
            
            // Разрешаем удаление
            if (text.isEmpty()) {
                return change;
            }
            
            // Разрешаем кириллицу, пробел, дефис, апостроф
            if (text.matches("^[\\u0400-\\u04FF\\s'-]+$")) {
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
        return CAR_NUMBER_PATTERN.matcher(number).matches();
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
     * Форматирование телефона для отображения в UI
     * Преобразует "+7XXXXXXXXXX" в "+7 (XXX) XXX-XX-XX"
     * Пример: "+79001234567" -> "+7 (900) 123-45-67"
     * REQ-VD-011
     */
    public static String formatPhoneForDisplay(String phone) {
        if (phone == null || phone.isEmpty()) {
            return "";
        }
        
        // Извлекаем только цифры
        String digits = phone.replaceAll("[^0-9]", "");
        
        // Если начинается с 8, заменяем на 7
        if (digits.startsWith("8") && digits.length() == 11) {
            digits = "7" + digits.substring(1);
        }
        
        // Проверяем что это российский номер
        if (!digits.startsWith("7") || digits.length() < 11) {
            return phone; // Возвращаем как есть если не совпадает формат
        }
        
        // Форматируем: +7 (XXX) XXX-XX-XX
        String result = "+7 (" + digits.substring(1, 4) + ") " 
                      + digits.substring(4, 7) + "-" 
                      + digits.substring(7, 9) + "-" 
                      + digits.substring(9, 11);
        
        return result;
    }

    /**
     * Приведение госномера к стандартному виду (верхний регистр)
     * Удаляет пробелы, дефисы и другие спецсимволы
     */
    public static String normalizeCarNumber(String number) {
        if (number == null) return "";
        return number.toUpperCase().replaceAll("[\\s-]", "").replaceAll("[^АВЕКМНОРСТУХ0-9]", "");
    }

    // ==================== НОВЫЕ МЕТОДЫ ВАЛИДАЦИИ ====================

    /**
     * Проверка на непустую строку (не null и не пустая)
     */
    public static boolean isNotBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * Проверка на положительное число (> 0)
     */
    public static boolean isPositiveNumber(double value) {
        return value > 0;
    }

    /**
     * Проверка на неотрицательное число (>= 0)
     */
    public static boolean isNonNegativeNumber(double value) {
        return value >= 0;
    }

    /**
     * Проверка email (формат: user@domain.ru)
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Проверка пробега (неотрицательное целое число)
     */
    public static boolean isValidMileage(String mileage) {
        if (mileage == null || mileage.trim().isEmpty()) return false;
        try {
            int value = Integer.parseInt(mileage.trim());
            return value >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Проверка цены (положительное число)
     */
    public static boolean isValidPrice(String price) {
        if (price == null || price.trim().isEmpty()) return false;
        try {
            double value = Double.parseDouble(price.trim());
            return value > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Проверка года (от 1900 до текущего года)
     */
    public static boolean isValidYear(String year) {
        if (year == null || year.trim().isEmpty()) return false;
        try {
            int value = Integer.parseInt(year.trim());
            int currentYear = Year.now().getValue();
            return value >= 1900 && value <= currentYear;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Проверка VIN (17 символов, без I, O, Q)
     */
    public static boolean isValidVIN(String vin) {
        if (vin == null || vin.length() != 17) return false;
        String upper = vin.toUpperCase();
        for (char c : upper.toCharArray()) {
            if (VIN_ALLOWED_CHARS.indexOf(c) < 0) {
                return false;
            }
        }
        return true;
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    /**
     * Подсветка ошибки на поле
     */
    public static void showError(TextField control, String message) {
        if (control != null) {
            control.setStyle("-fx-border-color: red; -fx-border-width: 2;");
            control.setPromptText(message);
        }
    }

    /**
     * Очистка подсветки ошибки
     */
    public static void clearError(TextField control) {
        if (control != null) {
            control.setStyle("");
        }
    }

    /**
     * Очистка всех ошибок в форме
     */
    public static void clearAllErrors(Region root) {
        if (root == null) return;
        root.lookupAll(".text-input").forEach(node -> {
            if (node instanceof TextField) {
                ((TextField) node).setStyle("");
            }
        });
    }

    // ==================== МАССОВАЯ ПРОВЕРКА ====================

    /**
     * Массовая проверка всех полей формы
     * @param validations карта: поле -> сообщение об ошибке
     * @return true если все поля валидны, false если есть ошибки
     */
    public static boolean validateAll(Map<TextField, String> validations) {
        boolean allValid = true;
        
        for (Map.Entry<TextField, String> entry : validations.entrySet()) {
            TextField field = entry.getKey();
            String errorMessage = entry.getValue();
            
            if (field == null) {
                continue;
            }
            
            String text = field.getText();
            
            if (text == null || text.trim().isEmpty()) {
                showError(field, errorMessage);
                allValid = false;
            } else {
                clearError(field);
            }
        }
        
        return allValid;
    }
}
