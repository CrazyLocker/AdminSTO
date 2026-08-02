package com.autoservice.utils;

import com.autoservice.DataStore;
import com.autoservice.Service;
import com.autoservice.SparePart;
import com.autoservice.WorkOrder;
import com.autoservice.model.ServicePart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Вспомогательный класс для работы с связями услуг-запчастей.
 * Заменяет старую систему с полями oilVolume, usesOil, sparePartName в Service.java
 * Теперь все связи хранятся в таблице service_parts и обрабатываются через модель ServicePart.
 */
public class ServicePartHelper {
    
    private static final Logger logger = LoggerFactory.getLogger(ServicePartHelper.class);

    // ==================== РАБОТА С ОДНОЙ УСЛУГОЙ ====================

    /**
     * Получить все запчасти для услуги
     */
    public static List<ServicePart> getServiceParts(int serviceId) {
        return DataStore.getAllServiceParts().stream()
                .filter(sp -> sp.getServiceId() == serviceId)
                .collect(Collectors.toList());
    }

    /**
     * Проверить, достаточно ли запчастей для услуги
     */
    public static boolean hasEnoughParts(int serviceId) {
        List<ServicePart> parts = getServiceParts(serviceId);
        for (ServicePart sp : parts) {
            SparePart sparePart = DataStore.getSparePartById(sp.getSparePartId());
            if (sparePart == null) {
                logger.warn("Запчасть с id={} не найдена для услуги id={}", sp.getSparePartId(), serviceId);
                return false;
            }
            if (sparePart.getStock() < sp.getQuantity()) {
                logger.warn("Недостаточно запчасти {} для услуги id={}: нужно={}, есть={}", 
                        sparePart.getName(), serviceId, sp.getQuantity(), sparePart.getStock());
                return false;
            }
        }
        return true;
    }

    /**
     * Списать запчасти для услуги
     */
    public static boolean deductPartsForService(int serviceId) {
        List<ServicePart> parts = getServiceParts(serviceId);
        for (ServicePart sp : parts) {
            SparePart sparePart = DataStore.getSparePartById(sp.getSparePartId());
            if (sparePart == null) {
                logger.error("Запчасть с id={} не найдена для услуги id={}", sp.getSparePartId(), serviceId);
                return false;
            }
            if (!sparePart.deductStock(sp.getQuantity())) {
                logger.error("Не удалось списать {} {} для услуги id={}", 
                        sp.getQuantity(), sparePart.getName(), serviceId);
                return false;
            }
        }
        return true;
    }

    /**
     * Добавить связь "услуга-запчасть"
     */
    public static void addServicePartLink(int serviceId, int sparePartId, double quantity, boolean isRequired) {
        ServicePart servicePart = new ServicePart(serviceId, sparePartId, quantity, isRequired);
        DataStore.addServicePart(servicePart);
    }

    /**
     * Удалить все связи для услуги
     */
    public static void removeServiceParts(int serviceId) {
        DataStore.deleteServicePartsByServiceId(serviceId);
    }

    // ==================== РАБОТА С ЗАПЧАСТЯМИ ====================

    /**
     * Поиск запчасти по ID
     */
    public static SparePart getSparePart(int sparePartId) {
        return DataStore.getSparePartById(sparePartId);
    }

    /**
     * Поиск услуги по ID
     */
    public static Service getService(int serviceId) {
        return DataStore.getServiceById(serviceId);
    }

    // ==================== РАБОТА С ЗАКАЗОМ ====================

    /**
     * Добавить все запчасти из связей услуги в заказ
     */
    public static void addServicePartsToOrder(WorkOrder order, int serviceId) {
        List<ServicePart> serviceParts = getServiceParts(serviceId);
        
        for (ServicePart sp : serviceParts) {
            SparePart sparePart = DataStore.getSparePartById(sp.getSparePartId());
            if (sparePart == null) {
                logger.error("Запчасть с id={} не найдена для связи id={}", sp.getSparePartId(), sp.getId());
                continue;
            }

            // Проверяем наличие на складе
            if (sparePart.getStock() < sp.getQuantity()) {
                logger.error("Недостаточно {} для услуги id={}: нужно={}, есть={}", 
                        sparePart.getName(), serviceId, sp.getQuantity(), sparePart.getStock());
                continue;
            }

            // Списываем и добавляем в заказ
            if (sparePart.deductStock(sp.getQuantity())) {
                order.addSparePart(sparePart, sp.getQuantity());
                logger.debug("Добавлено в заказ: {} x{} (из связи услуги id={})", 
                        sparePart.getName(), sp.getQuantity(), serviceId);
            } else {
                logger.error("Не удалось списать {} для заказа", sparePart.getName());
            }
        }
    }

