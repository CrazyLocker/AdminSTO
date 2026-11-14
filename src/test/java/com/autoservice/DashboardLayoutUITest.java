package com.autoservice;

import com.autoservice.builders.SparePartBuilder;
import com.autoservice.utils.ThemeManager;
import com.autoservice.utils.ThemePreferences;
import com.autoservice.views.DashboardView;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * UI-тест вёрстки дашборда на «боевых» по размеру данных: семизначная
 * выручка, длинные названия услуг и запчасти с низким остатком.
 *
 * <p>Проверяет четыре регрессии из задачи «Косяки №1»: сумма в круге
 * «Выручка» не выходит за его внутренний диаметр, календарь записей занимает
 * всю ширину колонки, названия в «ТОП-5 услуг» не обрезаются и их прогресс-бары
 * начинаются с одного левого края, а в блоке «Низкий остаток запчастей» нет
 * битых Unicode-символов. Снимки сохраняются в
 * {@code build/ui-test-evidence/dashboard-layout} и служат визуальным
 * подтверждением в PR.</p>
 */
@Tag(TestTags.UI)
@EnabledIfSystemProperty(named = "javafx.headless", matches = "true")
class DashboardLayoutUITest extends BaseUITest {

    private static final Path EVIDENCE_DIR =
            Path.of("build", "ui-test-evidence", "dashboard-layout");

    /** Сумма закрытых заказов: 500 000 + 300 000 + 74 850. */
    private static final double TOTAL_REVENUE = 874_850;
    private static final String REVENUE_TEXT = "874 850 ₽";

    /** Внутренний диаметр кольца диаграммы: (66 - 16/2) * 2 = 116. */
    private static final double DONUT_INNER_DIAMETER = 116;
    private static final double DONUT_TEXT_PADDING = 16;

    private static final String[] TOP_SERVICE_NAMES = {
            "Компьютерная диагностика двигателя",
            "Замена приводных ремней",
            "Замена масла в АКПП",
            "Ремонт тормозной системы",
            "Шиномонтаж и балансировка"
    };

    private static Locale previousLocale;

    @TempDir
    Path tempDir;

    private String previousThemeFile;

