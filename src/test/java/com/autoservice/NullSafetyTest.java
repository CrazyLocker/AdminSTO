package com.autoservice;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Тесты null-safe поведения моделей заказа и записи.
 */
@Tag(TestTags.UNIT)
class NullSafetyTest {

    @Test
    void workOrderConstructorAcceptsNullClient() {
        WorkOrder order = new WorkOrder((Client) null);

        assertThat(order.getClient()).isNull();
        assertThat(order.getClientId()).isZero();
    }

    @Test
    void appointmentConstructorAcceptsNullClient() {
        Appointment appointment = new Appointment(null, "Саныч", "Диагностика", "2024-06-02", "14:00");

        assertThat(appointment.getClient()).isNull();
    }

    @Test
    void workOrderWithoutClientDoesNotFailInToString() {
        WorkOrder order = new WorkOrder();
        order.setId("ZAK-1");
        order.setStatus(WorkOrder.STATUS_NEW);

        assertThatCode(order::toString).doesNotThrowAnyException();
        assertThat(order.toString()).contains(AppConstants.NO_CLIENT, "ZAK-1");
    }

    @Test
    void appointmentWithoutClientDoesNotFailInToString() {
        Appointment appointment = new Appointment();
        appointment.setDate("2024-06-01");
        appointment.setTime("10:00");
        appointment.setServiceName("Замена масла");

        assertThatCode(appointment::toString).doesNotThrowAnyException();
        assertThat(appointment.toString()).contains(AppConstants.NO_CLIENT, "Замена масла");
    }

    @Test
    void workOrderWithClientStillUsesClientNameInToString() {
        Client client = new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");
        WorkOrder order = new WorkOrder(client);
        order.setId("ZAK-2");

        assertThat(order.toString()).contains("Петров Иван");
    }

    @Test
    void appointmentWithClientStillUsesClientNameInToString() {
        Client client = new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");
        Appointment appointment = new Appointment(client, "Саныч", "Диагностика", "2024-06-02", "14:00");

        assertThat(appointment.toString()).contains("Петров Иван", "Диагностика");
    }
}
