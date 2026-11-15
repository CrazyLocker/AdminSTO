package com.autoservice.views;

import com.autoservice.AppConstants;
import com.autoservice.Appointment;
import com.autoservice.Client;
import com.autoservice.DataStore;
import com.autoservice.DatabaseFactory;
import com.autoservice.DateUtils;
import com.autoservice.WorkOrder;
import com.autoservice.support.JavaFxTestSupport;
import com.autoservice.utils.LoggerManager;
import com.autoservice.utils.ThemeManager;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.autoservice.TestTags;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UI-тест календаря вкладки «Записи»: проверяет, что вид календаря использует
 * стили темы (легенда, карточки со статусной полосой, точки статуса в месячной
 * сетке), а не плоскую текстовую таблицу.
 */
@Tag(TestTags.UI)
class AppointmentCalendarUITest {

    private static final int APPOINTMENTS_TODAY = 5;

    private Stage stage;

    @BeforeAll
    static void setUpClass() {
        JavaFxTestSupport.initToolkit();
        LoggerManager.init();
        ThemeManager.applySavedTheme();
        DatabaseFactory.initForTest();
        DataStore.load();
        seed();
    }

    @AfterAll
    static void tearDownClass() {
        DatabaseFactory.close();
    }

    @AfterEach
    void closeStage() throws InterruptedException {
        if (stage != null) {
            Stage current = stage;
            JavaFxTestSupport.runOnFxThread(current::close);
            stage = null;
        }
    }

    @Test
    void weekViewIsStyledWithThemeClasses() throws InterruptedException {
        VBox view = showView("Неделя");

        assertThat(view.getStylesheets())
                .as("стили календаря подключены к корню вкладки")
                .anyMatch(stylesheet -> stylesheet.endsWith("appointment-custom.css"));

        assertThat(view.lookupAll(".calendar-legend"))
                .as("легенда статусов отображается")
                .hasSize(1);
        assertThat(view.lookupAll(".legend-item"))
                .as("в легенде четыре статуса")
                .hasSize(4);

        assertThat(view.lookupAll(".week-cell"))
                .as("ячейки недели созданы для всех слотов")
                .hasSize(AppConstants.TIME_SLOTS.size() * 7);
        assertThat(view.lookupAll(".week-cell-free"))
                .as("свободные ячейки отличаются от занятых")
                .hasSize(AppConstants.TIME_SLOTS.size() * 7 - APPOINTMENTS_TODAY);
        assertThat(view.lookupAll(".appointment-card"))
                .as("каждая запись отрисована карточкой")
                .hasSize(APPOINTMENTS_TODAY);
    }

    @Test
    void appointmentStripeReflectsOrderStatus() throws InterruptedException {
        VBox view = showView("Неделя");

        Set<String> stripeStatuses = view.lookupAll(".appointment-stripe").stream()
                .map(node -> node.getStyleClass().stream()
                        .filter(styleClass -> styleClass.startsWith("status-"))
                        .findFirst()
                        .orElse(""))
                .collect(Collectors.toSet());

        assertThat(stripeStatuses)
                .as("полоса карточки отражает все четыре состояния заказа")
                .containsExactlyInAnyOrder("status-new", "status-in-progress", "status-closed", "status-none");
    }

    @Test
    void dayHeaderShowsAppointmentCounter() throws InterruptedException {
        VBox view = showView("Неделя");

        Set<Node> badges = view.lookupAll(".week-day-count");
        assertThat(badges)
                .as("счётчик записей показан только у дня с записями")
                .hasSize(1);
        assertThat(((Label) badges.iterator().next()).getText())
                .as("счётчик показывает число записей дня")
                .isEqualTo(String.valueOf(APPOINTMENTS_TODAY));

        assertThat(view.lookupAll(".week-day-card.today"))
                .as("сегодняшний день выделен")
                .hasSize(1);
    }

