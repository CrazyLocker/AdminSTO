package com.autoservice.utils;

import com.autoservice.SparePart;
import com.autoservice.Service;
import com.autoservice.WorkOrder;
import com.autoservice.DataStore;

import java.util.List;

public class OilHelper {

    /**
     * Проверяет, достаточно ли масла на складе для услуги
     */
    public static boolean hasEnoughOil(Service service) {
        if (!service.isUsesOil()) return true;

        SparePart oil = findOil();
        if (oil == null) return false;

        double neededLiters = service.getOilVolume();
        return oil.getStock() >= neededLiters;
    }

    /**
     * Списание масла для услуги
     */
    public static boolean deductOilForService(Service service) {
        if (!service.isUsesOil()) return true;

        SparePart oil = findOil();
        if (oil == null) return false;

        double neededLiters = service.getOilVolume();
        return oil.deductStock(neededLiters);
    }

    /**
     * Поиск масла на складе
     */
    public static SparePart findOil() {
        for (SparePart part : DataStore.getSpareParts()) {
            if (part.getName().contains("Масло")) {
                return part;
            }
        }
        return null;
    }

    /**
     * Поиск запчасти по имени
     */
    public static SparePart findSparePart(String name) {
        for (SparePart part : DataStore.getSpareParts()) {
            if (part.getName().equalsIgnoreCase(name)) {
                return part;
            }
        }
        return null;
    }

    /**
     * Поиск услуги по имени
     */
    public static Service findService(String name) {
        for (Service service : DataStore.getServices()) {
            if (service.getName().equalsIgnoreCase(name)) {
                return service;
            }
        }
        return null;
    }

    /**
     * Получение остатка масла в литрах и канистрах
     */
    public static String getOilStockFormatted() {
        SparePart oil = findOil();
        if (oil == null) return "Масло не найдено";
        return oil.getStockFormatted();
    }

    /**
     * Расчёт количества канистр для заказа
     */
    public static int getCansNeededForService(Service service) {
        if (!service.isUsesOil()) return 0;
        SparePart oil = findOil();
        if (oil == null) return 0;
        double neededLiters = service.getOilVolume();
        if (oil.getUnitType().equals("л")) {
            return (int) Math.ceil(neededLiters);
        }
        return (int) Math.ceil(neededLiters);
    }

    /**
     * Добавление масла и расходников к заказу
     */
    public static void addOilAndPartsToOrder(WorkOrder order, List<String> serviceNames) {
        for (String serviceName : serviceNames) {
            Service service = findService(serviceName);
            if (service == null) continue;

            // Добавляем масло
            if (service.isUsesOil()) {
                SparePart oil = findOil();
                if (oil == null) {
                    System.err.println("Масло не найдено для услуги: " + serviceName);
                    continue;
                }

                double neededLiters = service.getOilVolume();
                int cansNeeded;
                if (oil.getUnitType().equals("л")) {
                    cansNeeded = (int) Math.ceil(neededLiters);
                } else {
                    double unitVolume = 1.0;
                    if (oil.getUnitType().contains("л")) {
                        // Try to parse unitVolume from unitType or use default
                        cansNeeded = (int) Math.ceil(neededLiters);
                    } else {
                        cansNeeded = (int) Math.ceil(neededLiters);
                    }
                }

                if (!oil.deductStock(neededLiters)) {
                    System.err.println("Недостаточно масла! Нужно: " + neededLiters + " л, Доступно: " + oil.getStock() + " л");
                    continue;
                }

                // Добавляем масло в заказ (в канистрах)
                order.addSparePart(oil, cansNeeded);
            }

            // Добавляем расходники (фильтр и т.д.)
            if (service.getSparePartName() != null && !service.getSparePartName().isEmpty()) {
                SparePart part = findSparePart(service.getSparePartName());
                if (part != null) {
                    int quantity = service.getSparePartQuantity() > 0 ? service.getSparePartQuantity() : 1;
                    if (part.deductStock(quantity)) {
                        order.addSparePart(part, quantity);
                    } else {
                        System.err.println("Недостаточно " + part.getName() + "! Нужно: " + quantity + ", Доступно: " + part.getStock());
                    }
                }
            }
        }
    }
}