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
    private String note;
    private boolean active;

    public ToPart() {
        this.id = -1;
        this.carModel = "";
        this.sparePartId = 0;
        this.quantity = 1;
        this.unitType = "шт";
        this.note = "";
        this.active = true;
    }

    public ToPart(String carModel, int sparePartId, int quantity, String unitType) {
        this.id = -1;
        this.carModel = carModel;
        this.sparePartId = sparePartId;
        this.quantity = quantity;
        this.unitType = unitType;
        this.note = "";
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
        this.isDirty = true;
    }

    public int getSparePartId() {
        return sparePartId;
    }

    public void setSparePartId(int sparePartId) {
        this.sparePartId = sparePartId;
        this.isDirty = true;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        this.isDirty = true;
    }

    public String getUnitType() {
        return unitType;
    }

    public void setUnitType(String unitType) {
        this.unitType = unitType;
        this.isDirty = true;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
        this.isDirty = true;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        this.isDirty = true;
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
                ", note='" + note + '\'' +
                ", active=" + active +
                '}';
    }
}
