@echo off
chcp 65001 >nul
setlocal

set "PROJECT_ROOT=%~dp0"
set "SUPPORT_SCRIPT=%PROJECT_ROOT%.toolchains\tester-kit\tester-launch.ps1"
if not exist "%SUPPORT_SCRIPT%" set "SUPPORT_SCRIPT=%PROJECT_ROOT%scripts\tester-launch.ps1"

if not exist "%SUPPORT_SCRIPT%" (
    echo Missing tester launch script. Please extract the complete offline package again.
    pause
    exit /b 1
)

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%SUPPORT_SCRIPT%" -ProjectRoot "%PROJECT_ROOT%"
set "EXIT_CODE=%ERRORLEVEL%"

if not "%EXIT_CODE%"=="0" (
	echo.
	pause
)

exit /b %EXIT_CODE%
