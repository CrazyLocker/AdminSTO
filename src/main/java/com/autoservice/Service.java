package com.autoservice;

public class Service {
    private int id;
    private String name;
    private double price;
    private int duration;
    private String partNumber;

    // ====== ПОЛЯ ДЛЯ ГИБРИДНОГО УЧЁТА ======
    private double oilVolume;          // объём масла для замены (в литрах)
    private boolean usesOil;           // true — расходует масло
    private String sparePartName;      // какая запчасть расходуется (фильтр)
    private int sparePartQuantity;     // сколько штук

    private boolean dirty = false;

    // ==================== КОНСТРУКТОРЫ ====================

    public Service() {
        this.id = -1;
        this.name = "";
        this.price = 0;
        this.duration = 60;
        this.partNumber = "";
        this.oilVolume = 0;
        this.usesOil = false;
        this.sparePartName = "";
        this.sparePartQuantity = 0;
        this.dirty = true;
    }

    public Service(String name, double price) {
        this();
        this.name = name;
        this.price = price;
    }

    public Service(String name, double price, int duration, String partNumber) {
        this(name, price);
        this.duration = duration;
        this.partNumber = partNumber;
    }

    // ==================== ГЕТТЕРЫ ====================

    public int getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getDuration() { return duration; }
    public String getPartNumber() { return partNumber; }
    public double getOilVolume() { return oilVolume; }
    public boolean isUsesOil() { return usesOil; }
    public String getSparePartName() { return sparePartName; }
    public int getSparePartQuantity() { return sparePartQuantity; }
    public boolean isDirty() { return dirty; }

    // ==================== СЕТТЕРЫ ====================

    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; this.dirty = true; }
    public void setPrice(double price) { this.price = price; this.dirty = true; }
    public void setDuration(int duration) { this.duration = duration; this.dirty = true; }
    public void setPartNumber(String partNumber) { this.partNumber = partNumber; this.dirty = true; }
    public void setOilVolume(double oilVolume) { this.oilVolume = oilVolume; this.dirty = true; }
    public void setUsesOil(boolean usesOil) { this.usesOil = usesOil; this.dirty = true; }
    public void setSparePartName(String sparePartName) { this.sparePartName = sparePartName; this.dirty = true; }
    public void setSparePartQuantity(int sparePartQuantity) { this.sparePartQuantity = sparePartQuantity; this.dirty = true; }
    public void setDirty(boolean dirty) { this.dirty = dirty; }

    public void markClean() { this.dirty = false; }

    @Override
    public String toString() {
        return name + " (" + price + " руб.)";
    }
}