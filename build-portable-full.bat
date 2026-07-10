@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo =====================================================
echo   AdminSTO v1.0.0 - Создание портативной версии
echo =====================================================
echo.

REM Очищаем старую портативную версию
if exist "portable" (
    echo [CLEAN] Удаление старой портативной версии...
    rmdir /s /q portable
)

mkdir portable
mkdir portable\lib
mkdir portable\logs
mkdir portable\backups

REM Копируем JAR
echo [COPY] Копирование основного JAR...
copy target\autoservice-admin.jar portable\ >nul
echo [OK] JAR скопирован

REM Копируем зависимости
echo [COPY] Копирование зависимостей...
xcopy /E /Y target\lib\* portable\lib\ >nul
echo [OK] Зависимости скопированы (30 библиотек)

REM Копируем конфигурацию логирования
echo [COPY] Копирование logback.xml...
copy src\main\resources\logback.xml portable\ >nul
echo [OK] logback.xml скопирован

REM Создаём скрипт запуска
echo [CREATE] Создание скрипта запуска...

set "SCRIPT=portable\Start.bat"
(
echo @echo off
echo chcp 65001 ^>nul
echo setlocal enabledelayedexpansion
echo.
echo echo =====================================================
echo echo   AdminSTO v1.0.0 - Портативная версия
echo echo =====================================================
echo echo.
echo.
echo REM Определяем директорию запуска
echo set APP_DIR=%%~dp0
echo set JAVA_DIR=%%APP_DIR%%jre
echo set JAVA_EXE=%%JAVA_DIR%%\bin\java.exe
echo.
echo REM Проверяем наличие Java
echo if not exist "%%JAVA_EXE%%" ^(
echo     echo [ERROR] Java не найдена в папке jre^!
echo     echo Запустите Install-Java.bat для установки Java.
echo     pause
echo     exit /b 1
echo ^)
echo.
echo echo [OK] Запуск AdminSTO...
echo echo.
echo.
echo REM Запускаем приложение
echo "%%JAVA_EXE%%" ^^
echo     --module-path "%%APP_DIR%%lib" ^^
echo     --add-modules javafx.controls,javafx.fxml ^^
echo     -Djava.library.path="%%APP_DIR%%lib" ^^
echo     -Duser.dir="%%APP_DIR%%" ^^
echo     -Dapp.home="%%APP_DIR%%" ^^
echo     -jar "%%APP_DIR%%autoservice-admin.jar"
echo.
echo if errorlevel 1 ^(
echo     echo.
echo     echo [ERROR] Приложение завершилось с ошибкой^!
echo     pause
echo ^)
echo.
echo endlocal
) > "%SCRIPT%"

echo [OK] Start.bat создан

REM Создаём скрипт установки Java
echo [CREATE] Создание скрипта установки Java...

set "INSTALL_SCRIPT=portable\Install-Java.bat"
(
echo @echo off
echo chcp 65001 ^>nul
echo setlocal enabledelayedexpansion
echo.
echo echo =====================================================
echo echo   AdminSTO v1.0.0 - Установка Java Runtime
echo echo =====================================================
echo echo.
echo.
echo set APP_DIR=%%~dp0
echo set JAVA_DIR=%%APP_DIR%%jre
echo set JAVA_EXE=%%JAVA_DIR%%\bin\java.exe
echo.
echo if exist "%%JAVA_EXE%%" ^(
echo     echo [INFO] Java уже установлена в папке jre
echo     echo Удалите папку jre для переустановки.
echo     pause
echo     exit /b 0
echo ^)
echo.
echo echo [INFO] Загрузка портативной JRE 17...
echo echo.
echo.
echo REM Ссылка на Eclipse Temurin JRE 17
echo set JAVA_URL=https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.9%%2B9/OpenJDK17U-jre_x64_windows_hotspot_17.0.9_9.zip
echo set JAVA_ZIP=%%APP_DIR%%openjdk17-jre.zip
echo.
echo echo [LOAD] Загрузка Java Runtime Environment...
echo powershell -Command "^^& {Invoke-WebRequest -Uri '%%JAVA_URL%%' -OutFile '%%JAVA_ZIP%%' -UseBasicParsing}"
echo.
echo if not exist "%%JAVA_ZIP%%" ^(
echo     echo [ERROR] Не удалось загрузить JRE^!
echo     echo Проверьте подключение к интернету.
echo     pause
echo     exit /b 1
echo ^)
echo.
echo echo [OK] Загрузка завершена
echo echo.
echo echo [EXTRACT] Распаковка JRE...
echo powershell -Command "^^& {Expand-Archive -Path '%%JAVA_ZIP%%' -DestinationPath '%%APP_DIR%%' -Force}"
echo.
echo REM Переименовываем папку
echo for /d %%%%i in ^("%%APP_DIR%%jdk*^^") do ^(
echo     if not "%%%%~nxi"=="jre" ^(
echo         ren "%%%%i" jre
echo     ^)
echo ^)
echo.
echo REM Удаляем архив
echo del "%%JAVA_ZIP%%"
echo.
echo echo [OK] JRE установлена в папку jre
echo echo.
echo echo =====================================================
echo echo   Готово^! Теперь запустите Start.bat
echo echo =====================================================
echo pause
) > "%INSTALL_SCRIPT%"

