package com.autoservice.services;

import com.autoservice.DataStore;
import com.autoservice.WorkOrder;
import com.autoservice.Appointment;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class StatisticsService {

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("dd.MM");

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
                        LocalDate orderDate = LocalDate.parse(dateStr);
                        String key = orderDate.format(DAY_FORMATTER);
                        if (orderDate.isEqual(startDate) || (!orderDate.isBefore(startDate) && !orderDate.isAfter(endDate))) {
                            closedOrdersRevenue.merge(key, order.getTotal(), Double::sum);
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing: " + createdDate);
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

    public static Map<String, Integer> getMastersLoad() {
        Map<String, Integer> mastersLoad = new LinkedHashMap<>();

        String[] masters = {"Иван", "Петр", "Сергей", "Антон"};
        Set<String> knownMasters = new HashSet<>(Arrays.asList(masters));
        for (String master : masters) {
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

    public static Map<String, Integer> getStatusStats() {
        Map<String, Integer> statusStats = new LinkedHashMap<>();

        for (WorkOrder order : DataStore.getOrders()) {
            statusStats.merge(order.getStatus(), 1, Integer::sum);
        }

        return statusStats;
    }

}