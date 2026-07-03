package com.autoservice.services;

import com.autoservice.DataStore;
import com.autoservice.Service;
import com.autoservice.SparePart;
import com.autoservice.model.ServiceSparePart;

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

        List<ServiceSparePart> relations = DataStore.getServiceSparePartsByServiceId(-1);

        for (ServiceSparePart relation : relations) {
            if (!relation.isActive() || relation.getServiceId() != service.getId()) {
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
        List<ServiceSparePart> relations = DataStore.getServiceSparePartsByServiceId(serviceId);

        for (ServiceSparePart relation : relations) {
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
        private int quantity;
        private String unitType;

        public SparePart getSparePart() {
            return sparePart;
        }

        public void setSparePart(SparePart sparePart) {
            this.sparePart = sparePart;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
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
