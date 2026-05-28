package com.autoservice;

public class Service {
    private int id;
    private int categoryId;
    private String name;
    private double price;
    private int duration;
    private String partNumber;

    public Service(String name, double price) {
        this.id = -1;
        this.categoryId = 0;
        this.name = name;
        this.price = price;
        this.duration = 60;
        this.partNumber = "";
    }

    public Service(int id, int categoryId, String name, double price, int duration, String partNumber) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.price = price;
        this.duration = duration;
        this.partNumber = partNumber;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public String getPartNumber() { return partNumber; }
    public void setPartNumber(String partNumber) { this.partNumber = partNumber; }

    @Override
    public String toString() {
        return name + " (" + price + " руб., " + duration + " мин.)";
    }
}