@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion

echo ========================================
echo  AutoService STO - Admin (Portable)
echo ========================================
echo.

rem === Используем встроенный JRE ===
set SCRIPT_DIR=%~dp0
set JRE_PATH="%SCRIPT_DIR%jre\bin\java.exe"
set JAVAFX_PATH="%SCRIPT_DIR%javafx"
set APP_PATH="%SCRIPT_DIR%autoservice-admin.jar"

echo Launching AutoService STO...
echo.

if not exist !JRE_PATH! (
    echo ERROR: JRE not found!
    echo Application is corrupted.
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
