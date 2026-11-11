package com.autoservice;

import com.autoservice.services.LoyaltyService;

import java.util.ArrayList;
import java.util.List;

public class WorkOrder {
    public static final String STATUS_NEW = "Новый";
    public static final String STATUS_IN_PROGRESS = "В работе";
    public static final String STATUS_CLOSED = "Закрыт";

    private String id;
    /**
     * Клиент заказа. Может быть {@code null} у нового заказа, которому клиент
     * ещё не назначен; для сохранения в БД клиент обязателен.
     */
    private Client client;
    private String status;
    private double total;
    private String createdDate;
    private List<String> services = new ArrayList<>();
    private List<Double> servicePrices = new ArrayList<>();
    private List<SparePart> spareParts = new ArrayList<>();
    private List<Double> sparePartQuantities = new ArrayList<>();
    private List<Integer> serviceIds = new ArrayList<>();
    private String closedDate = "";
    private String notes = "";
    private int mileage = 0;
    private int clientId = 0;
    private String carModel = "";
    private String carNumber = "";
    private boolean dirty = false;

    // ==================== КОНСТРУКТОРЫ ====================

    public WorkOrder() {
        this.id = "";
        this.client = null;
        this.status = STATUS_NEW;
        this.total = 0;
        this.createdDate = "";
        this.dirty = true;
    }

    public WorkOrder(Client client) {
        this();
        this.client = client;
        this.clientId = client != null ? client.getId() : 0;
    }

    public WorkOrder(String id, Client client, String status, double total, String createdDate) {
        this.id = id;
        this.client = client;
        this.status = status;
        this.total = total;
        this.createdDate = createdDate;
        this.dirty = false;
    }

    // ==================== ГЕТТЕРЫ ====================

    public String getId() { return id; }
    public Client getClient() { return client; }
    public String getStatus() { return status; }
    public double getTotal() { return total; }
    public String getCreatedDate() { return createdDate; }
    public List<Integer> getServiceIds() { return serviceIds; }
    public String getClosedDate() { return closedDate; }
    public String getNotes() { return notes; }
    public int getMileage() { return mileage; }
    public List<String> getServices() { return services; }
    public List<Double> getServicePrices() { return servicePrices; }
    public List<SparePart> getSpareParts() { return spareParts; }
    public List<Double> getSparePartQuantities() { return sparePartQuantities; }
    public boolean isDirty() { return dirty; }

    // ==================== СЕТТЕРЫ ====================

    public void setId(String id) {
        this.id = id;
        this.dirty = true;
    }

    public void setClient(Client client) {
        this.client = client;
        this.dirty = true;
    }

    public void setStatus(String status) {
        this.status = status;
        this.dirty = true;
    }

    public void setTotal(double total) {
        this.total = total;
        this.dirty = true;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
        this.dirty = true;
    }

    public void setClosedDate(String closedDate) {
        this.closedDate = closedDate;
        this.dirty = true;
    }

    public void setNotes(String notes) {
        this.notes = notes;
        this.dirty = true;
    }

    public void setMileage(int mileage) {
        this.mileage = mileage;
        this.dirty = true;
    }

    public int getClientId() { return clientId; }
    public void setClientId(int clientId) {
        this.clientId = clientId;
        this.dirty = true;
    }

    public String getCarModel() { return carModel; }
    public void setCarModel(String carModel) {
        this.carModel = carModel;
        this.dirty = true;
    }

