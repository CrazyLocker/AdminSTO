package com.autoservice.services;

import com.autoservice.BaseTest;
import com.autoservice.Client;
import com.autoservice.DataStore;
import com.autoservice.Service;
import com.autoservice.SparePart;
import com.autoservice.TestTags;
import com.autoservice.WorkOrder;
import com.autoservice.model.ServiceCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты логики лояльности: звёзды ТО, стадии тюнинга, настройки и граничные
 * значения интервалов.
 */
@Tag(TestTags.INTEGRATION)
class LoyaltyServiceTest extends BaseTest {

    @BeforeEach
    void resetSettingsCache() {
        // BaseTest уже очистил app_settings, но статический кэш SettingService
        // может хранить настройки предыдущего теста — перечитываем его.
        SettingService.loadSettings();
    }

    // ==================== НАСТРОЙКИ ПО УМОЛЧАНИЮ ====================

    @Test
    void missingSettingsFallBackToDocumentedDefaults() {
        assertThat(LoyaltyService.getMaintenanceMinMileage()).isEqualTo(5_000);
        assertThat(LoyaltyService.getMaintenanceMaxMileage()).isEqualTo(10_000);
        assertThat(LoyaltyService.getMaintenanceMaxStars()).isEqualTo(5);
        assertThat(LoyaltyService.getMaintenanceDiscountPercent(2)).isZero();
        assertThat(LoyaltyService.getMaintenanceDiscountPercent(3)).isEqualTo(5.0);
        assertThat(LoyaltyService.getMaintenanceDiscountPercent(4)).isEqualTo(10.0);
        assertThat(LoyaltyService.getMaintenanceDiscountPercent(5)).isEqualTo(15.0);
    }

    @Test
    void invalidSettingValueFallsBackToDefault() {
        SettingService.setSettingValue(LoyaltyService.SETTING_MAINTENANCE_MIN_MILEAGE, "не число");

        assertThat(LoyaltyService.getMaintenanceMinMileage()).isEqualTo(5_000);
    }

    @Test
    void settingsOverrideDefaults() {
        SettingService.setSettingValue(LoyaltyService.SETTING_MAINTENANCE_MIN_MILEAGE, "1000");
        SettingService.setSettingValue(LoyaltyService.SETTING_MAINTENANCE_MAX_MILEAGE, "2000");
        SettingService.setSettingValue(LoyaltyService.SETTING_MAINTENANCE_MAX_STARS, "1");
        SettingService.setSettingValue(LoyaltyService.SETTING_MAINTENANCE_STARS3_DISCOUNT, "7");
        SettingService.setSettingValue(LoyaltyService.SETTING_TUNING_STAGE1_MIN_AMOUNT, "1000");
        SettingService.setSettingValue(LoyaltyService.SETTING_TUNING_STAGE1_DISCOUNT, "7,5");

        assertThat(LoyaltyService.getMaintenanceMinMileage()).isEqualTo(1_000);
        assertThat(LoyaltyService.getMaintenanceMaxMileage()).isEqualTo(2_000);
        assertThat(LoyaltyService.getMaintenanceMaxStars()).isEqualTo(1);
        assertThat(LoyaltyService.getMaintenanceDiscountPercent(3)).isEqualTo(7.0);
        assertThat(LoyaltyService.getTuningDiscountPercent(1_000)).isEqualTo(7.5);
    }

    // ==================== СКИДКИ ====================

    @Test
    void tuningStageBoundariesMatchDocumentation() {
        assertThat(LoyaltyService.getTuningDiscountPercent(0)).isZero();
        assertThat(LoyaltyService.getTuningDiscountPercent(29_999)).isZero();
        assertThat(LoyaltyService.getTuningDiscountPercent(30_000)).isEqualTo(5.0);
        assertThat(LoyaltyService.getTuningDiscountPercent(100_000)).isEqualTo(5.0);
        assertThat(LoyaltyService.getTuningDiscountPercent(100_001)).isEqualTo(10.0);
        assertThat(LoyaltyService.getTuningDiscountPercent(300_000)).isEqualTo(10.0);
        assertThat(LoyaltyService.getTuningDiscountPercent(300_001)).isEqualTo(15.0);
    }

