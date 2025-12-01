#!/bin/bash

echo "========================================"
echo "  CHAT-UNILLANOS - CLIENTE"
echo "  Con Observabilidad (OpenTelemetry)"
echo "========================================"
echo ""

cd "$(dirname "$0")"

# =============================================================================
# CONFIGURACION DE OPENTELEMETRY
# =============================================================================
# Modifica estas variables segun tu entorno

# IP de la maquina donde corre el stack de observabilidad (Docker)
OTEL_COLLECTOR_HOST="${OTEL_COLLECTOR_HOST:-localhost}"

# Puerto del collector (OTLP HTTP)
OTEL_COLLECTOR_PORT="${OTEL_COLLECTOR_PORT:-4318}"

# Nombre unico para este cliente (cambiar para cada cliente)
SERVICE_NAME="${SERVICE_NAME:-chat-cliente-$(hostname)}"

# =============================================================================
# VERIFICAR AGENTE OPENTELEMETRY
# =============================================================================
if [ ! -f "otel/opentelemetry-javaagent.jar" ]; then
    echo "[ERROR] No se encontro el agente OpenTelemetry."
    echo ""
    echo "Descargalo desde:"
    echo "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases"
    echo ""
    echo "Y colocalo en: $(pwd)/otel/opentelemetry-javaagent.jar"
    echo ""
    exit 1
fi

# =============================================================================
# COMPILAR PROYECTO
# =============================================================================
echo "[1/2] Compilando el cliente..."
mvn clean install -DskipTests -q

if [ $? -ne 0 ]; then
    echo ""
    echo "[ERROR] No se pudo compilar el cliente"
    exit 1
fi

echo "[OK] Compilacion exitosa"
echo ""

# =============================================================================
# INICIAR CLIENTE CON OPENTELEMETRY (via Maven JavaFX)
# =============================================================================
echo "[2/2] Iniciando cliente JavaFX con OpenTelemetry Agent..."
echo ""
echo "Configuracion:"
echo "  - Collector: http://${OTEL_COLLECTOR_HOST}:${OTEL_COLLECTOR_PORT}"
echo "  - Servicio:  ${SERVICE_NAME}"
echo ""
echo "========================================"
echo "  CLIENTE INICIADO"
echo "  Logs enviandose a Grafana/Loki"
echo "  Metricas enviandose a Prometheus"
echo "  Trazas enviandose a Tempo"
echo "========================================"
echo ""

cd Presentacion/Main

# Configurar MAVEN_OPTS con el agente OpenTelemetry
export MAVEN_OPTS="-javaagent:../../otel/opentelemetry-javaagent.jar \
    -Dotel.service.name=${SERVICE_NAME} \
    -Dotel.exporter.otlp.endpoint=http://${OTEL_COLLECTOR_HOST}:${OTEL_COLLECTOR_PORT} \
    -Dotel.exporter.otlp.protocol=http/protobuf \
    -Dotel.metrics.exporter=otlp \
    -Dotel.logs.exporter=otlp \
    -Dotel.traces.exporter=otlp \
    -Dotel.resource.attributes=service.namespace=chat-unillanos,deployment.environment=development \
    -Dotel.instrumentation.jdbc.enabled=true \
    -Dotel.instrumentation.java-util-logging.enabled=true"

mvn javafx:run

