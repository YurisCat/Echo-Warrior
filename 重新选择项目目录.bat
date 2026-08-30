@echo off
setlocal EnableExtensions
title Echo Warrior - Select Project Folder
set "KIT_ROOT=%~dp0.modeler-kit"
set "SCRIPT_PATH=%KIT_ROOT%\scripts\modeler-select-project.ps1"
if not exist "%SCRIPT_PATH%" goto missing_script
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_PATH%" -KitRoot "%KIT_ROOT%"
set "RESULT=%ERRORLEVEL%"
echo.
pause
exit /b %RESULT%

:missing_script
echo [ERROR] The modeler toolkit is incomplete or was not fully extracted.
echo Missing: "%SCRIPT_PATH%"
echo Please extract the whole ZIP again and keep the .modeler-kit folder.
echo.
pause
exit /b 1
