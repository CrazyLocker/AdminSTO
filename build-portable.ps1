﻿﻿﻿﻿﻿﻿# Скрипт сборки полностью автономной portable-версии
# Запуск: powershell -ExecutionPolicy Bypass -File build-portable.ps1

$ErrorActionPreference = "Stop"
$projectDir = $PSScriptRoot
$distDir = Join-Path $projectDir "portable"
$jarFile = Join-Path $projectDir "target\autoservice-admin.jar"
$javafxVersion = "17.0.6"
$repo = Join-Path $env:USERPROFILE ".m2\repository\org\openjfx"

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
    if (Test-Path "$path\bin\java.exe") {
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
Write-Host "[1/6] Сборка проекта..." -ForegroundColor Yellow
mvn clean package -DskipTests -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "ОШИБКА сборки!" -ForegroundColor Red
    exit 1
}
Write-Host "    Готово." -ForegroundColor Green

# 2. Очистка и подготовка portable
Write-Host "[2/6] Подготовка папки portable..." -ForegroundColor Yellow
if (Test-Path $distDir) { Remove-Item "$distDir\*" -Recurse -Force }
New-Item -ItemType Directory -Path "$distDir\lib" -Force | Out-Null
New-Item -ItemType Directory -Path "$distDir\logs" -Force | Out-Null
New-Item -ItemType Directory -Path "$distDir\backups" -Force | Out-Null
Write-Host "    Готово." -ForegroundColor Green

# 3. Создание минимального JRE через jlink
Write-Host "[3/6] Создание встроенного JRE (jlink)..." -ForegroundColor Yellow
& "$JDK_PATH\bin\jlink.exe" `
    --module-path "$JDK_PATH\jmods" `
    --add-modules java.base,java.sql,jdk.unsupported,java.scripting,java.desktop,java.logging,java.instrument,java.management,jdk.zipfs,java.sql.rowset,java.naming,java.security.sasl,java.xml,jdk.httpserver,jdk.management `
    --strip-debug `
    --no-man-pages `
    --no-header-files `
    --compress=2 `
    --output "$distDir\jre"
Write-Host "    JRE создан." -ForegroundColor Green

# 4. Копирование зависимостей из Maven
Write-Host "[4/6] Копирование библиотек..." -ForegroundColor Yellow
Copy-Item "$repo\javafx-base\$javafxVersion\javafx-base-$javafxVersion-win.jar" "$distDir\lib\" -Force
Copy-Item "$repo\javafx-controls\$javafxVersion\javafx-controls-$javafxVersion-win.jar" "$distDir\lib\" -Force
Copy-Item "$repo\javafx-fxml\$javafxVersion\javafx-fxml-$javafxVersion-win.jar" "$distDir\lib\" -Force
Copy-Item "$repo\javafx-graphics\$javafxVersion\javafx-graphics-$javafxVersion-win.jar" "$distDir\lib\" -Force

# Копируем остальные зависимости
$targetLib = Join-Path $projectDir "target\lib"
if (Test-Path $targetLib) {
    Copy-Item "$targetLib\*" "$distDir\lib\" -Force
}

Write-Host "    Библиотеки скопированы." -ForegroundColor Green

# 5. Копирование JAR
Write-Host "[5/6] Копирование autoservice-admin.jar..." -ForegroundColor Yellow
Copy-Item $jarFile "$distDir\autoservice-admin.jar" -Force
Write-Host "    Готово." -ForegroundColor Green

# 6. Создание run.bat
Write-Host "[6/6] Создание run.bat..." -ForegroundColor Yellow
$runBat = @'
@echo off
setlocal

echo Launching AdminSTO Portable v1.0.0...
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
[System.IO.File]::WriteAllText("$distDir\run.bat", $runBat, [System.Text.Encoding]::Default)
Write-Host "    run.bat создан." -ForegroundColor Green

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
Write-Host "  - 30 библиотек (sqlite, HikariCP, logback, iText...)" -ForegroundColor White
Write-Host "  - autoservice-admin.jar" -ForegroundColor White
Write-Host "  - Папки logs/ и backups/" -ForegroundColor White
Write-Host ""
Write-Host "ДЛЯ ФЛЕШКИ:" -ForegroundColor Yellow
Write-Host "  1. Скопируйте папку portable целиком на флешку" -ForegroundColor White
Write-Host "  2. На любом Windows: запустите run.bat" -ForegroundColor White
Write-Host "  3. JDK НЕ НУЖЕН! Работает автономно!" -ForegroundColor Green
Write-Host ""
Write-Host "====================================================="
