package com.autoservice;

public class Appointment {
    public static final String STATUS_NEW = "Новая";
    public static final String STATUS_COMPLETED = "Выполнено";

    private int id;
    private Client client;
    private String orderId;
    private String masterName;
    private String serviceName;
    private String date;
    private String time;
    private String status;
    private int serviceId = 0;
    private boolean dirty = false;

    // ==================== КОНСТРУКТОРЫ ====================

    public Appointment() {
        this.id = -1;
        this.client = null;
        this.orderId = null;
        this.masterName = "";
        this.serviceName = "";
        this.date = "";
        this.time = "";
        this.status = STATUS_NEW;
        this.dirty = true;
    }

    public Appointment(int id, Client client, String orderId, String masterName,
                       String serviceName, String date, String time, String status) {
        this.id = id;
        this.client = client;
        this.orderId = orderId;
        this.masterName = masterName;
        this.serviceName = serviceName;
        this.date = date;
        this.time = time;
        this.status = status;
        this.dirty = false;
    }

    public Appointment(Client client, String masterName, String serviceName, String date, String time) {
        this(-1, client, null, masterName, serviceName, date, time, STATUS_NEW);
    }

    // ==================== ГЕТТЕРЫ ====================

    public int getId() { return id; }
    public Client getClient() { return client; }
    public String getOrderId() { return orderId; }
    public String getMasterName() { return masterName; }
    public String getServiceName() { return serviceName; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getStatus() { return status; }
    public int getServiceId() { return serviceId; }
    public boolean isDirty() { return dirty; }

    // ==================== СЕТТЕРЫ ====================

    public void setId(int id) {
        this.id = id;
        this.dirty = true;
    }

    public void setClient(Client client) {
        this.client = client;
        this.dirty = true;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
        this.dirty = true;
    }

    public void setMasterName(String masterName) {
        this.masterName = masterName;
        this.dirty = true;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
        this.dirty = true;
    }

    public void setDate(String date) {
        this.date = date;
        this.dirty = true;
    }

    public void setTime(String time) {
        this.time = time;
        this.dirty = true;
    }

    public void setStatus(String status) {
        this.status = status;
        this.dirty = true;
    }

    public void setServiceId(int serviceId) {
        this.serviceId = serviceId;
        this.dirty = true;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    // ==================== МЕТОДЫ ====================

    public void markClean() {
        this.dirty = false;
    }

    @Override
    public String toString() {
        return date + " " + time + " - " + client.getFullName() + " (" + serviceName + ")";
    }
}