package com.autoservice;

public class SparePart {
    private String name;
    private double purchasePrice;
    private double retailPrice;
    private int stock;

    public SparePart(String name, double purchasePrice, double retailPrice, int stock) {
        this.name = name;
        this.purchasePrice = purchasePrice;
        this.retailPrice = retailPrice;
        this.stock = stock;
    }

    public String getName() { return name; }
    public double getPurchasePrice() { return purchasePrice; }
    public double getRetailPrice() { return retailPrice; }
    public int getStock() { return stock; }

    public void setName(String name) { this.name = name; }
    public void setPurchasePrice(double purchasePrice) { this.purchasePrice = purchasePrice; }
    public void setRetailPrice(double retailPrice) { this.retailPrice = retailPrice; }
    public void setStock(int stock) { this.stock = stock; }

    @Override
    public String toString() {
        return name + " (" + retailPrice + " руб., остаток: " + stock + ")";
    }
}