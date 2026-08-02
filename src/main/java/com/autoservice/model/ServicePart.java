package com.autoservice.model;

import java.time.LocalDate;

/**
 * Связь между услугой и запчастью для автоматического добавления.
 * Используется для хранения связей в таблице service_parts.
 * 
 * Заменяет старую систему с полями oilVolume, usesOil, sparePartName в Service.java
 */
public class ServicePart {
    private int id;
    private int serviceId;
    private int sparePartId;
    private double quantity;
    private boolean isRequired;
    private String createdDate;

    public ServicePart() {
        this.id = -1;
        this.serviceId = 0;
        this.sparePartId = 0;
        this.quantity = 1;
        this.isRequired = true;
        this.createdDate = LocalDate.now().toString();
        this.isDirty = true;
    }

    public ServicePart(int serviceId, int sparePartId, double quantity, boolean isRequired) {
        this.id = -1;
        this.serviceId = serviceId;
        this.sparePartId = sparePartId;
        this.quantity = quantity;
        this.isRequired = isRequired;
        this.createdDate = LocalDate.now().toString();
        this.isDirty = true;
    }

    // ==================== ГЕТТЕРЫ ====================

    public int getId() {
        return id;
    }

    public int getServiceId() {
        return serviceId;
    }

    public int getSparePartId() {
        return sparePartId;
    }

    public double getQuantity() {
        return quantity;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    // ==================== СЕТТЕРЫ ====================

    public void setId(int id) {
        this.id = id;
    }

    public void setServiceId(int serviceId) {
        this.serviceId = serviceId;
    }

    public void setSparePartId(int sparePartId) {
        this.sparePartId = sparePartId;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public void setRequired(boolean required) {
        isRequired = required;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    private boolean isDirty = false;

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }

    // ==================== МЕТОДЫ ====================

    @Override
    public String toString() {
        return "ServicePart{" +
                "id=" + id +
                ", serviceId=" + serviceId +
                ", sparePartId=" + sparePartId +
                ", quantity=" + quantity +
                ", isRequired=" + isRequired +
                ", createdDate='" + createdDate + '\'' +
                '}';
    }
}
