package com.autoservice.services;

import com.autoservice.DataStore;
import com.autoservice.Service;
import com.autoservice.SparePart;
import com.autoservice.model.ServiceSparePart;
import com.autoservice.model.ServiceSparePartsList;
import com.autoservice.model.ServiceSparePartsListItem;
import com.autoservice.model.ServicePart;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        Service service = DataStore.getServiceByName(serviceName);

        if (service == null) {
            return new ArrayList<>();
        }

        return collect(service.getId());
    }

    /**
     * Собрать запчасти, связанные с услугой, из всех поддерживаемых структур связей.
     * @param serviceId ID услуги
     * @return список объектов SparePart с количеством
     */
    private static List<SparePartWithQuantity> collect(int serviceId) {
        List<SparePartWithQuantity> result = new ArrayList<>();

        // Получаем связи из новой структуры service_parts
        List<ServicePart> newRelations = DataStore.getAllServiceParts().stream()
                .filter(sp -> sp.getServiceId() == serviceId)
                .collect(Collectors.toList());
        
        for (ServicePart relation : newRelations) {
            SparePart sparePart = DataStore.getSparePartById(relation.getSparePartId());

            if (sparePart != null) {
                SparePartWithQuantity item = new SparePartWithQuantity();
                item.setSparePart(sparePart);
                item.setQuantity(relation.getQuantity());
                item.setUnitType(sparePart.getUnitType());
                result.add(item);
            }
        }

        // Получаем списки из старой новой структуры (ServiceSparePartsList)
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

        // Также поддерживаем старую структуру для обратной совместимости (ServiceSparePart)
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

        /**
         * Получить запчасть.
         * @return запчасть
         */
        public SparePart getSparePart() {
            return sparePart;
        }

        /**
         * Установить запчасть.
         * @param sparePart запчасть
         */
        public void setSparePart(SparePart sparePart) {
            this.sparePart = sparePart;
        }

        /**
         * Получить количество.
         * @return количество
         */
        public double getQuantity() {
            return quantity;
        }

        /**
         * Установить количество.
         * @param quantity количество
         */
        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        /**
         * Установить количество с дробной частью.
         * @param quantity количество
         */
        public void setQuantity(double quantity) {
            this.quantity = quantity;
        }

        /**
         * Получить единицу измерения.
         * @return единица измерения
         */
        public String getUnitType() {
            return unitType;
        }

        /**
         * Установить единицу измерения.
         * @param unitType единица измерения
         */
        public void setUnitType(String unitType) {
            this.unitType = unitType;
        }

        /**
         * Создать копию объекта.
         * @return новая копия с теми же запчастью, количеством и единицей измерения
         */
        public SparePartWithQuantity copy() {
            SparePartWithQuantity copy = new SparePartWithQuantity();
            copy.setSparePart(this.sparePart);
            copy.setQuantity(this.quantity);
            copy.setUnitType(this.unitType);
            return copy;
        }

        /**
         * Строковое представление в виде «название x количество единица».
         * @return строковое представление
         */
        @Override
        public String toString() {
            return sparePart.getName() + " x" + quantity + " " + unitType;
        }
    }
}
