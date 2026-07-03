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

    // ====== ПОЛЯ ДЛЯ ГИБРИДНОГО УЧЁТА ======
    private double stock;              // остаток (в литрах или штуках)
    private double minStock;           // минимальный остаток
    private double unitVolume;         // объём одной единицы (4.0 для масла, 1.0 для фильтров)
    private String unitType;           // "л", "шт", "м", "кг"
    private boolean isLiquid;          // true — жидкость (учёт в литрах), false — штуки

    private String location;
    private boolean dirty = false;

    // ==================== КОНСТРУКТОРЫ ====================

    public SparePart() {
        this.id = -1;
        this.orderId = 0;
        this.name = "";
        this.partNumber = "";
        this.manufacturer = "";
        this.compatibleModels = "";
        this.purchasePrice = 0;
        this.retailPrice = 0;
        this.stock = 0;
        this.minStock = 0;
        this.unitVolume = 1.0;
        this.unitType = "шт";
        this.isLiquid = false;
        this.location = "";
        this.dirty = true;
    }

    // Полный конструктор
    public SparePart(int id, int orderId, String name, String partNumber, String manufacturer,
                     String compatibleModels, double purchasePrice, double retailPrice,
                     double stock, double minStock, double unitVolume,
                     String unitType, boolean isLiquid, String location) {
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
        this.unitVolume = unitVolume;
        this.unitType = unitType;
        this.isLiquid = isLiquid;
        this.location = location;
        this.dirty = false;
    }

    // Конструктор для обратной совместимости (int stock)
    public SparePart(int id, int orderId, String name, String partNumber, String manufacturer,
                     String compatibleModels, double purchasePrice, double retailPrice,
                     int stock, int minStock, String location) {
        this(id, orderId, name, partNumber, manufacturer, compatibleModels,
                purchasePrice, retailPrice, (double) stock, (double) minStock,
                1.0, "шт", false, location);
    }

    // Конструктор для быстрого создания (запчасть в заказе)
    public SparePart(String name, double purchasePrice, double retailPrice, int stock) {
        this(-1, 0, name, "", "", "", purchasePrice, retailPrice,
                (double) stock, 0, 1.0, "шт", false, "");
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
    public double getStock() { return stock; }
    public double getMinStock() { return minStock; }
    public double getUnitVolume() { return unitVolume; }
    public String getUnitType() { return unitType; }
    public boolean isLiquid() { return isLiquid; }
    public String getLocation() { return location; }
    public boolean isDirty() { return dirty; }

    // ==================== СЕТТЕРЫ ====================

    public void setId(int id) { this.id = id; this.dirty = true; }
    public void setOrderId(int orderId) { this.orderId = orderId; this.dirty = true; }
    public void setName(String name) { this.name = name; this.dirty = true; }
    public void setPartNumber(String partNumber) { this.partNumber = partNumber; this.dirty = true; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; this.dirty = true; }
    public void setCompatibleModels(String compatibleModels) { this.compatibleModels = compatibleModels; this.dirty = true; }
    public void setPurchasePrice(double purchasePrice) { this.purchasePrice = purchasePrice; this.dirty = true; }
    public void setRetailPrice(double retailPrice) { this.retailPrice = retailPrice; this.dirty = true; }

    public void setStock(double stock) {
        if (this.stock != stock) {
            this.stock = stock;
            this.dirty = true;
        }
    }

    public void setMinStock(double minStock) { this.minStock = minStock; this.dirty = true; }
    public void setUnitVolume(double unitVolume) { this.unitVolume = unitVolume; this.dirty = true; }
    public void setUnitType(String unitType) { this.unitType = unitType; this.dirty = true; }
    public void setIsLiquid(boolean isLiquid) { this.isLiquid = isLiquid; this.dirty = true; }
    public void setLocation(String location) { this.location = location; this.dirty = true; }
    public void setDirty(boolean dirty) { this.dirty = dirty; }

    // ==================== МЕТОДЫ ====================

    public boolean deductStock(double quantity) {
        if (quantity <= 0) return false;
        if (this.stock < quantity) return false;
        this.stock -= quantity;
        this.dirty = true;
        return true;
    }

    public void addStock(double quantity) {
        if (quantity <= 0) return;
        this.stock += quantity;
        this.dirty = true;
    }

    public boolean needsRestock() {
        return this.stock < this.minStock;
    }

    public int getCansNeeded(double requiredLiters) {
        if (!isLiquid || unitVolume <= 0) return (int) Math.ceil(requiredLiters);
        return (int) Math.ceil(requiredLiters / unitVolume);
    }

    public int getStockInCans() {
        if (!isLiquid || unitVolume <= 0) return (int) stock;
        return (int) (stock / unitVolume);
    }

    public String getStockFormatted() {
        if (isLiquid) {
            return String.format("%.1f %s (%.0f шт.)", stock, unitType, getStockInCans());
        }
        return String.format("%.0f %s", stock, unitType);
    }

    public void markClean() { this.dirty = false; }

    @Override
    public String toString() {
        if (isLiquid) {
            return name + " — " + getStockFormatted();
        }
        return name + " — " + String.format("%.0f", stock) + " " + unitType;
    }
}