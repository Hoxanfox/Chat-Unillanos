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
:: CONFIGURACION DE OPENTELEMETRY
:: =============================================================================
:: Modifica estas variables segun tu entorno

:: IP de la maquina donde corre el stack de observabilidad (Docker)
set OTEL_COLLECTOR_HOST=25.2.20.107

:: Puerto del collector (OTLP HTTP)
set OTEL_COLLECTOR_PORT=4318

:: Nombre unico para este servidor (cambiar si hay multiples peers)
set SERVICE_NAME=chat-servidor-peer2

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
echo Configuracion:
echo   - Collector: http://%OTEL_COLLECTOR_HOST%:%OTEL_COLLECTOR_PORT%
echo   - Servicio:  %SERVICE_NAME%
echo.
echo ========================================
echo   SERVIDOR INICIADO
echo   Logs enviandose a Grafana/Loki
echo   Metricas enviandose a Prometheus
echo   Trazas enviandose a Tempo
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

