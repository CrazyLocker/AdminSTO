package com.autoservice.services;

import com.autoservice.Client;
import com.autoservice.DataStore;
import com.autoservice.Service;
import com.autoservice.SparePart;
import com.autoservice.WorkOrder;
import com.autoservice.model.ServiceCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Логика двухтрековой системы лояльности (см. {@code docs/LOYALTY_SYSTEM.md}).
 *
 * <p>Скидки не суммируются: каждая строка заказа получает скидку своей
 * категории — {@link ServiceCategory#MAINTENANCE} считается по звёздам ТО,
 * {@link ServiceCategory#TUNING} — по накопленной сумме тюнинга,
 * {@link ServiceCategory#REGULAR} — без скидки.</p>
 *
 * <p>Все числовые параметры (интервал пробега, максимум звёзд, проценты и
 * пороги сумм) читаются из {@link SettingService}. Если настройка отсутствует
 * или содержит некорректное значение, используется значение по умолчанию из
 * документации. Ключи настроек собраны в константах этого класса, поэтому
 * будущий экран редактирования может переиспользовать их без изменений логики.</p>
 *
 * <p>Накопительные показатели клиента обновляются при закрытии заказа через
 * {@link #applyCompletedOrder(WorkOrder)}.</p>
 */
public final class LoyaltyService {

    private static final Logger logger = LoggerFactory.getLogger(LoyaltyService.class);

    // ==================== КЛЮЧИ НАСТРОЕК ====================

    /** Минимальная разница пробега между ТО, км. */
    public static final String SETTING_MAINTENANCE_MIN_MILEAGE = "loyalty.maintenance.min_mileage";
    /** Максимальная разница пробега между ТО, км. */
    public static final String SETTING_MAINTENANCE_MAX_MILEAGE = "loyalty.maintenance.max_mileage";
    /** Максимальное количество звёзд ТО. */
    public static final String SETTING_MAINTENANCE_MAX_STARS = "loyalty.maintenance.max_stars";
    /** Скидка на ТО при 3 звёздах, %. */
    public static final String SETTING_MAINTENANCE_STARS3_DISCOUNT = "loyalty.maintenance.stars3.discount";
    /** Скидка на ТО при 4 звёздах, %. */
    public static final String SETTING_MAINTENANCE_STARS4_DISCOUNT = "loyalty.maintenance.stars4.discount";
    /** Скидка на ТО при 5 звёздах, %. */
    public static final String SETTING_MAINTENANCE_STARS5_DISCOUNT = "loyalty.maintenance.stars5.discount";

    /** Нижняя граница Stage 1 по сумме тюнинга, руб. */
    public static final String SETTING_TUNING_STAGE1_MIN_AMOUNT = "loyalty.tuning.stage1.min_amount";
    /** Скидка Stage 1, %. */
    public static final String SETTING_TUNING_STAGE1_DISCOUNT = "loyalty.tuning.stage1.discount";
    /** Сумма тюнинга, после которой начинается Stage 2, руб. */
    public static final String SETTING_TUNING_STAGE2_MIN_AMOUNT = "loyalty.tuning.stage2.min_amount";
    /** Скидка Stage 2, %. */
    public static final String SETTING_TUNING_STAGE2_DISCOUNT = "loyalty.tuning.stage2.discount";
    /** Сумма тюнинга, после которой начинается Stage 3, руб. */
    public static final String SETTING_TUNING_STAGE3_MIN_AMOUNT = "loyalty.tuning.stage3.min_amount";
    /** Скидка Stage 3, %. */
    public static final String SETTING_TUNING_STAGE3_DISCOUNT = "loyalty.tuning.stage3.discount";

    // ==================== ЗНАЧЕНИЯ ПО УМОЛЧАНИЮ ====================

    public static final int DEFAULT_MAINTENANCE_MIN_MILEAGE = 5_000;
    public static final int DEFAULT_MAINTENANCE_MAX_MILEAGE = 10_000;
    public static final int DEFAULT_MAINTENANCE_MAX_STARS = 5;
    public static final double DEFAULT_MAINTENANCE_STARS3_DISCOUNT = 5.0;
    public static final double DEFAULT_MAINTENANCE_STARS4_DISCOUNT = 10.0;
    public static final double DEFAULT_MAINTENANCE_STARS5_DISCOUNT = 15.0;
    public static final double DEFAULT_TUNING_STAGE1_MIN_AMOUNT = 30_000.0;
    public static final double DEFAULT_TUNING_STAGE1_DISCOUNT = 5.0;
    public static final double DEFAULT_TUNING_STAGE2_MIN_AMOUNT = 100_000.0;
    public static final double DEFAULT_TUNING_STAGE2_DISCOUNT = 10.0;
    public static final double DEFAULT_TUNING_STAGE3_MIN_AMOUNT = 300_000.0;
    public static final double DEFAULT_TUNING_STAGE3_DISCOUNT = 15.0;

    private LoyaltyService() {
    }

    // ==================== НАСТРОЙКИ ====================

    /**
     * Минимальная допустимая разница пробега между ТО.
     * @return значение настройки или значение по умолчанию
     */
    public static int getMaintenanceMinMileage() {
        return getIntSetting(SETTING_MAINTENANCE_MIN_MILEAGE, DEFAULT_MAINTENANCE_MIN_MILEAGE);
    }

    /**
     * Максимальная допустимая разница пробега между ТО.
     * @return значение настройки или значение по умолчанию
     */
    public static int getMaintenanceMaxMileage() {
        return getIntSetting(SETTING_MAINTENANCE_MAX_MILEAGE, DEFAULT_MAINTENANCE_MAX_MILEAGE);
    }

    /**
     * Максимальное количество звёзд ТО.
     * @return значение настройки или значение по умолчанию
     */
    public static int getMaintenanceMaxStars() {
        return getIntSetting(SETTING_MAINTENANCE_MAX_STARS, DEFAULT_MAINTENANCE_MAX_STARS);
    }

    /**
     * Скидка на работы и расходники ТО в зависимости от количества звёзд.
     * @param stars количество звёзд ТО
     * @return процент скидки; {@code 0}, если звёзд меньше 3
     */
    public static double getMaintenanceDiscountPercent(int stars) {
        if (stars >= 5) {
            return getDoubleSetting(SETTING_MAINTENANCE_STARS5_DISCOUNT, DEFAULT_MAINTENANCE_STARS5_DISCOUNT);
        }
        if (stars == 4) {
            return getDoubleSetting(SETTING_MAINTENANCE_STARS4_DISCOUNT, DEFAULT_MAINTENANCE_STARS4_DISCOUNT);
        }
        if (stars == 3) {
            return getDoubleSetting(SETTING_MAINTENANCE_STARS3_DISCOUNT, DEFAULT_MAINTENANCE_STARS3_DISCOUNT);
        }
        return 0.0;
    }

    /**
     * Скидка на ТО по накопленным звёздам клиента.
     * @param client клиент; {@code null} трактуется как 0 звёзд
     * @return процент скидки
     */
    public static double getMaintenanceDiscountPercent(Client client) {
        return getMaintenanceDiscountPercent(client != null ? client.getMaintenanceStars() : 0);
    }

    /**
     * Скидка на тюнинг по накопленной сумме.
     *
     * <p>Границы стадий совпадают с документацией: Stage 1 — от 30 000 до
     * 100 000 включительно, Stage 2 — свыше 100 000 до 300 000 включительно,
     * Stage 3 — свыше 300 000.</p>
     * @param tuningTotal накопленная сумма тюнинга, руб.
     * @return процент скидки; {@code 0}, если сумма меньше нижней границы Stage 1
     */
    public static double getTuningDiscountPercent(double tuningTotal) {
        double stage1 = getDoubleSetting(SETTING_TUNING_STAGE1_MIN_AMOUNT, DEFAULT_TUNING_STAGE1_MIN_AMOUNT);
        double stage2 = getDoubleSetting(SETTING_TUNING_STAGE2_MIN_AMOUNT, DEFAULT_TUNING_STAGE2_MIN_AMOUNT);
        double stage3 = getDoubleSetting(SETTING_TUNING_STAGE3_MIN_AMOUNT, DEFAULT_TUNING_STAGE3_MIN_AMOUNT);

        if (tuningTotal > stage3) {
            return getDoubleSetting(SETTING_TUNING_STAGE3_DISCOUNT, DEFAULT_TUNING_STAGE3_DISCOUNT);
        }
        if (tuningTotal > stage2) {
            return getDoubleSetting(SETTING_TUNING_STAGE2_DISCOUNT, DEFAULT_TUNING_STAGE2_DISCOUNT);
        }
        if (tuningTotal >= stage1) {
            return getDoubleSetting(SETTING_TUNING_STAGE1_DISCOUNT, DEFAULT_TUNING_STAGE1_DISCOUNT);
        }
        return 0.0;
    }

    /**
     * Скидка на тюнинг по накопленной сумме клиента.
     * @param client клиент; {@code null} трактуется как 0 руб.
     * @return процент скидки
     */
    public static double getTuningDiscountPercent(Client client) {
        return getTuningDiscountPercent(client != null ? client.getTuningTotal() : 0.0);
    }

    /**
     * Скидка на позицию заказа по её категории.
     * @param client клиент
     * @param category категория услуги или запчасти
     * @return процент скидки; {@code 0} для обычных позиций и {@code null}-категории
     */
    public static double getDiscountPercent(Client client, ServiceCategory category) {
        if (category == null) {
            return 0.0;
        }
        return switch (category) {
            case MAINTENANCE -> getMaintenanceDiscountPercent(client);
            case TUNING -> getTuningDiscountPercent(client);
            default -> 0.0;
        };
    }

    /**
     * Рассчитать скидку в деньгах на строку заказа.
     * @param client клиент
     * @param category категория позиции
     * @param amount сумма строки до скидки
     * @return сумма скидки; {@code 0} при неположительной сумме
     */
    public static double calculateDiscount(Client client, ServiceCategory category, double amount) {
        if (amount <= 0) {
            return 0.0;
        }
        return amount * getDiscountPercent(client, category) / 100.0;
    }

    // ==================== НАЧИСЛЕНИЕ ЗВЁЗД ТО ====================

    /**
     * Проверить, попадает ли разница пробега в допустимый интервал.
     *
     * <p>Первое ТО (предыдущего пробега нет) начисляет звезду, если пробег
     * указан: проверять интервал ещё не с чем. Для последующих ТО разница
     * должна быть в диапазоне {@code [minMileage, maxMileage]}; если клиент
     * приехал позже максимального интервала, новая звезда не начисляется.</p>
     * @param currentMileage пробег текущего заказа
     * @param previousToMileage пробег предыдущего ТО или {@code null}, если его нет
     * @return {@code true}, если интервал ТО соблюдён
     */
    public static boolean qualifiesForMaintenanceStar(int currentMileage, Integer previousToMileage) {
        if (currentMileage <= 0) {
            return false;
        }
        if (previousToMileage == null || previousToMileage <= 0) {
            return true;
        }
        int difference = currentMileage - previousToMileage;
        return difference >= getMaintenanceMinMileage() && difference <= getMaintenanceMaxMileage();
    }

    /**
     * Рассчитать количество звёзд ТО после закрытия заказа.
     * @param currentStars текущее количество звёзд
     * @param currentMileage пробег закрываемого заказа
     * @param previousToMileage пробег предыдущего ТО или {@code null}
     * @return новое количество звёзд; максимум ограничен настройкой
     */
    public static int nextMaintenanceStars(int currentStars, int currentMileage, Integer previousToMileage) {
        int maxStars = getMaintenanceMaxStars();
        int stars = Math.max(0, currentStars);
        if (stars >= maxStars || !qualifiesForMaintenanceStar(currentMileage, previousToMileage)) {
            return stars;
        }
        return Math.min(maxStars, stars + 1);
    }

    /**
     * Проверить, содержит ли заказ услугу категории ТО.
     * @param order заказ
     * @return {@code true}, если в заказе есть хотя бы одна услуга ТО
     */
    public static boolean isMaintenanceOrder(WorkOrder order) {
        if (order == null) {
            return false;
        }
        for (int i = 0; i < order.getServices().size(); i++) {
            if (resolveServiceCategory(order, i) == ServiceCategory.MAINTENANCE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Найти пробег предыдущего закрытого ТО клиента.
     *
     * <p>Предыдущим считается закрытый заказ того же клиента с услугой ТО и
     * наибольшим пробегом, который меньше пробега закрываемого заказа.</p>
     * @param order закрываемый заказ
     * @return пробег предыдущего ТО или {@code null}, если истории нет
     */
    public static Integer findPreviousMaintenanceMileage(WorkOrder order) {
        if (order == null) {
            return null;
        }
        int clientId = resolveClientId(order);
        if (clientId <= 0) {
            return null;
        }
        Integer previousMileage = null;
        for (WorkOrder candidate : DataStore.getOrders()) {
            if (candidate == null || isSameOrder(candidate, order)) {
                continue;
            }
            if (resolveClientId(candidate) != clientId) {
                continue;
            }
            int candidateMileage = candidate.getMileage();
            if (candidateMileage <= 0 || candidateMileage >= order.getMileage()) {
                continue;
            }
            if (!WorkOrder.STATUS_CLOSED.equals(candidate.getStatus()) || !isMaintenanceOrder(candidate)) {
                continue;
            }
            if (previousMileage == null || candidateMileage > previousMileage) {
                previousMileage = candidateMileage;
            }
        }
        return previousMileage;
    }

    // ==================== ПРИМЕНЕНИЕ К ЗАКАЗУ ====================

    /**
     * Определить категорию услуги в строке заказа по справочнику.
     * @param order заказ
     * @param index индекс услуги
     * @return категория услуги; {@link ServiceCategory#REGULAR}, если услуга не найдена
     */
    public static ServiceCategory resolveServiceCategory(WorkOrder order, int index) {
        if (order == null || index < 0 || index >= order.getServices().size()) {
            return ServiceCategory.REGULAR;
        }
        int serviceId = index < order.getServiceIds().size() ? order.getServiceIds().get(index) : 0;
        Service service = serviceId > 0 ? DataStore.getServiceById(serviceId) : null;
        if (service == null) {
            service = DataStore.getServiceByName(order.getServices().get(index));
        }
        return service != null ? service.getCategory() : ServiceCategory.REGULAR;
    }

    /**
     * Определить категорию запчасти в строке заказа по справочнику.
     * @param part запчасть из заказа
     * @return категория запчасти; {@link ServiceCategory#REGULAR}, если запчасть не найдена
     */
    public static ServiceCategory resolveSparePartCategory(SparePart part) {
        if (part == null) {
            return ServiceCategory.REGULAR;
        }
        SparePart catalogPart = part.getId() > 0 ? DataStore.getSparePartById(part.getId()) : null;
        if (catalogPart == null && part.getName() != null) {
            catalogPart = findSparePartByName(part.getName());
        }
        if (catalogPart != null) {
            return catalogPart.getCategory();
        }
        return part.getCategory() != null ? part.getCategory() : ServiceCategory.REGULAR;
    }

    /**
     * Рассчитать сумму тюнинга в закрываемом заказе.
     * @param order заказ
     * @return сумма услуг и запчастей категории «Тюнинг», руб.
     */
    public static double calculateTuningAmount(WorkOrder order) {
        if (order == null) {
            return 0.0;
        }
        double amount = 0.0;
        for (int i = 0; i < order.getServices().size(); i++) {
            if (resolveServiceCategory(order, i) == ServiceCategory.TUNING
                    && i < order.getServicePrices().size()) {
                amount += order.getServicePrices().get(i);
            }
        }
        for (int i = 0; i < order.getSpareParts().size(); i++) {
            SparePart part = order.getSpareParts().get(i);
            if (resolveSparePartCategory(part) == ServiceCategory.TUNING) {
                double quantity = i < order.getSparePartQuantities().size()
                        ? order.getSparePartQuantities().get(i)
                        : 1.0;
                amount += part.getRetailPrice() * quantity;
            }
        }
        return amount;
    }

    /**
     * Обновить накопленные показатели клиента при закрытии заказа.
     *
     * <p>Если в заказе есть услуга ТО и интервал пробега соблюдён, клиент
     * получает звезду (не выше максимума). Сумма позиций категории «Тюнинг»
     * добавляется к накопленной сумме тюнинга. Изменения сохраняются через
     * {@link DataStore}.</p>
     * @param order закрытый заказ
     * @return результат начисления
     */
    public static LoyaltyUpdateResult applyCompletedOrder(WorkOrder order) {
        if (order == null) {
            return LoyaltyUpdateResult.EMPTY;
        }
        Client client = resolveClient(order);
        if (client == null) {
            logger.warn("Не удалось обновить лояльность: у заказа {} нет клиента", order.getId());
            return LoyaltyUpdateResult.EMPTY;
        }

        int starsBefore = client.getMaintenanceStars();
        int starsAfter = starsBefore;
        if (isMaintenanceOrder(order)) {
            Integer previousMileage = findPreviousMaintenanceMileage(order);
            starsAfter = nextMaintenanceStars(starsBefore, order.getMileage(), previousMileage);
        }
        double tuningAdded = calculateTuningAmount(order);
        double tuningTotalAfter = client.getTuningTotal() + tuningAdded;

        if (starsAfter == starsBefore && tuningAdded <= 0) {
            return new LoyaltyUpdateResult(0, starsBefore, 0.0, client.getTuningTotal());
        }

        client.setMaintenanceStars(starsAfter);
        client.setTuningTotal(tuningTotalAfter);
        persistClient(client);

        LoyaltyUpdateResult result = new LoyaltyUpdateResult(
                starsAfter - starsBefore, starsAfter, tuningAdded, tuningTotalAfter);
        logger.info("Лояльность клиента {} обновлена по заказу {}: звёзды {} -> {}, тюнинг +{} (итого {})",
                client.getId(), order.getId(), starsBefore, starsAfter, tuningAdded, tuningTotalAfter);
        return result;
    }

    /**
     * Результат применения закрытого заказа к показателям клиента.
     *
     * @param starsEarned начислено звёзд ТО
     * @param maintenanceStars всего звёзд ТО после начисления
     * @param tuningAdded добавленная сумма тюнинга, руб.
     * @param tuningTotal накопленная сумма тюнинга после начисления, руб.
     */
    public record LoyaltyUpdateResult(int starsEarned, int maintenanceStars,
                                      double tuningAdded, double tuningTotal) {

        /** Результат без изменений. */
        public static final LoyaltyUpdateResult EMPTY = new LoyaltyUpdateResult(0, 0, 0.0, 0.0);

        /**
         * Была ли начислена хотя бы одна звезда.
         * @return {@code true}, если количество звёзд выросло
         */
        public boolean isStarEarned() {
            return starsEarned > 0;
        }
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    private static Client resolveClient(WorkOrder order) {
        int clientId = resolveClientId(order);
        if (clientId > 0) {
            Client cached = DataStore.getClientById(clientId);
            if (cached != null) {
                return cached;
            }
        }
        return order.getClient();
    }

    private static int resolveClientId(WorkOrder order) {
        if (order.getClient() != null) {
            return order.getClient().getId();
        }
        return order.getClientId();
    }

    private static boolean isSameOrder(WorkOrder first, WorkOrder second) {
        if (first == second) {
            return true;
        }
        String firstId = first.getId();
        String secondId = second.getId();
        return firstId != null && firstId.equals(secondId);
    }

    private static SparePart findSparePartByName(String name) {
        List<SparePart> spareParts = DataStore.getSpareParts();
        for (SparePart part : spareParts) {
            if (part != null && name.equals(part.getName())) {
                return part;
            }
        }
        return null;
    }

    private static void persistClient(Client client) {
        if (client.getId() > 0) {
            DataStore.updateClient(client);
        }
    }

    private static int getIntSetting(String key, int defaultValue) {
        String value = SettingService.getSettingValue(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            logger.warn("Некорректное значение настройки {}='{}', используем {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    private static double getDoubleSetting(String key, double defaultValue) {
        String value = SettingService.getSettingValue(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            logger.warn("Некорректное значение настройки {}='{}', используем {}", key, value, defaultValue);
            return defaultValue;
        }
    }
}
