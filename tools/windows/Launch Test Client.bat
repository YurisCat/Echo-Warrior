@echo off
setlocal EnableExtensions
title Echo Warrior - Test Client

for %%I in ("%~dp0..\..") do set "PROJECT_ROOT=%%~fI"
set "LAUNCH_SCRIPT=%PROJECT_ROOT%\scripts\playtest-now.ps1"

if not exist "%LAUNCH_SCRIPT%" goto missing_script

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%LAUNCH_SCRIPT%"
set "RESULT=%ERRORLEVEL%"

if not "%RESULT%"=="0" (
    echo.
    pause
)

exit /b %RESULT%

:missing_script
echo [ERROR] The Echo Warrior repository is incomplete.
echo Missing: "%LAUNCH_SCRIPT%"
echo Please update the repository and try again.
echo.
pause
exit /b 1
