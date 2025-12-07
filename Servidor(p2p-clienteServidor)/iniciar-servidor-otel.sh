#!/bin/bash

echo "========================================"
echo "  CHAT-UNILLANOS - SERVIDOR P2P"
echo "  Con Observabilidad (OpenTelemetry)"
echo "========================================"
echo ""

cd "$(dirname "$0")"

# =============================================================================
# LEER CONFIGURACIÓN DESDE configuracion.txt
# =============================================================================
CONFIG_FILE="configuracion.txt"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "[ERROR] No se encontró el archivo de configuración: $CONFIG_FILE"
    exit 1
fi

# Leer variables del archivo de configuración
while IFS='=' read -r key value; do
    # Ignorar líneas vacías y comentarios
    [[ -z "$key" || "$key" =~ ^# ]] && continue
    # Limpiar espacios
    key=$(echo "$key" | xargs)
    value=$(echo "$value" | xargs)
    
    case "$key" in
        "peer.host") PEER_HOST="$value" ;;
        "peer.puerto") PEER_PORT="$value" ;;
        "cliente.host") CLIENTE_HOST="$value" ;;
        "cliente.puerto") CLIENTE_PORT="$value" ;;
        "otel.collector.host") OTEL_COLLECTOR_HOST="$value" ;;
        "otel.collector.port") OTEL_COLLECTOR_PORT="$value" ;;
        "otel.service.name") SERVICE_NAME="$value" ;;
    esac
done < "$CONFIG_FILE"

# Valores por defecto si no se encontraron
OTEL_COLLECTOR_HOST="${OTEL_COLLECTOR_HOST:-localhost}"
OTEL_COLLECTOR_PORT="${OTEL_COLLECTOR_PORT:-4318}"
SERVICE_NAME="${SERVICE_NAME:-chat-servidor-$(hostname)}"

echo "Configuración cargada desde $CONFIG_FILE:"
echo "  - Peer P2P:         $PEER_HOST:$PEER_PORT"
echo "  - Cliente-Servidor: $CLIENTE_HOST:$CLIENTE_PORT"
echo "  - OTel Collector:   http://$OTEL_COLLECTOR_HOST:$OTEL_COLLECTOR_PORT"
echo "  - Service Name:     $SERVICE_NAME"
echo ""

# =============================================================================
# VERIFICAR AGENTE OPENTELEMETRY
# =============================================================================
if [ ! -f "otel/opentelemetry-javaagent.jar" ]; then
    echo "[ERROR] No se encontró el agente OpenTelemetry."
    echo ""
    echo "Descárgalo desde:"
    echo "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases"
    echo ""
    echo "Y colócalo en: $(pwd)/otel/opentelemetry-javaagent.jar"
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

echo "[OK] Compilación exitosa"
echo ""

# =============================================================================
# VERIFICAR JAR
# =============================================================================
JAR_PATH="Presentacion/Main/target/Main-1.0-SNAPSHOT.jar"

if [ ! -f "$JAR_PATH" ]; then
    echo "[ERROR] No se encontró el JAR: $JAR_PATH"
    echo ""
    echo "Intentando buscar JAR alternativo..."
    
    JAR_PATH=$(find Presentacion/Main/target -name "*.jar" ! -name "*sources*" ! -name "*javadoc*" 2>/dev/null | head -1)
    
    if [ -z "$JAR_PATH" ]; then
        echo "No se encontró ningún JAR ejecutable."
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
echo "========================================"
echo "  SERVIDOR INICIADO"
echo "  P2P escuchando en:     $PEER_HOST:$PEER_PORT"
echo "  Clientes escuchando:   $CLIENTE_HOST:$CLIENTE_PORT"
echo "  Telemetría enviada a:  http://$OTEL_COLLECTOR_HOST:$OTEL_COLLECTOR_PORT"
echo "========================================"
echo ""

java -javaagent:otel/opentelemetry-javaagent.jar \
     -Dotel.service.name="$SERVICE_NAME" \
     -Dotel.exporter.otlp.endpoint="http://$OTEL_COLLECTOR_HOST:$OTEL_COLLECTOR_PORT" \
     -Dotel.exporter.otlp.protocol=http/protobuf \
     -Dotel.metrics.exporter=otlp \
     -Dotel.logs.exporter=otlp \
     -Dotel.traces.exporter=otlp \
     -Dotel.resource.attributes=service.namespace=chat-unillanos,deployment.environment=development \
     -Dotel.instrumentation.jdbc.enabled=true \
     -Dotel.instrumentation.java-util-logging.enabled=true \
     -jar "$JAR_PATH"

