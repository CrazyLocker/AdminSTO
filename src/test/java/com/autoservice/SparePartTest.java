package com.autoservice;

import com.autoservice.model.ServiceCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-тесты складских операций {@link SparePart}.
 *
 * <p>Проверяют гибридный учёт: физический остаток ({@code stock}), резерв под
 * заказы ({@code reserved}) и минимальный остаток ({@code minStock}). Тесты
 * доводят до границ условия {@code qty <= 0}, {@code availableStock < qty} и
 * {@code stock < qty} — именно там скрывались непокрытые мутации.</p>
 */
@Tag(TestTags.UNIT)
class SparePartTest {

    @Test
    @DisplayName("Запчасть из полного конструктора: остаток, резерв 0, не требует сохранения")
    void newPartHasStockAndNoReservation() {
        SparePart part = new SparePart("Масло", 800, 1200, 10);

        assertThat(part.getStock()).isEqualTo(10);
        assertThat(part.getReserved()).isZero();
        assertThat(part.getAvailableStock()).isEqualTo(10);
        assertThat(part.getUnitType()).isEqualTo("шт");
        assertThat(part.isDirty()).as("прочитанная из БД запчасть не требует сохранения").isFalse();
    }

    @Test
    @DisplayName("Пустой конструктор: id=-1, единица измерения «шт», требует сохранения")
    void emptyConstructorMarksDirty() {
        SparePart part = new SparePart();

        assertThat(part.getId()).isEqualTo(-1);
        assertThat(part.getName()).isEmpty();
        assertThat(part.getUnitType()).isEqualTo("шт");
        assertThat(part.isDirty()).isTrue();
    }

    @Test
    @DisplayName("Доступный остаток = остаток − резерв")
    void availableStockSubtractsReservation() {
        SparePart part = new SparePart("Фильтр", 300, 450, 7);
        part.reserve(3);

        assertThat(part.getAvailableStock()).isEqualTo(4);
    }

    @Test
    @DisplayName("reserve(0) и reserve(<0) считаются успешными и не меняют резерв")
    void reserveNonPositiveIsNoOp() {
        SparePart part = new SparePart("Масло", 800, 1200, 10);

        assertThat(part.reserve(0)).isTrue();
        assertThat(part.reserve(-5)).isTrue();
        assertThat(part.getReserved()).isZero();
    }

    @Test
    @DisplayName("reserve больше доступного остатка отклоняется")
    void reserveMoreThanAvailableFails() {
        SparePart part = new SparePart("Масло", 800, 1200, 10);
        part.reserve(8);

        assertThat(part.reserve(3)).as("доступно только 2").isFalse();
        assertThat(part.getReserved()).isEqualTo(8);
    }

    @Test
    @DisplayName("reserve ровно на доступный остаток разрешён")
    void reserveExactlyAvailableSucceeds() {
        SparePart part = new SparePart("Масло", 800, 1200, 10);
        part.reserve(4);

        assertThat(part.reserve(6)).isTrue();
        assertThat(part.getAvailableStock()).isZero();
        assertThat(part.getReserved()).isEqualTo(10);
    }

    @Test
    @DisplayName("reserve помечает запчасть изменённой")
    void reserveMarksDirty() {
        SparePart part = new SparePart("Масло", 800, 1200, 10);
        part.markClean();

        part.reserve(2);

        assertThat(part.isDirty()).isTrue();
    }

    @Test
    @DisplayName("unreserve(0) и unreserve(<0) ничего не делают")
    void unreserveNonPositiveIsNoOp() {
        SparePart part = new SparePart("Масло", 800, 1200, 10);
        part.reserve(4);

        part.unreserve(0);
        part.unreserve(-3);

        assertThat(part.getReserved()).isEqualTo(4);
    }

