@echo off
setlocal EnableExtensions
title Echo Warrior - Local Encyclopedia

set "LOCAL_SCRIPT=%~dp0scripts\start-local-encyclopedia.ps1"
if not exist "%LOCAL_SCRIPT%" goto missing_script
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%LOCAL_SCRIPT%" -ProjectRoot "%~dp0"
set "RESULT=%ERRORLEVEL%"
goto finish

:missing_script
echo [ERROR] The Echo Warrior project is incomplete or was not fully updated.
echo Missing: "%LOCAL_SCRIPT%"
echo Please update the project and try again.
set "RESULT=1"

:finish
echo.
pause
exit /b %RESULT%