    /**
     * Добавить запчасти для нескольких услуг в заказ
     */
    public static void addServicePartsToOrder(WorkOrder order, List<Integer> serviceIds) {
        for (int serviceId : serviceIds) {
            addServicePartsToOrder(order, serviceId);
        }
    }

    /**
     * Добавить запчасти для всех услуг заказа (используется при создании заказа)
     */
    public static void addPartsForServices(WorkOrder order, List<Integer> serviceIds) {
        addServicePartsToOrder(order, serviceIds);
        logger.info("Добавлены запчасти для {} услуг в заказ {}", serviceIds.size(), order.getId());
    }

    /**
     * Добавить запчасти для конкретной услуги в заказ
     */
    public static void addPartsForService(WorkOrder order, int serviceId) {
        addServicePartsToOrder(order, serviceId);
        logger.info("Добавлены запчасти для услуги id={}", serviceId);
    }

    /**
     * Вернуть запчасти на склад (при удалении/отмене заказа)
     */
    public static void returnPartsToStock(WorkOrder order) {
        List<SparePart> parts = order.getSpareParts();
        List<Double> quantities = order.getSparePartQuantities();
        
        for (int i = 0; i < parts.size(); i++) {
            SparePart part = parts.get(i);
            double qty = quantities.get(i);
            part.addStock(qty);
            logger.debug("Возвращено на склад: {} x{} (заказ {})", part.getName(), qty, order.getId());
        }
    }

    // ==================== ВАЛИДАЦИЯ ====================

    /**
     * Проверить наличие всех обязательных запчастей для услуги
     */
    public static boolean checkAvailability(int serviceId) {
        return hasEnoughParts(serviceId);
    }

    /**
     * Проверить наличие всех обязательных запчастей для нескольких услуг
     */
    public static boolean checkAvailability(List<Integer> serviceIds) {
        return hasEnoughPartsForServices(serviceIds);
    }

    /**
     * Получить список недостающих запчастей для услуги
     */
    public static String getMissingParts(int serviceId) {
        List<ServicePart> parts = getServiceParts(serviceId);
        StringBuilder missing = new StringBuilder();
        
        for (ServicePart sp : parts) {
            SparePart sparePart = DataStore.getSparePartById(sp.getSparePartId());
            if (sparePart == null) {
                if (missing.length() > 0) missing.append(", ");
                missing.append("Запчасть id=").append(sp.getSparePartId());
                continue;
            }
            if (sparePart.getStock() < sp.getQuantity()) {
                double needed = sp.getQuantity() - sparePart.getStock();
                if (missing.length() > 0) missing.append(", ");
                missing.append(sparePart.getName()).append(" (нужно ещё ").append(String.format("%.2f", needed)).append(" ").append(sparePart.getUnitType()).append(")");
            }
        }
        
        return missing.length() > 0 ? missing.toString() : "";
    }

    /**
     * Проверить, достаточно ли всех запчастей для нескольких услуг
     */
    public static boolean hasEnoughPartsForServices(List<Integer> serviceIds) {
        for (int serviceId : serviceIds) {
            if (!hasEnoughParts(serviceId)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Проверить, достаточно ли всех запчастей для услуги по имени
     */
    public static boolean hasEnoughPartsByName(String serviceName) {
        Service service = findServiceByName(serviceName);
        if (service == null) return false;
        return hasEnoughParts(service.getId());
    }

    // ==================== ПОИСК ====================

    /**
     * Поиск услуги по имени
     */
    public static Service findServiceByName(String name) {
        return DataStore.getServices().stream()
                .filter(s -> s.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Поиск запчасти по имени
     */
    public static SparePart findSparePartByName(String name) {
        return DataStore.getSpareParts().stream()
                .filter(s -> s.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    // ==================== СТАТУС И ЛОГИРОВАНИЕ ====================

    /**
     * Получить количество связей для услуги
     */
    public static int getServicePartCount(int serviceId) {
        return (int) getServiceParts(serviceId).size();
    }

    /**
     * Получить подробную информацию о связях услуги
     */
    public static String getServicePartsInfo(int serviceId) {
        List<ServicePart> parts = getServiceParts(serviceId);
        if (parts.isEmpty()) {
            return "Нет связей";
        }
        
        StringBuilder sb = new StringBuilder();
        for (ServicePart sp : parts) {
            SparePart sparePart = DataStore.getSparePartById(sp.getSparePartId());
            String partName = sparePart != null ? sparePart.getName() : "Неизвестно (id=" + sp.getSparePartId() + ")";
            sb.append(String.format("%s: %.2f %s [%s]\n", 
                    partName, 
                    sp.getQuantity(), 
                    sp.isRequired() ? "обязательная" : "опциональная",
                    sparePart != null ? sparePart.getUnitType() : "?"));
        }
        return sb.toString().trim();
    }
}
