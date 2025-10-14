@echo off
REM Chat-Unillanos: Script para ejecutar JAR ejecutable
echo === Chat-Unillanos: Ejecutar JAR ===

REM Verificar que Java 21 está instalado
java -version 2>&1 | findstr /C:"21" >nul
if %errorlevel% neq 0 (
    echo ERROR: Java 21 no está instalado o no está en PATH.
    echo Por favor instala Java 21 y configura JAVA_HOME.
    pause
    exit /b 1
)

REM Ir a la raíz del proyecto
cd /d %~dp0

REM Verificar que el JAR existe
if not exist "Presentacion\Main\target\main-1.0.0-SNAPSHOT.jar" (
    echo ERROR: JAR no encontrado.
    echo Por favor ejecuta primero: build-jar.bat
    pause
    exit /b 1
)

REM Verificar que Docker está corriendo
docker ps >nul 2>&1
if %errorlevel% neq 0 (
    echo ADVERTENCIA: Docker no está corriendo.
    echo Iniciando MySQL con Docker...
    docker-compose up -d
    timeout /t 10 /nobreak >nul
)

echo.
echo [1/2] Verificando MySQL...
docker inspect -f "{{.State.Health.Status}}" chat-unillanos-mysql 2>nul | findstr /C:"healthy" >nul
if %errorlevel% neq 0 (
    echo MySQL no está listo. Levantando MySQL...
    docker-compose up -d
    echo Esperando a que MySQL esté healthy...
    :wait_mysql
    timeout /t 3 /nobreak >nul
    docker inspect -f "{{.State.Health.Status}}" chat-unillanos-mysql 2>nul | findstr /C:"healthy" >nul
    if %errorlevel% neq 0 (
        echo Esperando MySQL...
        goto wait_mysql
    )
)
echo MySQL está listo!

echo.
echo [2/2] Ejecutando servidor...
echo.
java -jar Presentacion\Main\target\main-1.0.0-SNAPSHOT.jar

pause

