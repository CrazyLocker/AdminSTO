package com.autoservice;

import java.util.List;

/**
 * Глобальные константы приложения.
 * Вынесены из диалогов и views для устранения дублирования (DRY).
 */
public final class AppConstants {

    private AppConstants() {
    }

    // ==================== ВРЕМЯ ====================

    /**
     * Доступные временные слоты для записей в календаре.
     */
    public static final List<String> TIME_SLOTS = List.of(
            "08:00", "09:00", "10:00", "11:00", "12:00", "13:00",
            "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00"
    );

    /**
     * Имена мастеров.
     */
    public static final List<String> MASTERS = List.of("Саныч", "Малой");

    // ==================== АВТОМОБИЛИ ====================

    /**
     * Поддерживаемые модели автомобилей GWM/Haval.
     */
    public static final List<String> GWM_MODELS = List.of(
            "Haval Jolion", "Haval F7", "Haval F7x", "Haval Dargo", "Haval Big Dog",
            "Haval H6", "Haval H9", "GWM Poer", "GWM Tank 300", "GWM Tank 500",
            "GWM Wingle 7", "GWM Cannon", "Great Wall Poer"
    );
}
