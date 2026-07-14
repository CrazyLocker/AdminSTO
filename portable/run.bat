@echo off
setlocal

echo Launching AdminSTO Portable...
echo.

cd /d "%~dp0"

if not exist "jre\bin\java.exe" (
    echo ERROR: Java not found!
    pause
    exit /b 1
)

set APP_DIR=%CD%

jre\bin\java.exe ^
    --enable-native-access=javafx.graphics ^
    --enable-native-access=org.xerial.sqlitejdbc ^
    -Djava.library.path="%APP_DIR%\lib" ^
    -Duser.dir="%APP_DIR%" ^
    -Dapp.home="%APP_DIR%" ^
    --module-path "%APP_DIR%\lib" ^
    --add-modules javafx.controls,javafx.fxml ^
    -jar "%APP_DIR%\autoservice-admin.jar"

if errorlevel 1 (
    echo.
    echo Application failed!
    pause
)