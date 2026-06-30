package com.autoservice;

import java.util.ArrayList;
import java.util.List;

public class WorkOrder {
    public static final String STATUS_DRAFT = "Черновик";
    public static final String STATUS_IN_PROGRESS = "В работе";
    public static final String STATUS_CLOSED = "Закрыт";
    public static final String STATUS_CANCELLED = "Отменён";

    private String id;
    private Client client;
    private String status;
    private double total;
    private String createdDate;
    private List<String> services = new ArrayList<>();
    private List<Double> servicePrices = new ArrayList<>();
    private List<SparePart> spareParts = new ArrayList<>();
    private List<Integer> sparePartQuantities = new ArrayList<>();
    private boolean dirty = false;

    // ==================== КОНСТРУКТОРЫ ====================

    public WorkOrder(Client client) {
        this.client = client;
        this.status = STATUS_DRAFT;
        this.total = 0;
        this.dirty = true;
    }

    public WorkOrder(String id, Client client, String status, double total, String createdDate) {
        this.id = id;
        this.client = client;
        this.status = status;
        this.total = total;
        this.createdDate = createdDate;
        this.dirty = false;
    }

    // ==================== ГЕТТЕРЫ ====================

    public String getId() { return id; }
    public Client getClient() { return client; }
    public String getStatus() { return status; }
    public double getTotal() { return total; }
    public String getCreatedDate() { return createdDate; }
    public List<String> getServices() { return services; }
    public List<Double> getServicePrices() { return servicePrices; }
    public List<SparePart> getSpareParts() { return spareParts; }
    public List<Integer> getSparePartQuantities() { return sparePartQuantities; }
    public boolean isDirty() { return dirty; }

    // ==================== СЕТТЕРЫ ====================

    public void setId(String id) {
        this.id = id;
        this.dirty = true;
    }

    public void setClient(Client client) {
        this.client = client;
        this.dirty = true;
    }

    public void setStatus(String status) {
        this.status = status;
        this.dirty = true;
    }

    public void setTotal(double total) {
        this.total = total;
        this.dirty = true;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
        this.dirty = true;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    // ==================== МЕТОДЫ ====================

    public void addService(String name, double price) {
        this.services.add(name);
        this.servicePrices.add(price);
        this.dirty = true;
        recalculateTotal();
    }

    public void removeService(int index) {
        if (index >= 0 && index < services.size()) {
            services.remove(index);
            servicePrices.remove(index);
            this.dirty = true;
            recalculateTotal();
        }
    }

    public void addSparePart(SparePart part, int quantity) {
        this.spareParts.add(part);
        this.sparePartQuantities.add(quantity);
        this.dirty = true;
        recalculateTotal();
    }

    public void removeSparePart(int index) {
        if (index >= 0 && index < spareParts.size()) {
            spareParts.remove(index);
            sparePartQuantities.remove(index);
            this.dirty = true;
            recalculateTotal();
        }
    }

    public void recalculateTotal() {
        double newTotal = 0;
        for (Double price : servicePrices) {
            newTotal += price;
        }
        for (int i = 0; i < spareParts.size(); i++) {
            newTotal += spareParts.get(i).getRetailPrice() * sparePartQuantities.get(i);
        }
        if (this.total != newTotal) {
            this.total = newTotal;
            this.dirty = true;
        }
    }

    public void markClean() {
        this.dirty = false;
        for (SparePart part : spareParts) {
            if (part != null) part.markClean();
        }
    }

    @Override
    public String toString() {
        return id + " - " + client.getFullName() + " (" + status + ")";
    }
}