    @BeforeAll
    static void useRussianFormatting() {
        previousLocale = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("ru-RU"));
    }

    @AfterAll
    static void restoreLocale() {
        if (previousLocale != null) {
            Locale.setDefault(previousLocale);
        }
    }

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
    @DisplayName("Светлая тема: данные не ломают вёрстку дашборда")
    void dashboardLayoutFitsDataInLightTheme() {
        seedDashboardData(true);
        WritableImage image = launchDashboard("primer-light");

        writeEvidence("primer-light-dashboard", image);
        assertDashboardLayout();
    }

    @Test
    @DisplayName("Тёмная тема: данные не ломают вёрстку дашборда")
    void dashboardLayoutFitsDataInDarkTheme() {
        seedDashboardData(true);
        WritableImage image = launchDashboard("dracula");

        writeEvidence("dracula-dashboard", image);
        assertDashboardLayout();
    }

    @Test
    @DisplayName("Пустой блок низкого остатка: читаемая галочка вместо □")
    void lowStockEmptyStateUsesReadableCheckmark() {
        seedDashboardData(false);
        WritableImage image = launchDashboard("primer-light");

        writeEvidence("primer-light-low-stock-empty", image);
        Label empty = (Label) lookupNode("lowStockEmpty");
        assertThat(empty.getText())
                .as("Подпись пустого блока низкого остатка")
                .isEqualTo("Все запчасти в норме");
        assertThat(empty.getText())
                .as("В подписи нет битых символов")
                .doesNotContain("✅")
                .doesNotContain("□")
                .doesNotContain("\uFE0F");
        assertThat(lookupNode("lowStockCheckIcon"))
                .as("Галочка отрисована векторной иконкой")
                .isNotNull();
    }

    // ==================== ПРОВЕРКИ ВЁРСТКИ ====================

    private void assertDashboardLayout() {
        assertRevenueFitsDonut();
        assertCalendarFillsColumn();
        assertTopServicesUseFullWidth();
        assertLowStockHasNoBrokenGlyphs();
    }

    private void assertRevenueFitsDonut() {
        Label amount = (Label) lookupNode("revenueDonutAmount");
        assertThat(amount.getText().replace('\u00A0', ' '))
                .as("Сумма в круге «Выручка»")
                .isEqualTo(REVENUE_TEXT);
        assertThat(textWidth(amount))
                .as("Ширина суммы «%s» должна помещаться во внутренний диаметр круга",
                        amount.getText())
                .isLessThanOrEqualTo(DONUT_INNER_DIAMETER - DONUT_TEXT_PADDING);
    }

    private void assertCalendarFillsColumn() {
        Region calendar = (Region) lookupNode("appointmentCalendarCard");
        Region leftColumn = (Region) lookupNode("dashboardLeftColumn");
        assertThat(calendar.getWidth())
                .as("Ширина календаря равна ширине левой колонки")
                .isCloseTo(leftColumn.getWidth(), within(0.5));
    }

    private void assertTopServicesUseFullWidth() {
        Region bars = (Region) lookupNode("topServicesBars");
        double firstBarX = -1;
        for (int i = 0; i < TOP_SERVICE_NAMES.length; i++) {
            Label name = (Label) lookupNode("topServiceName" + i);
            assertThat(name.getText())
                    .as("Название услуги №%d не обрезано многоточием", i + 1)
                    .doesNotContain("...");
            assertThat(textWidth(name))
                    .as("Название «%s» целиком помещается в колонку", name.getText())
                    .isLessThanOrEqualTo(name.getWidth() + 0.5);

            StackPane barContainer = (StackPane) lookupNode("topServiceBar" + i);
            assertThat(barContainer.getBoundsInParent().getMinX())
                    .as("Строка услуг №%d начинается в общей колонке", i + 1)
                    .isGreaterThanOrEqualTo(bars.getInsets().getLeft());
            if (firstBarX < 0) {
                firstBarX = barContainer.getBoundsInParent().getMinX();
            } else {
                assertThat(barContainer.getBoundsInParent().getMinX())
                        .as("Прогресс-бары выровнены по левому краю")
                        .isCloseTo(firstBarX, within(0.5));
            }

            Node fill = barContainer.getChildren().get(0);
            assertThat(fill.getBoundsInParent().getMinX())
                    .as("Заливка прогресс-бара №%d прижата к левому краю", i + 1)
                    .isCloseTo(0, within(0.5));
        }
    }

    private void assertLowStockHasNoBrokenGlyphs() {
        Region card = (Region) lookupNode("lowStockCard");
        List<String> texts = new ArrayList<>();
        for (Node node : card.lookupAll(".label")) {
            if (node instanceof Label label) {
                texts.add(label.getText());
            }
        }
        assertThat(texts).as("В блоке низкого остатка есть подписи").isNotEmpty();
        for (String text : texts) {
            assertThat(text)
                    .as("Текст «%s» не содержит битых Unicode-символов", text)
                    .doesNotContain("✅")
                    .doesNotContain("⚠")
                    .doesNotContain("□")
                    .doesNotContain("\uFE0F");
        }
        Label title = (Label) lookupNode("lowStockTitle");
        assertThat(title.getFont().getSize())
                .as("Заголовок блока низкого остатка увеличен")
                .isGreaterThanOrEqualTo(14);
    }

    // ==================== ДАННЫЕ И СНИМКИ ====================

    /**
     * Наполняет тестовую БД данными, при которых видны все четыре дефекта:
     * выручка 874 850 ₽, длинные названия услуг, три запчасти с низким
     * остатком и записи в календаре.
     */
    private static void seedDashboardData(boolean withLowStock) {
        Client client = new Client("Андрей", "Соколов", "+79001112233",
                "Haval Jolion", "А111ВС163");
        DataStore.addClient(client);
        Client savedClient = DataStore.getClients().stream()
                .filter(c -> "+79001112233".equals(c.getPhone()))
                .findFirst()
                .orElseThrow();
        DataStore.addClient(new Client("Мария", "Петрова", "+79002223344",
                "Haval F7", "В222ЕЕ163"));
        DataStore.addClient(new Client("Олег", "Сидоров", "+79003334455",
                "Haval Dargo", "С333КК163"));

        LocalDate today = LocalDate.now();
        addClosedOrder(savedClient, 500_000, today.minusMonths(2),
                TOP_SERVICE_NAMES[0], TOP_SERVICE_NAMES[1],
                TOP_SERVICE_NAMES[2], TOP_SERVICE_NAMES[3]);
        addClosedOrder(savedClient, 300_000, today.minusMonths(1),
                TOP_SERVICE_NAMES[0], TOP_SERVICE_NAMES[1],
                TOP_SERVICE_NAMES[2], TOP_SERVICE_NAMES[4]);
        addClosedOrder(savedClient, 74_850, today,
                TOP_SERVICE_NAMES[0], TOP_SERVICE_NAMES[1]);
        addOrder(savedClient, WorkOrder.STATUS_IN_PROGRESS, 120_000, today,
                TOP_SERVICE_NAMES[0], TOP_SERVICE_NAMES[4]);
        addOrder(savedClient, WorkOrder.STATUS_NEW, 50_000, today,
                TOP_SERVICE_NAMES[3]);

        if (withLowStock) {
            DataStore.addSparePart(new SparePartBuilder()
                    .withName("Масляный фильтр").withStock(3).withMinStock(10)
                    .withUnitType("шт").withLocation("A-01").build());
            DataStore.addSparePart(new SparePartBuilder()
                    .withName("Тормозная колодка передняя").withStock(1).withMinStock(8)
                    .withUnitType("шт").withLocation("B-03").build());
            DataStore.addSparePart(new SparePartBuilder()
                    .withName("Свеча зажигания").withStock(2).withMinStock(12)
                    .withUnitType("шт").withLocation("C-02").build());
        }

        String[] masters = {"Иван", "Сергей", "Анна"};
        int[] days = {today.getDayOfMonth(), 1, 3, 8, 12, 18, 24};
        for (int i = 0; i < days.length; i++) {
            int day = Math.min(days[i], today.lengthOfMonth());
            DataStore.addAppointment(new Appointment(savedClient,
                    masters[i % masters.length], TOP_SERVICE_NAMES[i % TOP_SERVICE_NAMES.length],
                    DateUtils.formatDateForDB(today.withDayOfMonth(day)),
                    String.format("%02d:00", 9 + i)));
        }
    }

    private static void addClosedOrder(Client client, double total, LocalDate date,
                                       String... services) {
        addOrder(client, WorkOrder.STATUS_CLOSED, total, date, services);
    }

    private static void addOrder(Client client, String status, double total, LocalDate date,
                                 String... services) {
        WorkOrder order = new WorkOrder(client);
        for (String service : services) {
            order.addService(service, 1_000);
        }
        order.setStatus(status);
        order.setCreatedDate(DateUtils.formatDateForDB(date));
        order.setTotal(total);
        DataStore.addOrder(order);
    }

    private WritableImage launchDashboard(String themeId) {
        ThemePreferences.saveTheme(themeId);

        runOnFxThread(ThemeManager::applySavedTheme);
        createStage();
        runOnFxThread(() -> ThemeManager.applySceneStyles(currentScene));

        Stage stage = getOnFxThread(() -> (Stage) currentScene.getWindow());
        runOnFxThread(() -> {
            stage.setWidth(1200);
            stage.setHeight(700);
            stage.centerOnScreen();
            ((TabPane) findNode("mainTabPane")).getSelectionModel().select(0);
        });
        settle();

        return getOnFxThread(() -> currentScene.getRoot().snapshot(null, null));
    }

    private Node lookupNode(String id) {
        return getOnFxThread(() -> currentScene.lookup("#" + id));
    }

    private static double textWidth(Label label) {
        Text probe = new Text(label.getText());
        probe.setFont(label.getFont());
        return probe.getLayoutBounds().getWidth();
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
     * Дашборд — синглтон: между тестами возвращаем null, чтобы следующий
     * сценарий построил его заново уже с другой темой.
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
