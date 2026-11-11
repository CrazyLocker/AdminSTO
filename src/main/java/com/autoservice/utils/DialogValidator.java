package com.autoservice.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Собирает ошибки валидации полей диалога, чтобы показать пользователю
 * одно понятное сообщение вместо нескольких последовательных окон.
 */
public class DialogValidator {

    private final List<String> errors = new ArrayList<>();

    /**
     * Добавить сообщение об ошибке.
     * @param message текст ошибки; пустые значения игнорируются
     */
    public void add(String message) {
        if (message != null && !message.trim().isEmpty()) {
            errors.add(message.trim());
        }
    }

    /**
     * Добавить ошибку валидации, если она есть.
     * @param error ошибка валидации или {@code null}
     */
    public void add(ValidationUtils.ValidationError error) {
        if (error != null) {
            add(error.getErrorMessage());
        }
    }

    /**
     * Проверить, были ли найдены ошибки.
     * @return {@code true}, если есть хотя бы одна ошибка
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Получить список сообщений об ошибках.
     * @return неизменяемый список ошибок в порядке добавления
     */
    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * Сформировать одно сообщение для Alert.
     * @return сообщения об ошибках, разделённые переводом строки
     */
    public String getMessage() {
        return String.join("\n", errors);
    }

    /**
     * Убрать все собранные ошибки.
     */
    public void clear() {
        errors.clear();
    }
}
