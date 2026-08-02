package com.autoservice.views;

import com.autoservice.SparePart;
import com.autoservice.model.ToPart;

/**
 * Класс для отображения одной строки в таблице расходников ТО.
 * Содержит связь между моделью авто и запчастью.
 */
public class ToPartsRow {
    private ToPart toPart;
    private String carModel;
    private String sparePartName;
    private int quantity;
    private String unitType;
    private String note;

    public ToPartsRow(ToPart toPart, String carModel, String sparePartName, int quantity, String unitType, String note) {
        this.toPart = toPart;
        this.carModel = carModel;
        this.sparePartName = sparePartName;
        this.quantity = quantity;
        this.unitType = unitType;
        this.note = note;
    }

    public ToPart getToPart() {
        return toPart;
    }

    public void setToPart(ToPart toPart) {
        this.toPart = toPart;
    }

    public String getCarModel() {
        return carModel;
    }

    public void setCarModel(String carModel) {
        this.carModel = carModel;
    }

    public String getSparePartName() {
        return sparePartName;
    }

    public void setSparePartName(String sparePartName) {
        this.sparePartName = sparePartName;
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

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @Override
    public String toString() {
        return "ToPartsRow{" +
                "carModel='" + carModel + '\'' +
                ", sparePartName='" + sparePartName + '\'' +
                ", quantity=" + quantity +
                ", unitType='" + unitType + '\'' +
                ", note='" + note + '\'' +
                '}';
    }
}
