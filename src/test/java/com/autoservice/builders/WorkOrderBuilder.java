package com.autoservice.builders;

import com.autoservice.WorkOrder;
import com.autoservice.Client;
import com.autoservice.SparePart;

/**
 * Builder для создания тестовых объектов WorkOrder.
 */
public class WorkOrderBuilder {
    private WorkOrder order;

    public WorkOrderBuilder() {
        this.order = new WorkOrder();
    }

    public WorkOrderBuilder withId(String id) {
        order.setId(id);
        return this;
    }

    public WorkOrderBuilder withClient(Client client) {
        order.setClient(client);
        return this;
    }

    public WorkOrderBuilder withStatus(String status) {
        order.setStatus(status);
        return this;
    }

    public WorkOrderBuilder withTotal(double total) {
        order.setTotal(total);
        return this;
    }

    public WorkOrderBuilder withCreatedDate(String createdDate) {
        order.setCreatedDate(createdDate);
        return this;
    }

    public WorkOrderBuilder addService(String name, double price) {
        order.addService(name, price);
        return this;
    }

    public WorkOrderBuilder addSparePart(SparePart part, int quantity) {
        order.addSparePart(part, quantity);
        return this;
    }

    public WorkOrder build() {
        return order;
    }
}
