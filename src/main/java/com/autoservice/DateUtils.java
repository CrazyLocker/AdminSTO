package com.autoservice;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Утилитарный класс для форматирования и парсинга дат.
 * Поддерживает русский формат для отображения и ISO/dd/MM/yyyy для хранения в БД.
 */
public class DateUtils {

    private static final Locale RUSSIAN_LOCALE = new Locale.Builder().setLanguage("ru").build();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy 'г.'", RUSSIAN_LOCALE);
    private static final DateTimeFormatter DB_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Проверяет, является ли дата выходным днём (суббота или воскресенье).
     * @param date дата для проверки
     * @return true если это выходной день
     */
    public static boolean isWeekend(LocalDate date) {
        if (date == null) return false;
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    /**
     * Форматирует дату в русский формат (например, "15 июля 2026 г.").
     * @param date дата для форматирования
     * @return отформатированная строка или пустая строка, если дата null
     */
    public static String formatDate(LocalDate date) {
        if (date == null) return "";
        return date.format(FORMATTER);
    }

    /**
     * Преобразует строку даты в русский формат отображения.
     * Пытается распарсить дату из ISO или dd/MM/yyyy формата.
     * @param dateString строка с датой
     * @return отформатированная строка или оригинальная строка при ошибке
     */
    public static String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) return "";
        try {
            // Пробуем парсить как ISO-8601 или dd/MM/yyyy
            LocalDate date = parseDate(dateString);
            return date.format(FORMATTER);
        } catch (Exception e) {
            return dateString;
        }
    }

    /**
     * Парсит строку даты в LocalDate.
     * Поддерживает ISO-8601 (yyyy-MM-dd) и dd/MM/yyyy форматы.
     * @param dateString строка с датой
     * @return LocalDate или null при ошибке парсинга
     */
    public static LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) return null;
        try {
            // Сначала пробуем ISO-8601
            return LocalDate.parse(dateString);
        } catch (Exception e) {
            // Если не удалось, пробуем dd/MM/yyyy
            try {
                return LocalDate.parse(dateString, DB_FORMATTER);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    /**
     * Форматирует дату для сохранения в базу данных (dd/MM/yyyy).
     * @param date дата для форматирования
     * @return строка в формате dd/MM/yyyy или пустая строка, если дата null
     */
    public static String formatDateForDB(LocalDate date) {
        if (date == null) return "";
        return date.format(DB_FORMATTER);
    }
}
