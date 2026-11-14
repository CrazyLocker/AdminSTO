package com.autoservice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Регрессии для клиентов, у которых телефон в базе записан с форматированием
 * (например, {@code +7 900 111-00-02} после импорта или наполнения тестовыми
 * данными), а {@code last_name} может быть {@code NULL}.
 *
 * <p>Идентификатор клиента при сохранении заказа и записи на сервис ищется по
 * нормализованному телефону. Пока сравнение шло прямо в SQL, такой клиент не
 * находился ({@code client_id = -1}), вставка записи нарушала
 * {@code CHECK(client_id > 0)} и падала молча — созданные через редактирование
 * заказа записи не появлялись ни в календаре дашборда, ни на вкладке «Запись».</p>
 */
@Tag(TestTags.INTEGRATION)
class AppointmentLegacyClientTest extends BaseTest {

    @Test
    @DisplayName("Запись сохраняется для клиента с форматированным телефоном в БД")
    void appointmentIsSavedForClientWithFormattedPhone() throws Exception {
        int clientId = insertClient("Анна", "Смирнова", "+7 900 111-00-42", "Toyota Camry", "А111АА163");
        insertOrder("ZAK-14/11/26-9001", clientId);

        DataStore.load();
        WorkOrder order = DataStore.getOrders().get(0);
        assertThat(order.getClient().getPhone()).isEqualTo("+7 900 111-00-42");

        String date = DateUtils.formatDateForDB(LocalDate.now());
        Appointment appointment = new Appointment(
                order.getClient(), "Саныч", "Диагностика", date, "10:00");
        appointment.setOrderId(order.getId());
        DataStore.addAppointment(appointment);

        List<Appointment> saved = DataStore.getAppointments();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getClient().getId()).isEqualTo(clientId);
        assertThat(DataStore.getAppointmentsByDate(date))
                .as("Запись должна находиться по дате в формате dd/MM/yyyy")
                .hasSize(1);
    }

    @Test
    @DisplayName("ID клиента находится по форматированному телефону и NULL last_name")
    void clientIdLookupIgnoresPhoneFormattingAndNullLastName() throws Exception {
        int clientId = insertClient("Иван", null, "+7 900 111-00-43", "Lada Vesta", "В222ВВ163");

        DataStore.load();
        Client cached = DataStore.getClients().get(0);

        assertThat(DatabaseFactory.getDatabase().getClientId(cached))
                .as("Поиск клиента не зависит от форматирования телефона и NULL last_name")
                .isEqualTo(clientId);
    }

    /**
     * Вставляет клиента напрямую, минуя нормализацию {@code addClient}, —
     * так выглядят данные после импорта или наполнения тестовой БД.
     */
    private int insertClient(String name, String lastName, String phone,
                             String carModel, String carNumber) throws Exception {
        String sql = "INSERT INTO clients (name, last_name, phone, car_model, car_number, "
                + "last_repair_date, maintenance_stars, tuning_total) "
                + "VALUES (?, ?, ?, ?, ?, '', 0, 0)";
        try (Connection conn = DatabaseFactory.getDatabase().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, lastName);
            pstmt.setString(3, phone);
            pstmt.setString(4, carModel);
            pstmt.setString(5, carNumber);
            pstmt.executeUpdate();
        }
        String selectSql = "SELECT id FROM clients WHERE name = ? AND phone = ?";
        try (Connection conn = DatabaseFactory.getDatabase().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, phone);
            try (ResultSet rs = pstmt.executeQuery()) {
                assertThat(rs.next()).isTrue();
                return rs.getInt("id");
            }
        }
    }

    private void insertOrder(String orderId, int clientId) throws Exception {
        String sql = "INSERT INTO orders (id, client_id, status, total, created_date, "
                + "mileage, closed_date, notes) VALUES (?, ?, ?, 0, ?, 0, '', '')";
        try (Connection conn = DatabaseFactory.getDatabase().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, orderId);
            pstmt.setInt(2, clientId);
            pstmt.setString(3, WorkOrder.STATUS_NEW);
            pstmt.setString(4, DateUtils.formatDateForDB(LocalDate.now()));
            pstmt.executeUpdate();
        }
    }
}
