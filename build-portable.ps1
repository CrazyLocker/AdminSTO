# ============================================================
# build-portable.ps1
# Сборка портативной версии AdminSTO для CI (GitHub Actions)
# Адаптирован под локальный рабочий скрипт
# ============================================================

$ErrorActionPreference = "Stop"

$projectDir = $PSScriptRoot
$portableDir = Join-Path $projectDir "AdminSTO_Portable"
$jarFile = Join-Path $projectDir "build/libs/autoservice-admin.jar"
$JAVAFX_SDK = Join-Path $projectDir "javafx-sdk-21.0.6"

Write-Host ""
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "  AdminSTO Portable Builder (CI)" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""

# ============================================================
# ШАГ 1: Поиск JDK 21 (как в локальном скрипте)
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
    Write-Host "Error: JDK 21 not found!" -ForegroundColor Red
    exit 1
}

Write-Host " OK" -ForegroundColor Green
Write-Host "  Path: $JDK_PATH" -ForegroundColor Gray

# ============================================================
# ШАГ 2: Проверка fat JAR
# ============================================================
Write-Host "[2/6] Checking fat JAR..." -NoNewline

if (-not (Test-Path $jarFile)) {
    Write-Host " NOT FOUND!" -ForegroundColor Red
    Write-Host "Error: fat JAR not found: $jarFile" -ForegroundColor Red
    exit 1
}

$jarSize = (Get-Item $jarFile).Length / 1MB
Write-Host " OK ($([math]::Round($jarSize, 2)) MB)" -ForegroundColor Green

# ============================================================
# ШАГ 3: Подготовка папок
# ============================================================
Write-Host "[3/6] Preparing directories..." -NoNewline

if (Test-Path $portableDir) {
    Remove-Item $portableDir -Recurse -Force
}

$dirs = @("lib", "native", "data", "logs", "backups", "conf")
foreach ($d in $dirs) {
    New-Item -ItemType Directory -Path "$portableDir\$d" -Force | Out-Null
}

Write-Host " OK" -ForegroundColor Green

# ============================================================
# ШАГ 4: Создание встроенной JRE через jlink
# ============================================================
Write-Host "[4/6] Creating embedded JRE..." -NoNewline

$jrePath = Join-Path $portableDir "jre"

$jlinkCmd = "$JDK_PATH\bin\jlink.exe"
$jlinkArgs = @(
    "--module-path", "$JDK_PATH\jmods",
    "--add-modules", "java.base,java.sql,java.logging,java.instrument,java.management,jdk.zipfs,java.xml,jdk.httpserver,java.naming,java.scripting,java.desktop,jdk.localedata,jdk.unsupported",
    "--strip-debug",
    "--no-man-pages",
    "--no-header-files",
    "--compress=2",
    "--output", $jrePath
)

$oldEAP = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$null = & $jlinkCmd @jlinkArgs 2>&1
$ErrorActionPreference = $oldEAP

if ($LASTEXITCODE -ne 0) {
    Write-Host " ERROR!" -ForegroundColor Red
    exit 1
}

