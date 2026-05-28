package com.autoservice;

import java.util.ArrayList;
import java.util.List;

public class SparePart {
    private int id;
    private int categoryId;
    private String name;
    private String partNumber;
    private String manufacturer;
    private String compatibleModels;  // строка с моделями через запятую
    private double purchasePrice;
    private double retailPrice;
    private int stock;
    private int minStock;
    private String location;

    public SparePart(String name, double purchasePrice, double retailPrice, int stock) {
        this.id = -1;
        this.categoryId = 0;
        this.name = name;
        this.partNumber = "";
        this.manufacturer = "";
        this.compatibleModels = "";
        this.purchasePrice = purchasePrice;
        this.retailPrice = retailPrice;
        this.stock = stock;
        this.minStock = 0;
        this.location = "";
    }

    public SparePart(int id, int categoryId, String name, String partNumber,
                     String manufacturer, String compatibleModels,
                     double purchasePrice, double retailPrice,
                     int stock, int minStock, String location) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.partNumber = partNumber;
        this.manufacturer = manufacturer;
        this.compatibleModels = compatibleModels;
        this.purchasePrice = purchasePrice;
        this.retailPrice = retailPrice;
        this.stock = stock;
        this.minStock = minStock;
        this.location = location;
    }

    // Геттеры и сеттеры
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPartNumber() { return partNumber; }
    public void setPartNumber(String partNumber) { this.partNumber = partNumber; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public String getCompatibleModels() { return compatibleModels; }
    public void setCompatibleModels(String compatibleModels) { this.compatibleModels = compatibleModels; }
    public double getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(double purchasePrice) { this.purchasePrice = purchasePrice; }
    public double getRetailPrice() { return retailPrice; }
    public void setRetailPrice(double retailPrice) { this.retailPrice = retailPrice; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public int getMinStock() { return minStock; }
    public void setMinStock(int minStock) { this.minStock = minStock; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    @Override
    public String toString() {
        return name + " (" + partNumber + ") - " + retailPrice + " руб.";
    }
}