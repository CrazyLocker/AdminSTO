package com.autoservice;

import com.autoservice.utils.ThemeManager;
import com.autoservice.utils.ThemePreferences;
import com.autoservice.views.AppointmentView;
import com.autoservice.views.DashboardView;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Регрессия «записи не отображаются в календаре».
 *
 * <p>Сценарий повторяет ручной тест: база наполнена тестовыми данными, у
 * клиента телефон записан с форматированием ({@code +7 900 111-00-42}),
 * запись создаётся тем же путём, что и в диалоге редактирования заказа
 * ({@code DataStore.addAppointment}). До исправления вставка падала на
 * {@code CHECK(client_id > 0)}, поэтому запись не появлялась ни на дашборде,
 * ни на вкладке «Запись».</p>
 *
 * <p>Тест сохраняет снимки дашборда и вкладки «Запись» в
 * {@code .github/validation-evidence/bugfix-1}; суффикс каталога задаётся
 * переменной окружения {@code EVIDENCE_SUFFIX} ({@code after} по умолчанию),
 * чтобы получить настоящий Before на коде до исправления.</p>
 */
@Tag(TestTags.UI)
@EnabledIfSystemProperty(named = "javafx.headless", matches = "true")
class AppointmentVisibilityUITest extends BaseUITest {

    private static final Path EVIDENCE_DIR = Path.of(".github", "validation-evidence",
            "bugfix-1", System.getenv().getOrDefault("EVIDENCE_SUFFIX", "after"));

    private static final String CLIENT_NAME = "Анна";
    private static final String CLIENT_LAST_NAME = "Смирнова";
    private static final String CLIENT_PHONE = "+7 900 111-00-42";

    @TempDir
    Path tempDir;

    private String previousThemeFile;

    @BeforeEach
    void setUpThemeFile() {
        previousThemeFile = System.getProperty(ThemePreferences.THEME_FILE_PROPERTY);
        System.setProperty(ThemePreferences.THEME_FILE_PROPERTY,
                tempDir.resolve("config").resolve("theme.txt").toString());
        resetDashboardSingleton();
    }

    @AfterEach
    void tearDown() {
        if (previousThemeFile == null) {
            System.clearProperty(ThemePreferences.THEME_FILE_PROPERTY);
        } else {
            System.setProperty(ThemePreferences.THEME_FILE_PROPERTY, previousThemeFile);
        }
    }

    @Test
    @DisplayName("Запись, созданная как из редактирования заказа, видна на дашборде и вкладке")
    void appointmentIsVisibleOnDashboardAndAppointmentTab() throws Exception {
        seedLegacyClientOrderAndAppointment();

        ThemePreferences.saveTheme("primer-light");
        runOnFxThread(ThemeManager::applySavedTheme);
        createStage();
        runOnFxThread(() -> ThemeManager.applySceneStyles(currentScene));

        Stage stage = getOnFxThread(() -> (Stage) currentScene.getWindow());
        runOnFxThread(() -> {
            stage.setWidth(1200);
            stage.setHeight(700);
            stage.centerOnScreen();
            ((TabPane) findNode("mainTabPane")).getSelectionModel().select(0);
            DashboardView.refresh();
        });
        settle();
        WritableImage dashboard = getOnFxThread(() -> currentScene.snapshot((WritableImage) null));
        writeEvidence("primer-light-dashboard", dashboard);

        runOnFxThread(() -> {
            ((TabPane) findNode("mainTabPane")).getSelectionModel().select(6);
            AppointmentView.refresh();
        });
        settle();
        WritableImage appointmentTab = getOnFxThread(() -> currentScene.snapshot((WritableImage) null));
        writeEvidence("primer-light-appointment-tab", appointmentTab);

        assertThat(DataStore.getAppointments())
                .as("Запись сохранена, несмотря на форматированный телефон клиента")
                .hasSize(1);

        // Дашборд: в календаре текущего месяца отмечен день записи.
        Region calendarCard = (Region) lookupNode("appointmentCalendarCard");
        assertThat(countCircles(calendarCard))
                .as("В календаре дашборда отмечена дата записи")
                .isGreaterThanOrEqualTo(1);

        // Вкладка «Запись»: в сетке недели видна фамилия клиента.
        List<String> labels = labelTexts();
        assertThat(labels)
                .as("Запись отображается в расписании вкладки «Запись»")
                .anyMatch(text -> text.contains(CLIENT_LAST_NAME));
    }

