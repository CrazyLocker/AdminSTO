package com.autoservice.services;

import com.autoservice.AppConstants;
import com.autoservice.DataStore;
import com.autoservice.Appointment;
import com.autoservice.WorkOrder;
import com.autoservice.model.ReportData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис расчёта статистики по заказам, записям на обслуживание и выручке.
 */
public class StatisticsService {
    
    private static final Logger logger = LoggerFactory.getLogger(StatisticsService.class);

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("dd.MM");
    private static final DateTimeFormatter DB_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter REPORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /**
     * Рассчитать выручку по дням за последние {@code days} дней.
     *
     * <p>Учитываются только закрытые заказы; дни без выручки в результат не попадают.
     * @param days количество дней, включая сегодняшний
     * @return выручка по дням в формате {@code dd.MM}
     */
    public static Map<String, Double> getDailyRevenue(int days) {
        Map<String, Double> revenue = new LinkedHashMap<>();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        // Сначала собираем все закрытые заказы
        Map<String, Double> closedOrdersRevenue = new HashMap<>();
        for (WorkOrder order : DataStore.getOrders()) {
            if (order.getStatus().equals(WorkOrder.STATUS_CLOSED)) {
                String createdDate = order.getCreatedDate();
                if (createdDate != null && !createdDate.isEmpty()) {
                    try {
                        String dateStr = createdDate.length() >= 10 ? createdDate.substring(0, 10) : createdDate;
                        LocalDate orderDate;
                        try {
                            // Пытаемся распарсить как dd/MM/yyyy
                            orderDate = LocalDate.parse(dateStr, DB_DATE_FORMATTER);
                        } catch (Exception e) {
                            // Если не удалось, пробуем ISO-8601 (yyyy-MM-dd)
                            orderDate = LocalDate.parse(dateStr);
                        }
                        String key = orderDate.format(DAY_FORMATTER);
                        if (orderDate.isEqual(startDate) || (!orderDate.isBefore(startDate) && !orderDate.isAfter(endDate))) {
                            closedOrdersRevenue.merge(key, order.getTotal(), Double::sum);
                        }
                    } catch (Exception e) {
                        logger.error("Ошибка парсинга даты: {}", createdDate, e);
                    }
                }
            }
        }

        // Возвращаем только дни с ненулевой выручкой
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String key = date.format(DAY_FORMATTER);
            if (closedOrdersRevenue.containsKey(key) && closedOrdersRevenue.get(key) > 0) {
                revenue.put(key, closedOrdersRevenue.get(key));
            }
        }

        return revenue;
    }

    /**
     * Рассчитать загрузку мастеров по количеству записей.
     * @return количество записей для каждого мастера из {@link AppConstants#MASTERS}
     */
    public static Map<String, Integer> getMastersLoad() {
        Map<String, Integer> mastersLoad = new LinkedHashMap<>();
        Set<String> knownMasters = new HashSet<>(AppConstants.MASTERS);
        for (String master : AppConstants.MASTERS) {
            mastersLoad.put(master, 0);
        }

        for (Appointment a : DataStore.getAppointments()) {
            String name = a.getMasterName();
            if (knownMasters.contains(name)) {
                mastersLoad.merge(name, 1, Integer::sum);
            }
        }

        return mastersLoad;
    }

    /**
     * Получить самые популярные услуги.
     * @param limit максимальное количество услуг в результате
     * @return услуги, отсортированные по убыванию количества заказов
     */
    public static List<Map.Entry<String, Integer>> getTopServices(int limit) {
        Map<String, Integer> servicesCount = new HashMap<>();

        for (WorkOrder order : DataStore.getOrders()) {
            for (String service : order.getServices()) {
                servicesCount.merge(service, 1, Integer::sum);
            }
        }

        return servicesCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Получить самые используемые запчасти.
     * @param limit максимальное количество запчастей в результате
     * @return запчасти, отсортированные по убыванию количества заказов
     */
    public static List<Map.Entry<String, Integer>> getTopSpareParts(int limit) {
        Map<String, Integer> partsCount = new HashMap<>();

        for (WorkOrder order : DataStore.getOrders()) {
            for (var part : order.getSpareParts()) {
                partsCount.merge(part.getName(), 1, Integer::sum);
            }
        }

        return partsCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Собрать полный снимок статистики для экрана отчёта и PDF.
     *
     * <p>Выручка и средний чек рассчитываются только по закрытым заказам;
     * топ-5 услуг и запчастей определяется по частоте использования.
     * @return снимок статистики для отчёта
     */
    public static ReportData buildReport() {
        List<WorkOrder> orders = DataStore.getOrders();

        double totalRevenue = 0;
        for (WorkOrder order : orders) {
            if (WorkOrder.STATUS_CLOSED.equals(order.getStatus())) {
                totalRevenue += order.getTotal();
            }
        }

        return new ReportData(
                orders.size(),
                DataStore.getClients().size(),
                DataStore.getSpareParts().size(),
                DataStore.getAppointments().size(),
                totalRevenue,
                getStatusStats(),
                getMonthlyRevenue(),
                toLinkedMap(getTopServices(5)),
                toLinkedMap(getTopSpareParts(5)),
                LocalDateTime.now().format(REPORT_DATE_FORMATTER)
        );
    }

    /**
     * Рассчитать выручку по месяцам для закрытых заказов.
     * @return выручка по месяцам; ключ — первые 7 символов даты создания заказа
     */
    public static Map<String, Double> getMonthlyRevenue() {
        Map<String, Double> monthlyRevenue = new LinkedHashMap<>();

        for (WorkOrder order : DataStore.getOrders()) {
            if (!WorkOrder.STATUS_CLOSED.equals(order.getStatus())) {
                continue;
            }
            String createdDate = order.getCreatedDate();
            if (createdDate == null || createdDate.isEmpty()) {
                continue;
            }
            try {
                String monthKey = createdDate.substring(0, 7);
                monthlyRevenue.merge(monthKey, order.getTotal(), Double::sum);
            } catch (Exception e) {
                logger.error("Ошибка разбора даты: {}", createdDate, e);
            }
        }

        return monthlyRevenue;
    }

    /**
     * Рассчитать количество заказов по статусам.
     * @return статистика по статусам заказов
     */
    public static Map<String, Integer> getStatusStats() {
        Map<String, Integer> statusStats = new LinkedHashMap<>();

        for (WorkOrder order : DataStore.getOrders()) {
            statusStats.merge(normalizeStatus(order.getStatus()), 1, Integer::sum);
        }

        return statusStats;
    }

    private static String normalizeStatus(String status) {
        return status != null ? status : WorkOrder.STATUS_NEW;
    }

    private static Map<String, Integer> toLinkedMap(List<Map.Entry<String, Integer>> entries) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : entries) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

}
