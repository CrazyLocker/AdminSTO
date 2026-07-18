package com.autoservice.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Связь между услугой и списком запчастей для автоматического добавления.
 * Используется для хранения связи в таблице service_spare_parts_lists.
 */
public class ServiceSparePartsList {
    private int id;
    private int serviceId;
    private String createdDate;
    private boolean active;
    private List<ServiceSparePartsListItem> items;

    public ServiceSparePartsList() {
        this.id = -1;
        this.serviceId = 0;
        this.createdDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        this.active = true;
        this.items = new ArrayList<>();
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

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<ServiceSparePartsListItem> getItems() {
        return items;
    }

    public void setItems(List<ServiceSparePartsListItem> items) {
        this.items = items;
    }

    public void addItem(ServiceSparePartsListItem item) {
        this.items.add(item);
    }
    
    public ServiceSparePartsListItem addItem(int sparePartId, int quantity, String unitType) {
        ServiceSparePartsListItem item = new ServiceSparePartsListItem();
        item.setSparePartId(sparePartId);
        item.setQuantity(quantity);
        item.setUnitType(unitType);
        item.setDirty(true);
        this.items.add(item);
        return item;
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
        return "ServiceSparePartsList{" +
                "id=" + id +
                ", serviceId=" + serviceId +
                ", createdDate='" + createdDate + '\'' +
                ", active=" + active +
                ", items=" + items +
                '}';
    }
}
