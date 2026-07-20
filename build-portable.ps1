# Скрипт сборки полностью автономной portable-версии AdminSTO
# Требования: JDK 17 уже должен быть установлен
$ErrorActionPreference = "Stop"
$projectDir = $PSScriptRoot
$distDir = Join-Path $projectDir "AdminSTO_Portable"
$jarFile = Join-Path $projectDir "build/libs/autoservice-admin.jar"
$JAVAFX_VERSION = "21.0.6"

# Поиск JDK 21
$possibleJdks = @(
    "C:\Program Files\Eclipse Adoptium\jdk-21*",
    "C:\Program Files\Java\jdk-21*"
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
    Write-Host "OSHIBKA: JDK 21 ne nayden!" -ForegroundColor Red
    Write-Host "Ustanovite JDK 21 (Adoptium/Eclipse Temurin ili Oracle)" -ForegroundColor Yellow
    exit 1
}

Write-Host "JDK: $JDK_PATH" -ForegroundColor Cyan

# Проверка версии JDK
try {
    $javaVersion = & "$JDK_PATH\bin\java.exe" -version 2>&1 | Select-String "version" | Select-Object -First 1
    Write-Host "Версия JDK: $javaVersion" -ForegroundColor Cyan
} catch {
    Write-Host "Версия JDK: не удалось определить" -ForegroundColor Yellow
}

# Проверка наличия fat JAR
if (-not (Test-Path $jarFile)) {
    Write-Host "OSHIBKA: fat JAR ne nayden!" -ForegroundColor Red
    Write-Host "Zapustite: .\gradlew.bat clean fatJar" -ForegroundColor Yellow
    exit 1
}

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "  Sborka AdminSTO Portable" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

Write-Host "[1/6] Podgotovka... " -NoNewline
if (Test-Path $distDir) { Remove-Item "$distDir" -Recurse -Force }
New-Item -ItemType Directory -Path "$distDir\lib" -Force | Out-Null
New-Item -ItemType Directory -Path "$distDir\native" -Force | Out-Null
New-Item -ItemType Directory -Path "$distDir\data" -Force | Out-Null
New-Item -ItemType Directory -Path "$distDir\logs" -Force | Out-Null
New-Item -ItemType Directory -Path "$distDir\backups" -Force | Out-Null
Write-Host "Gotovo." -ForegroundColor Green

Write-Host "[2/6] Sozdanie JRE (jlink)... " -NoNewline
& "$JDK_PATH\bin\jlink.exe" `
    --module-path "$JDK_PATH\jmods" `
    --add-modules java.base,java.sql,java.logging,java.instrument,java.management,jdk.zipfs,java.xml,jdk.httpserver,jdk.unsupported,jdk.localedata,java.naming,java.scripting,java.desktop `
    --include-locales ru `
    --strip-debug `
    --no-man-pages `
    --no-header-files `
    --compress=2 `
    --output "$distDir\jre"
