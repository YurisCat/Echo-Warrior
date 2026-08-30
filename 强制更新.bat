@echo off
chcp 65001 >nul
setlocal

set "PROJECT_ROOT=%~dp0"
set "SUPPORT_SCRIPT=%PROJECT_ROOT%.toolchains\tester-kit\tester-force-update.ps1"
if not exist "%SUPPORT_SCRIPT%" set "SUPPORT_SCRIPT=%PROJECT_ROOT%scripts\tester-force-update.ps1"

if not exist "%SUPPORT_SCRIPT%" (
    echo Missing tester update script. Please ask the developer for a refreshed offline package.
    pause
    exit /b 1
)

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%SUPPORT_SCRIPT%" -ProjectRoot "%PROJECT_ROOT%"
set "EXIT_CODE=%ERRORLEVEL%"
echo.
pause
exit /b %EXIT_CODE%
