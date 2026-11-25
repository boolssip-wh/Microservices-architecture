@echo off
chcp 65001 >nul
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-all.ps1"
set "START_EXIT=%ERRORLEVEL%"
pause
exit /b %START_EXIT%
