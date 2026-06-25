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

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            revenue.put(date.format(DAY_FORMATTER), 0.0);
        }

        System.out.println("=== DEBUG: All orders in DataStore ===");
        System.out.println("Total orders: " + DataStore.getOrders().size());
        for (WorkOrder order : DataStore.getOrders()) {
            System.out.println("Order ID: " + order.getId() +
                    ", Status: " + order.getStatus() +
                    ", CreatedDate: " + order.getCreatedDate() +
                    ", Total: " + order.getTotal());
        }

        System.out.println("=== DEBUG: Filtering closed orders ===");
        int closedCount = 0;
        for (WorkOrder order : DataStore.getOrders()) {
            if (order.getStatus().equals(WorkOrder.STATUS_CLOSED)) {
                closedCount++;
                String createdDate = order.getCreatedDate();
                System.out.println("Closed order: " + order.getId() + ", date: " + createdDate);

                if (createdDate != null && !createdDate.isEmpty()) {
                    try {
                        String dateStr = createdDate.length() >= 10 ? createdDate.substring(0, 10) : createdDate;
                        LocalDate orderDate = LocalDate.parse(dateStr);
                        String key = orderDate.format(DAY_FORMATTER);
                        if (revenue.containsKey(key)) {
                            revenue.put(key, revenue.get(key) + order.getTotal());
                            System.out.println("  -> Added to " + key + ": +" + order.getTotal());
                        } else {
                            System.out.println("  -> Key " + key + " not in range");
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing: " + createdDate);
                    }
                } else {
                    System.out.println("  -> createdDate is null or empty!");
                }
            }
        }
        System.out.println("Total closed orders: " + closedCount);

        System.out.println("=== DEBUG: Final revenue ===");
        for (var entry : revenue.entrySet()) {
            if (entry.getValue() > 0) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
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