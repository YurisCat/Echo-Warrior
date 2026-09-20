@echo off
setlocal EnableExtensions
title Echo Warrior - Minecraft 1.20.1 Forge - Pause on World Entry
for %%I in ("%~dp0..\..") do set "ECHO_PROJECT_ROOT=%%~fI"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%ECHO_PROJECT_ROOT%\scripts\run-test-client.ps1" -TargetVersion 1.20.1 -Loader Forge -RequireExistingWorld -PauseOnJoin
set "ECHO_LAUNCH_RESULT=%ERRORLEVEL%"
if not "%ECHO_LAUNCH_RESULT%"=="0" pause
exit /b %ECHO_LAUNCH_RESULT%
