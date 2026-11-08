package com.autoservice.model;

/**
 * Состояние колонки таблицы.
 *
 * @param id идентификатор колонки
 * @param width текущая ширина колонки
 * @param visible признак видимости колонки
 * @param index позиция колонки в таблице; {@code null}, если индекс не сохранён
 */
public record ColumnState(String id, double width, boolean visible, Integer index) {
}
