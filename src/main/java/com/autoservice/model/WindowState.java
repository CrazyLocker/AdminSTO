package com.autoservice.model;

/**
 * Состояние окна: позиция и размеры.
 *
 * @param x координата окна по оси X
 * @param y координата окна по оси Y
 * @param width ширина окна
 * @param height высота окна
 */
public record WindowState(double x, double y, double width, double height) {
}
