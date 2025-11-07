@echo off
REM Script para iniciar el Servidor 2 (Secundario)
REM Requiere que copies el proyecto a Server-Nicolas-2

echo ========================================
echo  Iniciando Servidor Secundario
echo  Puerto Clientes: 8081
echo  Puerto P2P: 9091
echo ========================================
echo.

set SERVER2_DIR=%~dp0..\Server-Nicolas-2

if not exist "%SERVER2_DIR%" (
    echo.
    echo ERROR: No se encuentra el directorio Server-Nicolas-2
    echo.
    echo Para crear el Servidor 2:
    echo 1. Copia toda la carpeta Server-Nicolas
    echo 2. Renombrala a Server-Nicolas-2
    echo 3. Edita Server-Nicolas-2\config\server.properties
    echo    - Cambia server.port=8081
    echo    - Cambia peer.server.port=9091
    echo    - Agrega peer.bootstrap.nodes=localhost:9090
    echo.
    pause
    exit /b 1
)

cd /d "%SERVER2_DIR%"

echo Verificando configuracion...
if not exist "config\server.properties" (
    echo ERROR: No se encuentra config\server.properties
    pause
    exit /b 1
)

echo.
echo Iniciando servidor con Maven...
echo (Presiona Ctrl+C para detener)
echo.

mvn spring-boot:run

pause
@echo off
REM Script para iniciar el Servidor 1 (Principal)
REM Puerto 8080 para clientes, 9090 para P2P

echo ========================================
echo  Iniciando Servidor Principal
echo  Puerto Clientes: 8080
echo  Puerto P2P: 9090
echo ========================================
echo.

cd /d "%~dp0"

echo Verificando configuracion...
if not exist "config\server.properties" (
    echo ERROR: No se encuentra config\server.properties
    pause
    exit /b 1
)

echo.
echo Iniciando servidor con Maven...
echo (Presiona Ctrl+C para detener)
echo.

mvn spring-boot:run

pause