    @Test
    void discountIsAppliedByLineCategory() {
        Client client = new Client("Иван", "Петров", "+79001234567", "Lada", "А123ВС163");
        client.setMaintenanceStars(3);
        client.setTuningTotal(30_000);

        assertThat(LoyaltyService.calculateDiscount(client, ServiceCategory.MAINTENANCE, 1_000))
                .isEqualTo(50.0);
        assertThat(LoyaltyService.calculateDiscount(client, ServiceCategory.TUNING, 1_000))
                .isEqualTo(50.0);
        assertThat(LoyaltyService.calculateDiscount(client, ServiceCategory.REGULAR, 1_000)).isZero();
        assertThat(LoyaltyService.calculateDiscount(client, null, 1_000)).isZero();
        assertThat(LoyaltyService.calculateDiscount(client, ServiceCategory.MAINTENANCE, 0)).isZero();
    }

    @Test
    void tuningStageMatchesDiscountStages() {
        assertThat(LoyaltyService.getTuningStage(0)).isZero();
        assertThat(LoyaltyService.getTuningStage(29_999)).isZero();
        assertThat(LoyaltyService.getTuningStage(30_000)).isEqualTo(1);
        assertThat(LoyaltyService.getTuningStage(100_000)).isEqualTo(1);
        assertThat(LoyaltyService.getTuningStage(100_001)).isEqualTo(2);
        assertThat(LoyaltyService.getTuningStage(300_000)).isEqualTo(2);
        assertThat(LoyaltyService.getTuningStage(300_001)).isEqualTo(3);
    }

    @Test
    void maintenanceStatusShowsStarsAndDiscount() {
        Client client = new Client("Иван", "Петров", "+79001234567", "Lada", "А123ВС163");

        assertThat(LoyaltyService.formatMaintenanceStars(0)).isEqualTo("☆☆☆☆☆");
        assertThat(LoyaltyService.getMaintenanceStatusText(client))
                .isEqualTo("☆☆☆☆☆ — скидки на работы нет");

        client.setMaintenanceStars(4);
        assertThat(LoyaltyService.formatMaintenanceStars(4)).isEqualTo("★★★★☆");
        assertThat(LoyaltyService.getMaintenanceStatusText(client))
                .isEqualTo("★★★★☆ — скидка 10% на работы");
        assertThat(LoyaltyService.getMaintenanceShortStatus(client))
                .isEqualTo("★★★★☆ · 10%");
    }

    @Test
    void tuningStatusShowsStageAndDiscount() {
        Client client = new Client("Иван", "Петров", "+79001234567", "Lada", "А123ВС163");
        assertThat(LoyaltyService.getTuningStageName(client)).isEqualTo("Без stage");
        assertThat(LoyaltyService.getTuningStatusText(client)).contains("скидки на тюнинг нет");

        client.setTuningTotal(150_000);
        assertThat(LoyaltyService.getTuningStageName(client)).isEqualTo("Stage 2");
        assertThat(LoyaltyService.getTuningStatusText(client))
                .isEqualTo("Stage 2 — скидка 10% на тюнинг");
        assertThat(LoyaltyService.getTuningShortStatus(client))
                .isEqualTo("Stage 2 · 10%");
    }

    // ==================== ЗВЁЗДЫ ТО ====================

    @Test
    void twoMaintenanceVisitsGiveTwoStarsWithoutDiscount() {
        Client client = createClient("Два ТО");
        Service maintenance = createService("ТО-2", 2_000, ServiceCategory.MAINTENANCE);

        LoyaltyService.LoyaltyUpdateResult first = LoyaltyService.applyCompletedOrder(
                createClosedOrder(client, 50_000, maintenance, 30));
        assertThat(first.starsEarned()).isEqualTo(1);

        Client afterFirst = DataStore.getClientById(client.getId());
        LoyaltyService.LoyaltyUpdateResult second = LoyaltyService.applyCompletedOrder(
                createClosedOrder(afterFirst, 60_000, maintenance, 20));

        assertThat(second.starsEarned()).isEqualTo(1);
        assertThat(second.maintenanceStars()).isEqualTo(2);
        Client afterSecond = DataStore.getClientById(client.getId());
        assertThat(afterSecond.getMaintenanceStars()).isEqualTo(2);
        assertThat(LoyaltyService.getMaintenanceDiscountPercent(afterSecond)).isZero();
    }

