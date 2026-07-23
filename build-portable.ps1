<#
.SYNOPSIS
    Скрипт сборки полностью автономной portable-версии AdminSTO для Windows.
.DESCRIPTION
    Создаёт папку AdminSTO_Portable с встроенным JRE, JavaFX и bat-файлом.
#>

$ErrorActionPreference = "Stop"

# ============================================================
# ПЕРЕМЕННЫЕ
# ============================================================
$projectDir = $PSScriptRoot
$distDir = Join-Path $projectDir "AdminSTO_Portable"
$jarFile = Join-Path $projectDir "build/libs/autoservice-admin.jar"
$JAVAFX_VERSION = "21.0.6"

# Определяем, запущены ли мы в CI (GitVerse Actions)
$isCI = $env:GITHUB_ACTIONS -eq "true"

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "  AdminSTO Portable Builder" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""

# ============================================================
# ШАГ 0: ПОИСК JDK 21
# ============================================================
Write-Host "[1/7] Looking for JDK 21..." -NoNewline

$possibleJdks = @(
    "C:\Program Files\Eclipse Adoptium\jdk-21*",
    "C:\Program Files\Java\jdk-21*",
    "C:\Program Files\Amazon Corretto\jdk21*"
)

$JDK_PATH = $null

foreach ($pattern in $possibleJdks) {
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
    $javaPath = Get-Command java -ErrorAction SilentlyContinue
    if ($javaPath) {
        $javaExe = $javaPath.Source
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

# Проверяем версию
try {
    $javaVersion = & "$JDK_PATH\bin\java.exe" -version 2>&1 | Select-String "version" | Select-Object -First 1
    Write-Host "Version: $javaVersion" -ForegroundColor Gray
} catch {
    Write-Host "Version: unknown" -ForegroundColor Yellow
}
Write-Host ""

# ============================================================
# ШАГ 0.1: ПРОВЕРКА FAT JAR
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
# ШАГ 1: ПОДГОТОВКА СТРУКТУРЫ ПАПОК
# ============================================================
Write-Host "[3/7] Preparing directories..." -NoNewline

if (Test-Path $distDir) {
    Remove-Item "$distDir" -Recurse -Force
}

New-Item -ItemType Directory -Path "$distDir\lib" -Force | Out-Null
New-Item -ItemType Directory -Path "$distDir\native" -Force | Out-Null
New-Item -ItemType Directory -Path "$distDir\data" -Force | Out-Null
New-Item -ItemType Directory -Path "$distDir\logs" -Force | Out-Null
New-Item -ItemType Directory -Path "$distDir\backups" -Force | Out-Null

Write-Host " OK" -ForegroundColor Green

# ============================================================
# ШАГ 2: СОЗДАНИЕ МИНИМАЛЬНОГО JRE
# ============================================================
Write-Host "[4/7] Creating JRE via jlink..." -NoNewline

$modules = @(
    "java.base", "java.sql", "java.logging", "java.instrument",
    "java.management", "jdk.zipfs", "java.xml", "jdk.httpserver",
    "jdk.unsupported", "jdk.localedata", "java.naming",
    "java.scripting", "java.desktop"
)

try {
    & "$JDK_PATH\bin\jlink.exe" `
        --module-path "$JDK_PATH\jmods" `
        --add-modules ($modules -join ",") `
        --include-locales ru `
        --strip-debug `
        --no-man-pages `
        --no-header-files `
        --compress=2 `
        --output "$distDir\jre"
} catch {
    # Игнорируем ошибки
}

if ($LASTEXITCODE -ne 0 -and (Test-Path "$distDir\jre")) {
    # Если jlink вернул ошибку, но папка создана - продолжаем
} elseif ($LASTEXITCODE -ne 0) {
    Write-Host " ERROR!" -ForegroundColor Red
    exit 1
}

$jreSize = (Get-ChildItem "$distDir\jre" -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB
Write-Host " OK (~$([math]::Round($jreSize, 1)) MB)" -ForegroundColor Green

# ============================================================
# ШАГ 3: КОПИРОВАНИЕ JAVAFX
# ============================================================
Write-Host "[5/7] Copying JavaFX..." -NoNewline

$javafxModules = @(
    "javafx-controls",
    "javafx-fxml", 
    "javafx-base",
    "javafx-graphics"
)

# Ищем JavaFX в кэше Gradle
$gradleCachePaths = @(
    "$env:USERPROFILE\.gradle\caches\modules-2\files-2.1\org.openjfx",
    "$HOME\.gradle\caches\modules-2\files-2.1\org.openjfx",
    "$env:HOME\.gradle\caches\modules-2\files-2.1\org.openjfx",
    "C:\Users\$env:USERNAME\.gradle\caches\modules-2\files-2.1\org.openjfx"
)

$gradleCache = $null
foreach ($path in $gradleCachePaths) {
    if (Test-Path $path) {
        $gradleCache = $path
        break
    }
}

$javafxFound = $true
$javafxError = $null

if ($gradleCache) {
    Write-Host "`n   Found Gradle cache: $gradleCache" -ForegroundColor Gray
    
    # Копируем модульные JAR
    foreach ($mod in $javafxModules) {
        $foundJar = Get-ChildItem -Path "$gradleCache\$mod" -Recurse -Filter "*.jar" -ErrorAction SilentlyContinue | 
                    Where-Object { $_.Name -like "${mod}-*.jar" -and $_.Name -notlike "*-win.jar" } | 
                    Select-Object -First 1
        if ($foundJar) {
            Copy-Item $foundJar.FullName "$distDir\lib\$mod.jar" -Force
            Write-Host "   + ${mod}.jar" -ForegroundColor Gray
        } else {
            $javafxFound = $false
            $javafxError = "${mod} not found in cache"
            break
        }
    }
    
    # Если модульные JAR найдены, ищем Windows JAR
    if ($javafxFound) {
        foreach ($mod in $javafxModules) {
            $foundJar = Get-ChildItem -Path "$gradleCache\$mod" -Recurse -Filter "*-win.jar" -ErrorAction SilentlyContinue | 
                        Where-Object { $_.Name -like "${mod}-*" } | 
                        Select-Object -First 1
            if ($foundJar) {
                $tempDir = "$env:TEMP\javafx-${mod}-win"
                if (Test-Path $tempDir) { Remove-Item $tempDir -Recurse -Force }
                New-Item -ItemType Directory -Path $tempDir -Force | Out-Null
                
                Push-Location $tempDir
                & "$JDK_PATH\bin\jar.exe" xf $foundJar.FullName 2>&1 | Out-Null
                if (Test-Path "module-info.class") {
                    Remove-Item "module-info.class" -Force
                }
                & "$JDK_PATH\bin\jar.exe" cf "$distDir\lib\${mod}-win.jar" * 2>&1 | Out-Null
                Pop-Location
                Remove-Item $tempDir -Recurse -Force
                Write-Host "   + ${mod}-win.jar" -ForegroundColor Gray
            } else {
                $javafxFound = $false
                $javafxError = "${mod}-win not found in cache"
                break
            }
        }
    }
}

# Если JavaFX не найден в кэше - скачиваем напрямую
if (-not $javafxFound -or -not $gradleCache) {
    if (-not $javafxFound) {
        Write-Host "   ERROR: $javafxError" -ForegroundColor Yellow
    } else {
        Write-Host "`n   Gradle cache not found" -ForegroundColor Yellow
    }
    
    Write-Host "   Downloading JavaFX directly from Gluon..." -ForegroundColor Yellow
    
    $javafxTemp = "$env:TEMP\javafx"
    if (Test-Path $javafxTemp) { Remove-Item $javafxTemp -Recurse -Force }
    New-Item -ItemType Directory -Path $javafxTemp -Force | Out-Null
    
    $javafxUrl = "https://download2.gluonhq.com/openjfx/${JAVAFX_VERSION}/openjfx-${JAVAFX_VERSION}_windows-x64_bin-sdk.zip"
    $javafxZip = "$env:TEMP\javafx-sdk.zip"
    
    try {
        Invoke-WebRequest -Uri $javafxUrl -OutFile $javafxZip -UseBasicParsing -ErrorAction Stop
        Expand-Archive -Path $javafxZip -DestinationPath $javafxTemp -Force
    } catch {
        Write-Host "   ERROR: Failed to download JavaFX!" -ForegroundColor Red
        exit 1
    }
    
    $javafxSdkLib = "$javafxTemp\javafx-sdk-${JAVAFX_VERSION}\lib"
    
    foreach ($mod in $javafxModules) {
        $jarPath = "$javafxSdkLib\$mod.jar"
        if (Test-Path $jarPath) {
            Copy-Item $jarPath "$distDir\lib\$mod.jar" -Force
            Write-Host "   + ${mod}.jar" -ForegroundColor Gray
        }
    }
    
    $dllFiles = Get-ChildItem -Path $javafxSdkLib -Filter "*.dll" -Recurse
    foreach ($dll in $dllFiles) {
        Copy-Item $dll.FullName "$distDir\native\" -Force
        Write-Host "   + $($dll.Name)" -ForegroundColor Gray
    }
    
    Remove-Item $javafxZip -Force -ErrorAction SilentlyContinue
    Remove-Item $javafxTemp -Recurse -Force -ErrorAction SilentlyContinue
    
    Write-Host "   JavaFX downloaded successfully" -ForegroundColor Green
    
    # Извлечение DLL уже не нужно, они скопированы
    Write-Host "[6/7] Extracting DLL... SKIPPED (already copied)" -ForegroundColor Yellow
} else {
    Write-Host "`n   JavaFX copied from cache" -ForegroundColor Green
    
    # ============================================================
    # ШАГ 4: ИЗВЛЕЧЕНИЕ DLL
    # ============================================================
    Write-Host "[6/7] Extracting DLL from Windows JAR..." -NoNewline
    
    $dllExtracted = 0
    foreach ($mod in $javafxModules) {
        $winJarPath = "$distDir\lib\$mod-win.jar"
        if (Test-Path $winJarPath) {
            try {
                $dllFiles = & "$JDK_PATH\bin\jar.exe" tf "$winJarPath" 2>&1 | Where-Object { $_ -match '\.dll$' }
                $dllCount = ($dllFiles | Measure-Object).Count
                if ($dllCount -gt 0) {
                    Push-Location "$distDir\native"
                    & "$JDK_PATH\bin\jar.exe" xf "$winJarPath" $dllFiles 2>&1 | Out-Null
                    Pop-Location
                    $dllExtracted += $dllCount
                }
            } catch {
                # Игнорируем ошибки извлечения
            }
        }
    }
    
    if ($dllExtracted -gt 0) {
        Write-Host " OK ($dllExtracted DLLs)" -ForegroundColor Green
    } else {
        Write-Host " OK (no DLLs found)" -ForegroundColor Green
    }
}

# ============================================================
# ШАГ 5: КОПИРОВАНИЕ ФАЙЛОВ ПРИЛОЖЕНИЯ
# ============================================================
Write-Host "[7/7] Copying application files..." -NoNewline

Copy-Item $jarFile "$distDir\autoservice-admin.jar" -Force

$stylesSrc = Join-Path $projectDir "src\main\resources\styles.css"
if (Test-Path $stylesSrc) { 
    Copy-Item $stylesSrc "$distDir\styles.css" -Force
}

$logbackSrc = Join-Path $projectDir "src\main\resources\logback.xml"
if (Test-Path $logbackSrc) { 
    Copy-Item $logbackSrc "$distDir\logback.xml" -Force
}

$configSrc = Join-Path $projectDir "config"
if (Test-Path $configSrc) {
    if (Test-Path "$configSrc\table-state") {
        New-Item -ItemType Directory -Path "$distDir\config\table-state" -Force | Out-Null
        Copy-Item "$configSrc\table-state\*" "$distDir\config\table-state\" -Force
    }
    if (Test-Path "$configSrc\window-state") {
        New-Item -ItemType Directory -Path "$distDir\config\window-state" -Force | Out-Null
        Copy-Item "$configSrc\window-state\*" "$distDir\config\window-state\" -Force
    }
}

Write-Host " OK" -ForegroundColor Green

# ============================================================
# ШАГ 6: СОЗДАНИЕ STO.BAT
# ============================================================
Write-Host "Creating STO.bat..." -NoNewline

Set-Content "$distDir\STO.bat" @'
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
'@ -Encoding Default

Write-Host " OK" -ForegroundColor Green

# ============================================================
# ИТОГИ
# ============================================================
$folderSize = (Get-ChildItem -Path $distDir -Recurse | Measure-Object -Property Length -Sum).Sum
$sizeMB = [math]::Round($folderSize / 1MB, 2)

Write-Host ""
Write-Host "==================================================" -ForegroundColor Green
Write-Host "  PORTABLE VERSION READY!" -ForegroundColor Green
Write-Host "  Size: ${sizeMB} MB" -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Green
Write-Host ""
Write-Host "Folder: $distDir"
Write-Host "Run: copy folder to USB, execute STO.bat"
Write-Host ""