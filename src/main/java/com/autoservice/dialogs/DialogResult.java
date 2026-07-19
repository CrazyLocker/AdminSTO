package com.autoservice.dialogs;

/**
 * Результат взаимодействия с модальным диалогом.
 * Используется для возврата данных из диалогов, работающих по async-паттерну.
 */
public class DialogResult {

    /** Действие, выполненное пользователем */
    public enum Action { OK, CANCEL, ERROR }

    private final Action action;
    private final java.util.Map<String, Object> data;

    public DialogResult(Action action) {
        this.action = action;
        this.data = new java.util.HashMap<>();
    }

    public DialogResult(Action action, java.util.Map<String, Object> data) {
        this.action = action;
        this.data = data != null ? data : new java.util.HashMap<>();
    }

    /** Вернул ли пользователь OK (нажал "Сохранить" / "Создать") */
    public boolean isOK() {
        return action == Action.OK;
    }

    /** Вернул ли пользователь CANCEL (нажал "Отмена" / крестик) */
    public boolean isCancel() {
        return action == Action.CANCEL;
    }

    /** Получить данные по ключу */
    @SuppressWarnings("unchecked")
    public <T> T getData(String key, Class<T> type) {
        Object value = data.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    public Action getAction() {
        return action;
    }

    public java.util.Map<String, Object> getData() {
        return data;
    }
}
