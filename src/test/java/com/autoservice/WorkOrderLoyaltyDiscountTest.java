package com.autoservice;

import com.autoservice.model.ServiceCategory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты применения скидок лояльности к заказу и сохранения итоговой суммы.
 */
class WorkOrderLoyaltyDiscountTest extends BaseTest {

    @Test
    void loyaltyDiscountIsAppliedByLineCategory() {
        Client client = createClientWithLoyalty(4, 150_000);
        Service maintenance = createService("ТО-дисконт", 1_000, ServiceCategory.MAINTENANCE);
        Service regular = createService("Мойка-дисконт", 500, ServiceCategory.REGULAR);
        SparePart tuningPart = createSparePart("Спойлер-дисконт", 10_000, ServiceCategory.TUNING);

        WorkOrder order = new WorkOrder(client);
        order.addService(maintenance.getId(), maintenance.getName(), 1_000);
        order.addService(regular.getId(), regular.getName(), 500);
        order.addSparePart(tuningPart, 1);

        assertThat(order.getServiceDiscountPercent(0)).isEqualTo(10.0);
        assertThat(order.getServiceDiscountPercent(1)).isZero();
        assertThat(order.getSparePartDiscountPercent(0)).isEqualTo(10.0);
        assertThat(order.getSubtotal()).isEqualTo(11_500.0);
        assertThat(order.calculateLoyaltyTotal()).isEqualTo(10_400.0);

        order.applyLoyaltyDiscounts();

        assertThat(order.getTotal()).isEqualTo(10_400.0);
        assertThat(order.getDiscountTotal()).isEqualTo(1_100.0);
    }

    @Test
    void clientWithoutLoyaltyGetsNoDiscount() {
        Client client = createClientWithLoyalty(0, 0);
        Service maintenance = createService("ТО-без-скидки", 2_000, ServiceCategory.MAINTENANCE);

        WorkOrder order = new WorkOrder(client);
        order.addService(maintenance.getId(), maintenance.getName(), 2_000);
        order.applyLoyaltyDiscounts();

        assertThat(order.getTotal()).isEqualTo(2_000.0);
        assertThat(order.getDiscountTotal()).isZero();
    }

    @Test
    void savedDiscountedTotalIsNotRecalculatedWhenOrderIsReloaded() {
        Client client = createClientWithLoyalty(3, 0);
        Service maintenance = createService("ТО-сохранение-скидки", 1_000, ServiceCategory.MAINTENANCE);

        WorkOrder order = new WorkOrder(client);
        order.setStatus(WorkOrder.STATUS_NEW);
        order.addService(maintenance.getId(), maintenance.getName(), 1_000);
        order.applyLoyaltyDiscounts();
        assertThat(order.getTotal()).isEqualTo(950.0);

        DataStore.addOrder(order);
        WorkOrder loaded = findOrder(order.getId());
        assertThat(loaded).isNotNull();
        assertThat(loaded.getTotal()).isEqualTo(950.0);
        assertThat(loaded.getDiscountTotal()).isEqualTo(50.0);

        DataStore.load();
        WorkOrder reloaded = findOrder(order.getId());
        assertThat(reloaded).isNotNull();
        assertThat(reloaded.getTotal()).isEqualTo(950.0);
        assertThat(reloaded.getDiscountTotal()).isEqualTo(50.0);
    }

    private Client createClientWithLoyalty(int stars, double tuningTotal) {
        Client client = new Client("Иван", "Петров", "+79001234567", "Lada", "А123ВС163");
        DataStore.addClient(client);
        Client saved = DataStore.getClientById(client.getId());
        saved.setMaintenanceStars(stars);
        saved.setTuningTotal(tuningTotal);
        DataStore.updateClient(saved);
        return DataStore.getClientById(saved.getId());
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
        return DataStore.getSparePartById(part.getId());
    }

    private WorkOrder findOrder(String id) {
        return DataStore.getOrders().stream()
                .filter(order -> id != null && id.equals(order.getId()))
                .findFirst()
                .orElse(null);
    }
}
