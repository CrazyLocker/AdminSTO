package com.autoservice.model;

/**
 * Состояние сортировки таблицы.
 *
 * @param columnId идентификатор колонки
 * @param order направление сортировки: {@code ASC} или {@code DESC}
 */
public record SortState(String columnId, String order) {
}
