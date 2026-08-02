package com.autoservice.builders;

import com.autoservice.Appointment;
import com.autoservice.Client;

/**
 * Builder для создания тестовых объектов Appointment.
 */
public class AppointmentBuilder {
    private Appointment appointment;

    public AppointmentBuilder() {
        this.appointment = new Appointment(new Client("", "", "", "", ""), "", "", "", "");
    }

    public AppointmentBuilder withId(int id) {
        appointment.setId(id);
        return this;
    }

    public AppointmentBuilder withClient(Client client) {
        appointment.setClient(client);
        return this;
    }

    public AppointmentBuilder withOrderId(String orderId) {
        appointment.setOrderId(orderId);
        return this;
    }

    public AppointmentBuilder withMasterName(String masterName) {
        appointment.setMasterName(masterName);
        return this;
    }

    public AppointmentBuilder withServiceName(String serviceName) {
        appointment.setServiceName(serviceName);
        return this;
    }

    public AppointmentBuilder withDate(String date) {
        appointment.setDate(date);
        return this;
    }

    public AppointmentBuilder withTime(String time) {
        appointment.setTime(time);
        return this;
    }

    public AppointmentBuilder withStatus(String status) {
        appointment.setStatus(status);
        return this;
    }

    public Appointment build() {
        return appointment;
    }
}
