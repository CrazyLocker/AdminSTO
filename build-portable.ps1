<#
.SYNOPSIS
    Скрипт сборки полностью автономной portable-версии AdminSTO для Windows.
    Создаёт папку AdminSTO_Portable с встроенным JRE, JavaFX, зависимостями и bat-файлом для запуска.
.DESCRIPTION
    Скрипт выполняет следующие шаги:
    1. Поиск JDK 21 на системе
    2. Подготовка структуры папок
    3. Создание минимального JRE через jlink
    4. Копирование JavaFX (из кэша Gradle или скачивание напрямую)
    5. Копирование JAR и файлов конфигурации
    6. Создание STO.bat для запуска
.NOTES
    Требования: JDK 21, Gradle (для fatJar)
#>

# Отключаем остановку при ошибках для более гибкого контроля
$ErrorActionPreference = "Stop"

# ============================================================
# ПЕРЕМЕННЫЕ
# ============================================================
$projectDir = $PSScriptRoot                              # Корень проекта
$distDir = Join-Path $projectDir "AdminSTO_Portable"     # Папка для портативки
$jarFile = Join-Path $projectDir "build/libs/autoservice-admin.jar"
$JAVAFX_VERSION = "21.0.6"                               # Версия JavaFX

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "  СБОРКА ПОРТАТИВНОЙ ВЕРСИИ AdminSTO" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""

# ============================================================
# ШАГ 0: ПОИСК JDK 21
# ============================================================
Write-Host "[0/6] Поиск JDK 21..." -NoNewline

$possibleJdks = @(
    "C:\Program Files\Eclipse Adoptium\jdk-21*",
    "C:\Program Files\Java\jdk-21*",
    "C:\Program Files\Eclipse Adoptium\jdk-21*",
    "C:\Program Files\Java\jdk-21*"
)

$JDK_PATH = $null

# 1. Ищем в стандартных папках
foreach ($pattern in $possibleJdks) {
    $found = Get-ChildItem -Path $pattern -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($found -and (Test-Path "$($found.FullName)\bin\java.exe")) {
        $JDK_PATH = $found.FullName
        Write-Host " $JDK_PATH" -ForegroundColor Green
        break
    }
}

# 2. Если не нашли — проверяем JAVA_HOME
if (-not $JDK_PATH -and $env:JAVA_HOME) {
    if (Test-Path "$env:JAVA_HOME\bin\java.exe") {
        $JDK_PATH = $env:JAVA_HOME
        Write-Host " $JDK_PATH (из JAVA_HOME)" -ForegroundColor Green
    }
}

# 3. Если всё равно не нашли — ищем в PATH
if (-not $JDK_PATH) {
    $javaPath = Get-Command java -ErrorAction SilentlyContinue
    if ($javaPath) {
        $javaExe = $javaPath.Source
        $JDK_PATH = Split-Path (Split-Path $javaExe -Parent) -Parent
        if (-not (Test-Path "$JDK_PATH\bin\java.exe")) {
            $JDK_PATH = $null
        } else {
            Write-Host " $JDK_PATH (из PATH)" -ForegroundColor Green
        }
    }
}

# 4. Если не нашли — выходим с ошибкой
if (-not $JDK_PATH) {
    Write-Host " НЕ НАЙДЕН!" -ForegroundColor Red
    Write-Host ""
    Write-Host "❌ ОШИБКА: JDK 21 не найден!" -ForegroundColor Red
    Write-Host "Установите JDK 21 (Adoptium/Eclipse Temurin или Oracle)" -ForegroundColor Yellow
    Write-Host "Скачать: https://adoptium.net/temurin/releases/?version=21" -ForegroundColor Yellow
    exit 1
}

# Проверяем версию JDK
try {
    $javaVersion = & "$JDK_PATH\bin\java.exe" -version 2>&1 | Select-String "version" | Select-Object -First 1
    Write-Host "Версия JDK: $javaVersion" -ForegroundColor Cyan
} catch {
    Write-Host "⚠️ Версия JDK: не удалось определить" -ForegroundColor Yellow
}
Write-Host ""

