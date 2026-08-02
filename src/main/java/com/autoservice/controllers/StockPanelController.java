package com.autoservice.controllers;

import com.autoservice.DataStore;
import com.autoservice.SparePart;
import com.autoservice.views.StockPanel;

/**
 * Контроллер для панели управления складом.
 */
public class StockPanelController {

    /**
     * Обновляет таблицу склада — делегирует в View.
     * НЕ вызывает setItems напрямую — это ломает SortedList.
     */
    public static void refreshTable() {
        StockPanel.refreshTable();
    }

    /**
     * Обновляет минимальный остаток и обновляет таблицу.
     */
    public static void updateMinStock(SparePart part, double minStock) {
        part.setMinStock(minStock);
        DataStore.updateSparePart(part);
        refreshTable();
    }

    /**
     * Вносит приход запчасти и обновляет таблицу.
     */
    public static void addStockIncome(SparePart part, double amount) {
        if (amount > 0) {
            double newStock = part.getStock() + amount;
            part.setStock(newStock);
            DataStore.updateSparePart(part);
            refreshTable();
        }
    }
}
