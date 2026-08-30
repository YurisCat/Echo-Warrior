@echo off
chcp 65001 >nul
setlocal

set "PROJECT_ROOT=%~dp0"
set "SUPPORT_SCRIPT=%PROJECT_ROOT%.toolchains\tester-kit\tester-first-setup.ps1"
if not exist "%SUPPORT_SCRIPT%" set "SUPPORT_SCRIPT=%PROJECT_ROOT%scripts\tester-first-setup.ps1"

if not exist "%SUPPORT_SCRIPT%" (
    echo Missing tester setup script. Please extract the complete offline package again.
    pause
    exit /b 1
)

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%SUPPORT_SCRIPT%" -ProjectRoot "%PROJECT_ROOT%"
set "EXIT_CODE=%ERRORLEVEL%"
echo.
pause
exit /b %EXIT_CODE%
