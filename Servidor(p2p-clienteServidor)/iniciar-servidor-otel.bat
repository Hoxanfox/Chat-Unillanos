@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion

echo ========================================
echo   CHAT-UNILLANOS - SERVIDOR P2P
echo   Con Observabilidad (OpenTelemetry)
echo ========================================
echo.

cd /d "%~dp0"

:: =============================================================================
:: LEER CONFIGURACIÓN DESDE configuracion.txt
:: =============================================================================
set CONFIG_FILE=configuracion.txt

if not exist "%CONFIG_FILE%" (
    echo [ERROR] No se encontro el archivo de configuracion: %CONFIG_FILE%
    pause
    exit /b 1
)

:: Leer variables del archivo de configuración
for /f "usebackq tokens=1,* delims==" %%a in ("%CONFIG_FILE%") do (
    set "line=%%a"
    :: Ignorar líneas que empiezan con # (comentarios)
    if not "!line:~0,1!"=="#" (
        if "%%a"=="peer.host" set "PEER_HOST=%%b"
        if "%%a"=="peer.puerto" set "PEER_PORT=%%b"
        if "%%a"=="cliente.host" set "CLIENTE_HOST=%%b"
        if "%%a"=="cliente.puerto" set "CLIENTE_PORT=%%b"
        if "%%a"=="otel.collector.host" set "OTEL_COLLECTOR_HOST=%%b"
        if "%%a"=="otel.collector.port" set "OTEL_COLLECTOR_PORT=%%b"
        if "%%a"=="otel.service.name" set "SERVICE_NAME=%%b"
    )
)

:: Valores por defecto si no se encontraron
if not defined OTEL_COLLECTOR_HOST set OTEL_COLLECTOR_HOST=localhost
if not defined OTEL_COLLECTOR_PORT set OTEL_COLLECTOR_PORT=4318
if not defined SERVICE_NAME set SERVICE_NAME=chat-servidor-%COMPUTERNAME%

echo Configuracion cargada desde %CONFIG_FILE%:
echo   - Peer P2P:         %PEER_HOST%:%PEER_PORT%
echo   - Cliente-Servidor: %CLIENTE_HOST%:%CLIENTE_PORT%
echo   - OTel Collector:   http://%OTEL_COLLECTOR_HOST%:%OTEL_COLLECTOR_PORT%
echo   - Service Name:     %SERVICE_NAME%
echo.

:: =============================================================================
:: VERIFICAR AGENTE OPENTELEMETRY
:: =============================================================================
if not exist "otel\opentelemetry-javaagent.jar" (
    echo [ERROR] No se encontro el agente OpenTelemetry.
    echo.
    echo Descargalo desde:
    echo https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases
    echo.
    echo Y colocalo en: %cd%\otel\opentelemetry-javaagent.jar
    echo.
    pause
    exit /b 1
)

:: =============================================================================
:: COMPILAR PROYECTO
:: =============================================================================
echo [1/3] Compilando el servidor...
call mvn clean install -DskipTests -q

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] No se pudo compilar el servidor
    pause
    exit /b 1
)

echo [OK] Compilacion exitosa
echo.

:: =============================================================================
:: VERIFICAR JAR
:: =============================================================================
set JAR_PATH=Presentacion\Main\target\Main-1.0-SNAPSHOT.jar

if not exist "%JAR_PATH%" (
    echo [ERROR] No se encontro el JAR: %JAR_PATH%
    echo.
    echo Intentando buscar JAR alternativo...
    
    for /f "delims=" %%i in ('dir /b /s Presentacion\Main\target\*.jar 2^>nul ^| findstr /v sources ^| findstr /v javadoc') do (
        set JAR_PATH=%%i
        echo Encontrado: %%i
        goto :found_jar
    )
    
    echo No se encontro ningun JAR ejecutable.
    pause
    exit /b 1
)

:found_jar
echo [2/3] JAR encontrado: %JAR_PATH%
echo.

:: =============================================================================
:: INICIAR SERVIDOR CON OPENTELEMETRY
:: =============================================================================
echo [3/3] Iniciando servidor con OpenTelemetry Agent...
echo.
echo ========================================
echo   SERVIDOR INICIADO
echo   P2P escuchando en:     %PEER_HOST%:%PEER_PORT%
echo   Clientes escuchando:   %CLIENTE_HOST%:%CLIENTE_PORT%
echo   Telemetria enviada a:  http://%OTEL_COLLECTOR_HOST%:%OTEL_COLLECTOR_PORT%
echo ========================================
echo.

java -javaagent:otel\opentelemetry-javaagent.jar ^
     -Dotel.service.name=%SERVICE_NAME% ^
     -Dotel.exporter.otlp.endpoint=http://%OTEL_COLLECTOR_HOST%:%OTEL_COLLECTOR_PORT% ^
     -Dotel.exporter.otlp.protocol=http/protobuf ^
     -Dotel.metrics.exporter=otlp ^
     -Dotel.logs.exporter=otlp ^
     -Dotel.traces.exporter=otlp ^
     -Dotel.resource.attributes=service.namespace=chat-unillanos,deployment.environment=development ^
     -Dotel.instrumentation.jdbc.enabled=true ^
     -Dotel.instrumentation.java-util-logging.enabled=true ^
     -jar %JAR_PATH%

pause
