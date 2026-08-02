package com.autoservice;

public class Car {
    private int id;
    private int clientId;
    private String carModel;
    private String carNumber;
    private int mileage;

    public Car() {
        this.id = -1;
        this.clientId = -1;
        this.carModel = "";
        this.carNumber = "";
        this.mileage = 0;
    }

    public Car(int clientId, String carModel, String carNumber, int mileage) {
        this.id = -1;
        this.clientId = clientId;
        this.carModel = carModel;
        this.carNumber = carNumber;
        this.mileage = mileage;
    }

    public Car(int id, int clientId, String carModel, String carNumber, int mileage) {
        this.id = id;
        this.clientId = clientId;
        this.carModel = carModel;
        this.carNumber = carNumber;
        this.mileage = mileage;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }

    public String getCarModel() { return carModel; }
    public void setCarModel(String carModel) { this.carModel = carModel; }

    public String getCarNumber() { return carNumber; }
    public void setCarNumber(String carNumber) { this.carNumber = carNumber; }

    public int getMileage() { return mileage; }
    public void setMileage(int mileage) { this.mileage = mileage; }

    public String getDisplayName() {
        return (carModel != null ? carModel : "") + (carNumber != null && !carNumber.isEmpty() ? " (" + carNumber + ")" : "");
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
