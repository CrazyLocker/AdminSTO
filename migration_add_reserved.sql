-- Миграция: Добавление поля reserved для системы резервирования запчастей
-- Дата: 2026-08-10

-- Добавляем колонку reserved в таблицу spare_parts
ALTER TABLE spare_parts ADD COLUMN reserved DOUBLE DEFAULT 0;

-- Обновляем данные в DataStore (при запуске приложения)
-- reserved = 0 для всех существующих запчастей (по умолчанию)