# ============================================================
# ШАГ 0.1: ПРОВЕРКА FAT JAR
# ============================================================
Write-Host "[0.1/6] Проверка fat JAR..." -NoNewline
if (-not (Test-Path $jarFile)) {
    Write-Host " НЕ НАЙДЕН!" -ForegroundColor Red
    Write-Host ""
    Write-Host "❌ ОШИБКА: fat JAR не найден!" -ForegroundColor Red
    Write-Host "Запустите: .\gradlew.bat clean fatJar" -ForegroundColor Yellow
    exit 1
}
Write-Host " ОК" -ForegroundColor Green
Write-Host ""

# ============================================================
# ШАГ 1: ПОДГОТОВКА СТРУКТУРЫ ПАПОК
# ============================================================
Write-Host "[1/6] Подготовка структуры папок..." -NoNewline

# Удаляем старую папку, если есть
if (Test-Path $distDir) {
    Remove-Item "$distDir" -Recurse -Force
}

# Создаём необходимые папки
New-Item -ItemType Directory -Path "$distDir\lib" -Force | Out-Null
New-Item -ItemType Directory -Path "$distDir\native" -Force | Out-Null
New-Item -ItemType Directory -Path "$distDir\data" -Force | Out-Null
New-Item -ItemType Directory -Path "$distDir\logs" -Force | Out-Null
New-Item -ItemType Directory -Path "$distDir\backups" -Force | Out-Null

Write-Host " Готово" -ForegroundColor Green

# ============================================================
# ШАГ 2: СОЗДАНИЕ МИНИМАЛЬНОГО JRE ЧЕРЕЗ JLINK
# ============================================================
Write-Host "[2/6] Создание минимального JRE (jlink)..." -NoNewline

# Формируем список модулей для JRE
$modules = @(
    "java.base",
    "java.sql",
    "java.logging",
    "java.instrument",
    "java.management",
    "jdk.zipfs",
    "java.xml",
    "jdk.httpserver",
    "jdk.unsupported",
    "jdk.localedata",
    "java.naming",
    "java.scripting",
    "java.desktop"
)

# Запускаем jlink
& "$JDK_PATH\bin\jlink.exe" `
    --module-path "$JDK_PATH\jmods" `
    --add-modules ($modules -join ",") `
    --include-locales ru `
    --strip-debug `
    --no-man-pages `
    --no-header-files `
    --compress=2 `
    --output "$distDir\jre"

# Проверяем успешность
if ($LASTEXITCODE -ne 0) {
    Write-Host " ОШИБКА!" -ForegroundColor Red
    exit 1
}

# Показываем размер JRE
$jreSize = (Get-ChildItem "$distDir\jre" -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB
Write-Host " Готово (~$([math]::Round($jreSize, 1)) МБ)" -ForegroundColor Green

# ============================================================
# ШАГ 3: КОПИРОВАНИЕ JAVAFX
# ============================================================
Write-Host "[3/6] Копирование JavaFX..." -NoNewline

# Список модулей JavaFX, которые нужно скопировать
$javafxModules = @(
    "javafx-controls",
    "javafx-fxml", 
    "javafx-base",
    "javafx-graphics"
)

# Пути для поиска JavaFX в кэше Gradle (в разных местах)
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
        Write-Host "`n   Найден кэш Gradle: $gradleCache" -ForegroundColor Gray
        break
    }
}

# Если кэш не найден — пытаемся скачать зависимости через Gradle
if (-not $gradleCache) {
    Write-Host "`n   ⚠️ Кэш JavaFX не найден! Попытка скачать зависимости через Gradle..." -ForegroundColor Yellow
    
    # Запускаем Gradle для скачивания зависимостей
    & .\gradlew.bat dependencies --no-daemon
    
    Write-Host "   Повторный поиск кэша..." -ForegroundColor Gray
    
    # Повторяем поиск после скачивания
    foreach ($path in $gradleCachePaths) {
        if (Test-Path $path) {
            $gradleCache = $path
            Write-Host "   Найден кэш Gradle: $gradleCache" -ForegroundColor Gray
            break
        }
    }
}

