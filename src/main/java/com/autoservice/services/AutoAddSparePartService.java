package com.autoservice.services;

import com.autoservice.DataStore;
import com.autoservice.Service;
import com.autoservice.SparePart;
import com.autoservice.model.ServiceSparePart;
import com.autoservice.model.ServiceSparePartsList;
import com.autoservice.model.ServiceSparePartsListItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для автоматического добавления запчастей при выборе услуги.
 */
public class AutoAddSparePartService {

    /**
     * Получить список запчастей, связанных с услугой.
     * @param serviceName название услуги
     * @return список объектов SparePart с количеством
     */
    public static List<SparePartWithQuantity> getSparePartsByService(String serviceName) {
        List<SparePartWithQuantity> result = new ArrayList<>();
        Service service = DataStore.getServiceByName(serviceName);

        if (service == null) {
            return result;
        }

        // Получаем списки из новой структуры
        List<ServiceSparePartsList> lists = DataStore.getServiceSparePartsListsByServiceId(service.getId());
        
        for (ServiceSparePartsList list : lists) {
            if (!list.isActive()) {
                continue;
            }
            
            for (ServiceSparePartsListItem item : list.getItems()) {
                if (!item.isDirty()) { // item не имеет isDirty, используем только активность
                    SparePart sparePart = DataStore.getSparePartById(item.getSparePartId());

                    if (sparePart != null) {
                        SparePartWithQuantity partItem = new SparePartWithQuantity();
                        partItem.setSparePart(sparePart);
                        partItem.setQuantity(item.getQuantity());
                        partItem.setUnitType(item.getUnitType());
                        result.add(partItem);
                    }
                }
            }
        }

        // Также поддерживаем старую структуру для обратной совместимости
        List<ServiceSparePart> oldRelations = DataStore.getServiceSparePartsByServiceId(service.getId());
        for (ServiceSparePart relation : oldRelations) {
            if (!relation.isActive()) {
                continue;
            }

            SparePart sparePart = DataStore.getSparePartById(relation.getSparePartId());

            if (sparePart != null) {
                SparePartWithQuantity item = new SparePartWithQuantity();
                item.setSparePart(sparePart);
                item.setQuantity(relation.getQuantity());
                item.setUnitType(relation.getUnitType());
                result.add(item);
            }
        }

        return result;
    }

    /**
     * Получить список запчастей по ID услуги.
     * @param serviceId ID услуги
     * @return список объектов SparePart с количеством
     */
    public static List<SparePartWithQuantity> getSparePartsByServiceId(int serviceId) {
        List<SparePartWithQuantity> result = new ArrayList<>();
        
        // Получаем списки из новой структуры
        List<ServiceSparePartsList> lists = DataStore.getServiceSparePartsListsByServiceId(serviceId);
        
        for (ServiceSparePartsList list : lists) {
            if (!list.isActive()) {
                continue;
            }
            
            for (ServiceSparePartsListItem item : list.getItems()) {
                SparePart sparePart = DataStore.getSparePartById(item.getSparePartId());

                if (sparePart != null) {
                    SparePartWithQuantity partItem = new SparePartWithQuantity();
                    partItem.setSparePart(sparePart);
                    partItem.setQuantity(item.getQuantity());
                    partItem.setUnitType(item.getUnitType());
                    result.add(partItem);
                }
            }
        }

        // Также поддерживаем старую структуру для обратной совместимости
        List<ServiceSparePart> oldRelations = DataStore.getServiceSparePartsByServiceId(serviceId);
        for (ServiceSparePart relation : oldRelations) {
            if (!relation.isActive()) {
                continue;
            }

            SparePart sparePart = DataStore.getSparePartById(relation.getSparePartId());

            if (sparePart != null) {
                SparePartWithQuantity item = new SparePartWithQuantity();
                item.setSparePart(sparePart);
                item.setQuantity(relation.getQuantity());
                item.setUnitType(relation.getUnitType());
                result.add(item);
            }
        }

        return result;
    }

    /**
     * Класс для хранения запчасти с количеством.
     */
    public static class SparePartWithQuantity {
        private SparePart sparePart;
        private double quantity;
        private String unitType;

        public SparePart getSparePart() {
            return sparePart;
        }

        public void setSparePart(SparePart sparePart) {
            this.sparePart = sparePart;
        }

        public double getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public void setQuantity(double quantity) {
            this.quantity = quantity;
        }

        public String getUnitType() {
            return unitType;
        }

        public void setUnitType(String unitType) {
            this.unitType = unitType;
        }

        /**
         * Создает копию объекта.
         */
        public SparePartWithQuantity copy() {
            SparePartWithQuantity copy = new SparePartWithQuantity();
            copy.setSparePart(this.sparePart);
            copy.setQuantity(this.quantity);
            copy.setUnitType(this.unitType);
            return copy;
        }

        @Override
        public String toString() {
            return sparePart.getName() + " x" + quantity + " " + unitType;
        }
    }
}
