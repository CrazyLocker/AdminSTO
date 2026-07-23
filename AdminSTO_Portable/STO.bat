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