# Если всё равно не найден — скачиваем JavaFX напрямую с Gluon
if (-not $gradleCache) {
    Write-Host "`n   ⚠️ Кэш Gradle не найден! Скачивание JavaFX напрямую..." -ForegroundColor Yellow
    
    # Создаём временную папку
    $javafxTemp = "$env:TEMP\javafx"
    if (Test-Path $javafxTemp) { Remove-Item $javafxTemp -Recurse -Force }
    New-Item -ItemType Directory -Path $javafxTemp -Force | Out-Null
    
    # Скачиваем SDK
    $javafxUrl = "https://download2.gluonhq.com/openjfx/${JAVAFX_VERSION}/openjfx-${JAVAFX_VERSION}_windows-x64_bin-sdk.zip"
    $javafxZip = "$env:TEMP\javafx-sdk.zip"
    
    Write-Host "   Загрузка JavaFX SDK..." -ForegroundColor Gray
    try {
        Invoke-WebRequest -Uri $javafxUrl -OutFile $javafxZip -UseBasicParsing -ErrorAction Stop
    } catch {
        Write-Host "   ❌ Ошибка загрузки JavaFX!" -ForegroundColor Red
        Write-Host "   Проверьте интернет-соединение" -ForegroundColor Yellow
        exit 1
    }
    
    Write-Host "   Распаковка..." -ForegroundColor Gray
    Expand-Archive -Path $javafxZip -DestinationPath $javafxTemp -Force
    
    # Копируем JAR файлы
    $javafxSdkLib = "$javafxTemp\javafx-sdk-${JAVAFX_VERSION}\lib"
    foreach ($mod in $javafxModules) {
        $jarPath = "$javafxSdkLib\$mod.jar"
        if (Test-Path $jarPath) {
            Copy-Item $jarPath "$distDir\lib\$mod.jar" -Force
            Write-Host "   + ${mod}.jar" -ForegroundColor Gray
        } else {
            Write-Host "   ❌ ${mod}.jar не найден!" -ForegroundColor Red
        }
    }
    
    # Копируем DLL файлы
    $dllFiles = Get-ChildItem -Path $javafxSdkLib -Filter "*.dll" -Recurse
    foreach ($dll in $dllFiles) {
        Copy-Item $dll.FullName "$distDir\native\" -Force
        Write-Host "   + $($dll.Name)" -ForegroundColor Gray
    }
    
    # Очищаем временные файлы
    Remove-Item $javafxZip -Force -ErrorAction SilentlyContinue
    Remove-Item $javafxTemp -Recurse -Force -ErrorAction SilentlyContinue
    
    Write-Host "   Готово" -ForegroundColor Green
    Write-Host "[4/6] Извлечение DLL... Пропущено (уже скопированы)" -ForegroundColor Green
    Write-Host "[5/6] Файлы приложения..." -NoNewline
    
    # Копируем JAR приложения
    Copy-Item $jarFile "$distDir\autoservice-admin.jar" -Force
    
    # Копируем стили
    $stylesSrc = Join-Path $projectDir "src\main\resources\styles.css"
    if (Test-Path $stylesSrc) { 
        Copy-Item $stylesSrc "$distDir\styles.css" -Force
        Write-Host " + styles.css" -ForegroundColor Gray
    }
    
    # Копируем logback
    $logbackSrc = Join-Path $projectDir "src\main\resources\logback.xml"
    if (Test-Path $logbackSrc) { 
        Copy-Item $logbackSrc "$distDir\logback.xml" -Force
        Write-Host " + logback.xml" -ForegroundColor Gray
    }
    
    Write-Host " Готово" -ForegroundColor Green
    
    # Переходим к созданию STO.bat
    Write-Host "[6/6] Создание STO.bat..." -NoNewline
    
    # Создаём bat-файл
    Set-Content "$distDir\STO.bat" @'
@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

cd /d "%~dp0"

:: Проверка наличия Java
if not exist "jre\bin\java.exe" (
    echo ОШИБКА: Java не найдена!
    pause
    exit /b 1
)

:: Проверка наличия JavaFX
if not exist "lib\javafx-controls.jar" (
    echo ОШИБКА: JavaFX не найдена!
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

:: Запуск приложения
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

:: Проверка ошибок
if errorlevel 1 (
    echo.
    echo ============================================
    echo  ОШИБКА ЗАПУСКА!
    echo ============================================
    echo.
    echo Проверьте:
    echo   1. Папка скопирована целиком?
    echo   2. Windows 64-bit?
    echo   3. Файлы не блокирует антивирус?
    echo.
    pause
)
'@ -Encoding Default

    Write-Host " Готово" -ForegroundColor Green
    
    # Вывод итогов
    $folderSize = (Get-ChildItem -Path $distDir -Recurse | Measure-Object -Property Length -Sum).Sum
    $sizeMB = [math]::Round($folderSize / 1MB, 2)
    Write-Host ""
    Write-Host "==================================================" -ForegroundColor Green
    Write-Host "  ✅ ПОРТАТИВНАЯ ВЕРСИЯ ГОТОВА!" -ForegroundColor Green
    Write-Host "  Размер: ${sizeMB} МБ" -ForegroundColor Green
    Write-Host "==================================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "📁 Папка: $distDir"
    Write-Host "▶️  Для запуска: скопируйте папку на флешку и запустите STO.bat"
    Write-Host ""
    
    exit 0
}

# ============================================================
# ЕСЛИ КЭШ НАЙДЕН — КОПИРУЕМ ИЗ НЕГО
# ============================================================

# Копируем модульные JAR (с module-info.class)
foreach ($mod in $javafxModules) {
    $foundJar = Get-ChildItem -Path "$gradleCache\$mod" -Recurse -Filter "*.jar" | 
                Where-Object { $_.Name -like "${mod}-*.jar" -and $_.Name -notlike "*-win.jar" } | 
                Select-Object -First 1
    if ($foundJar) {
        Copy-Item $foundJar.FullName "$distDir\lib\$mod.jar" -Force
        Write-Host "`n   + ${mod}.jar (модульный)" -ForegroundColor Gray
    } else {
        Write-Host "`n   ❌ ${mod} не найден в кэше Gradle!" -ForegroundColor Red
        exit 1
    }
}

# Копируем Windows JAR (с DLL внутри) и удаляем module-info.class
foreach ($mod in $javafxModules) {
    $foundJar = Get-ChildItem -Path "$gradleCache\$mod" -Recurse -Filter "*-win.jar" | 
                Where-Object { $_.Name -like "${mod}-*" } | 
                Select-Object -First 1
    if ($foundJar) {
        $tempDir = "$env:TEMP\javafx-${mod}-win"
        if (Test-Path $tempDir) { Remove-Item $tempDir -Recurse -Force }
        New-Item -ItemType Directory -Path $tempDir -Force | Out-Null
        
        # Распаковываем во временную папку
        Push-Location $tempDir
        & "$JDK_PATH\bin\jar.exe" xf $foundJar.FullName
        
        # Удаляем module-info.class (если есть)
        if (Test-Path "module-info.class") {
            Remove-Item "module-info.class" -Force
        }
        
        # Пересобираем без module-info.class
        & "$JDK_PATH\bin\jar.exe" cf "$distDir\lib\${mod}-win.jar" *
        Pop-Location
        
        # Удаляем временную папку
        Remove-Item $tempDir -Recurse -Force
        Write-Host "   + ${mod}-win.jar (DLL внутри)" -ForegroundColor Gray
    } else {
        Write-Host "   ❌ ${mod}-win не найден в кэше Gradle!" -ForegroundColor Red
        exit 1
    }
}

Write-Host " Готово" -ForegroundColor Green

# ============================================================
# ШАГ 4: ИЗВЛЕЧЕНИЕ DLL ИЗ WINDOWS JAR
# ============================================================
Write-Host "[4/6] Извлечение DLL из Windows JAR..." -NoNewline

$dllExtracted = 0
foreach ($mod in $javafxModules) {
    $winJarPath = "$distDir\lib\$mod-win.jar"
    if (Test-Path $winJarPath) {
        try {
            # Получаем список DLL в JAR
            $dllFiles = & "$JDK_PATH\bin\jar.exe" tf "$winJarPath" | Where-Object { $_ -match '\.dll$' }
            $dllCount = ($dllFiles | Measure-Object).Count
            
            if ($dllCount -gt 0) {
                # Извлекаем DLL
                Push-Location "$distDir\native"
                & "$JDK_PATH\bin\jar.exe" xf "$winJarPath" $dllFiles
                Pop-Location
                $dllExtracted += $dllCount
                Write-Host "`n   + ${mod}: $dllCount DLL извлечено" -ForegroundColor Gray
            }
        } catch {
            Write-Host "`n   ⚠️ ${mod}: ошибка извлечения" -ForegroundColor Yellow
        }
    }
}

if ($dllExtracted -gt 0) {
    Write-Host "`n   Всего извлечено: $dllExtracted DLL" -ForegroundColor Green
}
Write-Host " Готово" -ForegroundColor Green

# ============================================================
# ШАГ 5: КОПИРОВАНИЕ ФАЙЛОВ ПРИЛОЖЕНИЯ
# ============================================================
Write-Host "[5/6] Копирование файлов приложения..." -NoNewline

# Копируем JAR приложения
Copy-Item $jarFile "$distDir\autoservice-admin.jar" -Force

# Копируем стили
$stylesSrc = Join-Path $projectDir "src\main\resources\styles.css"
if (Test-Path $stylesSrc) {
    Copy-Item $stylesSrc "$distDir\styles.css" -Force
    Write-Host " + styles.css" -ForegroundColor Gray
}

# Копируем logback
$logbackSrc = Join-Path $projectDir "src\main\resources\logback.xml"
if (Test-Path $logbackSrc) {
    Copy-Item $logbackSrc "$distDir\logback.xml" -Force
    Write-Host " + logback.xml" -ForegroundColor Gray
}

# Копируем папку config
$configSrc = Join-Path $projectDir "config"
if (Test-Path $configSrc) {
    # Таблицы
    if (Test-Path "$configSrc\table-state") {
        New-Item -ItemType Directory -Path "$distDir\config\table-state" -Force | Out-Null
        Copy-Item "$configSrc\table-state\*" "$distDir\config\table-state\" -Force
        Write-Host " + config/table-state/" -ForegroundColor Gray
    }
    # Окна
    if (Test-Path "$configSrc\window-state") {
        New-Item -ItemType Directory -Path "$distDir\config\window-state" -Force | Out-Null
        Copy-Item "$configSrc\window-state\*" "$distDir\config\window-state\" -Force
        Write-Host " + config/window-state/" -ForegroundColor Gray
    }
}

Write-Host " Готово" -ForegroundColor Green

# ============================================================
# ШАГ 6: СОЗДАНИЕ STO.BAT
# ============================================================
Write-Host "[6/6] Создание STO.bat..." -NoNewline

Set-Content "$distDir\STO.bat" @'
@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

cd /d "%~dp0"

:: Проверка наличия Java
if not exist "jre\bin\java.exe" (
    echo ОШИБКА: Java не найдена!
    pause
    exit /b 1
)

:: Проверка наличия JavaFX
if not exist "lib\javafx-controls.jar" (
    echo ОШИБКА: JavaFX не найдена!
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

:: Запуск приложения
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

:: Проверка ошибок
if errorlevel 1 (
    echo.
    echo ============================================
    echo  ОШИБКА ЗАПУСКА!
    echo ============================================
    echo.
    echo Проверьте:
    echo   1. Папка скопирована целиком?
    echo   2. Windows 64-bit?
    echo   3. Файлы не блокирует антивирус?
    echo.
    pause
)
'@ -Encoding Default

Write-Host " Готово" -ForegroundColor Green

# ============================================================
# ИТОГИ
# ============================================================
$folderSize = (Get-ChildItem -Path $distDir -Recurse | Measure-Object -Property Length -Sum).Sum
$sizeMB = [math]::Round($folderSize / 1MB, 2)

Write-Host ""
Write-Host "==================================================" -ForegroundColor Green
Write-Host "  ✅ ПОРТАТИВНАЯ ВЕРСИЯ ГОТОВА!" -ForegroundColor Green
Write-Host "  Размер: ${sizeMB} МБ" -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Green
Write-Host ""
Write-Host "📁 Папка: $distDir"
Write-Host "▶️  Для запуска: скопируйте папку на флешку и запустите STO.bat"
Write-Host ""