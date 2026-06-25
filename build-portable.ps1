# Скрипт сборки полностью автономной portable-версии
# Запуск: powershell -ExecutionPolicy Bypass -File build-portable.ps1

$ErrorActionPreference = "Stop"
$projectDir = $PSScriptRoot
$distDir = Join-Path $projectDir "dist"
$jarFile = Join-Path $projectDir "target\autoservice-admin-1.0-SNAPSHOT-jar-with-dependencies.jar"
$javafxVersion = "17.0.6"
$repo = Join-Path $env:USERPROFILE ".m2\repository\org\openjfx"
$JDK_PATH = "D:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Сборка Portable-версии (автономная)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. Сборка проекта
Write-Host "[1/5] Сборка проекта..." -ForegroundColor Yellow
mvn clean package -DskipTests -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "ОШИБКА сборки!" -ForegroundColor Red
    exit 1
}
Write-Host "    Готово." -ForegroundColor Green

# 2. Очистка и подготовка dist
Write-Host "[2/5] Подготовка dist..." -ForegroundColor Yellow
if (Test-Path $distDir) { Remove-Item "$distDir\*" -Recurse -Force }
New-Item -ItemType Directory -Path "$distDir\javafx" -Force | Out-Null
Write-Host "    Готово." -ForegroundColor Green

# 3. Создание минимального JRE через jlink
Write-Host "[3/5] Создание встроенного JRE (jlink)..." -ForegroundColor Yellow
& "$JDK_PATH\bin\jlink.exe" --module-path "$JDK_PATH\jmods" --add-modules java.base,java.sql,jdk.unsupported,java.scripting,java.desktop,java.logging,java.instrument --strip-debug --no-man-pages --no-header-files --compress=2 --output "$distDir\jre"
Write-Host "    JRE создан." -ForegroundColor Green

# 4. Копирование файлов
Write-Host "[4/5] Копирование файлов..." -ForegroundColor Yellow
Copy-Item $jarFile "$distDir\autoservice-admin.jar" -Force
Copy-Item "$repo\javafx-base\$javafxVersion\javafx-base-$javafxVersion-win.jar" "$distDir\javafx\" -Force
Copy-Item "$repo\javafx-controls\$javafxVersion\javafx-controls-$javafxVersion-win.jar" "$distDir\javafx\" -Force
Copy-Item "$repo\javafx-fxml\$javafxVersion\javafx-fxml-$javafxVersion-win.jar" "$distDir\javafx\" -Force
Copy-Item "$repo\javafx-graphics\$javafxVersion\javafx-graphics-$javafxVersion-win.jar" "$distDir\javafx\" -Force
Write-Host "    Готово." -ForegroundColor Green

# 5. Создание run.bat
Write-Host "[5/5] Создание run.bat..." -ForegroundColor Yellow
$runBat = @'
@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion

echo ========================================
echo  AutoService STO - Admin (Portable)
echo ========================================
echo.

set SCRIPT_DIR=%~dp0
set JRE_PATH="%SCRIPT_DIR%jre\bin\java.exe"
set JAVAFX_PATH="%SCRIPT_DIR%javafx"
set APP_PATH="%SCRIPT_DIR%autoservice-admin.jar"

echo Launching AutoService STO...
echo.

if not exist !JRE_PATH! (
    echo ERROR: JRE not found!
    pause
    exit /b 1
)

!JRE_PATH! ^
    --enable-native-access=javafx.graphics ^
    --enable-native-access=ALL-UNNAMED ^
    --module-path !JAVAFX_PATH! ^
    --add-modules javafx.controls,javafx.fxml ^
    -jar !APP_PATH!

if errorlevel 1 (
    echo.
    echo ERROR: Application failed!
    pause
)
'@
[System.IO.File]::WriteAllText("$distDir\run.bat", $runBat, [System.Text.Encoding]::UTF8)
Write-Host "    Готово." -ForegroundColor Green

# Итог
$folderSize = (Get-ChildItem -Path $distDir -Recurse | Measure-Object -Property Length -Sum).Sum
$sizeMB = [math]::Round($folderSize / 1MB, 2)

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Portable-версия собрана!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Папка: $distDir" -ForegroundColor White
Write-Host "Размер: ${sizeMB} МБ" -ForegroundColor White
Write-Host ""
Write-Host "СОДЕРЖИТ:" -ForegroundColor Yellow
Write-Host "  - Встроенный JRE (не нужен JDK!)" -ForegroundColor White
Write-Host "  - JavaFX для Windows" -ForegroundColor White
Write-Host "  - Приложение со всеми зависимостями" -ForegroundColor White
Write-Host ""
Write-Host "Для флешки:" -ForegroundColor Yellow
Write-Host "  1. Скопируйте папку dist целиком" -ForegroundColor White
Write-Host "  2. На любом Windows: запустите run.bat" -ForegroundColor White
Write-Host "  3. JDK NOT NEEDED!" -ForegroundColor Green
Write-Host ""