    public String getCarNumber() { return carNumber; }
    public void setCarNumber(String carNumber) {
        this.carNumber = carNumber;
        this.dirty = true;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    // ==================== МЕТОДЫ ====================

    public void addService(String name, double price) {
        this.services.add(name);
        this.servicePrices.add(price);
        this.serviceIds.add(0);
        this.dirty = true;
        recalculateTotal();
    }

    public void addService(int serviceId, String name, double price) {
        this.services.add(name);
        this.servicePrices.add(price);
        this.serviceIds.add(serviceId);
        this.dirty = true;
        recalculateTotal();
    }

    /**
     * Добавить услугу при загрузке заказа из базы без пересчёта итоговой
     * суммы: сохранённая сумма заказа уже учитывает скидки на момент
     * оформления и не должна меняться при последующем чтении.
     * @param serviceId идентификатор услуги
     * @param name название услуги
     * @param price цена услуги без скидки
     */
    public void addLoadedService(int serviceId, String name, double price) {
        this.services.add(name);
        this.servicePrices.add(price);
        this.serviceIds.add(serviceId);
    }

    public void removeService(int index) {
        if (index >= 0 && index < services.size()) {
            services.remove(index);
            servicePrices.remove(index);
            serviceIds.remove(index);
            this.dirty = true;
            recalculateTotal();
        }
    }

    public void addSparePart(SparePart part, double quantity) {
        this.spareParts.add(part);
        this.sparePartQuantities.add(quantity);
        this.dirty = true;
        recalculateTotal();
    }

    /**
     * Добавить запчасть при загрузке заказа из базы без пересчёта итоговой
     * суммы (см. {@link #addLoadedService(int, String, double)}).
     * @param part запчасть
     * @param quantity количество
     */
    public void addLoadedSparePart(SparePart part, double quantity) {
        this.spareParts.add(part);
        this.sparePartQuantities.add(quantity);
    }

    public void removeSparePart(int index) {
        if (index >= 0 && index < spareParts.size()) {
            spareParts.remove(index);
            sparePartQuantities.remove(index);
            this.dirty = true;
            recalculateTotal();
        }
    }

    /**
     * Пересчитать сумму заказа по прайсу позиций без скидок по лояльности.
     *
     * <p>Скидки применяются отдельно методом {@link #applyLoyaltyDiscounts()},
     * поэтому сумма сохраняется в базе уже с их учётом.</p>
     */
    public void recalculateTotal() {
        double newTotal = getSubtotal();
        if (Double.compare(this.total, newTotal) != 0) {
            this.total = newTotal;
            this.dirty = true;
        }
    }

    /**
     * Сумма позиций заказа по прайсу, без скидок по лояльности.
     * @return сумма услуг и запчастей по прайсу, руб.
     */
    public double getSubtotal() {
        double subtotal = 0;
        for (Double price : servicePrices) {
            subtotal += price;
        }
        for (int i = 0; i < spareParts.size(); i++) {
            SparePart part = spareParts.get(i);
            double quantity = i < sparePartQuantities.size() ? sparePartQuantities.get(i) : 1.0;
            subtotal += part.getRetailPrice() * quantity;
        }
        return subtotal;
    }

    /**
     * Процент скидки по лояльности на услугу в строке заказа.
     * @param index индекс услуги
     * @return процент скидки; {@code 0}, если скидка не действует
     */
    public double getServiceDiscountPercent(int index) {
        if (index < 0 || index >= servicePrices.size()) {
            return 0.0;
        }
        return LoyaltyService.getDiscountPercent(client, LoyaltyService.resolveServiceCategory(this, index));
    }

    /**
     * Процент скидки по лояльности на запчасть в строке заказа.
     * @param index индекс запчасти
     * @return процент скидки; {@code 0}, если скидка не действует
     */
    public double getSparePartDiscountPercent(int index) {
        if (index < 0 || index >= spareParts.size()) {
            return 0.0;
        }
        return LoyaltyService.getDiscountPercent(client, LoyaltyService.resolveSparePartCategory(spareParts.get(index)));
    }

    /**
     * Рассчитать сумму заказа с учётом скидок по лояльности.
     * @return сумма к оплате, руб.
     */
    public double calculateLoyaltyTotal() {
        double result = 0;
        for (int i = 0; i < servicePrices.size(); i++) {
            double price = servicePrices.get(i);
            result += price - LoyaltyService.calculateDiscount(client,
                    LoyaltyService.resolveServiceCategory(this, i), price);
        }
        for (int i = 0; i < spareParts.size(); i++) {
            SparePart part = spareParts.get(i);
            double quantity = i < sparePartQuantities.size() ? sparePartQuantities.get(i) : 1.0;
            double amount = part.getRetailPrice() * quantity;
            result += amount - LoyaltyService.calculateDiscount(client,
                    LoyaltyService.resolveSparePartCategory(part), amount);
        }
        return result;
    }

    /**
     * Применить скидки по лояльности к итоговой сумме заказа.
     */
    public void applyLoyaltyDiscounts() {
        setTotal(calculateLoyaltyTotal());
    }

    /**
     * Сумма скидок по лояльности, уже учтённая в итоговой сумме заказа.
     * @return сумма скидки, руб.
     */
    public double getDiscountTotal() {
        return Math.max(0.0, getSubtotal() - total);
    }

    public void markClean() {
        this.dirty = false;
        for (SparePart part : spareParts) {
            if (part != null) part.markClean();
        }
    }

    @Override
    public String toString() {
        String clientName = client != null ? client.getFullName() : AppConstants.NO_CLIENT;
        return id + " - " + clientName + " (" + status + ")";
    }
}