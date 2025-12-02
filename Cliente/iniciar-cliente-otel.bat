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
:: LEER CONFIGURACIÓN DESDE configuracion.txt
:: =============================================================================
set CONFIG_FILE=configuracion.txt

if not exist "%CONFIG_FILE%" (
    echo [ERROR] No se encontro el archivo de configuracion: %CONFIG_FILE%
    echo Creando archivo de configuracion por defecto...
    (
        echo ip=127.0.0.1
        echo port=8000
        echo otel.collector.host=localhost
        echo otel.collector.port=4318
        echo otel.service.name=chat-cliente
    ) > %CONFIG_FILE%
    echo [OK] Archivo creado. Por favor, edita %CONFIG_FILE% con tus valores.
    pause
    exit /b 1
)

:: Leer variables del archivo de configuración
for /f "usebackq tokens=1,* delims==" %%a in ("%CONFIG_FILE%") do (
    set "line=%%a"
    :: Ignorar líneas que empiezan con # (comentarios)
    if not "!line:~0,1!"=="#" (
        if "%%a"=="ip" set "SERVER_IP=%%b"
        if "%%a"=="port" set "SERVER_PORT=%%b"
        if "%%a"=="otel.collector.host" set "OTEL_COLLECTOR_HOST=%%b"
        if "%%a"=="otel.collector.port" set "OTEL_COLLECTOR_PORT=%%b"
        if "%%a"=="otel.service.name" set "SERVICE_NAME=%%b"
    )
)

:: Valores por defecto si no se encontraron
if not defined OTEL_COLLECTOR_HOST set OTEL_COLLECTOR_HOST=localhost
if not defined OTEL_COLLECTOR_PORT set OTEL_COLLECTOR_PORT=4318
if not defined SERVICE_NAME set SERVICE_NAME=chat-cliente-%COMPUTERNAME%

echo Configuracion cargada desde %CONFIG_FILE%:
echo   - Servidor destino: %SERVER_IP%:%SERVER_PORT%
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
:: VERIFICAR/COMPILAR JAR
:: =============================================================================
set JAR_PATH=Presentacion\Main\target\Main-1.0-SNAPSHOT-jar-with-dependencies.jar

if not exist "%JAR_PATH%" (
    echo [1/2] JAR no encontrado. Compilando el cliente...
    call mvn clean install -DskipTests -q
    
    if %errorlevel% neq 0 (
        echo.
        echo [ERROR] No se pudo compilar el cliente
        pause
        exit /b 1
    )
    echo [OK] Compilacion exitosa
) else (
    echo [1/2] JAR encontrado: %JAR_PATH%
)
echo.

:: =============================================================================
:: COPIAR CONFIGURACION AL DIRECTORIO DE EJECUCIÓN
:: =============================================================================
:: El cliente busca configuracion.txt en el directorio de trabajo actual
if not exist "Presentacion\Main\configuracion.txt" (
    copy "%CONFIG_FILE%" "Presentacion\Main\configuracion.txt" >nul
    echo [INFO] Archivo de configuracion copiado a Presentacion\Main\
)

:: Sincronizar si el archivo raíz es más reciente
for %%A in ("%CONFIG_FILE%") do set "CONFIG_DATE=%%~tA"
for %%B in ("Presentacion\Main\configuracion.txt") do set "DEST_DATE=%%~tB"
if not "%CONFIG_DATE%"=="%DEST_DATE%" (
    copy "%CONFIG_FILE%" "Presentacion\Main\configuracion.txt" >nul
    echo [INFO] Archivo de configuracion actualizado en Presentacion\Main\
)

:: =============================================================================
:: INICIAR CLIENTE CON OPENTELEMETRY
:: =============================================================================
echo [2/2] Iniciando cliente con OpenTelemetry Agent...
echo.
echo ========================================
echo   CLIENTE INICIADO
echo   Conectando a: %SERVER_IP%:%SERVER_PORT%
echo   Telemetria:   http://%OTEL_COLLECTOR_HOST%:%OTEL_COLLECTOR_PORT%
echo ========================================
echo.

cd Presentacion\Main

java -javaagent:..\..\otel\opentelemetry-javaagent.jar ^
     -Dotel.service.name=%SERVICE_NAME% ^
     -Dotel.exporter.otlp.endpoint=http://%OTEL_COLLECTOR_HOST%:%OTEL_COLLECTOR_PORT% ^
     -Dotel.exporter.otlp.protocol=http/protobuf ^
     -Dotel.metrics.exporter=otlp ^
     -Dotel.logs.exporter=otlp ^
     -Dotel.traces.exporter=otlp ^
     -Dotel.resource.attributes=service.namespace=chat-unillanos,deployment.environment=development ^
     -Dotel.instrumentation.jdbc.enabled=true ^
     -Dotel.instrumentation.java-util-logging.enabled=true ^
     -jar target\Main-1.0-SNAPSHOT-jar-with-dependencies.jar

pause
