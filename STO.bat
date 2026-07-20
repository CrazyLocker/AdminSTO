@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: ============================================
:: AdminSTO - Администратор СТО (Portable)
:: ============================================

cd /d "%~dp0"

:: Проверка наличия JRE
if not exist "jre\bin\java.exe" (
    echo ОШИБКА: Не найдена Java Runtime Environment!
    echo Убедитесь, что вся папка скопирована целиком.
    pause
    exit /b 1
)

:: Проверка наличия JavaFX JAR
if not exist "lib\javafx.controls.jar" (
    echo ОШИБКА: Не найдены библиотеки JavaFX!
    echo Убедитесь, что вся папка скопирована целиком.
    pause
    exit /b 1
)

set APP_DIR=%CD%

echo ============================================
echo  AdminSTO - Администратор СТО
echo ============================================
echo.
echo Запуск...
echo.

:: Запуск приложения:
:: -fat JAR на classpath (классы приложения + non-JavaFX зависимости)
:: -JavaFX JAR на module-path (с нативными DLL в lib/)
:: -Данные и конфиги в папке приложения (portable)
jre\bin\java.exe ^
    -Duser.language=ru ^
    -Duser.country=RU ^
    -Dfile.encoding=UTF-8 ^
    -Dapp.home="!APP_DIR!" ^
    -Djava.library.path="!APP_DIR!\lib" ^
    --module-path "lib" ^
    --add-modules javafx.controls,javafx.fxml ^
    --add-opens javafx.controls/javafx.scene.control.skin=ALL-UNNAMED ^
    --add-opens javafx.graphics/javafx.scene=ALL-UNNAMED ^
    --add-opens javafx.graphics/javafx.scene.effect=ALL-UNNAMED ^
    --add-opens javafx.graphics/javafx.scene.shape=ALL-UNNAMED ^
    --add-opens javafx.base/com.sun.javafx.event=ALL-UNNAMED ^
    --add-opens javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED ^
    --add-opens javafx.controls/com.sun.javafx.scene.control.skin=ALL-UNNAMED ^
    --add-opens javafx.graphics/com.sun.javafx.stage=ALL-UNNAMED ^
    --add-opens javafx.base/com.sun.javafx.binding=ALL-UNNAMED ^
    --add-opens javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED ^
    --add-opens javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED ^
    -cp "autoservice-admin.jar;styles.css" ^
    com.autoservice.Launcher

if errorlevel 1 (
    echo.
    echo ============================================
    echo  ОШИБКА запуска!
    echo ============================================
    echo.
    echo Проверьте:
    echo   1. Вся папка скопирована целиком?
    echo   2. Windows 64-bit?
    echo   3. Файлы не заблокированы антивирусом?
    echo.
    pause
)
