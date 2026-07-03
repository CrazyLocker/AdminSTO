package com.autoservice.model;

/**
 * Связь между моделью авто и расходником для ТО.
 * Используется для хранения связей в таблице to_parts.
 */
public class ToPart {
    private int id;
    private String carModel;
    private int sparePartId;
    private int quantity;
    private String unitType;
    private boolean active;

    public ToPart() {
        this.id = -1;
        this.carModel = "";
        this.sparePartId = 0;
        this.quantity = 1;
        this.unitType = "шт";
        this.active = true;
    }

    public ToPart(String carModel, int sparePartId, int quantity, String unitType) {
        this.id = -1;
        this.carModel = carModel;
        this.sparePartId = sparePartId;
        this.quantity = quantity;
        this.unitType = unitType;
        this.active = true;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCarModel() {
        return carModel;
    }

    public void setCarModel(String carModel) {
        this.carModel = carModel;
    }

    public int getSparePartId() {
        return sparePartId;
    }

    public void setSparePartId(int sparePartId) {
        this.sparePartId = sparePartId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getUnitType() {
        return unitType;
    }

    public void setUnitType(String unitType) {
        this.unitType = unitType;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    private boolean isDirty = false;

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }

    @Override
    public String toString() {
        return "ToPart{" +
                "id=" + id +
                ", carModel='" + carModel + '\'' +
                ", sparePartId=" + sparePartId +
                ", quantity=" + quantity +
                ", unitType='" + unitType + '\'' +
                ", active=" + active +
                '}';
    }
}
