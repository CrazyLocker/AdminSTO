package com.autoservice.controllers;

import com.autoservice.DataStore;
import com.autoservice.SparePart;
import com.autoservice.views.SparePartPanel;

import java.util.List;

/**
 * Контроллер для панели управления запчастями.
 */
public class SparePartPanelController {

    /**
     * Обновляет таблицу запчастей — делегирует в View.
     * НЕ вызывает setItems напрямую — это ломает SortedList.
     */
    public static void refreshTable() {
        SparePartPanel.refreshTable();
    }

    /**
     * Добавляет новую запчасть и обновляет таблицу.
     */
    public static void addSparePart(SparePart part) {
        DataStore.addSparePart(part);
        refreshTable();
    }

    /**
     * Обновляет существующую запчасть и обновляет таблицу.
     */
    public static void updateSparePart(SparePart part) {
        DataStore.updateSparePart(part);
        refreshTable();
    }

    /**
     * Удаляет выбранные запчасти и обновляет таблицу.
     */
    public static void removeSpareParts(List<SparePart> parts) {
        for (SparePart part : parts) {
            DataStore.removeSparePart(part);
        }
        refreshTable();
    }
}
