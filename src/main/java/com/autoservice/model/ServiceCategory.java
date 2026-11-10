package com.autoservice.model;

/**
 * Категория услуги или запчасти для системы лояльности.
 *
 * <p>Категория определяет, какая шкала лояльности применяется к строке заказа:
 * <ul>
 *     <li>{@link #REGULAR} — обычные позиции без скидки по лояльности;</li>
 *     <li>{@link #MAINTENANCE} — работы и расходники ТО (шкала звёзд ТО);</li>
 *     <li>{@link #TUNING} — тюнинг (шкала накопленной суммы тюнинга).</li>
 * </ul>
 *
 * <p>В базе данных категория хранится строкой — именем константы. Для старых
 * записей и неизвестных значений используется {@link #REGULAR}.
 */
public enum ServiceCategory {

    REGULAR("Обычный"),
    MAINTENANCE("ТО"),
    TUNING("Тюнинг");

    private final String displayName;

    ServiceCategory(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Отображаемое название категории для интерфейса.
     * @return название на русском языке
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Разобрать категорию из значения, прочитанного из базы данных.
     * @param value имя константы или {@code null}
     * @return найденная категория; {@link #REGULAR}, если значение пустое или неизвестно
     */
    public static ServiceCategory fromDatabase(String value) {
        if (value == null || value.isBlank()) {
            return REGULAR;
        }
        try {
            return valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            return REGULAR;
        }
    }
}
