package com.autoservice.model;

import com.autoservice.WorkOrder;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Общий снимок статистики автосервиса для отчётов.
 *
 * <p>Единственный источник данных — {@code StatisticsService}: экран отчёта
 * отображает этот DTO, а {@code ReportGenerator} рендерит по нему PDF.
 * Порядок элементов в картах сохраняется, чтобы списки статусов, услуг и
 * запчастей выводились в том же порядке, что и при расчёте.
 *
 * @param totalOrders      общее количество заказов
 * @param totalClients     общее количество клиентов
 * @param totalSpareParts  общее количество запчастей
 * @param totalAppointments общее количество записей на обслуживание
 * @param totalRevenue     выручка по закрытым заказам
 * @param statusCount      количество заказов по статусам
 * @param monthlyRevenue   выручка по месяцам; ключ — первые 7 символов даты создания заказа
 * @param topServices      топ услуг по количеству упоминаний
 * @param topParts         топ запчастей по количеству упоминаний
 * @param generatedDate    дата и время формирования отчёта
 */
public record ReportData(
        int totalOrders,
        int totalClients,
        int totalSpareParts,
        int totalAppointments,
        double totalRevenue,
        Map<String, Integer> statusCount,
        Map<String, Double> monthlyRevenue,
        Map<String, Integer> topServices,
        Map<String, Integer> topParts,
        String generatedDate) {

    public ReportData {
        statusCount = immutableCopy(statusCount);
        monthlyRevenue = immutableCopy(monthlyRevenue);
        topServices = immutableCopy(topServices);
        topParts = immutableCopy(topParts);
    }

    /**
     * Количество новых заказов.
     * @return количество заказов в статусе {@link WorkOrder#STATUS_NEW}
     */
    public int newOrders() {
        return statusCount.getOrDefault(WorkOrder.STATUS_NEW, 0);
    }

    /**
     * Количество заказов в работе.
     * @return количество заказов в статусе {@link WorkOrder#STATUS_IN_PROGRESS}
     */
    public int inProgressOrders() {
        return statusCount.getOrDefault(WorkOrder.STATUS_IN_PROGRESS, 0);
    }

    /**
     * Количество закрытых заказов.
     * @return количество заказов в статусе {@link WorkOrder#STATUS_CLOSED}
     */
    public int closedOrders() {
        return statusCount.getOrDefault(WorkOrder.STATUS_CLOSED, 0);
    }

    /**
     * Средний чек по закрытым заказам.
     * @return средняя сумма закрытого заказа или {@code 0}, если закрытых заказов нет
     */
    public double averageOrderValue() {
        return closedOrders() > 0 ? totalRevenue / closedOrders() : 0;
    }

    private static <K, V> Map<K, V> immutableCopy(Map<K, V> source) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
