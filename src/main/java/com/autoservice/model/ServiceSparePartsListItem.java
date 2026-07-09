package com.autoservice.model;

/**
 * Элемент списка запчастей, связанных с услугой.
 * Используется для хранения элементов в таблице service_spare_parts_list_items.
 */
public class ServiceSparePartsListItem {
    private int id;
    private int listId;
    private int sparePartId;
    private int quantity;
    private String unitType;

    public ServiceSparePartsListItem() {
        this.id = -1;
        this.listId = 0;
        this.sparePartId = 0;
        this.quantity = 1;
        this.unitType = "шт";
    }

    public ServiceSparePartsListItem(int listId, int sparePartId, int quantity, String unitType) {
        this.id = -1;
        this.listId = listId;
        this.sparePartId = sparePartId;
        this.quantity = quantity;
        this.unitType = unitType;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getListId() {
        return listId;
    }

    public void setListId(int listId) {
        this.listId = listId;
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

    private boolean isDirty = false;

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }

    @Override
    public String toString() {
        return "ServiceSparePartsListItem{" +
                "id=" + id +
                ", listId=" + listId +
                ", sparePartId=" + sparePartId +
                ", quantity=" + quantity +
                ", unitType='" + unitType + '\'' +
                '}';
    }
}