if ($LASTEXITCODE -ne 0) { Write-Host "OSHIBKA!" -ForegroundColor Red; exit 1 }
$jreSize = (Get-ChildItem "$distDir\jre" -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB
Write-Host "Gotovo. (~$([math]::Round($jreSize, 1)) MB)" -ForegroundColor Green

Write-Host "[3/6] Kopirovanie JavaFX... " -NoNewline
$gradleCache = "$env:USERPROFILE\.gradle\caches\modules-2\files-2.1\org.openjfx"

# Kopiruem obychnye JAR (modul s module-info.class)
foreach ($mod in "javafx-controls", "javafx-fxml", "javafx-base", "javafx-graphics") {
    $foundJar = Get-ChildItem -Path "$gradleCache\$mod" -Recurse -Filter "*.jar" | Where-Object { $_.Name -like "${mod}-*.jar" -and $_.Name -notlike "*-win.jar" } | Select-Object -First 1
    if ($foundJar) {
        Copy-Item $foundJar.FullName "$distDir\lib\$mod.jar" -Force
        Write-Host "    + ${mod}.jar (module)" -ForegroundColor Gray
    } else {
        Write-Host "    OSHIBKA: ${mod} ne nayden v gradle kache!" -ForegroundColor Red
        exit 1
    }
}

# Kopiruem Windows JAR (s DLL vnutri) - uдаляем module-info.class
foreach ($mod in "javafx-controls", "javafx-fxml", "javafx-base", "javafx-graphics") {
    $foundJar = Get-ChildItem -Path "$gradleCache\$mod" -Recurse -Filter "*-win.jar" | Where-Object { $_.Name -like "${mod}-*" } | Select-Object -First 1
    if ($foundJar) {
        $tempDir = "$env:TEMP\javafx-${mod}-win"
        if (Test-Path $tempDir) { Remove-Item $tempDir -Recurse -Force }
        New-Item -ItemType Directory -Path $tempDir -Force | Out-Null
        
        # Распаковываем в временную папку
        Push-Location $tempDir
        & "$JDK_PATH\bin\jar.exe" xf $foundJar.FullName
        if (Test-Path "module-info.class") {
            Remove-Item "module-info.class" -Force
        }
        # Пересобираем без module-info.class
        & "$JDK_PATH\bin\jar.exe" cf "$distDir\lib\${mod}-win.jar" *
        Pop-Location
        
        # Удаляем временную папку
        Remove-Item $tempDir -Recurse -Force
        Write-Host "    + ${mod}-win.jar (DLL)" -ForegroundColor Gray
    } else {
        Write-Host "    OSHIBKA: ${mod}-win ne nayden!" -ForegroundColor Red
        exit 1
    }
}

# Izvlekaem DLL iz Windows JAR v otdelnuyu papku native
Write-Host "[4/6] Izvlechenie DLL... " -NoNewline
foreach ($mod in "javafx-controls", "javafx-fxml", "javafx-base", "javafx-graphics") {
    $winJarPath = "$distDir\lib\$mod-win.jar"
    if (Test-Path $winJarPath -ErrorAction SilentlyContinue) {
        try {
            $dllFiles = & "$JDK_PATH\bin\jar.exe" tf "$winJarPath" | Where-Object { $_ -match '\.dll$' }
            $dllCount = ($dllFiles | Measure-Object).Count
            if ($dllCount -gt 0) {
                Push-Location "$distDir\native"
                & "$JDK_PATH\bin\jar.exe" xf "$winJarPath" $dllFiles
                Pop-Location
                Write-Host "    + ${mod}: $dllCount DLL izvlecheno" -ForegroundColor Gray
            }
        } catch {
            Write-Host ("    Warning: {0}: {1}" -f $mod, $_) -ForegroundColor Yellow
        }
    }
}
Write-Host "Gotovo." -ForegroundColor Green

Write-Host "[5/6] Fayly prilozheniya... " -NoNewline
Copy-Item $jarFile "$distDir\autoservice-admin.jar" -Force

# Styles
$stylesSrc = Join-Path $projectDir "src\main\resources\styles.css"
if (Test-Path $stylesSrc) {
    Copy-Item $stylesSrc "$distDir\styles.css" -Force
}

# Logback
$logbackSrc = Join-Path $projectDir "src\main\resources\logback.xml"
if (Test-Path $logbackSrc) {
    Copy-Item $logbackSrc "$distDir\logback.xml" -Force
}

# Config files
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
Write-Host "Gotovo." -ForegroundColor Green

Write-Host "[6/6] STO.bat... " -NoNewline
Set-Content "$distDir\STO.bat" @'
@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

cd /d "%~dp0"

if not exist "jre\bin\java.exe" (
    echo OSHIBKA: Java ne naydena!
    pause
    exit /b 1
)

if not exist "lib\javafx-controls.jar" (
    echo OSHIBKA: JavaFX ne naydena!
    pause
    exit /b 1
)

set APP_DIR=%CD%

echo ============================================
echo  AdminSTO - Administrator STO
echo ============================================
echo.
echo Zapusk...
echo.

:: JavaFX na module-path, DLL v native/, aplikaciya na classpath
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
    echo  OSHIBKA zapuska!
    echo ============================================
    echo.
    echo Proverьте:
    echo   1. Vsya papka skopirovana celikom?
    echo   2. Windows 64-bit?
    echo   3. Faili ne blokiruyut antivirus?
    echo.
    pause
)
'@ -Encoding Default

Write-Host "Gotovo." -ForegroundColor Green

$folderSize = (Get-ChildItem -Path $distDir -Recurse | Measure-Object -Property Length -Sum).Sum
$sizeMB = [math]::Round($folderSize / 1MB, 2)
Write-Host ""
Write-Host "==================================================" -ForegroundColor Green
Write-Host "  Portable gotovo! Razmer: ${sizeMB} MB" -ForegroundColor Green
Write-Host "=================================================="
Write-Host "Papka: $distDir"
Write-Host "Dlya flashki: skopiruyte AdminSTO_Portable, zapustite STO.bat"
