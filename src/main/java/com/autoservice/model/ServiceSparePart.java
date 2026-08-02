package com.autoservice.model;

/**
 * Связь между услугой и запчастью для автоматического добавления.
 * Используется для хранения связей в таблице service_spare_parts.
 */
public class ServiceSparePart {
    private int id;
    private int serviceId;
    private int sparePartId;
    private int quantity;
    private String unitType;
    private boolean active;

    public ServiceSparePart() {
        this.id = -1;
        this.serviceId = 0;
        this.sparePartId = 0;
        this.quantity = 1;
        this.unitType = "шт";
        this.active = true;
    }

    public ServiceSparePart(int serviceId, int sparePartId, int quantity, String unitType) {
        this.id = -1;
        this.serviceId = serviceId;
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

    public int getServiceId() {
        return serviceId;
    }

    public void setServiceId(int serviceId) {
        this.serviceId = serviceId;
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
        return "ServiceSparePart{" +
                "id=" + id +
                ", serviceId=" + serviceId +
                ", sparePartId=" + sparePartId +
                ", quantity=" + quantity +
                ", unitType='" + unitType + '\'' +
                ", active=" + active +
                '}';
    }
}
