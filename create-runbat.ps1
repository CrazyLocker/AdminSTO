# Скрипт для создания правильного run.bat
$batContent = @"
@echo off
setlocal EnableDelayedExpansion

set APP_DIR=%~dp0

echo Launching AdminSTO Portable v1.0.0...
echo.

""!APP_DIR!jre\bin\java.exe"" ^
    -Djava.library.path=""!APP_DIR!lib"" ^
    -Duser.dir=""!APP_DIR!"" ^
    -Dapp.home=""!APP_DIR!"" ^
    --module-path ""!APP_DIR!lib"" ^
    --add-modules javafx.controls,javafx.fxml ^
    -jar ""!APP_DIR!autoservice-admin.jar""

if errorlevel 1 (
    echo.
    echo Application failed!
    pause
)
"@

[System.IO.File]::WriteAllText("$PSScriptRoot\portable\run.bat", $batContent, [System.Text.Encoding]::Default)
Write-Host "run.bat created successfully"
