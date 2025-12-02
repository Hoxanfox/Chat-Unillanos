#!/bin/bash

echo "========================================"
echo "  CHAT-UNILLANOS - CLIENTE"
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
    echo "Creando archivo de configuración por defecto..."
    cat > "$CONFIG_FILE" << EOF
ip=127.0.0.1
port=8000
otel.collector.host=localhost
otel.collector.port=4318
otel.service.name=chat-cliente
EOF
    echo "[OK] Archivo creado. Por favor, edita $CONFIG_FILE con tus valores."
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
        "ip") SERVER_IP="$value" ;;
        "port") SERVER_PORT="$value" ;;
        "otel.collector.host") OTEL_COLLECTOR_HOST="$value" ;;
        "otel.collector.port") OTEL_COLLECTOR_PORT="$value" ;;
        "otel.service.name") SERVICE_NAME="$value" ;;
    esac
done < "$CONFIG_FILE"

# Valores por defecto si no se encontraron
OTEL_COLLECTOR_HOST="${OTEL_COLLECTOR_HOST:-localhost}"
OTEL_COLLECTOR_PORT="${OTEL_COLLECTOR_PORT:-4318}"
SERVICE_NAME="${SERVICE_NAME:-chat-cliente-$(hostname)}"

echo "Configuración cargada desde $CONFIG_FILE:"
echo "  - Servidor destino: $SERVER_IP:$SERVER_PORT"
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
# VERIFICAR/COMPILAR JAR
# =============================================================================
JAR_PATH="target/Cliente-1.0-SNAPSHOT-jar-with-dependencies.jar"

if [ ! -f "$JAR_PATH" ]; then
    echo "[1/2] JAR no encontrado. Compilando el cliente..."
    mvn clean install -DskipTests -q
    
    if [ $? -ne 0 ]; then
        echo ""
        echo "[ERROR] No se pudo compilar el cliente"
        exit 1
    fi
    echo "[OK] Compilación exitosa"
else
    echo "[1/2] JAR encontrado: $JAR_PATH"
fi
echo ""

# =============================================================================
# COPIAR CONFIGURACIÓN AL DIRECTORIO DE EJECUCIÓN
# =============================================================================
if [ ! -f "Presentacion/Main/configuracion.txt" ]; then
    cp "$CONFIG_FILE" "Presentacion/Main/configuracion.txt"
    echo "[INFO] Archivo de configuración copiado a Presentacion/Main/"
fi

# Sincronizar si el archivo raíz es más reciente
if [ "$CONFIG_FILE" -nt "Presentacion/Main/configuracion.txt" ]; then
    cp "$CONFIG_FILE" "Presentacion/Main/configuracion.txt"
    echo "[INFO] Archivo de configuración actualizado en Presentacion/Main/"
fi

# =============================================================================
# INICIAR CLIENTE CON OPENTELEMETRY
# =============================================================================
echo "[2/2] Iniciando cliente con OpenTelemetry Agent..."
echo ""
echo "========================================"
echo "  CLIENTE INICIADO"
echo "  Conectando a: $SERVER_IP:$SERVER_PORT"
echo "  Telemetría:   http://$OTEL_COLLECTOR_HOST:$OTEL_COLLECTOR_PORT"
echo "========================================"
echo ""

cd Presentacion/Main

java -javaagent:../../otel/opentelemetry-javaagent.jar \
     -Dotel.service.name="$SERVICE_NAME" \
     -Dotel.exporter.otlp.endpoint="http://$OTEL_COLLECTOR_HOST:$OTEL_COLLECTOR_PORT" \
     -Dotel.exporter.otlp.protocol=http/protobuf \
     -Dotel.metrics.exporter=otlp \
     -Dotel.logs.exporter=otlp \
     -Dotel.traces.exporter=otlp \
     -Dotel.resource.attributes=service.namespace=chat-unillanos,deployment.environment=development \
     -Dotel.instrumentation.jdbc.enabled=true \
     -Dotel.instrumentation.java-util-logging.enabled=true \
     -jar ../../target/Cliente-1.0-SNAPSHOT-jar-with-dependencies.jar