    @Test
    @DisplayName("unreserve уменьшает резерв и не уходит в минус")
    void unreserveNeverGoesBelowZero() {
        SparePart part = new SparePart("Масло", 800, 1200, 10);
        part.reserve(3);

        part.unreserve(5);

        assertThat(part.getReserved()).isZero();
        assertThat(part.getAvailableStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("unreserve помечает запчасть изменённой")
    void unreserveMarksDirty() {
        SparePart part = new SparePart("Масло", 800, 1200, 10);
        part.reserve(3);
        part.markClean();

        part.unreserve(1);

        assertThat(part.isDirty()).isTrue();
        assertThat(part.getReserved()).isEqualTo(2);
    }

    @Test
    @DisplayName("deduct(0) считается успешным и ничего не списывает")
    void deductNonPositiveIsNoOp() {
        SparePart part = new SparePart("Масло", 800, 1200, 10);

        assertThat(part.deduct(0)).isTrue();
        assertThat(part.getStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("deduct больше остатка отклоняется")
    void deductMoreThanStockFails() {
        SparePart part = new SparePart("Масло", 800, 1200, 5);

        assertThat(part.deduct(6)).isFalse();
        assertThat(part.getStock()).isEqualTo(5);
    }

    @Test
    @DisplayName("deduct уменьшает остаток и снимает резерв")
    void deductRemovesStockAndReservation() {
        SparePart part = new SparePart("Масло", 800, 1200, 5);
        part.reserve(2);

        assertThat(part.deduct(3)).isTrue();

        assertThat(part.getStock()).isEqualTo(2);
        assertThat(part.getReserved()).as("резерв снят при списании").isZero();
    }

    @Test
    @DisplayName("deduct ровно на весь остаток обнуляет склад")
    void deductWholeStockSucceeds() {
        SparePart part = new SparePart("Масло", 800, 1200, 5);

        assertThat(part.deduct(5)).isTrue();
        assertThat(part.getStock()).isZero();
    }

    @Test
    @DisplayName("deductStock: неположительное количество и нехватка остатка — отказ")
    void deductStockRejectsInvalidQuantities() {
        SparePart part = new SparePart("Масло", 800, 1200, 5);

        assertThat(part.deductStock(0)).isFalse();
        assertThat(part.deductStock(-1)).isFalse();
        assertThat(part.deductStock(6)).isFalse();
        assertThat(part.getStock()).isEqualTo(5);
    }

    @Test
    @DisplayName("deductStock не снимает резерв и помечает dirty")
    void deductStockReducesStockOnly() {
        SparePart part = new SparePart("Масло", 800, 1200, 5);
        part.reserve(2);
        part.markClean();

        assertThat(part.deductStock(1)).isTrue();

        assertThat(part.getStock()).isEqualTo(4);
        assertThat(part.getReserved()).as("резерв не трогаем").isEqualTo(2);
        assertThat(part.isDirty()).isTrue();
    }

    @Test
    @DisplayName("addStock игнорирует неположительный приход")
    void addStockIgnoresNonPositive() {
        SparePart part = new SparePart("Масло", 800, 1200, 5);

        part.addStock(0);
        part.addStock(-2);

        assertThat(part.getStock()).isEqualTo(5);
    }

    @Test
    @DisplayName("addStock увеличивает остаток и помечает dirty")
    void addStockIncreasesStock() {
        SparePart part = new SparePart("Масло", 800, 1200, 5);
        part.markClean();

        part.addStock(3);

        assertThat(part.getStock()).isEqualTo(8);
        assertThat(part.isDirty()).isTrue();
    }

    @Test
    @DisplayName("needsRestock: доступный остаток ниже минимума")
    void needsRestockTrueWhenAvailableBelowMin() {
        SparePart part = new SparePart("Масло", 800, 1200, 5);
        part.setMinStock(10);

        assertThat(part.needsRestock()).isTrue();
    }

    @Test
    @DisplayName("needsRestock учитывает резерв, а не весь остаток")
    void needsRestockUsesAvailableStock() {
        SparePart part = new SparePart("Масло", 800, 1200, 10);
        part.setMinStock(5);
        part.reserve(8);

        assertThat(part.getStock()).as("остаток формально выше минимума").isEqualTo(10);
        assertThat(part.needsRestock()).isTrue();
    }

    @Test
    @DisplayName("needsRestock false, когда доступного остатка хватает")
    void needsRestockFalseWhenEnoughStock() {
        SparePart part = new SparePart("Масло", 800, 1200, 10);
        part.setMinStock(5);

        assertThat(part.needsRestock()).isFalse();
    }

    @Test
    @DisplayName("setStock не помечает dirty, если значение не изменилось")
    void setStockSameValueKeepsDirtyFlag() {
        SparePart part = new SparePart("Масло", 800, 1200, 10);
        part.markClean();

        part.setStock(10);

        assertThat(part.isDirty()).isFalse();
    }

    @Test
    @DisplayName("setStock с новым значением помечает dirty")
    void setStockChangedMarksDirty() {
        SparePart part = new SparePart("Масло", 800, 1200, 10);
        part.markClean();

        part.setStock(4);

        assertThat(part.getStock()).isEqualTo(4);
        assertThat(part.isDirty()).isTrue();
    }

    @Test
    @DisplayName("setReserved не помечает dirty при том же значении")
    void setReservedSameValueKeepsDirtyFlag() {
        SparePart part = new SparePart("Масло", 800, 1200, 10);
        part.markClean();

        part.setReserved(0);

        assertThat(part.isDirty()).isFalse();
    }

    @Test
    @DisplayName("setReserved с новым значением помечает dirty")
    void setReservedChangedMarksDirty() {
        SparePart part = new SparePart("Масло", 800, 1200, 10);
        part.markClean();

        part.setReserved(2.5);

        assertThat(part.getReserved()).isEqualTo(2.5);
        assertThat(part.isDirty()).isTrue();
    }

    @Test
    @DisplayName("setCategory(null) не роняет запчасть и откатывается к REGULAR")
    void setCategoryNullFallsBackToRegular() {
        SparePart part = new SparePart("Масло", 800, 1200, 10);
        part.setCategory(ServiceCategory.TUNING);

        part.setCategory(null);

        assertThat(part.getCategory()).isEqualTo(ServiceCategory.REGULAR);
    }

    @Test
    @DisplayName("getStockFormatted печатает остаток с единицей измерения")
    void getStockFormattedIncludesUnit() {
        SparePart part = new SparePart("Масло", 800, 1200, 7);
        part.setUnitType("л");

        assertThat(part.getStockFormatted()).isEqualTo("7 л");
    }

    @Test
    @DisplayName("toString показывает наименование, остаток и примечание")
    void toStringShowsStockAndNote() {
        SparePart part = new SparePart("Масло", 800, 1200, 7);
        part.setNote("Shell");

        assertThat(part.toString()).isEqualTo("Масло — 7 шт (Shell)");

        part.setNote("");
        assertThat(part.toString()).as("пустое примечание не печатается").isEqualTo("Масло — 7 шт");

        part.setNote(null);
        assertThat(part.toString()).as("null-примечание не печатается").isEqualTo("Масло — 7 шт");
    }
}
