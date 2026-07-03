package com.autoservice.builders;

import com.autoservice.Client;

/**
 * Builder для создания тестовых объектов Client.
 */
public class ClientBuilder {
    private Client client;

    public ClientBuilder() {
        this.client = new Client("", "", "", "", "");
    }

    public ClientBuilder withId(int id) {
        client.setId(id);
        return this;
    }

    public ClientBuilder withName(String name) {
        client.setName(name);
        return this;
    }

    public ClientBuilder withLastName(String lastName) {
        client.setLastName(lastName);
        return this;
    }

    public ClientBuilder withPhone(String phone) {
        client.setPhone(phone);
        return this;
    }

    public ClientBuilder withCarModel(String carModel) {
        client.setCarModel(carModel);
        return this;
    }

    public ClientBuilder withCarNumber(String carNumber) {
        client.setCarNumber(carNumber);
        return this;
    }

    public ClientBuilder withLastRepairDate(String lastRepairDate) {
        client.setLastRepairDate(lastRepairDate);
        return this;
    }

    public Client build() {
        return client;
    }
}
