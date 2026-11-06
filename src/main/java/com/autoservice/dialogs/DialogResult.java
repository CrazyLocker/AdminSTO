package com.autoservice.dialogs;

/**
 * Результат взаимодействия с модальным диалогом.
 * Используется для возврата данных из диалогов, работающих по async-паттерну.
 */
public class DialogResult {

    /** Действие, выполненное пользователем. */
    public enum Action {
        /** Пользователь подтвердил действие. */
        OK,
        /** Пользователь отменил действие. */
        CANCEL,
        /** При выполнении действия произошла ошибка. */
        ERROR
    }

    private final Action action;
    private final java.util.Map<String, Object> data;

    /**
     * Создать результат без дополнительных данных.
     * @param action действие, выполненное пользователем
     */
    public DialogResult(Action action) {
        this.action = action;
        this.data = new java.util.HashMap<>();
    }

    /**
     * Создать результат с дополнительными данными.
     * @param action действие, выполненное пользователем
     * @param data данные диалога; при {@code null} используется пустая карта
     */
    public DialogResult(Action action, java.util.Map<String, Object> data) {
        this.action = action;
        this.data = data != null ? data : new java.util.HashMap<>();
    }

    /**
     * Проверить, что пользователь подтвердил действие (нажал «Сохранить» или «Создать»).
     * @return {@code true}, если действие — {@link Action#OK}
     */
    public boolean isOK() {
        return action == Action.OK;
    }

    /**
     * Проверить, что пользователь отменил действие (нажал «Отмена» или закрыл диалог).
     * @return {@code true}, если действие — {@link Action#CANCEL}
     */
    public boolean isCancel() {
        return action == Action.CANCEL;
    }

    /**
     * Получить данные диалога по ключу с приведением к заданному типу.
     * @param key ключ данных
     * @param type ожидаемый тип значения
     * @param <T> тип возвращаемого значения
     * @return значение указанного типа или {@code null}, если ключ отсутствует
     *         или значение имеет другой тип
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String key, Class<T> type) {
        Object value = data.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * Получить действие, выполненное пользователем.
     * @return действие диалога
     */
    public Action getAction() {
        return action;
    }

    /**
     * Получить все данные диалога.
     * @return карта данных диалога
     */
    public java.util.Map<String, Object> getData() {
        return data;
    }
}