    // ==================== ПОДГОТОВКА ДАННЫХ ====================

    /**
     * Клиент вставлен напрямую, минуя нормализацию {@code addClient}: так
     * выглядят демо-данные, после которых запись не сохранялась.
     */
    private void seedLegacyClientOrderAndAppointment() throws Exception {
        int clientId = insertClient();
        String today = DateUtils.formatDateForDB(LocalDate.now());
        insertOrder("ZAK-14/11/26-9001", clientId, today);
        DataStore.load();

        WorkOrder order = DataStore.getOrders().get(0);
        Appointment appointment = new Appointment(
                order.getClient(), "Саныч", "Диагностика", today, "10:00");
        appointment.setOrderId(order.getId());
        DataStore.addAppointment(appointment);
    }

    private int insertClient() throws Exception {
        String sql = "INSERT INTO clients (name, last_name, phone, car_model, car_number, "
                + "last_repair_date, maintenance_stars, tuning_total) "
                + "VALUES (?, ?, ?, 'Toyota Camry', 'А111АА163', '', 0, 0)";
        try (Connection conn = DatabaseFactory.getDatabase().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, CLIENT_NAME);
            pstmt.setString(2, CLIENT_LAST_NAME);
            pstmt.setString(3, CLIENT_PHONE);
            pstmt.executeUpdate();
        }
        try (Connection conn = DatabaseFactory.getDatabase().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT id FROM clients WHERE name = ? AND phone = ?")) {
            pstmt.setString(1, CLIENT_NAME);
            pstmt.setString(2, CLIENT_PHONE);
            try (ResultSet rs = pstmt.executeQuery()) {
                assertThat(rs.next()).isTrue();
                return rs.getInt("id");
            }
        }
    }

    private void insertOrder(String orderId, int clientId, String date) throws Exception {
        String sql = "INSERT INTO orders (id, client_id, status, total, created_date, "
                + "mileage, closed_date, notes) VALUES (?, ?, ?, 0, ?, 0, '', '')";
        try (Connection conn = DatabaseFactory.getDatabase().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, orderId);
            pstmt.setInt(2, clientId);
            pstmt.setString(3, WorkOrder.STATUS_NEW);
            pstmt.setString(4, date);
            pstmt.executeUpdate();
        }
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    private Node lookupNode(String id) {
        return getOnFxThread(() -> currentScene.lookup("#" + id));
    }

    private int countCircles(Region parent) {
        return getOnFxThread(() -> {
            int count = 0;
            for (Node node : parent.lookupAll("*")) {
                if (node instanceof Circle) {
                    count++;
                }
            }
            return count;
        });
    }

    private List<String> labelTexts() {
        List<String> texts = new ArrayList<>();
        runOnFxThread(() -> {
            for (Node node : currentScene.getRoot().lookupAll(".label")) {
                if (node instanceof Label label && label.getText() != null) {
                    texts.add(label.getText());
                }
            }
        });
        return texts;
    }

    private void settle() {
        for (int i = 0; i < 8; i++) {
            waitForFxEvents();
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void writeEvidence(String fileName, WritableImage image) {
        try {
            Files.createDirectories(EVIDENCE_DIR);
            int width = (int) image.getWidth();
            int height = (int) image.getHeight();
            BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            PixelReader reader = image.getPixelReader();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    buffered.setRGB(x, y, reader.getArgb(x, y));
                }
            }
            ImageIO.write(buffered, "png", EVIDENCE_DIR.resolve(fileName + ".png").toFile());
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось сохранить скриншот " + fileName, e);
        }
    }

    /**
     * Дашборд — синглтон: между тестами возвращаем null, чтобы сценарий
     * построил его заново уже с другой темой.
     */
    private static void resetDashboardSingleton() {
        try {
            Field instance = DashboardView.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Не удалось сбросить синглтон DashboardView", e);
        }
    }
}
