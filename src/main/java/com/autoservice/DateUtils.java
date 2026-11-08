package com.autoservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Утилитарный класс для форматирования и парсинга дат.
 * Поддерживает русский формат для отображения и ISO/dd/MM/yyyy для хранения в БД.
 */
public class DateUtils {

    private static final Logger logger = LoggerFactory.getLogger(DateUtils.class);

    private static final Locale RUSSIAN_LOCALE = new Locale.Builder().setLanguage("ru").build();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy 'г.'", RUSSIAN_LOCALE);
    private static final DateTimeFormatter DB_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter RUSSIAN_DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy", RUSSIAN_LOCALE);
    private static final String RUSSIAN_DATE_TIME_SEPARATOR = " г. в ";

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
     * Парсит дату и время из строки.
     *
     * <p>В нестрогом режиме ({@code strict == false}) поддерживаются форматы:
     * <ul>
     *   <li>{@code "18 июля 2026 г. в 12-00"} — русский формат;</li>
     *   <li>{@code "2026-07-18 12-00"} — ISO-формат даты;</li>
     *   <li>{@code "18/07/2026 12-00"} — формат {@code dd/MM/yyyy}.</li>
     * </ul>
     *
     * <p>В строгом режиме ({@code strict == true}) допускается только формат
     * {@code dd/MM/yyyy HH:mm}: дата должна содержать ровно два слеша, время —
     * не короче четырёх символов и находиться в диапазоне 00:00–23:59.
     * В обоих режимах время можно записывать через дефис или двоеточие.
     *
     * @param value строка с датой и временем
     * @param strict включает строгую проверку формата и диапазона времени
     * @return LocalDateTime или null, если строку не удалось разобрать
     */
    public static LocalDateTime parseDateTime(String value, boolean strict) {
        if (value == null || value.isEmpty()) return null;

        try {
            int russianSeparatorIdx = value.indexOf(RUSSIAN_DATE_TIME_SEPARATOR);
            if (!strict && russianSeparatorIdx > 0) {
                String dateStr = value.substring(0, russianSeparatorIdx).trim();
                String timeStr = value.substring(russianSeparatorIdx + RUSSIAN_DATE_TIME_SEPARATOR.length()).trim();
                LocalDate date = LocalDate.parse(dateStr, RUSSIAN_DATE_FORMATTER);
                LocalTime time = parseTime(timeStr, false);
                return time == null ? null : date.atTime(time);
            }

            // Ищем последний пробел, разделяющий дату и время.
            int spaceIdx = value.lastIndexOf(' ');
            if (spaceIdx <= 0 || spaceIdx >= value.length() - 1) {
                return null;
            }

            String dateStr = value.substring(0, spaceIdx).trim();
            String timeStr = value.substring(spaceIdx + 1).trim();
            LocalDate date = parseDatePart(dateStr, strict);
            if (date == null) {
                return null;
            }
            LocalTime time = parseTime(timeStr, strict);
            return time == null ? null : date.atTime(time);
        } catch (Exception e) {
            logger.debug("Ошибка парсинга даты-времени из строки '{}': {}", value, e.getMessage());
            return null;
        }
    }

    /**
     * Парсит дату из части строки с учётом режима строгости.
     *
     * @param dateStr строка с датой
     * @param strict включает проверку формата {@code dd/MM/yyyy}
     * @return LocalDate или null, если формат не распознан
     */
    private static LocalDate parseDatePart(String dateStr, boolean strict) {
        if (strict) {
            if (dateStr.length() < 10 || countChar(dateStr, '/') != 2) {
                logger.debug("Некорректный формат даты: {} (ожидается dd/MM/yyyy)", dateStr);
                return null;
            }
            return LocalDate.parse(dateStr, DB_FORMATTER);
        }
        if (dateStr.contains("-") && !dateStr.contains("/")) {
            return LocalDate.parse(dateStr);
        }
        if (dateStr.contains("/")) {
            return LocalDate.parse(dateStr, DB_FORMATTER);
        }
        return null;
    }

    /**
     * Парсит время из строки: час и минуту, разделённые дефисом или двоеточием.
     *
     * @param timeStr строка со временем
     * @param strict включает проверку длины строки и диапазона значений
     * @return LocalTime или null, если время некорректно
     */
    private static LocalTime parseTime(String timeStr, boolean strict) {
        if (strict && timeStr.length() < 4) {
            logger.debug("Некорректное время: {} (слишком короткое)", timeStr);
            return null;
        }

        String[] timeParts = timeStr.contains("-") ? timeStr.split("-") : timeStr.split(":");
        if (timeParts.length < 2) {
            return null;
        }

        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);
        if (strict && (hour < 0 || hour > 23 || minute < 0 || minute > 59)) {
            logger.debug("Некорректное время: {} (ожидается 00:00-23:59)", timeStr);
            return null;
        }
        return LocalTime.of(hour, minute);
    }

    /**
     * Считает вхождения символа в строку.
     *
     * @param value строка для проверки
     * @param target искомый символ
     * @return количество вхождений
     */
    private static int countChar(String value, char target) {
        int count = 0;
        for (char c : value.toCharArray()) {
            if (c == target) {
                count++;
            }
        }
        return count;
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
