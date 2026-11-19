@echo off
REM Inicia la API de ODAPAS Reporte en http://localhost:8080
powershell -NoProfile -File "%~dp0iniciar-api.ps1"
if errorlevel 1 pause
