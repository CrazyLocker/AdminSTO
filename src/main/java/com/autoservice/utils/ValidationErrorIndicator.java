package com.autoservice.utils;

import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * Утилита для визуальной индикации ошибок валидации в JavaFX UI.
 * Добавляет красную рамку и иконку ошибки к полям с невалидными данными.
 */
public class ValidationErrorIndicator {
    
    private static final String ERROR_STYLE_CLASS = "error-field";
    private static final Color ERROR_COLOR = Color.RED;
    private static final double ERROR_BORDER_WIDTH = 2.0;
    
    /**
     * Добавить индикатор ошибки к полю.
     * @param field поле (TextField или другой Control)
     * @param errorMessage сообщение об ошибке (для tooltip)
     */
    public static void showError(Node field, String errorMessage) {
        if (field == null) {
            return;
        }
        
        // Добавить стиль ошибки
        field.getStyleClass().add(ERROR_STYLE_CLASS);
        
        // Добавить tooltip с сообщением об ошибке
        if (errorMessage != null && !errorMessage.isEmpty()) {
            TooltipHelper.setToolTip(field, errorMessage);
        }
        
        // Для TextField добавить красную рамку
        if (field instanceof TextField) {
            TextField textField = (TextField) field;
            String originalStyle = textField.getStyle();
            if (originalStyle == null || !originalStyle.contains("border-color")) {
                textField.setStyle("-fx-border-color: " + toHex(ERROR_COLOR) + 
                                   "; -fx-border-width: " + ERROR_BORDER_WIDTH);
            }
        } else if (field instanceof Control) {
            Control control = (Control) field;
            String originalStyle = control.getStyle();
            if (originalStyle == null || !originalStyle.contains("border-color")) {
                control.setStyle("-fx-border-color: " + toHex(ERROR_COLOR) + 
                                 "; -fx-border-width: " + ERROR_BORDER_WIDTH);
            }
        }
    }
    
    /**
     * Убрать индикатор ошибки с поля.
     * @param field поле
     */
    public static void clearError(Node field) {
        if (field == null) {
            return;
        }
        
        // Убрать стиль ошибки
        field.getStyleClass().remove(ERROR_STYLE_CLASS);
        
        // Убрать tooltip
        TooltipHelper.clearTooltip(field);
        
        // Убрать красную рамку
        if (field instanceof TextField) {
            TextField textField = (TextField) field;
            String style = textField.getStyle();
            if (style != null && style.contains("-fx-border-color")) {
                // Удалить все свойства border
                style = style.replaceAll("-fx-border-color[^;]*;", "");
                style = style.replaceAll("-fx-border-width[^;]*;", "");
                textField.setStyle(style);
            }
        } else if (field instanceof Control) {
            Control control = (Control) field;
            String style = control.getStyle();
            if (style != null && style.contains("-fx-border-color")) {
                // Удалить все свойства border
                style = style.replaceAll("-fx-border-color[^;]*;", "");
                style = style.replaceAll("-fx-border-width[^;]*;", "");
                control.setStyle(style);
            }
        }
    }
    
    /**
     * Убрать все индикаторы ошибок с поля и его дочерних элементов.
     * @param root корневой узел
     */
    public static void clearAllErrors(Node root) {
        if (root == null) {
            return;
        }
        
        clearError(root);
        
        if (root instanceof Region) {
            Region region = (Region) root;
            region.getChildrenUnmodifiable().forEach(child -> clearAllErrors(child));
        }
    }
    
    /**
     * Преобразовать Color в HEX строку.
     * @param color цвет
     * @return HEX строка
     */
    private static String toHex(Color color) {
        return String.format("#%02X%02X%02X",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
    }
}