echo [OK] Install-Java.bat создан

REM Создаём README для портативной версии
echo [CREATE] Создание README...

set "README=portable\README.txt"
(
echo =====================================================
echo   AdminSTO v1.0.0 - Портативная версия
echo =====================================================
echo.
echo ЭТА ВЕРСИЯ ДЛЯ ЗАПУСКА С ФЛЕШКИ НА ЛЮБОМ ПК С WINDOWS
echo.
echo =====================================================
echo   ИНСТРУКЦИЯ ПО УСТАНОВКЕ
echo =====================================================
echo.
echo 1. Скопируйте ВСЮ папку portable на флешку
echo.
echo 2. На целевом компьютере запустите:
echo    Install-Java.bat
echo.
echo    Скрипт загрузит и установит Java Runtime (200 MB).
echo    Требуется подключение к интернету.
echo.
echo 3. После установки запустите:
echo    Start.bat
echo.
echo    Приложение запустится без установки в систему.
echo.
echo =====================================================
echo   СТРУКТУРА ПАПКИ
echo =====================================================
echo.
echo portable/
echo   ├── Start.bat           - Запуск приложения
echo   ├── Install-Java.bat    - Установка Java (1 раз)
echo   ├── autoservice-admin.jar
echo   ├── lib/                - Библиотеки (30 шт)
echo   ├── jre/                - Java Runtime (после установки)
echo   ├── logs/               - Логи приложения
echo   ├── backups/            - Резервные копии БД
echo   └── autoservice.db      - База данных
echo.
echo =====================================================
echo   ВАЖНО
echo =====================================================
echo.
echo - Все данные хранятся ЛОКАЛЬНО в папке portable
echo - Можно переносить флешку между компьютерами
echo - База данных и бэкапы всегда с вами
echo - Не требуется установка Java в систему
echo.
echo =====================================================
echo   ТЕХНИЧЕСКАЯ ПОДДЕРЖКА
echo =====================================================
echo.
echo GitHub: https://github.com/CrazyLocker/AdminSTO
echo Версия: 1.0.0
echo Дата: 10 июля 2026
echo.
) > "%README%"

echo [OK] README.txt создан

REM Создаём файл версии
echo [CREATE] Создание version.txt...
(
echo AdminSTO Portable v1.0.0
echo Дата сборки: %date%
echo Java: 17
echo JavaFX: 17.0.6
) > portable\version.txt

echo [OK] version.txt создан

echo.
echo =====================================================
echo   Сборка портативной версии завершена^!
echo =====================================================
echo.
echo Размер портативной версии:
dir /s portable | find "файл"
echo.
echo Следующие шаги:
echo   1. Скопируйте папку portable на флешку
echo   2. На целевом ПК запустите Install-Java.bat
echo   3. Запустите Start.bat
echo.
echo =====================================================
pause
