package com.autoservice;

/**
 * Утилита для создания типизированных тегов тестов.
 */
public class TestTags {
    /**
     * Unit-тесты (быстрые, изолированные).
     */
    public static final String UNIT = "unit";

    /**
     * Интеграционные тесты (с БД, сетью).
     */
    public static final String INTEGRATION = "integration";

    /**
     * Долгие тесты.
     */
    public static final String SLOW = "slow";

    /**
     * Тесты диалогов UI.
     */
    public static final String UI = "ui";

    /**
     * Тесты контроллеров.
     */
    public static final String CONTROLLER = "controller";

    /**
     * Тесты сервисов.
     */
    public static final String SERVICE = "service";
}
