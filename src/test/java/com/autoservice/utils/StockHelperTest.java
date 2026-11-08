package com.autoservice.utils;

import com.autoservice.BaseTest;
import com.autoservice.DataStore;
import com.autoservice.SparePart;
import com.autoservice.TestTags;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты {@link StockHelper}: списание и возврат остатков запчастей.
 */
@Tag(TestTags.INTEGRATION)
class StockHelperTest extends BaseTest {

    @Test
    void reserveDeductsAvailableQuantity() {
        SparePart part = createPart(10);

        assertThat(StockHelper.reserve(part, 3)).isTrue();

        assertThat(currentStock(part)).isEqualTo(7);
    }

    @Test
    void reserveRejectsQuantityAboveStock() {
        SparePart part = createPart(2);

        assertThat(StockHelper.reserve(part, 3)).isFalse();

        assertThat(currentStock(part)).isEqualTo(2);
    }

    @Test
    void reserveRejectsNonPositiveQuantity() {
        SparePart part = createPart(5);

        assertThat(StockHelper.reserve(part, 0)).isFalse();
        assertThat(StockHelper.reserve(part, -1)).isFalse();

        assertThat(currentStock(part)).isEqualTo(5);
    }

    @Test
    void restoreReturnsQuantityToCurrentStock() {
        SparePart part = createPart(10);
        assertThat(StockHelper.reserve(part, 4)).isTrue();

        StockHelper.restore(part, 4);

        assertThat(currentStock(part)).isEqualTo(10);
    }

    @Test
    void restoreUsesCurrentStockInsteadOfStalePartInstance() {
        SparePart part = createPart(10);
        assertThat(StockHelper.reserve(part, 2)).isTrue();

        // Другая операция меняет остаток в хранилище: экземпляр part теперь
        // устарел и хранит значение 8.
        SparePart current = DataStore.getSparePartById(part.getId());
        DataStore.updateSparePartStock(current, 5);

        StockHelper.restore(part, 1);

        // Возврат должен опираться на актуальный остаток 5, а не на 8 из
        // устаревшего экземпляра.
        assertThat(currentStock(part)).isEqualTo(6);
    }

    @Test
    void restoreIgnoresPartMissingFromStore() {
        SparePart part = createPart(10);
        DataStore.removeSparePart(part);

        StockHelper.restore(part, 1);

        assertThat(DataStore.getSparePartById(part.getId())).isNull();
    }

    private static SparePart createPart(int stock) {
        SparePart part = new SparePart("Тестовая запчасть", 100, 200, stock);
        DataStore.addSparePart(part);
        return part;
    }

    private static double currentStock(SparePart part) {
        SparePart stored = DataStore.getSparePartById(part.getId());
        assertThat(stored).isNotNull();
        return stored.getStock();
    }
}
