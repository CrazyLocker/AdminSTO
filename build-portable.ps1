﻿﻿﻿﻿# Скрипт сборки полностью автономной portable-версии
# Запуск: powershell -ExecutionPolicy Bypass -File build-portable.ps1

$ErrorActionPreference = "Stop"
$projectDir = $PSScriptRoot
$distDir = Join-Path $projectDir "portable"
$jarFile = Join-Path $projectDir "build/libs/autoservice-admin-1.1.0.jar"
$buildLib = Join-Path $projectDir "build/lib"

# Ищем JDK в системе
$possibleJdks = @(
    "C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot",
    "C:\Program Files\Java\jdk-17",
    "C:\Program Files\Java\jdk-21",
    "C:\Program Files\Eclipse Adoptium\jdk-21",
    "D:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot"
)

$JDK_PATH = $null
foreach ($path in $possibleJdks) {
    if (Test-Path "$path/bin/java.exe") {
        $JDK_PATH = $path
        break
    }
}

if (-not $JDK_PATH) {
    Write-Host "ОШИБКА: JDK не найден!" -ForegroundColor Red
    Write-Host "Пожалуйста, укажите путь к JDK в переменной JDK_PATH" -ForegroundColor Yellow
    exit 1
}

Write-Host "Используется JDK: $JDK_PATH" -ForegroundColor Cyan
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Сборка Portable-версии (автономная)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. Сборка проекта
Write-Host "[1/7] Сборка проекта..." -ForegroundColor Yellow
& "$projectDir\gradlew.bat" clean build -x test --no-daemon -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "ОШИБКА сборки!" -ForegroundColor Red
    exit 1
}
Write-Host "    Готово." -ForegroundColor Green

# 2. Очистка и подготовка portable
Write-Host "[2/7] Подготовка папки portable..." -ForegroundColor Yellow
if (Test-Path $distDir) { Remove-Item "$distDir/*" -Recurse -Force }
New-Item -ItemType Directory -Path "$distDir/lib" -Force | Out-Null
New-Item -ItemType Directory -Path "$distDir/logs" -Force | Out-Null
New-Item -ItemType Directory -Path "$distDir/backups" -Force | Out-Null
New-Item -ItemType Directory -Path "$distDir/config/table-state" -Force | Out-Null
New-Item -ItemType Directory -Path "$distDir/config/window-state" -Force | Out-Null
Write-Host "    Готово." -ForegroundColor Green

# 3. Создание минимального JRE через jlink
Write-Host "[3/7] Создание встроенного JRE (jlink)..." -ForegroundColor Yellow
& "$JDK_PATH/bin/jlink.exe" `
    --module-path "$JDK_PATH/jmods" `
    --add-modules java.base,java.sql,jdk.unsupported,java.scripting,java.desktop,java.logging,java.instrument,java.management,jdk.zipfs,java.sql.rowset,java.naming,java.security.sasl,java.xml,jdk.httpserver,jdk.management,jdk.localedata `
    --include-locales ru `
    --strip-debug `
    --no-man-pages `
    --no-header-files `
    --compress=2 `
    --output "$distDir/jre"
Write-Host "    JRE создан." -ForegroundColor Green

# 4. Копирование зависимостей из Gradle (copyDependencies -> build/lib)
Write-Host "[4/7] Копирование библиотек..." -ForegroundColor Yellow
if (Test-Path $buildLib) {
    Copy-Item "$buildLib/*" "$distDir/lib/" -Force
}

Write-Host "    Библиотеки скопированы." -ForegroundColor Green

# 5. Копирование JAR и ресурсов
Write-Host "[5/7] Копирование приложения и ресурсов..." -ForegroundColor Yellow
Copy-Item $jarFile "$distDir/autoservice-admin.jar" -Force

# Копируем logback.xml (конфигурация логирования)
$logbackSrc = Join-Path $projectDir "src/main/resources/logback.xml"
if (Test-Path $logbackSrc) {
    Copy-Item $logbackSrc "$distDir/logback.xml" -Force
}

# Копируем config/table-state если есть сохранённые настройки таблиц
$configStateDir = Join-Path $projectDir "config/table-state"
if (Test-Path $configStateDir) {
    Copy-Item "$configStateDir/*.json" "$distDir/config/table-state/" -Force
}

# Копируем config/window-state если есть сохранённые настройки окон
$configWindowDir = Join-Path $projectDir "config/window-state"
if (Test-Path $configWindowDir) {
    Copy-Item "$configWindowDir/*.json" "$distDir/config/window-state/" -Force
}

Write-Host "    Готово." -ForegroundColor Green

# 6. Создание run.bat
Write-Host "[6/7] Создание run.bat..." -ForegroundColor Yellow
$runBat = @'
@echo off
chcp 65001 >nul
setlocal

echo Launching AdminSTO Portable...
echo.

cd /d "%~dp0"

