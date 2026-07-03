package com.autoservice.builders;

import com.autoservice.Service;

/**
 * Builder для создания тестовых объектов Service.
 */
public class ServiceBuilder {
    private Service service;

    public ServiceBuilder() {
        this.service = new Service("", 0.0);
    }

    public ServiceBuilder withName(String name) {
        service.setName(name);
        return this;
    }

    public ServiceBuilder withPrice(double price) {
        service.setPrice(price);
        return this;
    }

    public ServiceBuilder withDuration(int duration) {
        service.setDuration(duration);
        return this;
    }

    public ServiceBuilder withPartNumber(String partNumber) {
        service.setPartNumber(partNumber);
        return this;
    }

    public ServiceBuilder withOilVolume(double oilVolume) {
        service.setOilVolume(oilVolume);
        return this;
    }

    public ServiceBuilder withUsesOil(boolean usesOil) {
        service.setUsesOil(usesOil);
        return this;
    }

    public ServiceBuilder withSparePartName(String sparePartName) {
        service.setSparePartName(sparePartName);
        return this;
    }

    public ServiceBuilder withSparePartQuantity(int sparePartQuantity) {
        service.setSparePartQuantity(sparePartQuantity);
        return this;
    }

    public Service build() {
        return service;
    }
}
