@echo off
REM Ejecuta las 45 pruebas automaticas (controladores, seguridad y reglas contra PostgreSQL).
REM Las pruebas con base de datos se revierten solas: no dejan datos.
cd /d "%~dp0"
if "%JAVA_HOME%"=="" for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk*") do set "JAVA_HOME=%%D"
set ODAPAS_TEST_DB_URL=jdbc:postgresql://127.0.0.1:5432/odapas_report
set ODAPAS_TEST_DB_USER=postgres
if "%ODAPAS_TEST_DB_PASSWORD%"=="" set /p ODAPAS_TEST_DB_PASSWORD=Contrasena de PostgreSQL: 
call "%~dp0mvnw.cmd" test
pause
