package com.autoservice;

public class Service {
    private String name;
    private double price;
    private int duration;
    private String partNumber;
    private boolean dirty = false;

    // ==================== КОНСТРУКТОРЫ ====================

    public Service(String name, double price) {
        this.name = name;
        this.price = price;
        this.duration = 60;
        this.partNumber = "";
        this.dirty = true;
    }

    public Service(String name, double price, int duration, String partNumber) {
        this.name = name;
        this.price = price;
        this.duration = duration;
        this.partNumber = partNumber;
        this.dirty = true;
    }

    // ==================== ГЕТТЕРЫ ====================

    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getDuration() { return duration; }
    public String getPartNumber() { return partNumber; }
    public boolean isDirty() { return dirty; }

    // ==================== СЕТТЕРЫ ====================

    public void setName(String name) {
        this.name = name;
        this.dirty = true;
    }

    public void setPrice(double price) {
        this.price = price;
        this.dirty = true;
    }

    public void setDuration(int duration) {
        this.duration = duration;
        this.dirty = true;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
        this.dirty = true;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    // ==================== МЕТОДЫ ====================

    public void markClean() {
        this.dirty = false;
    }

    @Override
    public String toString() {
        return name + " (" + price + " руб.)";
    }
}