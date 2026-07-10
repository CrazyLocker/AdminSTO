package com.autoservice;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DateUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy 'г.'", new Locale("ru"));
    private static final DateTimeFormatter DB_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static String formatDate(LocalDate date) {
        if (date == null) return "";
        return date.format(FORMATTER);
    }

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

    public static String formatDateForDB(LocalDate date) {
        if (date == null) return "";
        return date.format(DB_FORMATTER);
    }
}