if not exist "jre\bin\java.exe" (
    echo ERROR: Java not found!
    pause
    exit /b 1
)

set APP_DIR=%CD%

jre\bin\java.exe ^
    --enable-native-access=javafx.graphics ^
    --enable-native-access=org.xerial.sqlitejdbc ^
    -Duser.language=ru ^
    -Duser.country=RU ^
    -Djava.library.path="%APP_DIR%\lib" ^
    -Duser.dir="%APP_DIR%" ^
    -Dapp.home="%APP_DIR%" ^
    --module-path "%APP_DIR%\lib" ^
    --add-modules javafx.controls,javafx.fxml ^
    -jar "%APP_DIR%\autoservice-admin.jar"

if errorlevel 1 (
    echo.
    echo Application failed!
    pause
)
'@
[System.IO.File]::WriteAllText("$distDir/run.bat", $runBat, [System.Text.Encoding]::UTF8)
Write-Host "    run.bat создан." -ForegroundColor Green

# 7. Создание README.txt и version.txt
Write-Host "[7/7] Создание документации..." -ForegroundColor Yellow

$buildDate = Get-Date -Format "dd MMMM yyyy"

$readme = @"
=====================================================
  AdminSTO - Портативная версия
=====================================================

ЭТА ВЕРСИЯ ДЛЯ ЗАПУСКА НА ЛЮБОМ ПК С WINDOWS
Java уже встроена - ничего устанавливать не нужно!

=====================================================
   ИНСТРУКЦИЯ
=====================================================

1. Скопируйте ВСЮ папку portable на флешку или в любую
   директорию на целевом компьютере.

2. Запустите run.bat

3. Приложение запустится автоматически.
   JDK/JRE в системе НЕ НУЖЕН!

=====================================================
   СТРУКТУРА ПАПКИ
=====================================================

portable/
  |-- run.bat              Запуск приложения
  |-- autoservice-admin.jar  Основное приложение
  |-- version.txt          Версия и дата сборки
  |-- lib/                 Библиотеки (JavaFX, SQLite, и т.д.)
  |-- jre/                 Встроенная Java Runtime
  |-- logs/                Логи приложения
  |-- backups/             Резервные копии БД
  |-- config/              Конфигурация:
  |   |-- table-state/      Настройки таблиц (ширина, порядок, сортировка)
  |   '-- window-state/     Настройки окон (позиция, размеры)
  '-- autoservice.db       База данных (создаётся при первом запуске)

=====================================================
   ВАЖНО
=====================================================

- Все данные хранятся ЛОКАЛЬНО в папке portable
- Можно переносить папку между компьютерами
- База данных и бэкапы всегда с вами
- Не требуется установка Java в систему

=====================================================
   ТЕХНИЧЕСКАЯ ПОДДЕРЖКА
=====================================================

GitHub: https://github.com/CrazyLocker/AdminSTO
Дата сборки: $buildDate
"@
[System.IO.File]::WriteAllText("$distDir/README.txt", $readme, [System.Text.Encoding]::UTF8)

$version = @"
AdminSTO Portable
Дата сборки: $buildDate
Java: встроенная (jlink)
JavaFX: 17.0.6
"@
[System.IO.File]::WriteAllText("$distDir/version.txt", $version, [System.Text.Encoding]::UTF8)

Write-Host "    Документация создана." -ForegroundColor Green

# Итог
$folderSize = (Get-ChildItem -Path $distDir -Recurse | Measure-Object -Property Length -Sum).Sum
$sizeMB = [math]::Round($folderSize / 1MB, 2)

Write-Host ""
Write-Host "=====================================================" -ForegroundColor Green
Write-Host "  Portable-версия собрана успешно!" -ForegroundColor Green
Write-Host "====================================================="
Write-Host ""
Write-Host "Папка: $distDir" -ForegroundColor White
Write-Host "Размер: ${sizeMB} МБ" -ForegroundColor White
Write-Host ""
Write-Host "СОДЕРЖИТ:" -ForegroundColor Yellow
Write-Host "  - Встроенный JRE (не нужен JDK!)" -ForegroundColor White
Write-Host "  - JavaFX для Windows" -ForegroundColor White
Write-Host "  - Библиотеки (sqlite, HikariCP, logback, iText, gson...)" -ForegroundColor White
Write-Host "  - autoservice-admin.jar" -ForegroundColor White
Write-Host "  - Папки logs/, backups/, config/" -ForegroundColor White
Write-Host "  - README.txt и version.txt" -ForegroundColor White
Write-Host ""
Write-Host "ДЛЯ ФЛЕШКИ:" -ForegroundColor Yellow
Write-Host "  1. Скопируйте папку portable целиком на флешку" -ForegroundColor White
Write-Host "  2. На любом Windows: запустите run.bat" -ForegroundColor White
Write-Host "  3. JDK НЕ НУЖЕН! Работает автономно!" -ForegroundColor Green
Write-Host ""
Write-Host "====================================================="
