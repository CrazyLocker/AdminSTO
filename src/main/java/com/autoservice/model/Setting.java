package com.autoservice.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

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
        if (!constantTimeEquals(this.key, key)) {
            this.key = key;
            this.isDirty = true;
        }
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        if (!constantTimeEquals(this.value, value)) {
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

    /**
     * Constant-time string comparison to prevent timing attacks (CWE-208).
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        if (aBytes.length != bBytes.length) {
            for (int i = 0; i < Math.min(aBytes.length, bBytes.length); i++) {
                MessageDigest.isEqual(new byte[]{aBytes[i]}, new byte[]{bBytes[i]});
            }
            return false;
        }
        return MessageDigest.isEqual(aBytes, bBytes);
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
