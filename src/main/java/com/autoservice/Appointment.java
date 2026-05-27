package com.autoservice;

public class Appointment {
    private int id;
    private Client client;
    private String orderId;  // Теперь String
    private String masterName;
    private String serviceName;
    private String date;
    private String time;
    private String status;

    public static final String STATUS_SCHEDULED = "ЗАПЛАНИРОВАНО";
    public static final String STATUS_COMPLETED = "ВЫПОЛНЕНО";
    public static final String STATUS_CANCELLED = "ОТМЕНЕНО";

    public Appointment(int id, Client client, String orderId, String masterName, String serviceName, String date, String time, String status) {
        this.id = id;
        this.client = client;
        this.orderId = orderId;
        this.masterName = masterName;
        this.serviceName = serviceName;
        this.date = date;
        this.time = time;
        this.status = status;
    }

    public Appointment(Client client, String masterName, String serviceName, String date, String time) {
        this.id = -1;
        this.client = client;
        this.orderId = null;
        this.masterName = masterName;
        this.serviceName = serviceName;
        this.date = date;
        this.time = time;
        this.status = STATUS_SCHEDULED;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getMasterName() { return masterName; }
    public void setMasterName(String masterName) { this.masterName = masterName; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return time + " - " + client.getName() + " (" + serviceName + ") - " + masterName;
    }
}