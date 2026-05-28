package com.autoservice;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DateUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy 'г.'", new Locale("ru"));

    public static String formatDate(LocalDate date) {
        if (date == null) return "";
        return date.format(FORMATTER);
    }

    public static String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) return "";
        try {
            LocalDate date = LocalDate.parse(dateString);
            return date.format(FORMATTER);
        } catch (Exception e) {
            return dateString;
        }
    }

    public static LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) return null;
        try {
            return LocalDate.parse(dateString);
        } catch (Exception e) {
            return null;
        }
    }
}