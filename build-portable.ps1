# ============================================================
# build-portable.ps1
# Сборка портативной версии AdminSTO для Windows
# ============================================================

$ErrorActionPreference = "Stop"

$projectDir = $PSScriptRoot
$distDir = Join-Path $projectDir "AdminSTO_Portable"
$jarFile = Join-Path $projectDir "build/libs/autoservice-admin.jar"
$JAVAFX_VERSION = "21.0.6"
$javafxSourceDir = Join-Path $projectDir "javafx-sdk-21.0.6"
$javafxLib = Join-Path $javafxSourceDir "lib"

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "  AdminSTO Portable Builder" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""

# ============================================================
# 1. ПОИСК JDK 21
# ============================================================
Write-Host "[1/7] Looking for JDK 21..." -NoNewline

$JDK_PATH = $null
$possiblePaths = @(
    "C:\Program Files\Eclipse Adoptium\jdk-21*",
    "C:\Program Files\Java\jdk-21*",
    "C:\Program Files\Amazon Corretto\jdk21*"
)

foreach ($pattern in $possiblePaths) {
    $found = Get-ChildItem -Path $pattern -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($found -and (Test-Path "$($found.FullName)\bin\java.exe")) {
        $JDK_PATH = $found.FullName
        break
    }
}

if (-not $JDK_PATH -and $env:JAVA_HOME) {
    if (Test-Path "$env:JAVA_HOME\bin\java.exe") {
        $JDK_PATH = $env:JAVA_HOME
    }
}

if (-not $JDK_PATH) {
    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCmd) {
        $javaExe = $javaCmd.Source
        $JDK_PATH = Split-Path (Split-Path $javaExe -Parent) -Parent
        if (-not (Test-Path "$JDK_PATH\bin\java.exe")) {
            $JDK_PATH = $null
        }
    }
}

if (-not $JDK_PATH) {
    Write-Host " NOT FOUND!" -ForegroundColor Red
    Write-Host "ERROR: JDK 21 not found!" -ForegroundColor Red
    exit 1
}

Write-Host " OK" -ForegroundColor Green
Write-Host "JDK: $JDK_PATH" -ForegroundColor Gray

try {
    $version = & "$JDK_PATH\bin\java.exe" -version 2>&1 | Select-String "version" | Select-Object -First 1
    Write-Host "Version: $version" -ForegroundColor Gray
} catch {
    Write-Host "Version: unknown" -ForegroundColor Yellow
}
Write-Host ""

# ============================================================
# 2. ПРОВЕРКА JAR
# ============================================================
Write-Host "[2/7] Checking fat JAR..." -NoNewline
if (-not (Test-Path $jarFile)) {
    Write-Host " NOT FOUND!" -ForegroundColor Red
    Write-Host "ERROR: fat JAR not found!" -ForegroundColor Red
    Write-Host "Run: .\gradlew.bat clean fatJar" -ForegroundColor Yellow
    exit 1
}
Write-Host " OK" -ForegroundColor Green
Write-Host ""

# ============================================================
# 3. ПРОВЕРКА НАЛИЧИЯ JAVAFX
# ============================================================
Write-Host "[3/7] Checking JavaFX SDK..." -NoNewline

$javafxExists = $false
$allJars = $null

if (Test-Path $javafxLib) {
    $allJars = Get-ChildItem -Path $javafxLib -Filter "*.jar" -ErrorAction SilentlyContinue
    if ($allJars -and $allJars.Count -gt 0) {
        $javafxExists = $true
        Write-Host " FOUND at $javafxSourceDir ($($allJars.Count) JARs)" -ForegroundColor Green
    }
}

