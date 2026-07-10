@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion

echo =====================================================
echo   AdminSTO v1.0.0 - Портативная версия (автономная)
echo =====================================================
echo.

set SCRIPT_DIR=%~dp0
set JRE_PATH=!SCRIPT_DIR!jre\bin\java.exe
set LIB_PATH=!SCRIPT_DIR!lib
set APP_PATH=!SCRIPT_DIR!autoservice-admin.jar

echo Запуск AdminSTO...
echo.

if not exist !JRE_PATH! (
    echo ERROR: JRE not found in !SCRIPT_DIR!jre
    pause
    exit /b 1
)

!JRE_PATH! ^
    --module-path !LIB_PATH! ^
    --add-modules javafx.controls,javafx.fxml ^
    -Djava.library.path=!LIB_PATH! ^
    -Duser.dir=!SCRIPT_DIR! ^
    -Dapp.home=!SCRIPT_DIR! ^
    -jar !APP_PATH!

if errorlevel 1 (
    echo.
    echo ERROR: Application failed!
    pause
)