    @Test
    void fiveMaintenanceVisitsGiveMaxStarsAndDiscount() {
        Client client = createClient("Пять ТО");
        Service maintenance = createService("ТО-5", 2_000, ServiceCategory.MAINTENANCE);

        int previousMileage = 50_000;
        for (int visit = 1; visit <= 5; visit++) {
            Client current = DataStore.getClientById(client.getId());
            LoyaltyService.LoyaltyUpdateResult result = LoyaltyService.applyCompletedOrder(
                    createClosedOrder(current, previousMileage, maintenance, 40 - visit));
            assertThat(result.maintenanceStars()).isEqualTo(visit);
            previousMileage += 5_000;
        }

        Client afterFive = DataStore.getClientById(client.getId());
        assertThat(afterFive.getMaintenanceStars()).isEqualTo(5);
        assertThat(LoyaltyService.getMaintenanceDiscountPercent(afterFive)).isEqualTo(15.0);

        // Шестое ТО не может превысить максимум звёзд.
        LoyaltyService.LoyaltyUpdateResult sixth = LoyaltyService.applyCompletedOrder(
                createClosedOrder(afterFive, previousMileage, maintenance, 1));
        assertThat(sixth.starsEarned()).isZero();
        assertThat(sixth.maintenanceStars()).isEqualTo(5);
    }

    @Test
    void missedIntervalDoesNotAddStarButKeepsAccumulatedStars() {
        Client client = createClient("Пропуск интервала");
        Service maintenance = createService("ТО-интервал", 2_000, ServiceCategory.MAINTENANCE);

        LoyaltyService.applyCompletedOrder(createClosedOrder(client, 50_000, maintenance, 50));

        // Приехал раньше минимального интервала — звезда не начисляется.
        Client afterFirst = DataStore.getClientById(client.getId());
        LoyaltyService.LoyaltyUpdateResult early = LoyaltyService.applyCompletedOrder(
                createClosedOrder(afterFirst, 52_000, maintenance, 40));
        assertThat(early.starsEarned()).isZero();
        assertThat(early.maintenanceStars()).isEqualTo(1);

        // Приехал позже максимального интервала — звезда тоже не начисляется.
        Client afterEarly = DataStore.getClientById(client.getId());
        LoyaltyService.LoyaltyUpdateResult late = LoyaltyService.applyCompletedOrder(
                createClosedOrder(afterEarly, 70_000, maintenance, 30));
        assertThat(late.starsEarned()).isZero();
        assertThat(late.maintenanceStars()).isEqualTo(1);
    }

    @Test
    void intervalBoundariesAreInclusive() {
        assertThat(LoyaltyService.qualifiesForMaintenanceStar(60_000, 50_000)).isTrue();
        assertThat(LoyaltyService.qualifiesForMaintenanceStar(55_000, 50_000)).isTrue();
        assertThat(LoyaltyService.qualifiesForMaintenanceStar(54_999, 50_000)).isFalse();
        assertThat(LoyaltyService.qualifiesForMaintenanceStar(60_001, 50_000)).isFalse();
        assertThat(LoyaltyService.qualifiesForMaintenanceStar(0, 50_000)).isFalse();
    }

    // ==================== ТЮНИНГ ====================

    @Test
    void tuningStageTransitionFromStage1ToStage2() {
        Client client = createClient("Тюнинг");
        Service tuning = createService("Тюнинг Stage 1", 100_000, ServiceCategory.TUNING);

        LoyaltyService.LoyaltyUpdateResult stage1 = LoyaltyService.applyCompletedOrder(
                createClosedOrder(client, 0, tuning, 3));

        assertThat(stage1.tuningAdded()).isEqualTo(100_000);
        Client afterStage1 = DataStore.getClientById(client.getId());
        assertThat(afterStage1.getTuningTotal()).isEqualTo(100_000);
        assertThat(LoyaltyService.getTuningDiscountPercent(afterStage1)).isEqualTo(5.0);

        Service tuningExtra = createService("Тюнинг +1", 1, ServiceCategory.TUNING);
        LoyaltyService.LoyaltyUpdateResult stage2 = LoyaltyService.applyCompletedOrder(
                createClosedOrder(afterStage1, 0, tuningExtra, 2));

        assertThat(stage2.tuningTotal()).isEqualTo(100_001);
        Client afterStage2 = DataStore.getClientById(client.getId());
        assertThat(LoyaltyService.getTuningDiscountPercent(afterStage2)).isEqualTo(10.0);
    }

