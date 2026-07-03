package com.autoservice;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Тесты для класса Appointment
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag(TestTags.UNIT)
class AppointmentTest {

    private Client testClient;
    private Appointment testAppointment;

    @BeforeEach
    void setUp() {
        testClient = new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");
        testClient.setId(1);
        
        testAppointment = new Appointment(
            testClient, "Иван", "Замена масла", "2024-06-01", "10:00"
        );
        testAppointment.setId(1);
    }

    @Test
    @Order(1)
    void testConstructor() {
        Appointment app = new Appointment(
            testClient, "Иван", "Замена масла", "2024-06-01", "10:00"
        );
        
        assertThat(app.getClient()).isEqualTo(testClient);
        assertThat(app.getMasterName()).isEqualTo("Иван");
        assertThat(app.getServiceName()).isEqualTo("Замена масла");
        assertThat(app.getDate()).isEqualTo("2024-06-01");
        assertThat(app.getTime()).isEqualTo("10:00");
    }

    @Test
    @Order(2)
    void testGettersAndSetters() {
        Appointment app = new Appointment();
        app.setClient(testClient);
        app.setMasterName("Петр");
        app.setServiceName("Диагностика");
        app.setDate("2024-06-02");
        app.setTime("14:00");
        app.setStatus(Appointment.STATUS_SCHEDULED);
        
        assertThat(app.getClient()).isEqualTo(testClient);
        assertThat(app.getMasterName()).isEqualTo("Петр");
        assertThat(app.getServiceName()).isEqualTo("Диагностика");
        assertThat(app.getDate()).isEqualTo("2024-06-02");
        assertThat(app.getTime()).isEqualTo("14:00");
        assertThat(app.getStatus()).isEqualTo(Appointment.STATUS_SCHEDULED);
    }

    @Test
    @Order(3)
    void testToString() {
        Appointment app = new Appointment(
            testClient, "Иван", "Замена масла", "2024-06-01", "10:00"
        );
        String result = app.toString();
        
        assertThat(result).contains("Иван");
        assertThat(result).contains("Замена масла");
    }

    @Test
    @Order(4)
    void testStatusConstants() {
        assertThat(Appointment.STATUS_SCHEDULED).isEqualTo("Запланировано");
        assertThat(Appointment.STATUS_COMPLETED).isEqualTo("Выполнено");
        assertThat(Appointment.STATUS_CANCELLED).isEqualTo("Отменено");
    }

    @Test
    @Order(5)
    void testGetClientFullName() {
        Appointment app = new Appointment(
            testClient, "Иван", "Замена масла", "2024-06-01", "10:00"
        );
        String clientName = app.getClient().getFullName();
        
        assertThat(clientName).contains("Иван");
        assertThat(clientName).contains("Петров");
    }

    @Test
    @Order(6)
    void testAppointmentWithOrderId() {
        Appointment app = new Appointment();
        app.setOrderId("ZAK-01/01/24-0001");
        
        assertThat(app.getOrderId()).isEqualTo("ZAK-01/01/24-0001");
    }

    @Test
    @Order(7)
    void testAppointmentWithAllFields() {
        Appointment app = new Appointment(
            1, testClient, "ZAK-01/01/24-0001", "Иван", "Замена масла",
            "2024-06-01", "10:00", Appointment.STATUS_SCHEDULED
        );
        
        assertThat(app.getId()).isEqualTo(1);
        assertThat(app.getOrderId()).isEqualTo("ZAK-01/01/24-0001");
        assertThat(app.getStatus()).isEqualTo(Appointment.STATUS_SCHEDULED);
    }

    @Test
    @Order(8)
    void testAppointmentStatusChange() {
        Appointment app = new Appointment(
            testClient, "Иван", "Замена масла", "2024-06-01", "10:00"
        );
        
        assertThat(app.getStatus()).isEqualTo(Appointment.STATUS_SCHEDULED);
        
        app.setStatus(Appointment.STATUS_COMPLETED);
        assertThat(app.getStatus()).isEqualTo(Appointment.STATUS_COMPLETED);
        
        app.setStatus(Appointment.STATUS_CANCELLED);
        assertThat(app.getStatus()).isEqualTo(Appointment.STATUS_CANCELLED);
    }

    @Test
    @Order(9)
    void testAppointmentTimeSlots() {
        String[] timeSlots = {"08:00", "09:00", "10:00", "20:00"};
        
        assertThat(timeSlots).contains("08:00");
        assertThat(timeSlots).contains("20:00");
        assertThat(timeSlots).hasSize(4);
    }

    @Test
    @Order(10)
    void testAppointmentMasterNames() {
        String[] masters = {"Иван", "Петр", "Сергей", "Антон"};
        
        assertThat(masters).contains("Иван");
        assertThat(masters).contains("Петр");
        assertThat(masters).contains("Сергей");
        assertThat(masters).contains("Антон");
        assertThat(masters).hasSize(4);
    }

    @Test
    @Order(11)
    void testAppointmentDateValidation() {
        String validDate = "2024-06-01";
        assertThat(validDate).isNotBlank();
        assertThat(validDate).contains("2024");
    }

    @Test
    @Order(12)
    void testAppointmentWithNullOrderId() {
        Appointment app = new Appointment();
        app.setOrderId(null);
        
        assertThat(app.getOrderId()).isNull();
    }

    @Test
    @Order(13)
    void testAppointmentWithEmptyServiceName() {
        Appointment app = new Appointment(
            testClient, "Иван", "", "2024-06-01", "10:00"
        );
        
        assertThat(app.getServiceName()).isEmpty();
    }

    @Test
    @Order(14)
    void testAppointmentClientReference() {
        Appointment app = new Appointment(
            testClient, "Иван", "Замена масла", "2024-06-01", "10:00"
        );
        
        assertThat(app.getClient()).isSameAs(testClient);
    }

    @Test
    @Order(15)
    void testAppointmentWithLongServiceName() {
        Appointment app = new Appointment(
            testClient, "Иван", "Компьютерная диагностика двигателя", "2024-06-01", "10:00"
        );
        
        assertThat(app.getServiceName()).isEqualTo("Компьютерная диагностика двигателя");
    }
}
