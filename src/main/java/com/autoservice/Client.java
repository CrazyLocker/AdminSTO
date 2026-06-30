package com.autoservice;

public class Client {
    private int id;
    private String name;
    private String lastName;
    private String phone;
    private String carModel;
    private String carNumber;
    private String lastRepairDate;
    private boolean dirty = false;

    // ==================== КОНСТРУКТОРЫ ====================

    public Client(int id, String name, String lastName, String phone, String carModel, String carNumber, String lastRepairDate) {
        this.id = id;
        this.name = name;
        this.lastName = lastName;
        this.phone = phone;
        this.carModel = carModel;
        this.carNumber = carNumber;
        this.lastRepairDate = lastRepairDate;
        this.dirty = false;
    }

    public Client(String name, String lastName, String phone, String carModel, String carNumber) {
        this(-1, name, lastName, phone, carModel, carNumber, "");
    }

    public Client(String name, String lastName, String phone, String carModel, String carNumber, String lastRepairDate) {
        this(-1, name, lastName, phone, carModel, carNumber, lastRepairDate);
    }

    // ==================== ГЕТТЕРЫ ====================

    public int getId() { return id; }
    public String getName() { return name; }
    public String getLastName() { return lastName; }
    public String getPhone() { return phone; }
    public String getCarModel() { return carModel; }
    public String getCarNumber() { return carNumber; }
    public String getLastRepairDate() { return lastRepairDate; }
    public boolean isDirty() { return dirty; }

    // ==================== СЕТТЕРЫ ====================

    public void setId(int id) {
        this.id = id;
        this.dirty = true;
    }

    public void setName(String name) {
        this.name = name;
        this.dirty = true;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
        this.dirty = true;
    }

    public void setPhone(String phone) {
        this.phone = phone;
        this.dirty = true;
    }

    public void setCarModel(String carModel) {
        this.carModel = carModel;
        this.dirty = true;
    }

    public void setCarNumber(String carNumber) {
        this.carNumber = carNumber;
        this.dirty = true;
    }

    public void setLastRepairDate(String lastRepairDate) {
        this.lastRepairDate = lastRepairDate;
        this.dirty = true;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    // ==================== МЕТОДЫ ====================

    public String getFullName() {
        return (lastName != null && !lastName.isEmpty()) ? lastName + " " + name : name;
    }

    public void markClean() {
        this.dirty = false;
    }

    @Override
    public String toString() {
        return getFullName() + " (" + carModel + ", " + carNumber + ")";
    }
}