    @Test
    void tuningSparePartsAreAccumulated() {
        Client client = createClient("Тюнинг-детали");
        SparePart spoiler = createSparePart("Спойлер", 20_000, ServiceCategory.TUNING);

        WorkOrder order = new WorkOrder(client);
        order.setCreatedDate(LocalDate.now().toString());
        order.setStatus(WorkOrder.STATUS_CLOSED);
        order.addSparePart(spoiler, 2);
        DataStore.addOrder(order);

        LoyaltyService.LoyaltyUpdateResult result = LoyaltyService.applyCompletedOrder(order);

        assertThat(result.tuningAdded()).isEqualTo(40_000);
        assertThat(result.tuningTotal()).isEqualTo(40_000);
        assertThat(LoyaltyService.getTuningDiscountPercent(result.tuningTotal())).isEqualTo(5.0);
    }

    // ==================== СОХРАНЕНИЕ В БД ====================

    @Test
    void loyaltyValuesArePersistedForClient() {
        Client client = createClient("Сохранение");
        Service maintenance = createService("ТО-сохранение", 2_000, ServiceCategory.MAINTENANCE);

        LoyaltyService.applyCompletedOrder(createClosedOrder(client, 80_000, maintenance, 10));
        DataStore.load();

        Client reloaded = DataStore.getClientById(client.getId());
        assertThat(reloaded).isNotNull();
        assertThat(reloaded.getMaintenanceStars()).isEqualTo(1);
    }

    @Test
    void categoriesArePersistedForCatalogEntities() {
        createService("Тюнинг-услуга", 1_000, ServiceCategory.TUNING);
        createSparePart("Тюнинг-запчасть", 2_000, ServiceCategory.MAINTENANCE);

        Service service = DataStore.getServiceByName("Тюнинг-услуга");
        SparePart sparePart = findByPartName("Тюнинг-запчасть");

        assertThat(service).isNotNull();
        assertThat(service.getCategory()).isEqualTo(ServiceCategory.TUNING);
        assertThat(sparePart).isNotNull();
        assertThat(sparePart.getCategory()).isEqualTo(ServiceCategory.MAINTENANCE);
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    private Client createClient(String name) {
        Client client = new Client(name, "Тестов", "+79001234567", "Lada", "А123ВС163");
        DataStore.addClient(client);
        return DataStore.getClientById(client.getId());
    }

    private Service createService(String name, double price, ServiceCategory category) {
        Service service = new Service(name, price);
        service.setCategory(category);
        DataStore.addService(service);
        return DataStore.getServiceByName(name);
    }

    private SparePart createSparePart(String name, double retailPrice, ServiceCategory category) {
        SparePart part = new SparePart(name, 0, retailPrice, 10);
        part.setCategory(category);
        DataStore.addSparePart(part);
        return findByPartName(name);
    }

    private SparePart findByPartName(String name) {
        List<SparePart> parts = DataStore.getSpareParts();
        return parts.stream()
                .filter(part -> name.equals(part.getName()))
                .findFirst()
                .orElse(null);
    }

    private WorkOrder createClosedOrder(Client client, int mileage, Service service, int daysAgo) {
        WorkOrder order = new WorkOrder(client);
        order.setCreatedDate(LocalDate.now().minusDays(daysAgo).toString());
        order.setMileage(mileage);
        order.setStatus(WorkOrder.STATUS_CLOSED);
        if (service != null) {
            order.addService(service.getId(), service.getName(), service.getPrice());
        }
        DataStore.addOrder(order);
        return order;
    }
}
