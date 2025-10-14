@echo off
REM Chat-Unillanos: Script de inicio para Windows (Batch)
echo === Chat-Unillanos: Start Script ===

REM Verificar que Docker está instalado
where docker >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Docker no está instalado o no está en PATH.
    pause
    exit /b 1
)

REM Ir a la raíz del proyecto
cd /d %~dp0

REM 1. Levantar base de datos
echo.
echo [1/3] Levantando base de datos con Docker...
docker compose up -d
if %errorlevel% neq 0 (
    echo ERROR: Fallo al levantar Docker Compose.
    pause
    exit /b 1
)

REM 2. Esperar a que MySQL esté healthy
echo.
echo [2/3] Esperando a que MySQL esté healthy...
:wait_mysql
timeout /t 3 /nobreak >nul
docker inspect -f "{{.State.Health.Status}}" chat-unillanos-mysql 2>nul | findstr /C:"healthy" >nul
if %errorlevel% neq 0 (
    echo Esperando MySQL...
    goto wait_mysql
)
echo MySQL está listo!

REM 3. Compilar proyecto (si no se pasa argumento --skip-build)
if "%1"=="--skip-build" goto run_server

echo.
echo [3/3] Compilando proyecto...
call mvn clean install -DskipTests
if %errorlevel% neq 0 (
    echo ERROR: Compilación fallida.
    pause
    exit /b 1
)

:run_server
echo.
echo [3/3] Ejecutando servidor y GUI...
call mvn -pl Presentacion/Main spring-boot:run
if %errorlevel% neq 0 (
    echo ERROR: Servidor falló al iniciar.
    pause
    exit /b 1
)

pause

