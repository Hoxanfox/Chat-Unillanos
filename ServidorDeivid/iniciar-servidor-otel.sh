#!/bin/bash

echo "========================================"
echo "  CHAT-UNILLANOS - SERVIDOR P2P"
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

# Nombre unico para este servidor (cambiar si hay multiples peers)
SERVICE_NAME="${SERVICE_NAME:-chat-servidor-peer1}"

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
echo "[1/3] Compilando el servidor..."
mvn clean install -DskipTests -q

if [ $? -ne 0 ]; then
    echo ""
    echo "[ERROR] No se pudo compilar el servidor"
    exit 1
fi

echo "[OK] Compilacion exitosa"
echo ""

# =============================================================================
# VERIFICAR JAR
# =============================================================================
JAR_PATH="Presentacion/Main/target/Main-1.0-SNAPSHOT.jar"

if [ ! -f "$JAR_PATH" ]; then
    echo "[ERROR] No se encontro el JAR: $JAR_PATH"
    echo ""
    echo "Intentando buscar JAR alternativo..."
    
    JAR_PATH=$(find Presentacion/Main/target -name "*.jar" ! -name "*sources*" ! -name "*javadoc*" | head -1)
    
    if [ -z "$JAR_PATH" ]; then
        echo "No se encontro ningun JAR ejecutable."
        exit 1
    fi
    
    echo "Encontrado: $JAR_PATH"
fi

echo "[2/3] JAR encontrado: $JAR_PATH"
echo ""

# =============================================================================
# INICIAR SERVIDOR CON OPENTELEMETRY
# =============================================================================
echo "[3/3] Iniciando servidor con OpenTelemetry Agent..."
echo ""
echo "Configuracion:"
echo "  - Collector: http://${OTEL_COLLECTOR_HOST}:${OTEL_COLLECTOR_PORT}"
echo "  - Servicio:  ${SERVICE_NAME}"
echo ""
echo "========================================"
echo "  SERVIDOR INICIADO"
echo "  Logs enviandose a Grafana/Loki"
echo "  Metricas enviandose a Prometheus"
echo "  Trazas enviandose a Tempo"
echo "========================================"
echo ""

java -javaagent:otel/opentelemetry-javaagent.jar \
     -Dotel.service.name="${SERVICE_NAME}" \
     -Dotel.exporter.otlp.endpoint="http://${OTEL_COLLECTOR_HOST}:${OTEL_COLLECTOR_PORT}" \
     -Dotel.exporter.otlp.protocol=http/protobuf \
     -Dotel.metrics.exporter=otlp \
     -Dotel.logs.exporter=otlp \
     -Dotel.traces.exporter=otlp \
     -Dotel.resource.attributes="service.namespace=chat-unillanos,deployment.environment=development" \
     -Dotel.instrumentation.jdbc.enabled=true \
     -Dotel.instrumentation.java-util-logging.enabled=true \
     -jar "$JAR_PATH"

