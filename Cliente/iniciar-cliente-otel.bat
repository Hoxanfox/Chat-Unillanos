@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion

echo ========================================
echo   CHAT-UNILLANOS - CLIENTE
echo   Con Observabilidad (OpenTelemetry)
echo ========================================
echo.

cd /d "%~dp0"

:: =============================================================================
:: CONFIGURACION DE OPENTELEMETRY
:: =============================================================================
:: Modifica estas variables segun tu entorno

:: IP de la maquina donde corre el stack de observabilidad (Docker)
set OTEL_COLLECTOR_HOST=localhost

:: Puerto del collector (OTLP HTTP)
set OTEL_COLLECTOR_PORT=4318

:: Nombre unico para este cliente (cambiar para cada cliente)
set SERVICE_NAME=chat-cliente-%COMPUTERNAME%

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
echo [1/2] Compilando el cliente...
call mvn clean install -DskipTests -q

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] No se pudo compilar el cliente
    pause
    exit /b 1
)

echo [OK] Compilacion exitosa
echo.

:: =============================================================================
:: INICIAR CLIENTE CON OPENTELEMETRY (via Maven JavaFX)
:: =============================================================================
echo [2/2] Iniciando cliente JavaFX con OpenTelemetry Agent...
echo.
echo Configuracion:
echo   - Collector: http://%OTEL_COLLECTOR_HOST%:%OTEL_COLLECTOR_PORT%
echo   - Servicio:  %SERVICE_NAME%
echo.
echo ========================================
echo   CLIENTE INICIADO
echo   Logs enviandose a Grafana/Loki
echo   Metricas enviandose a Prometheus
echo   Trazas enviandose a Tempo
echo ========================================
echo.

cd Presentacion\Main

:: Configurar MAVEN_OPTS con el agente OpenTelemetry
set MAVEN_OPTS=-javaagent:..\..\otel\opentelemetry-javaagent.jar ^
    -Dotel.service.name=%SERVICE_NAME% ^
    -Dotel.exporter.otlp.endpoint=http://%OTEL_COLLECTOR_HOST%:%OTEL_COLLECTOR_PORT% ^
    -Dotel.exporter.otlp.protocol=http/protobuf ^
    -Dotel.metrics.exporter=otlp ^
    -Dotel.logs.exporter=otlp ^
    -Dotel.traces.exporter=otlp ^
    -Dotel.resource.attributes=service.namespace=chat-unillanos,deployment.environment=development ^
    -Dotel.instrumentation.jdbc.enabled=true ^
    -Dotel.instrumentation.java-util-logging.enabled=true

call mvn javafx:run

pause

