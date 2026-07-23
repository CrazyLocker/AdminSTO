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
