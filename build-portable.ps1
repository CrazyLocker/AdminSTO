# ============================================================
# build-portable.ps1
# Сборка портативной версии AdminSTO для Windows
# ============================================================

$ErrorActionPreference = "Stop"

# ----- ПЕРЕМЕННЫЕ -----
$projectDir = $PSScriptRoot
$distDir = Join-Path $projectDir "AdminSTO_Portable"
$jarFile = Join-Path $projectDir "build/libs/autoservice-admin.jar"
$JAVAFX_VERSION = "21.0.6"

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "  AdminSTO Portable Builder" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""

# ============================================================
# 1. ПОИСК JDK 21
# ============================================================
Write-Host "[1/6] Looking for JDK 21..." -NoNewline

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
Write-Host "[2/6] Checking fat JAR..." -NoNewline
if (-not (Test-Path $jarFile)) {
    Write-Host " NOT FOUND!" -ForegroundColor Red
    Write-Host "ERROR: fat JAR not found!" -ForegroundColor Red
    Write-Host "Run: .\gradlew.bat clean fatJar" -ForegroundColor Yellow
    exit 1
}
Write-Host " OK" -ForegroundColor Green
Write-Host ""

# ============================================================
# 3. ПОДГОТОВКА ПАПОК
# ============================================================
Write-Host "[3/6] Preparing directories..." -NoNewline

if (Test-Path $distDir) {
    Remove-Item $distDir -Recurse -Force
}

$dirs = @("lib", "native", "data", "logs", "backups")
foreach ($d in $dirs) {
    New-Item -ItemType Directory -Path "$distDir\$d" -Force | Out-Null
}

Write-Host " OK" -ForegroundColor Green

# ============================================================
# 4. СОЗДАНИЕ JRE
# ============================================================
Write-Host "[4/6] Creating JRE via jlink..." -NoNewline

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
# 5. СКАЧИВАНИЕ И КОПИРОВАНИЕ JAVAFX
# ============================================================
Write-Host "[5/6] Downloading and copying JavaFX..." -NoNewline

$javafxModules = @(
    "javafx-controls",
    "javafx-fxml",
    "javafx-base",
    "javafx-graphics"
)

$javafxZip = "javafx-sdk.zip"
$javafxTemp = "javafx-sdk-temp"

# Источники для скачивания
$sources = @(
    "https://download2.gluonhq.com/openjfx/${JAVAFX_VERSION}/openjfx-${JAVAFX_VERSION}_windows-x64_bin-sdk.zip",
    "https://download.andreschleich.de/openjfx/${JAVAFX_VERSION}/openjfx-${JAVAFX_VERSION}_windows-x64_bin-sdk.zip"
)

$downloaded = $false

foreach ($source in $sources) {
    Write-Host "`n   Trying source: $source" -ForegroundColor Gray

    for ($attempt = 1; $attempt -le 3; $attempt++) {
        try {
            Write-Host "   Attempt $attempt/3..." -ForegroundColor Gray
            Invoke-WebRequest -Uri $source -OutFile $javafxZip -UseBasicParsing -ErrorAction Stop -TimeoutSec 60
            Write-Host "   Downloaded successfully!" -ForegroundColor Green
            $downloaded = $true
            break
        } catch {
            Write-Host "   Attempt $attempt failed: $($_.Exception.Message)" -ForegroundColor Yellow
            if ($attempt -lt 3) {
                Write-Host "   Waiting 5 seconds..." -ForegroundColor Gray
                Start-Sleep -Seconds 5
            }
        }
    }

    if ($downloaded) {
        break
    }
}

if (-not $downloaded) {
    Write-Host "`n   ERROR: Failed to download JavaFX from all sources!" -ForegroundColor Red
    exit 1
}

# Распаковка в конкретную папку
try {
    if (Test-Path $javafxTemp) {
        Remove-Item $javafxTemp -Recurse -Force
    }
    New-Item -ItemType Directory -Path $javafxTemp -Force | Out-Null
    Expand-Archive -Path $javafxZip -DestinationPath $javafxTemp -Force
    Remove-Item $javafxZip -Force
    Write-Host "   JavaFX SDK extracted to: $javafxTemp" -ForegroundColor Gray
} catch {
    Write-Host "   ERROR: Failed to extract JavaFX SDK!" -ForegroundColor Red
    exit 1
}

# --- ОТЛАДКА: показываем структуру распакованного архива ---
Write-Host "   Debug: Contents of extracted JavaFX:" -ForegroundColor Gray
Get-ChildItem -Path $javafxTemp -Recurse | ForEach-Object { Write-Host "     $($_.FullName)" -ForegroundColor Gray }

# Ищем папку lib с JAR-файлами
$javafxLib = $null

# Ищем в разных возможных местах
$searchPaths = @(
    "$javafxTemp\javafx-sdk-${JAVAFX_VERSION}\lib",
    "$javafxTemp\javafx-sdk\lib",
    "$javafxTemp\lib",
    "$javafxTemp\javafx-sdk-21.0.6\lib"
)

