package com.autoservice;

public class SparePart {
    private int id;
    private int orderId;
    private String name;
    private String partNumber;
    private String manufacturer;
    private String compatibleModels;
    private String note;
    private double purchasePrice;
    private double retailPrice;

    // ====== ПОЛЯ ДЛЯ ГИБРИДНОГО УЧЁТА ======
    private double stock;              // физический остаток
    private double reserved;           // зарезервировано под заказы
    private double minStock;           // минимальный остаток
    private String unitType;           // "л", "шт", "компл"

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
        this.note = "";
        this.purchasePrice = 0;
        this.retailPrice = 0;
        this.stock = 0;
        this.reserved = 0;
        this.minStock = 0;
        this.unitType = "шт";
        this.location = "";
        this.dirty = true;
    }

    // Полный конструктор
    public SparePart(int id, int orderId, String name, String partNumber, String manufacturer,
                     String compatibleModels, String note, double purchasePrice, double retailPrice,
                     double stock, double minStock, String unitType, String location) {
        this.id = id;
        this.orderId = orderId;
        this.name = name;
        this.partNumber = partNumber;
        this.manufacturer = manufacturer;
        this.compatibleModels = compatibleModels;
        this.note = note;
        this.purchasePrice = purchasePrice;
        this.retailPrice = retailPrice;
        this.stock = stock;
        this.reserved = 0;
        this.minStock = minStock;
        this.unitType = unitType;
        this.location = location;
        this.dirty = false;
    }

    // Конструктор для обратной совместимости (int stock)
    public SparePart(int id, int orderId, String name, String partNumber, String manufacturer,
                     String compatibleModels, double purchasePrice, double retailPrice,
                     int stock, int minStock, String location) {
        this(id, orderId, name, partNumber, manufacturer, compatibleModels, "",
                purchasePrice, retailPrice, (double) stock, (double) minStock,
                "шт", location);
    }

    // Конструктор для быстрого создания (запчасть в заказе)
    public SparePart(String name, double purchasePrice, double retailPrice, int stock) {
        this(-1, 0, name, "", "", "", "", purchasePrice, retailPrice,
                (double) stock, 0, "шт", "");
    }

    // ==================== ГЕТТЕРЫ ====================

    public int getId() { return id; }
    public int getOrderId() { return orderId; }
    public String getName() { return name; }
    public String getPartNumber() { return partNumber; }
    public String getManufacturer() { return manufacturer; }
    public String getCompatibleModels() { return compatibleModels; }
    public String getNote() { return note; }
    public double getPurchasePrice() { return purchasePrice; }
    public double getRetailPrice() { return retailPrice; }
    public double getStock() { return stock; }
    public double getReserved() { return reserved; }
    public double getAvailableStock() { return stock - reserved; }
    public double getMinStock() { return minStock; }
    public String getUnitType() { return unitType; }
    public String getLocation() { return location; }
    public boolean isDirty() { return dirty; }

    // ==================== СЕТТЕРЫ ====================

    public void setId(int id) { this.id = id; this.dirty = true; }
    public void setOrderId(int orderId) { this.orderId = orderId; this.dirty = true; }
    public void setName(String name) { this.name = name; this.dirty = true; }
    public void setPartNumber(String partNumber) { this.partNumber = partNumber; this.dirty = true; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; this.dirty = true; }
    public void setCompatibleModels(String compatibleModels) { this.compatibleModels = compatibleModels; this.dirty = true; }
    public void setNote(String note) { this.note = note; this.dirty = true; }
    public void setPurchasePrice(double purchasePrice) { this.purchasePrice = purchasePrice; this.dirty = true; }
    public void setRetailPrice(double retailPrice) { this.retailPrice = retailPrice; this.dirty = true; }

    public void setStock(double stock) {
        if (this.stock != stock) {
            this.stock = stock;
            this.dirty = true;
        }
    }

    public void setReserved(double reserved) {
        if (this.reserved != reserved) {
            this.reserved = reserved;
            this.dirty = true;
        }
    }

    public void setMinStock(double minStock) { this.minStock = minStock; this.dirty = true; }
    public void setUnitType(String unitType) { this.unitType = unitType; this.dirty = true; }
    public void setLocation(String location) { this.location = location; this.dirty = true; }
    public void setDirty(boolean dirty) { this.dirty = dirty; }

    // ==================== МЕТОДЫ ====================

    /**
     * Резервирует указанное количество запчастей.
     * @param qty количество для резервирования
     * @return true, если успешно зарезервировано
     */
    public boolean reserve(double qty) {
        if (qty <= 0) return true;
        if (this.getAvailableStock() < qty) return false;
        this.reserved += qty;
        this.dirty = true;
        return true;
    }

    /**
     * Снимает резерв с указанного количества запчастей.
     * @param qty количество для снятия резерва
     */
    public void unreserve(double qty) {
        if (qty <= 0) return;
        this.reserved = Math.max(0, this.reserved - qty);
        this.dirty = true;
    }

    /**
     * Списывает запчасти со склада (уменьшает stock и reserved).
     * @param qty количество для списания
     * @return true, если успешно списано
     */
    public boolean deduct(double qty) {
        if (qty <= 0) return true;
        if (this.stock < qty) return false;
        this.stock -= qty;
        this.unreserve(qty); // Снимаем резерв при списании
        this.dirty = true;
        return true;
    }

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
        return this.getAvailableStock() < this.minStock;
    }

    public String getStockFormatted() {
        return String.format("%.0f %s", stock, unitType);
    }

    public void markClean() { this.dirty = false; }

    @Override
    public String toString() {
        return name + " — " + String.format("%.0f", stock) + " " + unitType + (note != null && !note.isEmpty() ? " (" + note + ")" : "");
    }
}