package com.autoservice.utils;

import com.autoservice.DataStore;
import com.autoservice.SparePart;

/**
 * Вспомогательные операции с остатками запчастей на складе.
 *
 * <p>Объединяет корректировку остатков, которую выполняют диалоги заказа при
 * добавлении запчастей и при отмене изменений.</p>
 */
public final class StockHelper {

    private StockHelper() {
    }

    /**
     * Проверяет наличие и списывает запрошенное количество со склада.
     *
     * @param part     запчасть
     * @param quantity количество
     * @return {@code true}, если остаток списан; {@code false}, если количество
     *         не положительное или превышает текущий остаток
     */
    public static boolean reserve(SparePart part, double quantity) {
        if (part == null || quantity <= 0 || quantity > part.getStock()) {
            return false;
        }
        DataStore.updateSparePartStock(part, part.getStock() - quantity);
        return true;
    }

    /**
     * Возвращает ранее списанное количество на склад.
     *
     * <p>Текущий остаток перечитывается из хранилища по ID, чтобы не опираться
     * на устаревший объект запчасти.</p>
     *
     * @param part     запчасть
     * @param quantity количество
     */
    public static void restore(SparePart part, double quantity) {
        if (part == null || quantity <= 0) {
            return;
        }
        SparePart current = DataStore.getSparePartById(part.getId());
        if (current == null) {
            return;
        }
        DataStore.updateSparePartStock(current, current.getStock() + quantity);
    }
}
