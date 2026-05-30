@echo off
title AdminSTO - Система управления автосервисом

echo ============================================
echo   AdminSTO - Система управления автосервисом
echo ============================================
echo.

REM Проверяем наличие Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java не найдена!
    echo Пожалуйста, установите Java 17 или выше.
    echo Скачать можно с: https://adoptium.net/
    pause
    exit /b 1
)

REM Получаем путь к текущей папке
set "APP_DIR=%~dp0"

REM Запускаем приложение
echo Запускаем приложение...
cd /d "%APP_DIR%"
java --module-path "lib" --add-modules javafx.controls,javafx.fxml -jar "autoservice-admin-1.0-SNAPSHOT-jar-with-dependencies.jar"

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Произошла ошибка при запуске приложения.
    pause
)
