@echo off
setlocal EnableExtensions
title Echo Warrior - Local Encyclopedia

for %%I in ("%~dp0..\..") do set "PROJECT_ROOT=%%~fI"
set "LAUNCH_SCRIPT=%PROJECT_ROOT%\scripts\start-local-encyclopedia.ps1"

if not exist "%LAUNCH_SCRIPT%" goto missing_script

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%LAUNCH_SCRIPT%" -ProjectRoot "%PROJECT_ROOT%" %*
set "RESULT=%ERRORLEVEL%"
goto finish

:missing_script
echo [ERROR] The Echo Warrior repository is incomplete.
echo Missing: "%LAUNCH_SCRIPT%"
echo Please update the repository and try again.
set "RESULT=1"

:finish
echo.
pause
exit /b %RESULT%
