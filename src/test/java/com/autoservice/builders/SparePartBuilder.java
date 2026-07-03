package com.autoservice.builders;

import com.autoservice.SparePart;

/**
 * Builder для создания тестовых объектов SparePart.
 */
public class SparePartBuilder {
    private SparePart sparePart;

    public SparePartBuilder() {
        this.sparePart = new SparePart("", 0.0, 0.0, 0);
    }

    public SparePartBuilder withId(int id) {
        sparePart.setId(id);
        return this;
    }

    public SparePartBuilder withOrderId(int orderId) {
        sparePart.setOrderId(orderId);
        return this;
    }

    public SparePartBuilder withName(String name) {
        sparePart.setName(name);
        return this;
    }

    public SparePartBuilder withPartNumber(String partNumber) {
        sparePart.setPartNumber(partNumber);
        return this;
    }

    public SparePartBuilder withManufacturer(String manufacturer) {
        sparePart.setManufacturer(manufacturer);
        return this;
    }

    public SparePartBuilder withCompatibleModels(String compatibleModels) {
        sparePart.setCompatibleModels(compatibleModels);
        return this;
    }

    public SparePartBuilder withPurchasePrice(double purchasePrice) {
        sparePart.setPurchasePrice(purchasePrice);
        return this;
    }

    public SparePartBuilder withRetailPrice(double retailPrice) {
        sparePart.setRetailPrice(retailPrice);
        return this;
    }

    public SparePartBuilder withStock(double stock) {
        sparePart.setStock(stock);
        return this;
    }

    public SparePartBuilder withMinStock(double minStock) {
        sparePart.setMinStock(minStock);
        return this;
    }

    public SparePartBuilder withUnitVolume(double unitVolume) {
        sparePart.setUnitVolume(unitVolume);
        return this;
    }

    public SparePartBuilder withUnitType(String unitType) {
        sparePart.setUnitType(unitType);
        return this;
    }

    public SparePartBuilder withIsLiquid(boolean isLiquid) {
        sparePart.setIsLiquid(isLiquid);
        return this;
    }

    public SparePartBuilder withLocation(String location) {
        sparePart.setLocation(location);
        return this;
    }

    public SparePart build() {
        return sparePart;
    }
}
