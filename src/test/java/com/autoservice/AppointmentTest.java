package com.autoservice;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Тесты для класса Appointment
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AppointmentTest {

    private Client testClient;

    @BeforeEach
    void setUp() {
        testClient = new Client("Иван", "+79001234567", "Haval Jolion", "А123ВС163");
    }

    @Test
    @Order(1)
    void testAppointmentConstructorFull() {
        Appointment appointment = new Appointment(
            1, testClient, "ZAK-15/01/24-00001",
            "Иван", "Замена масла",
            "2024-02-20", "10:00",
            Appointment.STATUS_SCHEDULED
        );
        
        assertThat(appointment.getId()).isEqualTo(1);
        assertThat(appointment.getClient()).isEqualTo(testClient);
        assertThat(appointment.getOrderId()).isEqualTo("ZAK-15/01/24-00001");
        assertThat(appointment.getMasterName()).isEqualTo("Иван");
        assertThat(appointment.getServiceName()).isEqualTo("Замена масла");
        assertThat(appointment.getDate()).isEqualTo("2024-02-20");
        assertThat(appointment.getTime()).isEqualTo("10:00");
        assertThat(appointment.getStatus()).isEqualTo(Appointment.STATUS_SCHEDULED);
    }

    @Test
    @Order(2)
    void testAppointmentConstructorBasic() {
        Appointment appointment = new Appointment(
            testClient, "Петр", "Диагностика",
            "2024-02-21", "14:30"
        );
        
        assertThat(appointment.getId()).isEqualTo(-1);
        assertThat(appointment.getClient()).isEqualTo(testClient);
        assertThat(appointment.getOrderId()).isNull();
        assertThat(appointment.getMasterName()).isEqualTo("Петр");
        assertThat(appointment.getServiceName()).isEqualTo("Диагностика");
        assertThat(appointment.getDate()).isEqualTo("2024-02-21");
        assertThat(appointment.getTime()).isEqualTo("14:30");
        assertThat(appointment.getStatus()).isEqualTo(Appointment.STATUS_SCHEDULED);
    }

    @Test
    @Order(3)
    void testAppointmentStatuses() {
        Appointment appointment1 = new Appointment(
            testClient, "Иван", "Замена масла",
            "2024-02-20", "10:00"
        );
        
        assertThat(Appointment.STATUS_SCHEDULED).isEqualTo("ЗАПЛАНИРОВАНО");
        assertThat(Appointment.STATUS_COMPLETED).isEqualTo("ВЫПОЛНЕНО");
        assertThat(Appointment.STATUS_CANCELLED).isEqualTo("ОТМЕНЕНО");
        
        appointment1.setStatus(Appointment.STATUS_COMPLETED);
        assertThat(appointment1.getStatus()).isEqualTo(Appointment.STATUS_COMPLETED);
        
        appointment1.setStatus(Appointment.STATUS_CANCELLED);
        assertThat(appointment1.getStatus()).isEqualTo(Appointment.STATUS_CANCELLED);
    }

    @Test
    @Order(4)
    void testAppointmentSetters() {
        Appointment appointment = new Appointment(
            testClient, "Иван", "Замена масла",
            "2024-02-20", "10:00"
        );
        
        appointment.setId(10);
        appointment.setOrderId("ZAK-20/02/24-00005");
        appointment.setMasterName("Сергей");
        appointment.setServiceName("Замена тормозных колодок");
        appointment.setDate("2024-02-25");
        appointment.setTime("15:30");
        appointment.setStatus(Appointment.STATUS_COMPLETED);
        
        assertThat(appointment.getId()).isEqualTo(10);
        assertThat(appointment.getOrderId()).isEqualTo("ZAK-20/02/24-00005");
        assertThat(appointment.getMasterName()).isEqualTo("Сергей");
        assertThat(appointment.getServiceName()).isEqualTo("Замена тормозных колодок");
        assertThat(appointment.getDate()).isEqualTo("2024-02-25");
        assertThat(appointment.getTime()).isEqualTo("15:30");
        assertThat(appointment.getStatus()).isEqualTo(Appointment.STATUS_COMPLETED);
    }

    @Test
    @Order(5)
    void testAppointmentToString() {
        Appointment appointment = new Appointment(
            testClient, "Иван", "Замена масла",
            "2024-02-20", "10:00"
        );
        
        String result = appointment.toString();
        assertThat(result).contains("10:00");
        assertThat(result).contains("Иван");
        assertThat(result).contains("Замена масла");
    }

    @Test
    @Order(6)
    void testAppointmentWithNullOrderId() {
        Appointment appointment = new Appointment(
            testClient, "Иван", "Консультация",
            "2024-02-20", "09:00"
        );
        
        assertThat(appointment.getOrderId()).isNull();
    }
}