if (-not $javafxExists) {
    Write-Host " NOT FOUND or incomplete - downloading..." -ForegroundColor Yellow

    # Удаляем старую папку, если она есть
    if (Test-Path $javafxSourceDir) {
        Write-Host "   Removing existing incomplete JavaFX folder..." -ForegroundColor Gray
        Remove-Item $javafxSourceDir -Recurse -Force -ErrorAction SilentlyContinue
    }

    $javafxZip = "javafx-sdk.zip"
    $url = "https://download2.gluonhq.com/openjfx/${JAVAFX_VERSION}/openjfx-${JAVAFX_VERSION}_windows-x64_bin-sdk.zip"

    try {
        Write-Host "   Downloading JavaFX SDK..." -ForegroundColor Gray
        Invoke-WebRequest -Uri $url -OutFile $javafxZip -UseBasicParsing -ErrorAction Stop -TimeoutSec 120
        Write-Host "   Downloaded successfully" -ForegroundColor Green

        Write-Host "   Extracting..." -ForegroundColor Gray
        Expand-Archive -Path $javafxZip -DestinationPath . -Force
        Remove-Item $javafxZip -Force
        Write-Host "   Extracted successfully" -ForegroundColor Green

        $javafxLib = Join-Path $javafxSourceDir "lib"
        if (-not (Test-Path $javafxLib)) {
            Write-Host "   ERROR: lib folder not found!" -ForegroundColor Red
            exit 1
        }

        $allJars = Get-ChildItem -Path $javafxLib -Filter "*.jar"
        Write-Host "   JAR files found: $($allJars.Count)" -ForegroundColor Gray

    } catch {
        Write-Host "   ERROR: Failed to download/extract JavaFX!" -ForegroundColor Red
        exit 1
    }
}
Write-Host ""

# ============================================================
# 4. ПОДГОТОВКА ПАПОК
# ============================================================
Write-Host "[4/7] Preparing directories..." -NoNewline

if (Test-Path $distDir) {
    Remove-Item $distDir -Recurse -Force
}

$dirs = @("lib", "native", "data", "logs", "backups")
foreach ($d in $dirs) {
    New-Item -ItemType Directory -Path "$distDir\$d" -Force | Out-Null
}

Write-Host " OK" -ForegroundColor Green

# ============================================================
# 5. СОЗДАНИЕ JRE
# ============================================================
Write-Host "[5/7] Creating JRE via jlink..." -NoNewline

$modules = @(
    "java.base", "java.sql", "java.logging", "java.instrument",
    "java.management", "jdk.zipfs", "java.xml", "jdk.httpserver",
    "jdk.unsupported", "jdk.localedata", "java.naming",
    "java.scripting", "java.desktop"
)

& "$JDK_PATH\bin\jlink.exe" `
    --module-path "$JDK_PATH\jmods" `
    --add-modules ($modules -join ",") `
    --include-locales ru `
    --strip-debug `
    --no-man-pages `
    --no-header-files `
    --compress=2 `
    --output "$distDir\jre" 2>&1 | Out-Null

if ($LASTEXITCODE -ne 0) {
    Write-Host " ERROR!" -ForegroundColor Red
    exit 1
}

