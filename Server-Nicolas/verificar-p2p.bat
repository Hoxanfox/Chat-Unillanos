@echo off
REM Script de Verificación Rápida del Sistema P2P

echo ========================================
echo  VERIFICACION SISTEMA P2P
echo ========================================
echo.

echo [1/5] Verificando estructura de directorios...
if exist "config\server.properties" (
    echo   ✓ config\server.properties encontrado
) else (
    echo   ✗ FALTA: config\server.properties
)

if exist "pom.xml" (
    echo   ✓ pom.xml encontrado
) else (
    echo   ✗ FALTA: pom.xml
)

echo.
echo [2/5] Verificando archivos P2P implementados...

set "FILES_OK=0"

if exist "negocio\server-LogicaPeers\src\main\java\com\arquitectura\logicaPeers\IPeerNotificationService.java" (
    echo   ✓ IPeerNotificationService.java
    set /a FILES_OK+=1
) else (
    echo   ✗ FALTA: IPeerNotificationService.java
)

if exist "negocio\server-LogicaPeers\src\main\java\com\arquitectura\logicaPeers\PeerNotificationServiceImpl.java" (
    echo   ✓ PeerNotificationServiceImpl.java
    set /a FILES_OK+=1
) else (
    echo   ✗ FALTA: PeerNotificationServiceImpl.java
)

if exist "transporte\server-controladorTransporte\src\main\java\com\arquitectura\controlador\controllers\P2PNotificationController.java" (
    echo   ✓ P2PNotificationController.java
    set /a FILES_OK+=1
) else (
    echo   ✗ FALTA: P2PNotificationController.java
)

if exist "vista\server-vista\src\main\java\com\arquitectura\vista\PeersReportPanel.java" (
    echo   ✓ PeersReportPanel.java
    set /a FILES_OK+=1
) else (
    echo   ✗ FALTA: PeersReportPanel.java
)

echo.
echo [3/5] Verificando configuración P2P...
findstr /C:"peer.server.port" config\server.properties >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo   ✓ Configuración P2P presente
    findstr "peer.server.port" config\server.properties
) else (
    echo   ✗ FALTA: Configuración peer.server.port
)

echo.
echo [4/5] Verificando Java y Maven...
java -version >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo   ✓ Java instalado
    java -version 2>&1 | findstr "version"
) else (
    echo   ✗ Java NO encontrado
)

mvn -version >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo   ✓ Maven instalado
    mvn -version 2>&1 | findstr "Apache Maven"
) else (
    echo   ✗ Maven NO encontrado
)

echo.
echo [5/5] Resumen de verificación...
echo   Archivos P2P implementados: %FILES_OK%/4
echo.

if %FILES_OK% EQU 4 (
    echo ========================================
    echo  ✓ SISTEMA LISTO PARA PRUEBAS
    echo ========================================
    echo.
    echo Siguiente paso:
    echo 1. Ejecutar: mvn clean install -DskipTests
    echo 2. Iniciar servidor: start-server1.bat
    echo 3. Ver guía: GUIA_PRUEBAS_P2P.md
) else (
    echo ========================================
    echo  ✗ SISTEMA INCOMPLETO
    echo ========================================
    echo.
    echo Faltan archivos P2P. Revisa la implementación.
)

echo.
pause

