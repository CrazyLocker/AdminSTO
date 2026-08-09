package com.autoservice;

import java.util.ArrayList;
import java.util.List;

/**
 * Модель заказа на выполнение работ в СТО.
 * 
 * Ответственность: хранение данных заказа (клиент, статус, услуги с ценами,
 * запчасти с количеством, даты, пробег, примечания) и автоматический
 * пересчёт общей стоимости при изменении состава услуг и запчастей.
 * 
 * Зависимости: Client, SparePart.
 * 
 * Особенности: заказ может содержать несколько услуг (списки {@code services}
 * и {@code servicePrices}) и несколько запчастей с количеством; общая сумма
 * пересчитывается методом {@link #recalculateTotal()}. Содержит dirty-флаг для
 * отслеживания изменений перед сохранением в БД.
 * 
 * @author AdminSTO Team
 * @since 1.0
 * @see Client
 * @see SparePart
 * @see DataStore
 */
public class WorkOrder {
    /** Статус: новый заказ. */
    public static final String STATUS_NEW = "Новый";
    /** Статус: заказ в работе. */
    public static final String STATUS_IN_PROGRESS = "В работе";
    /** Статус: заказ закрыт (выполнен). */
    public static final String STATUS_CLOSED = "Закрыт";

    /** Идентификатор заказа (строка). */
    private String id;
    /** Клиент, оформивший заказ. */
    private Client client;
    /** Текущий статус заказа (см. константы STATUS_*). */
    private String status;
    /** Общая стоимость заказа (услуги + запчасти). */
    private double total;
    /** Дата создания заказа (строка). */
    private String createdDate;
    /** Названия услуг в заказе. */
    private List<String> services = new ArrayList<>();
    /** Цены услуг (соответствует {@link #services}). */
    private List<Double> servicePrices = new ArrayList<>();
    /** Запчасти в заказе. */
    private List<SparePart> spareParts = new ArrayList<>();
    /** Количество каждой запчасти (соответствует {@link #spareParts}). */
    private List<Double> sparePartQuantities = new ArrayList<>();
    /** Идентификаторы услуг (0, если услуга не привязана к каталогу). */
    private List<Integer> serviceIds = new ArrayList<>();
    /** Дата закрытия заказа (пустая, если не закрыт). */
    private String closedDate = "";
    /** Примечания к заказу. */
    private String notes = "";
    /** Пробег автомобиля на момент заказа. */
    private int mileage = 0;
    /** Идентификатор клиента. */
    private int clientId = 0;
    /** Модель автомобиля. */
    private String carModel = "";
    /** Госномер автомобиля. */
    private String carNumber = "";
    /** Флаг наличия несохранённых изменений. */
    private boolean dirty = false;

    // ==================== КОНСТРУКТОРЫ ====================

    /** Создаёт пустой заказ со статусом «Новый» и признаком изменения. */
    public WorkOrder() {
        this.id = "";
        this.client = null;
        this.status = STATUS_NEW;
        this.total = 0;
        this.createdDate = "";
        this.dirty = true;
    }

    /**
     * Создаёт заказ для указанного клиента.
     * 
     * @param client клиент, оформляющий заказ
     */
    public WorkOrder(Client client) {
        this();
        this.client = client;
        this.clientId = client.getId();
    }

    /**
     * Полный конструктор заказа (обычно используется при загрузке из БД).
     * 
     * @param id          идентификатор заказа
     * @param client      клиент
     * @param status      статус заказа
     * @param total       общая стоимость
     * @param createdDate дата создания
     */
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

    /**
     * Добавляет услугу в заказ по названию и цене (без привязки к каталогу
     * услуг — id услуги = 0). После добавления пересчитывается общая сумма.
     * 
     * @param name  название услуги
     * @param price цена услуги
     */
    public void addService(String name, double price) {
        this.services.add(name);
        this.servicePrices.add(price);
        this.serviceIds.add(0);
        this.dirty = true;
        recalculateTotal();
    }

    /**
     * Добавляет услугу в заказ с привязкой к каталогу услуг.
     * 
     * @param serviceId идентификатор услуги в каталоге
     * @param name      название услуги
     * @param price     цена услуги
     */
    public void addService(int serviceId, String name, double price) {
        this.services.add(name);
        this.servicePrices.add(price);
        this.serviceIds.add(serviceId);
        this.dirty = true;
        recalculateTotal();
    }

    /**
     * Удаляет услугу из заказа по индексу и пересчитывает сумму.
     * 
     * @param index индекс услуги в списке
     */
    public void removeService(int index) {
        if (index >= 0 && index < services.size()) {
            services.remove(index);
            servicePrices.remove(index);
            serviceIds.remove(index);
            this.dirty = true;
            recalculateTotal();
        }
    }

    /**
     * Добавляет запчасть в заказ с указанным количеством и пересчитывает сумму.
     * 
     * @param part     запчасть
     * @param quantity количество
     */
    public void addSparePart(SparePart part, double quantity) {
        this.spareParts.add(part);
        this.sparePartQuantities.add(quantity);
        this.dirty = true;
        recalculateTotal();
    }

    /**
     * Удаляет запчасть из заказа по индексу и пересчитывает сумму.
     * 
     * @param index индекс запчасти в списке
     */
    public void removeSparePart(int index) {
        if (index >= 0 && index < spareParts.size()) {
            spareParts.remove(index);
            sparePartQuantities.remove(index);
            this.dirty = true;
            recalculateTotal();
        }
    }

    /**
     * Пересчитывает общую стоимость заказа как сумму цен услуг и стоимости
     * запчастей (розничная цена × количество). Если сумма изменилась — заказ
     * помечается как изменённый.
     */
    public void recalculateTotal() {
        double newTotal = 0;
        for (Double price : servicePrices) {
            newTotal += price;
        }
        for (int i = 0; i < spareParts.size(); i++) {
            newTotal += spareParts.get(i).getRetailPrice() * sparePartQuantities.get(i);
        }
        if (this.total != newTotal) {
            this.total = newTotal;
            this.dirty = true;
        }
    }

    /**
     * Снимает флаг изменения с заказа и всех его запчастей. Вызывается после
     * успешного сохранения в БД.
     */
    public void markClean() {
        this.dirty = false;
        for (SparePart part : spareParts) {
            if (part != null) part.markClean();
        }
    }

    /**
     * Возвращает строковое представление заказа «id - ФИО клиента (статус)».
     * 
     * @return строка с данными заказа
     * @throws NullPointerException если клиент не задан (client == null)
     */
    @Override
    public String toString() {
        return id + " - " + client.getFullName() + " (" + status + ")";
    }
}