foreach ($path in $searchPaths) {
    if (Test-Path $path) {
        $javafxLib = $path
        Write-Host "   Found JavaFX lib at: $javafxLib" -ForegroundColor Green
        break
    }
}

# Если не нашли — ищем любую папку lib
if (-not $javafxLib) {
    $foundDirs = Get-ChildItem -Path $javafxTemp -Directory -Recurse | Where-Object { $_.Name -eq "lib" }
    foreach ($dir in $foundDirs) {
        # Проверяем, есть ли там javafx-controls.jar
        $testJar = Join-Path $dir.FullName "javafx-controls.jar"
        if (Test-Path $testJar) {
            $javafxLib = $dir.FullName
            Write-Host "   Found JavaFX lib at: $javafxLib" -ForegroundColor Green
            break
        }
    }
}

if (-not $javafxLib) {
    Write-Host "`n   ERROR: JavaFX lib folder not found!" -ForegroundColor Red
    Write-Host "   Full contents of $javafxTemp:" -ForegroundColor Yellow
    Get-ChildItem -Path $javafxTemp -Recurse | ForEach-Object { Write-Host "     $($_.FullName)" -ForegroundColor Gray }
    exit 1
}

# Копируем JAR-файлы
foreach ($mod in $javafxModules) {
    $src = Join-Path $javafxLib "$mod.jar"
    if (Test-Path $src) {
        Copy-Item $src "$distDir\lib\$mod.jar" -Force
        Write-Host "   + $mod.jar" -ForegroundColor Gray
    } else {
        Write-Host "   WARNING: $mod.jar not found, searching..." -ForegroundColor Yellow
        # Ищем файл с похожим именем
        $foundJar = Get-ChildItem -Path $javafxLib -Filter "$mod*.jar" | Select-Object -First 1
        if ($foundJar) {
            Copy-Item $foundJar.FullName "$distDir\lib\$mod.jar" -Force
            Write-Host "   + $mod.jar (found as $($foundJar.Name))" -ForegroundColor Gray
        } else {
            Write-Host "   ERROR: $mod.jar not found in $javafxLib" -ForegroundColor Red
        }
    }
}

# Копируем Windows JAR с DLL
$winJarFound = Get-ChildItem -Path $javafxLib -Filter "*win*.jar" | Select-Object -First 1
if ($winJarFound) {
    Push-Location "$distDir\native"
    & "$JDK_PATH\bin\jar.exe" xf $winJarFound.FullName *.dll 2>&1 | Out-Null
    Pop-Location
    Write-Host "   + DLL extracted from $($winJarFound.Name)" -ForegroundColor Gray
} else {
    Write-Host "   WARNING: No Windows JAR with DLL found!" -ForegroundColor Yellow
}

# Удаляем временную папку
if (Test-Path $javafxTemp) {
    Remove-Item $javafxTemp -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Host "   JavaFX ready" -ForegroundColor Green

# ============================================================
# 6. КОПИРОВАНИЕ ФАЙЛОВ ПРИЛОЖЕНИЯ
# ============================================================
Write-Host "[6/6] Copying application files..." -NoNewline

# JAR
Copy-Item $jarFile "$distDir\autoservice-admin.jar" -Force

# styles.css
$styles = Join-Path $projectDir "src\main\resources\styles.css"
if (Test-Path $styles) {
    Copy-Item $styles "$distDir\styles.css" -Force
}

# logback.xml
$logback = Join-Path $projectDir "src\main\resources\logback.xml"
if (Test-Path $logback) {
    Copy-Item $logback "$distDir\logback.xml" -Force
}

# config
$config = Join-Path $projectDir "config"
if (Test-Path $config) {
    Copy-Item $config "$distDir\" -Recurse -Force
}

Write-Host " OK" -ForegroundColor Green

# ============================================================
# 7. ПРОВЕРКА РЕЗУЛЬТАТА
# ============================================================
Write-Host "Checking result..." -NoNewline

$libFiles = Get-ChildItem "$distDir\lib" -Filter "*.jar"
$missingJars = @()
foreach ($mod in $javafxModules) {
    $jarName = "$mod.jar"
    if ($libFiles.Name -notcontains $jarName) {
        $missingJars += $jarName
    }
}

if ($missingJars.Count -gt 0) {
    Write-Host " WARNING: Missing JARs: $($missingJars -join ', ')" -ForegroundColor Yellow
} else {
    Write-Host " OK - all JARs present" -ForegroundColor Green
}

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
$size = (Get-ChildItem $distDir -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB

Write-Host ""
Write-Host "==================================================" -ForegroundColor Green
Write-Host "  PORTABLE VERSION READY!" -ForegroundColor Green
Write-Host "  Size: $([math]::Round($size, 2)) MB" -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Green
Write-Host ""
Write-Host "Folder: $distDir"
Write-Host "Run: copy folder to USB, execute STO.bat"
Write-Host ""