package com.autoservice.utils;

import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

/**
 * Утилита для работы с Tooltip в JavaFX.
 */
public class TooltipHelper {
    
    /**
     * Установить tooltip для узла.
     * @param node узел
     * @param text текст tooltip
     */
    public static void setToolTip(Node node, String text) {
        if (node == null || text == null || text.isEmpty()) {
            return;
        }
        
        Tooltip tooltip = new Tooltip(text);
        tooltip.setShowDelay(Duration.ZERO);
        Tooltip.install(node, tooltip);
    }
    
    /**
     * Удалить tooltip из узла.
     * @param node узел
     */
    public static void clearTooltip(Node node) {
        if (node == null) {
            return;
        }
        
        // Для удаления tooltip нужен хак - сохраняем ссылку при установке
        // В JavaFX нет стандартного способа получить tooltip из узла
        // Поэтому просто uninstall без проверки
        Tooltip tooltip = new Tooltip("");
        tooltip.setShowDelay(Duration.ZERO);
        Tooltip.uninstall(node, tooltip);
    }
    
    /**
     * Обновить текст tooltip для узла.
     * @param node узел
     * @param text новый текст
     */
    public static void updateTooltip(Node node, String text) {
        clearTooltip(node);
        setToolTip(node, text);
    }
}