$jreSize = (Get-ChildItem "$distDir\jre" -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB
Write-Host " OK (~$([math]::Round($jreSize, 1)) MB)" -ForegroundColor Green

# ============================================================
# 6. КОПИРОВАНИЕ JAVAFX
# ============================================================
Write-Host "[6/7] Copying JavaFX..." -NoNewline

$javafxModules = @(
    "javafx-controls",
    "javafx-fxml",
    "javafx-base",
    "javafx-graphics"
)

$copiedCount = 0
foreach ($mod in $javafxModules) {
    $jarFileFound = $allJars | Where-Object { $_.Name -eq "$mod.jar" } | Select-Object -First 1
    if (-not $jarFileFound) {
        $jarFileFound = $allJars | Where-Object { $_.Name -like "*$mod*.jar" -and $_.Name -notlike "*-win.jar" } | Select-Object -First 1
    }

    if ($jarFileFound) {
        Copy-Item $jarFileFound.FullName "$distDir\lib\$mod.jar" -Force
        Write-Host "`n   + $mod.jar" -ForegroundColor Gray
        $copiedCount++
    } else {
        Write-Host "`n   WARNING: $mod.jar not found" -ForegroundColor Yellow
    }
}

# Копируем Windows JAR с DLL
$winJars = $allJars | Where-Object { $_.Name -like "*-win.jar" }
if ($winJars) {
    foreach ($winJar in $winJars) {
        Push-Location "$distDir\native"
        & "$JDK_PATH\bin\jar.exe" xf $winJar.FullName *.dll 2>&1 | Out-Null
        Pop-Location
        Write-Host "   + DLL extracted from $($winJar.Name)" -ForegroundColor Gray
    }
} else {
    Write-Host "   WARNING: No Windows JAR with DLL found" -ForegroundColor Yellow
}

# Копируем все DLL-файлы из папки lib
$dllFiles = Get-ChildItem -Path $javafxLib -Filter "*.dll"
foreach ($dll in $dllFiles) {
    Copy-Item $dll.FullName "$distDir\native\" -Force
    Write-Host "   + DLL: $($dll.Name)" -ForegroundColor Gray
}

Write-Host "`n   JavaFX copied ($copiedCount modules)" -ForegroundColor Green

# ============================================================
# 7. КОПИРОВАНИЕ ФАЙЛОВ ПРИЛОЖЕНИЯ
# ============================================================
Write-Host "[7/7] Copying application files..." -NoNewline

Copy-Item $jarFile "$distDir\autoservice-admin.jar" -Force

$styles = Join-Path $projectDir "src\main\resources\styles.css"
if (Test-Path $styles) { Copy-Item $styles "$distDir\styles.css" -Force }

$logback = Join-Path $projectDir "src\main\resources\logback.xml"
if (Test-Path $logback) { Copy-Item $logback "$distDir\logback.xml" -Force }

$config = Join-Path $projectDir "config"
if (Test-Path $config) { Copy-Item $config "$distDir\" -Recurse -Force }

Write-Host " OK" -ForegroundColor Green

# ============================================================
# 8. СОЗДАНИЕ STO.BAT
# ============================================================
Write-Host "Creating STO.bat..." -NoNewline

$batContent = @'
@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

cd /d "%~dp0"

if not exist "jre\bin\java.exe" (
    echo ERROR: Java not found!
    pause
    exit /b 1
)

if not exist "lib\javafx-controls.jar" (
    echo ERROR: JavaFX not found!
    pause
    exit /b 1
)

set APP_DIR=%CD%

echo ============================================
echo  AdminSTO - Administrator STO
echo ============================================
echo.
echo Starting...
echo.

jre\bin\java.exe ^
    -Duser.language=ru ^
    -Duser.country=RU ^
    -Dfile.encoding=UTF-8 ^
    -Dapp.home="!APP_DIR!" ^
    -Djava.library.path="!APP_DIR!\native" ^
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
    echo  LAUNCH ERROR!
    echo ============================================
    echo.
    echo Please check:
    echo   1. Folder copied completely?
    echo   2. Windows 64-bit?
    echo   3. Files not blocked by antivirus?
    echo.
    pause
)
'@

Set-Content "$distDir\STO.bat" $batContent -Encoding Default

Write-Host " OK" -ForegroundColor Green

# ============================================================
# 9. ИТОГИ
# ============================================================
$libJars = Get-ChildItem "$distDir\lib" -Filter "*.jar"
$dllCount = (Get-ChildItem "$distDir\native" -Filter "*.dll").Count
$size = (Get-ChildItem $distDir -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB

Write-Host ""
Write-Host "==================================================" -ForegroundColor Green
Write-Host "  PORTABLE VERSION READY!" -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Green
Write-Host ""
Write-Host "JARs in lib:" -ForegroundColor Gray
foreach ($jar in $libJars) {
    Write-Host "  - $($jar.Name)" -ForegroundColor Gray
}
Write-Host "DLLs in native: $dllCount" -ForegroundColor Gray
Write-Host "Total size: $([math]::Round($size, 2)) MB" -ForegroundColor Gray
Write-Host ""
Write-Host "Folder: $distDir"
Write-Host "Run: copy folder to USB, execute STO.bat"
Write-Host ""