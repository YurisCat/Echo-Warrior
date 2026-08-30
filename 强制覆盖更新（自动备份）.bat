@echo off
setlocal EnableExtensions
title Echo Warrior - Modeler Force Update
set "KIT_ROOT=%~dp0.modeler-kit"
set "SCRIPT_PATH=%KIT_ROOT%\scripts\modeler-force-update.ps1"
set "ECHO_WARRIOR_GIT_PROXY=http://127.0.0.1:7897"
set "HTTPS_PROXY=%ECHO_WARRIOR_GIT_PROXY%"
set "HTTP_PROXY=%ECHO_WARRIOR_GIT_PROXY%"
if not exist "%SCRIPT_PATH%" goto missing_script
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_PATH%" -KitRoot "%KIT_ROOT%" -ProxyUrl "%ECHO_WARRIOR_GIT_PROXY%"
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