    @Test
    void monthViewShowsStatusDotsAndTodayHighlight() throws InterruptedException {
        VBox view = showView("Месяц");

        assertThat(view.lookupAll(".month-cell"))
                .as("месячная сетка содержит шесть недель по семь дней")
                .hasSize(42);
        assertThat(view.lookupAll(".month-cell.today"))
                .as("сегодняшняя ячейка выделена")
                .hasSize(1);
        assertThat(view.lookupAll(".month-appointment-row"))
                .as("в ячейке дня показываются первые две записи")
                .hasSize(2);
        assertThat(view.lookupAll(".month-more-label"))
                .as("остальные записи сворачиваются в «... и ещё N»")
                .hasSize(1);

        Set<String> dotStatuses = view.lookupAll(".month-status-dot").stream()
                .flatMap(node -> node.getStyleClass().stream())
                .filter(styleClass -> styleClass.startsWith("status-"))
                .collect(Collectors.toSet());
        assertThat(dotStatuses)
                .as("точка статуса окрашена по заказу")
                .isNotEmpty();
    }

    private VBox showView(String viewName) throws InterruptedException {
        VBox view = JavaFxTestSupport.callOnFxThread(AppointmentView::create);

        JavaFxTestSupport.runOnFxThread(() -> {
            Scene scene = new Scene(view, 1500, 940);
            ThemeManager.applySceneStyles(scene);
            stage = new Stage();
            stage.setScene(scene);
            stage.show();
            selectView(view, viewName);
            view.applyCss();
            view.layout();
        });

        return view;
    }

    private static void selectView(VBox view, String viewName) {
        RadioButton target = view.lookupAll(".view-radio").stream()
                .filter(node -> node instanceof RadioButton)
                .map(node -> (RadioButton) node)
                .filter(radio -> viewName.equals(radio.getText()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Не найден переключатель вида: " + viewName));
        if (!target.isSelected()) {
            target.fire();
        }
    }

    private static void seed() {
        String today = DateUtils.formatDateForDB(LocalDate.now());

        DataStore.addClient(new Client("Алексей", "Кузнецов", "+7 900 111-00-01", "Toyota Camry", "А123ВС163"));
        DataStore.addClient(new Client("Анна", "Морозова", "+7 900 111-00-02", "Hyundai Creta", "В456ЕК163"));
        DataStore.addClient(new Client("Олег", "Белов", "+7 900 111-00-03", "Mercedes C200", "С789МН163"));
        DataStore.addClient(new Client("Ирина", "Смирнова", "+7 900 111-00-04", "Renault Logan", "Е321ОР163"));

        Client kuznetsov = clientByLastName("Кузнецов");
        Client morozova = clientByLastName("Морозова");
        Client belov = clientByLastName("Белов");
        Client smirnova = clientByLastName("Смирнова");

        WorkOrder closed = new WorkOrder(kuznetsov);
        closed.setStatus(WorkOrder.STATUS_CLOSED);
        DataStore.addOrder(closed);

        WorkOrder inProgress = new WorkOrder(morozova);
        inProgress.setStatus(WorkOrder.STATUS_IN_PROGRESS);
        DataStore.addOrder(inProgress);

        WorkOrder fresh = new WorkOrder(belov);
        fresh.setStatus(WorkOrder.STATUS_NEW);
        DataStore.addOrder(fresh);

        DataStore.addAppointment(new Appointment(-1, kuznetsov, orderIdFor(kuznetsov), "Иванов А.",
                "Комплексное ТО", today, "09:00", Appointment.STATUS_CONFIRMED));
        DataStore.addAppointment(new Appointment(-1, morozova, orderIdFor(morozova), "Петров С.",
                "Чип-тюнинг", today, "11:00", Appointment.STATUS_CONFIRMED));
        DataStore.addAppointment(new Appointment(-1, belov, orderIdFor(belov), "Сидоров В.",
                "Подвеска", today, "14:00", Appointment.STATUS_NEW));
        DataStore.addAppointment(new Appointment(-1, smirnova, null, "Иванов А.",
                "Замена колодок", today, "16:00", Appointment.STATUS_NEW));
        DataStore.addAppointment(new Appointment(-1, kuznetsov, orderIdFor(kuznetsov), "Петров С.",
                "Диагностика", today, "18:00", Appointment.STATUS_CONFIRMED));
    }

    private static Client clientByLastName(String lastName) {
        return DataStore.getClients().stream()
                .filter(client -> lastName.equals(client.getLastName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Клиент не найден: " + lastName));
    }

    private static String orderIdFor(Client client) {
        return DataStore.getOrders().stream()
                .filter(order -> order.getClient() != null && order.getClient().getId() == client.getId())
                .map(WorkOrder::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Заказ клиента не найден"));
    }
}
