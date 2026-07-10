package com.autoservice.utils;

/**
 * Класс для формирования понятных сообщений об ошибках для пользователя.
 * Все сообщения доступны централизованно для локализации.
 */
public class UserFriendlyErrorMessage {
    
    // ==================== ОБЩИЕ ОШИБКИ ====================
    
    public static final String GENERAL_ERROR = "Произошла непредвиденная ошибка. Пожалуйста, сохраните вашу работу и перезапустите приложение.";
    public static final String DATA_SAVE_ERROR = "Не удалось сохранить данные. Пожалуйста, проверьте правильность введенной информации.";
    public static final String DATA_LOAD_ERROR = "Не удалось загрузить данные. Проверьте подключение к базе данных.";
    public static final String CONNECTION_ERROR = "Не удалось подключиться к базе данных. Проверьте сетевое подключение.";
    
    // ==================== ОШИБКИ ВАЛИДАЦИИ ====================
    
    public static final String INVALID_PHONE = "Неверный формат номера телефона. Используйте формат: +79991234567";
    public static final String INVALID_CAR_NUMBER = "Неверный формат госномера. Используйте формат: А123ВС777";
    public static final String INVALID_DATE = "Неверный формат даты. Используйте формат: дд.мм.гггг";
    public static final String INVALID_PRICE = "Цена должна быть положительным числом";
    public static final String INVALID_QUANTITY = "Количество должно быть положительным числом";
    
    // ==================== ОШИБКИ КЛИЕНТОВ ====================
    
    public static final String CLIENT_NOT_FOUND = "Клиент не найден. Пожалуйста, проверьте данные.";
    public static final String CLIENT_ALREADY_EXISTS = "Клиент с таким номером телефона уже существует в базе.";
    public static final String CLIENT_DELETE_ERROR = "Невозможно удалить клиента - есть активные заказы.";
    public static final String CLIENT_SAVE_ERROR = "Не удалось сохранить клиента. Проверьте правильность данных.";
    
    // ==================== ОШИБКИ ЗАКАЗОВ ====================
    
    public static final String ORDER_NOT_FOUND = "Заказ не найден. Пожалуйста, проверьте ID заказа.";
    public static final String ORDER_UPDATE_ERROR = "Не удалось обновить заказ. Пожалуйста, попробуйте снова.";
    public static final String ORDER_DELETE_ERROR = "Невозможно удалить заказ - он связан с другими записями.";
    public static final String ORDER_STATUS_ERROR = "Недопустимый переход статуса заказа.";
    public static final String ORDER_EMPTY = "Заказ не может быть пустым - добавьте услуги или запчасти.";
    
    // ==================== ОШИБКИ УСЛУГ ====================
    
    public static final String SERVICE_NOT_FOUND = "Услуга не найдена. Пожалуйста, проверьте название.";
    public static final String SERVICE_DELETE_ERROR = "Невозможно удалить услугу - она используется в заказах.";
    public static final String SERVICE_SAVE_ERROR = "Не удалось сохранить услугу. Проверьте правильность данных.";
    
    // ==================== ОШИБКИ ЗАПЧАСТЕЙ ====================
    
    public static final String SPARE_PART_NOT_FOUND = "Запчасть не найдена. Пожалуйста, проверьте название.";
    public static final String SPARE_PART_NOT_ENOUGH = "Недостаточно запчастей на складе. Доступно: ";
    public static final String SPARE_PART_DELETE_ERROR = "Невозможно удалить запчасть - она используется в заказах.";
    public static final String SPARE_PART_SAVE_ERROR = "Не удалось сохранить запчасть. Проверьте правильность данных.";
    public static final String SPARE_PART_STOCK_ERROR = "Остаток запчасти не может быть отрицательным.";
    
    // ==================== ОШИБКИ ЗАПИСИ ====================
    
    public static final String APPOINTMENT_NOT_FOUND = "Запись не найдена. Пожалуйста, проверьте данные.";
    public static final String APPOINTMENT_CONFLICT = "Это время уже занято другой записью.";
    public static final String APPOINTMENT_DELETE_ERROR = "Невозможно удалить запись - она уже выполняется.";
    public static final String APPOINTMENT_SAVE_ERROR = "Не удалось сохранить запись. Проверьте правильность данных.";
    
    // ==================== ОШИБКИ БАЗЫ ДАННЫХ ====================
    
    public static final String DB_CONNECTION_ERROR = "Ошибка подключения к базе данных. Проверьте файл базы данных.";
    public static final String DB_OPERATION_ERROR = "Ошибка выполнения операции с базой данных.";
    public static final String DB_TABLE_ERROR = "Ошибка структуры базы данных. Попробуйте пересоздать таблицы.";
    public static final String DB_DUPLICATE_ERROR = "Запись с таким уникальным ключом уже существует.";
    
    // ==================== МЕТОДЫ ПОЛУЧЕНИЯ СООБЩЕНИЙ ====================
    
    /**
     * Получить сообщение об ошибке для запчастей с недостаточным остатком.
     * @param available доступное количество
     * @param requested запрошенное количество
     * @return понятное сообщение
     */
    public static String getNotEnoughSparePartMessage(double available, double requested) {
        return SPARE_PART_NOT_ENOUGH + String.format("%.0f", available) + ". Запрошено: " + String.format("%.0f", requested);
    }
    
    /**
     * Получить сообщение об ошибке для запчастей с недостаточным остатком (штук).
     * @param available доступное количество
     * @param requested запрошенное количество
     * @return понятное сообщение
     */
    public static String getNotEnoughSparePartMessage(int available, int requested) {
        return SPARE_PART_NOT_ENOUGH + available + " шт. Запрошено: " + requested;
    }
    
    /**
     * Получить сообщение об ошибке для недопустимого перехода статуса.
     * @param currentStatus текущий статус
     * @param newStatus новый статус
     * @return понятное сообщение
     */
    public static String getInvalidStatusTransitionMessage(String currentStatus, String newStatus) {
        return "Недопустимый переход из статуса '" + currentStatus + "' в статус '" + newStatus + "'.";
    }
}