$jreSize = (Get-ChildItem $jrePath -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB
Write-Host " OK ($([math]::Round($jreSize, 1)) MB)" -ForegroundColor Green

# ============================================================
# ШАГ 5: Копирование JavaFX JAR и DLL (КАК В ЛОКАЛЬНОМ СКРИПТЕ!)
# ============================================================
Write-Host "[5/6] Copying JavaFX..." -NoNewline

$javafxLib = Join-Path $JAVAFX_SDK "lib"
$javafxBin = Join-Path $JAVAFX_SDK "bin"

if (-not (Test-Path $javafxLib)) {
    Write-Host " ERROR (JavaFX SDK not found)!" -ForegroundColor Red
    exit 1
}

# Копируем ТОЛЬКО конкретные файлы (как в локальном скрипте)
$javafxFiles = @(
    "javafx.base.jar",
    "javafx.controls.jar",
    "javafx.fxml.jar",
    "javafx.graphics.jar",
    "javafx.media.jar",
    "javafx.swing.jar",
    "javafx.web.jar"
)

$copied = 0
$allJars = Get-ChildItem -Path $javafxLib -Filter "*.jar" -ErrorAction SilentlyContinue
foreach ($jarName in $javafxFiles) {
    $src = $allJars | Where-Object { $_.Name -eq $jarName } | Select-Object -First 1

    if ($src) {
        Copy-Item $src.FullName "$portableDir\lib\$jarName" -Force
        $copied++
        Write-Host "`n   + $jarName" -ForegroundColor Gray
    } else {
        Write-Host "`n   WARNING: $jarName not found" -ForegroundColor Yellow
    }
}

# Копируем DLL (как в локальном скрипте)
if (Test-Path $javafxBin) {
    Get-ChildItem -Path $javafxBin -Filter "*.dll" | ForEach-Object {
        Copy-Item $_.FullName "$portableDir\native\" -Force
        Write-Host "   + DLL (bin): $($_.Name)" -ForegroundColor Gray
    }
}
Get-ChildItem -Path $javafxLib -Filter "*.dll" | ForEach-Object {
    Copy-Item $_.FullName "$portableDir\native\" -Force
    Write-Host "   + DLL (lib): $($_.Name)" -ForegroundColor Gray
}

Write-Host "`n   Copied $copied modules" -ForegroundColor Green

# ============================================================
# ШАГ 6: Копирование файлов приложения
# ============================================================
Write-Host "[6/6] Copying application files..." -NoNewline

Copy-Item $jarFile "$portableDir\autoservice-admin.jar" -Force

$styles = Join-Path $projectDir "src\main\resources\styles.css"
if (Test-Path $styles) { Copy-Item $styles "$portableDir\styles.css" -Force }

$logback = Join-Path $projectDir "src\main\resources\logback.xml"
if (Test-Path $logback) { Copy-Item $logback "$portableDir\logback.xml" -Force }

$config = Join-Path $projectDir "config"
if (Test-Path $config) { Copy-Item $config "$portableDir\" -Recurse -Force }

Write-Host " OK" -ForegroundColor Green

# ============================================================
# СОЗДАНИЕ STO.BAT (ТОЧНО КАК В ЛОКАЛЬНОМ СКРИПТЕ!)
# ============================================================
Write-Host "Creating STO.bat..." -NoNewline

$batContent = @'
@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

cd /d "%~dp0"

set APP_DIR=%CD%

echo ============================================
echo   AdminSTO - Administrator STO
echo ============================================
echo.
echo Starting...
echo.

if not exist "jre\bin\java.exe" (
    echo ERROR: Java Runtime Environment not found!
    echo Make sure the entire AdminSTO_Portable folder is copied.
    pause
    exit /b 1
)

if not exist "lib\javafx.controls.jar" (
    echo ERROR: JavaFX libraries not found!
    echo Make sure the entire folder is copied.
    pause
    exit /b 1
)

set MODULE_PATH=lib\javafx.base.jar;lib\javafx.controls.jar;lib\javafx.fxml.jar;lib\javafx.graphics.jar;lib\javafx.media.jar;lib\javafx.swing.jar;lib\javafx.web.jar

jre\bin\java.exe ^
    -Duser.language=ru ^
    -Duser.country=RU ^
    -Dfile.encoding=UTF-8 ^
    -Dapp.home="!APP_DIR!" ^
    -Djava.library.path="!APP_DIR!\native" ^
    -Dprism.order=sw ^
    -Dprism.text=t2k ^
    -Djavafx.headless=false ^
    --module-path "%MODULE_PATH%" ^
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
    echo   LAUNCH ERROR!
    echo ============================================
    echo.
    echo Check:
    echo   1. Entire AdminSTO_Portable folder copied?
    echo   2. Windows 64-bit?
    echo   3. Files not blocked by antivirus?
    echo.
    pause
)
'@

Set-Content "$portableDir\STO.bat" $batContent -Encoding Default

Write-Host " OK" -ForegroundColor Green

# ============================================================
# ИТОГИ
# ============================================================
$libJars = Get-ChildItem "$portableDir\lib" -Filter "*.jar"
$dllCount = (Get-ChildItem "$portableDir\native" -Filter "*.dll").Count
$totalSize = (Get-ChildItem $portableDir -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB

Write-Host ""
Write-Host "==================================================" -ForegroundColor Green
Write-Host "  PORTABLE VERSION READY!" -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Green
Write-Host ""
Write-Host "  JARs in lib:    $($libJars.Count)" -ForegroundColor Gray
Write-Host "  DLLs:           $dllCount" -ForegroundColor Gray
Write-Host "  Size:           $([math]::Round($totalSize, 2)) MB" -ForegroundColor Gray
Write-Host ""
Write-Host "  Folder:         $portableDir" -ForegroundColor White
Write-Host "  Run:            $portableDir\STO.bat" -ForegroundColor White
Write-Host ""