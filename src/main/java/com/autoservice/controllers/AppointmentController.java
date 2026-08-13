package com.autoservice.controllers;

import com.autoservice.Appointment;
import com.autoservice.Client;
import com.autoservice.DataStore;
import com.autoservice.DateUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AppointmentController implements Initializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppointmentController.class);

    @FXML private Button weekBtn;
    @FXML private Button monthBtn;
    @FXML private Button todayBtn;
    @FXML private Button prevBtn;
    @FXML private Button nextBtn;

    @FXML private Label dateLabel;
    @FXML private Label periodLabel;

    @FXML private HBox dayHeaders;
    @FXML private GridPane calendarGrid;
    @FXML private ListView<Appointment> appointmentsList;

    private LocalDate currentDate;
    private ViewMode currentMode = ViewMode.WEEK;
    private final ObservableList<Appointment> appointments = FXCollections.observableArrayList();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("d MMM");

    private enum ViewMode {
        WEEK, MONTH
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentDate = LocalDate.now();

        appointmentsList.setItems(appointments);
        appointmentsList.setCellFactory(listView -> new AppointmentListCell());

        setupEventHandlers();
        loadCalendar();
    }

    private void setupEventHandlers() {
        weekBtn.setOnAction(e -> {
            currentMode = ViewMode.WEEK;
            loadCalendar();
        });

        monthBtn.setOnAction(e -> {
            currentMode = ViewMode.MONTH;
            loadCalendar();
        });

        todayBtn.setOnAction(e -> {
            currentDate = LocalDate.now();
            loadCalendar();
        });

        prevBtn.setOnAction(e -> navigate(-1));
        nextBtn.setOnAction(e -> navigate(1));
    }

    private void navigate(int direction) {
        switch (currentMode) {
            case WEEK:
                currentDate = currentDate.plusWeeks(direction);
                break;
            case MONTH:
                currentDate = currentDate.plusMonths(direction);
                break;
        }
        loadCalendar();
    }

    private void loadCalendar() {
        if (calendarGrid == null) return;

        calendarGrid.getChildren().clear();
        updateDayHeaders();

        List<LocalDate> datesToShow = getDatesToShow();

        // Группируем все записи по дате один раз, чтобы избежать многократных
        // проходов по всем записям для каждой ячейки календаря (проблема N+1).
        Map<String, List<Appointment>> appointmentsByDate = groupAppointmentsByDate();

        int row = 0, col = 0;
        for (LocalDate date : datesToShow) {
            VBox dayCell = createDayCell(date, appointmentsByDate);
            calendarGrid.add(dayCell, col, row);

            col++;
            if (col == 7) {
                col = 0;
                row++;
            }
        }

        updateLabels(datesToShow);
        getAppointmentsForDate(currentDate);

        LOGGER.debug("Календарь загружен: режим={}, дата={}, ячеек={}",
                currentMode, currentDate.format(DATE_FORMATTER), datesToShow.size());
    }

    /**
     * Группирует все записи из DataStore по дате (ключ — строка даты в формате dd/MM/yyyy).
     *
     * @return Map, где ключ — дата записи, значение — список записей на эту дату
     */
    private Map<String, List<Appointment>> groupAppointmentsByDate() {
        Map<String, List<Appointment>> result = new HashMap<>();

        for (Appointment app : DataStore.getAppointments()) {
            result.computeIfAbsent(app.getDate(), k -> new ArrayList<>()).add(app);
        }

        return result;
    }

    private List<LocalDate> getDatesToShow() {
        List<LocalDate> dates = new ArrayList<>();

        switch (currentMode) {
            case WEEK:
                LocalDate startOfWeek = currentDate.with(java.time.DayOfWeek.MONDAY);
                for (int i = 0; i < 7; i++) {
                    dates.add(startOfWeek.plusDays(i));
                }
                break;

            case MONTH:
                LocalDate firstDay = currentDate.withDayOfMonth(1);
                int firstDayOfWeek = firstDay.getDayOfWeek().getValue();

                for (int i = 1; i < firstDayOfWeek; i++) {
                    dates.add(null);
                }

                int daysInMonth = currentDate.lengthOfMonth();
                for (int i = 1; i <= daysInMonth; i++) {
                    dates.add(firstDay.plusDays(i - 1));
                }

                while (dates.size() < 42) {
                    dates.add(null);
                }
                break;
        }

        return dates;
    }

    private VBox createDayCell(LocalDate date, Map<String, List<Appointment>> appointmentsByDate) {
        VBox cell = new VBox(2);
        cell.setAlignment(Pos.TOP_CENTER);
        cell.getStyleClass().add("appointment-day-cell");

        if (date == null) {
            cell.getStyleClass().add("appointment-day-cell-empty");
            return cell;
        }

        Label dayLabel = new Label(String.valueOf(date.getDayOfMonth()));
        dayLabel.getStyleClass().add("appointment-day-number");

        if (date.equals(LocalDate.now())) {
            cell.getStyleClass().add("appointment-day-cell-today");
        }

        if (date.getDayOfWeek().getValue() >= 6) {
            cell.getStyleClass().add("appointment-day-cell-weekend");
        }

        List<Appointment> dayAppointments = appointmentsByDate.getOrDefault(
                DateUtils.formatDateForDB(date), Collections.emptyList());
        if (!dayAppointments.isEmpty()) {
            Rectangle dot = new Rectangle(8, 8);
            dot.getStyleClass().add("appointment-day-dot");

            Label countLabel = new Label(String.valueOf(dayAppointments.size()));
            countLabel.getStyleClass().add("appointment-day-count");

            cell.getChildren().addAll(dayLabel, dot, countLabel);
        } else {
            cell.getChildren().add(dayLabel);
        }

        cell.setOnMouseClicked(e -> {
            currentDate = date;
            getAppointmentsForDate(date);
            highlightSelectedCell(cell);
        });

        return cell;
    }

    private void highlightSelectedCell(VBox selectedCell) {
        for (var node : calendarGrid.getChildren()) {
            if (node instanceof VBox cell) {
                cell.getStyleClass().remove("appointment-day-cell-selected");
            }
        }
        selectedCell.getStyleClass().add("appointment-day-cell-selected");
    }

    private void updateDayHeaders() {
        dayHeaders.getChildren().clear();

        String[] days = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        for (String day : days) {
            Label label = new Label(day);
            label.getStyleClass().add("appointment-day-header");
            dayHeaders.getChildren().add(label);
        }
    }

    private void updateLabels(List<LocalDate> dates) {
        List<LocalDate> validDates = dates.stream()
                .filter(Objects::nonNull)
                .toList();

        if (validDates.isEmpty()) return;

        dateLabel.setText("Дата: " + currentDate.format(DATE_FORMATTER));

        switch (currentMode) {
            case WEEK:
                LocalDate first = validDates.get(0);
                LocalDate last = validDates.get(validDates.size() - 1);
                periodLabel.setText(first.format(PERIOD_FORMATTER) + " - " + last.format(PERIOD_FORMATTER));
                break;
            case MONTH:
                periodLabel.setText(currentDate.format(DateTimeFormatter.ofPattern("LLLL yyyy")));
                break;
        }
    }

    /**
     * Загружает записи на указанную дату в список {@link #appointments} и возвращает их.
     *
     * @param date дата, для которой фильтруются записи
     * @return список записей на указанную дату
     */
    private List<Appointment> getAppointmentsForDate(LocalDate date) {
        appointments.clear();

        List<Appointment> result = new ArrayList<>();
        String dateStr = DateUtils.formatDateForDB(date);

        for (Appointment app : DataStore.getAppointments()) {
            if (app.getDate().equals(dateStr)) {
                result.add(app);
            }
        }

        appointments.addAll(result);
        return result;
    }

    /**
     * Обновляет содержимое календаря и списка записей.
     * Вызывается при изменении данных вне контроллера.
     */
    public void refresh() {
        loadCalendar();
    }

    // ===== ВНУТРЕННИЙ КЛАСС ДЛЯ ЯЧЕЕК LISTVIEW =====
    private static class AppointmentListCell extends ListCell<Appointment> {
        @Override
        protected void updateItem(Appointment appointment, boolean empty) {
            super.updateItem(appointment, empty);

            if (empty || appointment == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            HBox root = new HBox(10);
            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(8, 12, 8, 12));
            root.getStyleClass().add("appointment-list-cell-root");

            Rectangle statusDot = new Rectangle(10, 10);
            statusDot.setArcWidth(5);
            statusDot.setArcHeight(5);

            switch (appointment.getStatus()) {
                case Appointment.STATUS_NEW:
                    statusDot.getStyleClass().add("appointment-status-dot-new");
                    break;
                case Appointment.STATUS_CONFIRMED:
                    statusDot.getStyleClass().add("appointment-status-dot-confirmed");
                    break;
                case Appointment.STATUS_CANCELLED:
                    statusDot.getStyleClass().add("appointment-status-dot-cancelled");
                    break;
                case Appointment.STATUS_COMPLETED:
                    statusDot.getStyleClass().add("appointment-status-dot-completed");
                    break;
                default:
                    statusDot.getStyleClass().add("appointment-status-dot-completed");
            }

            VBox infoBox = new VBox(2);

            Client client = appointment.getClient();
            String clientName = client != null ? client.getFullName() : "Клиент не найден";

            Label nameLabel = new Label(clientName);
            nameLabel.getStyleClass().add("appointment-list-client");

            Label detailLabel = new Label(appointment.getServiceName() + " | Мастер: " + appointment.getMasterName());
            detailLabel.getStyleClass().add("appointment-list-info");

            infoBox.getChildren().addAll(nameLabel, detailLabel);

            Label timeLabel = new Label(appointment.getTime());
            timeLabel.getStyleClass().add("appointment-list-time");
            timeLabel.setAlignment(Pos.CENTER_RIGHT);

            // "Пружина" между infoBox и timeLabel: растягивается, прижимая время к правому краю.
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            root.getChildren().addAll(statusDot, infoBox, spacer, timeLabel);
            setGraphic(root);
        }
    }
}