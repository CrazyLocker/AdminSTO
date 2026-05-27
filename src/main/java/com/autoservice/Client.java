package com.autoservice;

public class Client {
    private int id;
    private String name;
    private String phone;
    private String carModel;
    private String carNumber;

    public Client(String name, String phone, String carModel, String carNumber) {
        this.id = -1;
        this.name = name;
        this.phone = phone;
        this.carModel = carModel;
        this.carNumber = carNumber;
    }

    public Client(int id, String name, String phone, String carModel, String carNumber) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.carModel = carModel;
        this.carNumber = carNumber;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getCarModel() { return carModel; }
    public void setCarModel(String carModel) { this.carModel = carModel; }

    public String getCarNumber() { return carNumber; }
    public void setCarNumber(String carNumber) { this.carNumber = carNumber; }

    @Override
    public String toString() {
        return name + " (" + carNumber + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Client client = (Client) obj;
        return name.equals(client.name) && phone.equals(client.phone);
    }

    @Override
    public int hashCode() {
        return name.hashCode() + phone.hashCode();
    }
}