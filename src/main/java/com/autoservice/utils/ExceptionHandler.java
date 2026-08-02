package com.autoservice.utils;

import com.autoservice.WorkOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.format.DateTimeParseException;

/**
 * Глобальный обработчик исключений для UI.
 * Перехватывает все необработанные исключения и показывает пользователю понятные сообщения.
 */
public class ExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandler.class);
    
    /**
     * Обработчик неотловленных исключений в UI потоке.
     */
    public static final Thread.UncaughtExceptionHandler UI_HANDLER = (t, e) -> {
        String errorMessage = getFriendlyMessage(e);
        String technicalDetails = getTechnicalDetails(e);
        
        showErrorDialog(errorMessage, technicalDetails);
        logException(e, technicalDetails);
    };
    
    /**
     * Получить понятное сообщение для пользователя.
     * @param e исключение
     * @return понятное сообщение
     */
    public static String getFriendlyMessage(Throwable e) {
        if (e instanceof SQLException) {
            return UserFriendlyErrorMessage.DB_CONNECTION_ERROR;
        } else if (e instanceof NumberFormatException) {
            return UserFriendlyErrorMessage.INVALID_PRICE;
        } else if (e instanceof DateTimeParseException) {
            return UserFriendlyErrorMessage.INVALID_DATE;
        } else if (e instanceof IllegalArgumentException) {
            return UserFriendlyErrorMessage.GENERAL_ERROR;
        } else if (e instanceof NullPointerException) {
            return UserFriendlyErrorMessage.CLIENT_NOT_FOUND;
        } else if (e instanceof SecurityException) {
            return UserFriendlyErrorMessage.GENERAL_ERROR;
        } else {
            return UserFriendlyErrorMessage.GENERAL_ERROR;
        }
    }
    
    /**
     * Получить технические детали исключения.
     * @param e исключение
     * @return технические детали
     */
    public static String getTechnicalDetails(Throwable e) {
        StringBuilder details = new StringBuilder();
        details.append("Тип: ").append(e.getClass().getSimpleName()).append("\n");
        details.append("Сообщение: ").append(e.getMessage()).append("\n");
        details.append("Класс: ").append(e.getClass().getName()).append("\n");
        
        StackTraceElement[] stackTrace = e.getStackTrace();
        if (stackTrace.length > 0) {
            details.append("Место возникновения:\n");
            for (int i = 0; i < Math.min(3, stackTrace.length); i++) {
                details.append("  - ").append(stackTrace[i].getClassName())
                       .append(".").append(stackTrace[i].getMethodName())
                       .append(" (").append(stackTrace[i].getFileName())
                       .append(":").append(stackTrace[i].getLineNumber()).append(")\n");
            }
        }
        
        // Добавить причину если есть
        Throwable cause = e.getCause();
        if (cause != null && cause != e) {
            details.append("\nПричина:\n");
            details.append("  - ").append(cause.getClass().getSimpleName())
                   .append(": ").append(cause.getMessage()).append("\n");
        }
        
        return details.toString();
    }
    
    /**
     * Показать диалог ошибки.
     * @param message понятное сообщение
     * @param technicalDetails технические детали
     */
    private static void showErrorDialog(String message, String technicalDetails) {
        // В реальном приложении здесь должен быть вызов JavaFX Alert
        logger.error("КРИТИЧЕСКАЯ ОШИБКА: {}", message);
        logger.error("Технические детали:\n{}", technicalDetails);
    }
    
    /**
     * Записать исключение в лог.
     * @param e исключение
     * @param technicalDetails технические детали
     */
    private static void logException(Throwable e, String technicalDetails) {
        logger.error("Критическая ошибка в UI", e);
        logger.error("Технические детали:\n{}", technicalDetails);
    }
    
    /**
     * Обработать исключение в служебном методе (без UI).
     * @param e исключение
     * @param context контекст операции
     * @return true если ошибка обработана, false если нужно прервать выполнение
     */
    public static boolean handleServiceError(Throwable e, String context) {
        String friendlyMessage = getFriendlyMessage(e);
        String technicalDetails = getTechnicalDetails(e);
        
        logger.error("Ошибка в операции '{}': {}", context, e.getMessage(), e);
        logger.error("Детали:\n{}", technicalDetails);
        
        // Возвращаем false для прерывания выполнения
        return false;
    }
}
