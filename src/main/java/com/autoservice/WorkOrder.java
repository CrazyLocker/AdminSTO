package com.autoservice;

import java.util.ArrayList;
import java.util.List;

public class WorkOrder {
    private String id;
    private Client client;
    private List<String> services;
    private List<Double> servicePrices;
    private List<SparePart> spareParts;
    private List<Integer> sparePartQuantities;
    private double total;
    private String status;
    private String createdDate;

    public static final String STATUS_NEW = "НОВЫЙ";
    public static final String STATUS_DIAGNOSTICS = "ДИАГНОСТИКА";
    public static final String STATUS_IN_WORK = "В РАБОТЕ";
    public static final String STATUS_WAITING_PARTS = "ОЖИДАНИЕ ЗАПЧАСТЕЙ";
    public static final String STATUS_READY = "ГОТОВ";
    public static final String STATUS_CLOSED = "ЗАКРЫТ";

    public static List<String> getAllStatuses() {
        List<String> statuses = new ArrayList<>();
        statuses.add("Новый");
        statuses.add("В работе");
        statuses.add("Закрыт");
        return statuses;
    }

    public WorkOrder(Client client) {
        this.id = null;
        this.client = client;
        this.services = new ArrayList<>();
        this.servicePrices = new ArrayList<>();
        this.spareParts = new ArrayList<>();
        this.sparePartQuantities = new ArrayList<>();
        this.total = 0;
        this.status = STATUS_NEW;
        this.createdDate = "";
    }

    public WorkOrder(String id, Client client, String status, double total) {
        this.id = id;
        this.client = client;
        this.services = new ArrayList<>();
        this.servicePrices = new ArrayList<>();
        this.spareParts = new ArrayList<>();
        this.sparePartQuantities = new ArrayList<>();
        this.total = total;
        this.status = status;
        this.createdDate = "";
    }

    public WorkOrder(String id, Client client, String status, double total, String createdDate) {
        this.id = id;
        this.client = client;
        this.services = new ArrayList<>();
        this.servicePrices = new ArrayList<>();
        this.spareParts = new ArrayList<>();
        this.sparePartQuantities = new ArrayList<>();
        this.total = total;
        this.status = status;
        this.createdDate = createdDate != null ? createdDate : "";
    }

    public void addService(String name, double price) {
        services.add(name);
        servicePrices.add(price);
        recalculateTotal();
    }

    public void removeService(int index) {
        if (index >= 0 && index < services.size()) {
            services.remove(index);
            servicePrices.remove(index);
            recalculateTotal();
        }
    }

    public void addSparePart(SparePart part, int quantity) {
        spareParts.add(part);
        sparePartQuantities.add(quantity);
        recalculateTotal();
    }

    public void removeSparePart(int index) {
        if (index >= 0 && index < spareParts.size()) {
            spareParts.remove(index);
            sparePartQuantities.remove(index);
            recalculateTotal();
        }
    }

    public void recalculateTotal() {
        total = 0;
        for (Double price : servicePrices) {
            total += price;
        }
        for (int i = 0; i < spareParts.size(); i++) {
            total += spareParts.get(i).getRetailPrice() * sparePartQuantities.get(i);
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public double getTotal() { return total; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<String> getServices() { return services; }
    public List<Double> getServicePrices() { return servicePrices; }

    public List<SparePart> getSpareParts() { return spareParts; }
    public List<Integer> getSparePartQuantities() { return sparePartQuantities; }

    public String getCreatedDate() { return createdDate; }
    public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }

    @Override
    public String toString() {
        return (id != null ? id : "Новый") + " | " + client.getName() + " | " + total + " руб. | " + status;
    }
}