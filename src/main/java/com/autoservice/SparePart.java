package com.autoservice;

public class SparePart {
    private int id;
    private int orderId;
    private String name;
    private String partNumber;
    private String manufacturer;
    private String compatibleModels;
    private double purchasePrice;
    private double retailPrice;
    private int stock;
    private int minStock;
    private String location;
    private boolean dirty = false;

    // ==================== КОНСТРУКТОРЫ ====================

    public SparePart(int id, int orderId, String name, String partNumber, String manufacturer,
                     String compatibleModels, double purchasePrice, double retailPrice,
                     int stock, int minStock, String location) {
        this.id = id;
        this.orderId = orderId;
        this.name = name;
        this.partNumber = partNumber;
        this.manufacturer = manufacturer;
        this.compatibleModels = compatibleModels;
        this.purchasePrice = purchasePrice;
        this.retailPrice = retailPrice;
        this.stock = stock;
        this.minStock = minStock;
        this.location = location;
        this.dirty = false;
    }

    public SparePart(String name, double purchasePrice, double retailPrice, int stock) {
        this(-1, 0, name, "", "", "", purchasePrice, retailPrice, stock, 0, "");
    }

    // ==================== ГЕТТЕРЫ ====================

    public int getId() { return id; }
    public int getOrderId() { return orderId; }
    public String getName() { return name; }
    public String getPartNumber() { return partNumber; }
    public String getManufacturer() { return manufacturer; }
    public String getCompatibleModels() { return compatibleModels; }
    public double getPurchasePrice() { return purchasePrice; }
    public double getRetailPrice() { return retailPrice; }
    public int getStock() { return stock; }
    public int getMinStock() { return minStock; }
    public String getLocation() { return location; }
    public boolean isDirty() { return dirty; }

    // ==================== СЕТТЕРЫ ====================

    public void setId(int id) {
        this.id = id;
        this.dirty = true;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
        this.dirty = true;
    }

    public void setName(String name) {
        this.name = name;
        this.dirty = true;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
        this.dirty = true;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
        this.dirty = true;
    }

    public void setCompatibleModels(String compatibleModels) {
        this.compatibleModels = compatibleModels;
        this.dirty = true;
    }

    public void setPurchasePrice(double purchasePrice) {
        this.purchasePrice = purchasePrice;
        this.dirty = true;
    }

    public void setRetailPrice(double retailPrice) {
        this.retailPrice = retailPrice;
        this.dirty = true;
    }

    public void setStock(int stock) {
        if (this.stock != stock) {
            this.stock = stock;
            this.dirty = true;
        }
    }

    public void setMinStock(int minStock) {
        this.minStock = minStock;
        this.dirty = true;
    }

    public void setLocation(String location) {
        this.location = location;
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
        return name + " (остаток: " + stock + ")";
    }
}