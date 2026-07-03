package com.autoservice.model;

/**
 * Настройка приложения для хранения в таблице app_settings.
 */
public class Setting {
    private int id;
    private String key;
    private String value;
    private String description;
    private boolean isDirty = false;

    public Setting() {
        this.id = -1;
        this.key = "";
        this.value = "";
        this.description = "";
    }

    public Setting(String key, String value, String description) {
        this.id = -1;
        this.key = key;
        this.value = value;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        if (!this.key.equals(key)) {
            this.key = key;
            this.isDirty = true;
        }
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        if (!this.value.equals(value)) {
            this.value = value;
            this.isDirty = true;
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }

    public void markClean() {
        this.isDirty = false;
    }

    @Override
    public String toString() {
        return "Setting{" +
                "id=" + id +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", description='" + description + '\'' +
                ", isDirty=" + isDirty +
                '}';
    }
}
