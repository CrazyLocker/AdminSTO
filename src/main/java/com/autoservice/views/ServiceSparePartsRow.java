package com.autoservice.views;

import com.autoservice.Service;
import com.autoservice.SparePart;
import com.autoservice.DataStore;

/**
 * Класс для отображения одной строки в таблице связей услуг-запчастей.
 * Одна услуга может иметь несколько запчастей, которые отображаются через запятую.
 */
public class ServiceSparePartsRow {
    private Service service;
    private String sparePartsList;
    private String totalQuantity;

    public ServiceSparePartsRow(Service service, String sparePartsList, String totalQuantity) {
        this.service = service;
        this.sparePartsList = sparePartsList;
        this.totalQuantity = totalQuantity;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public String getSparePartsList() {
        return sparePartsList;
    }

    public void setSparePartsList(String sparePartsList) {
        this.sparePartsList = sparePartsList;
    }

    public String getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(String totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    @Override
    public String toString() {
        return "ServiceSparePartsRow{" +
                "service=" + service.getName() +
                ", sparePartsList='" + sparePartsList + '\'' +
                ", totalQuantity='" + totalQuantity + '\'' +
                '}';
    }
}
