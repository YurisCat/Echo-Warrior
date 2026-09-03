@echo off
setlocal EnableExtensions
title Echo Warrior - Build Dual Package

for %%I in ("%~dp0..\..") do set "PROJECT_ROOT=%%~fI"
set "BUILD_SCRIPT=%PROJECT_ROOT%\scripts\build-dual-candidate.ps1"

if not exist "%BUILD_SCRIPT%" goto missing_script

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%BUILD_SCRIPT%" %*
set "RESULT=%ERRORLEVEL%"
goto finish

:missing_script
echo [ERROR] The Echo Warrior repository is incomplete.
echo Missing: "%BUILD_SCRIPT%"
echo Please update the repository and try again.
set "RESULT=1"

:finish
echo.
pause
exit /b %RESULT%
