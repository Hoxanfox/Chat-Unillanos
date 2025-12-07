# Observabilidad - Chat Unillanos

Stack de observabilidad completo para monitorear toda la red P2P del chat, incluyendo servidores y clientes.

## Componentes

| Servicio | Puerto | Descripcion |
|----------|--------|-------------|
| **Grafana** | 3000 | Dashboards y visualizacion |
| **Prometheus** | 9090 | Almacenamiento de metricas |
| **Loki** | 3100 | Almacenamiento de logs |
| **Tempo** | 3200 | Almacenamiento de trazas |
| **OTel Collector** | 4317/4318 | Receptor central de telemetria |

## Arquitectura

```
                    ┌─────────────────────────────────────┐
                    │         GRAFANA (:3000)             │
                    │    ┌───────┴───────┐                │
                    │    ▼       ▼       ▼                │
                    │ Prometheus Loki  Tempo              │
                    │    ▲       ▲       ▲                │
                    │    └───────┼───────┘                │
                    │            ▼                        │
                    │   OpenTelemetry Collector           │
                    └────────────┬────────────────────────┘
                                 │
          ┌──────────────────────┼──────────────────────┐
          ▼                      ▼                      ▼
   ┌──────────────┐      ┌──────────────┐      ┌──────────────┐
   │ SERVIDOR P2P │      │ SERVIDOR P2P │      │   CLIENTE    │
   │   + OTel     │      │   + OTel     │      │   + OTel     │
   │    Agent     │      │    Agent     │      │    Agent     │
   └──────────────┘      └──────────────┘      └──────────────┘
```

## Inicio Rapido

### 1. Levantar Stack de Observabilidad

```bash
cd observability
docker-compose up -d
```

### 2. Verificar que todo esta corriendo

```bash
docker-compose ps
```

### 3. Descargar OpenTelemetry Java Agent

Descarga la ultima version desde:
https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases

Busca el archivo `opentelemetry-javaagent.jar` y colocalo en:
- `ServidorDeivid/otel/opentelemetry-javaagent.jar`
- `Cliente/otel/opentelemetry-javaagent.jar`

**Comando rapido (Linux/Mac):**
```bash
# Descargar version 2.1.0 (ajustar segun ultima version)
wget https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.1.0/opentelemetry-javaagent.jar

# Copiar a ambos directorios
cp opentelemetry-javaagent.jar ../ServidorDeivid/otel/
cp opentelemetry-javaagent.jar ../Cliente/otel/
```

**Comando rapido (Windows PowerShell):**
```powershell
# Descargar version 2.1.0 (ajustar segun ultima version)
Invoke-WebRequest -Uri "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.1.0/opentelemetry-javaagent.jar" -OutFile "opentelemetry-javaagent.jar"

# Copiar a ambos directorios
Copy-Item opentelemetry-javaagent.jar ..\ServidorDeivid\otel\
Copy-Item opentelemetry-javaagent.jar ..\Cliente\otel\
```

### 4. Configurar IPs

Edita los archivos de configuracion segun tu red:

**Para el Servidor (`ServidorDeivid/configuracion.txt`):**
```properties
otel.collector.host=IP_MAQUINA_CON_DOCKER
otel.collector.port=4318
otel.service.name=chat-servidor-peer1
```

**Para el Cliente (`Cliente/configuracion.txt`):**
```properties
otel.collector.host=IP_MAQUINA_CON_DOCKER
otel.collector.port=4318
otel.service.name=chat-cliente
```

### 5. Iniciar Servidor con Observabilidad

```bash
cd ServidorDeivid
./iniciar-servidor-otel.sh   # Linux/Mac
# o
iniciar-servidor-otel.bat    # Windows
```

### 6. Iniciar Cliente con Observabilidad

```bash
cd Cliente
./iniciar-cliente-otel.sh    # Linux/Mac
# o
iniciar-cliente-otel.bat     # Windows
```

### 7. Acceder a Grafana

Abre en tu navegador: http://localhost:3000

- **Usuario:** admin
- **Password:** admin

## Dashboards Disponibles

### Chat Unillanos - Vista General
- Servicios activos en la red
- Servidores P2P y clientes conectados
- Metricas JVM (memoria, threads)
- Conexiones a base de datos

### Chat Unillanos - Logs
- Logs en tiempo real de toda la red
- Filtrado por nivel (ERROR, WARN, INFO, DEBUG)
- Logs separados por servidores y clientes

### Chat Unillanos - Trazas
- Trazas distribuidas
- Latencia por servicio (P50, P95, P99)
- Operaciones lentas (> 500ms)
- Trazas con errores

## Variables de Entorno

Los scripts de inicio usan estas variables (opcionales):

| Variable | Descripcion | Default |
|----------|-------------|---------|
| `OTEL_COLLECTOR_HOST` | IP del collector | localhost |
| `OTEL_COLLECTOR_PORT` | Puerto del collector | 4318 |
| `SERVICE_NAME` | Nombre del servicio | chat-servidor-peer1 / chat-cliente-hostname |

**Ejemplo de uso:**
```bash
# Linux/Mac
export OTEL_COLLECTOR_HOST=192.168.1.100
export SERVICE_NAME=chat-servidor-peer2
./iniciar-servidor-otel.sh

# Windows
set OTEL_COLLECTOR_HOST=192.168.1.100
set SERVICE_NAME=chat-servidor-peer2
iniciar-servidor-otel.bat
```

## Multiples Peers P2P

Para monitorear multiples servidores P2P, cada uno debe tener un `SERVICE_NAME` unico:

- Peer 1: `chat-servidor-peer1`
- Peer 2: `chat-servidor-peer2`
- Peer 3: `chat-servidor-peer3`

Esto permite identificar cada nodo en los dashboards.

## Troubleshooting

### El collector no recibe datos
1. Verifica que el collector esta corriendo: `docker-compose ps`
2. Verifica conectividad: `curl http://localhost:4318/v1/traces`
3. Revisa logs del collector: `docker-compose logs otel-collector`

### No aparecen metricas en Grafana
1. Espera 1-2 minutos (Prometheus scrape interval)
2. Verifica que Prometheus recibe datos: http://localhost:9090/targets
3. En Grafana, verifica el datasource: Settings > Data Sources > Prometheus > Test

### No aparecen logs en Loki
1. Verifica que Loki esta corriendo: `docker-compose logs loki`
2. En Grafana, verifica el datasource: Settings > Data Sources > Loki > Test

### Errores de memoria
Si el agente consume mucha memoria, ajusta los parametros:
```bash
java -javaagent:otel/opentelemetry-javaagent.jar \
     -Dotel.javaagent.experimental.span.max=1000 \
     -Dotel.bsp.max.queue.size=2048 \
     ...
```

## Detener Stack

```bash
cd observability
docker-compose down
```

Para eliminar todos los datos persistentes:
```bash
docker-compose down -v
```

## Puertos Requeridos

Asegurate de que estos puertos esten disponibles y accesibles:

| Puerto | Servicio | Direccion |
|--------|----------|-----------|
| 3000 | Grafana | Entrante (navegador) |
| 4317 | OTel Collector gRPC | Entrante (apps) |
| 4318 | OTel Collector HTTP | Entrante (apps) |
| 9090 | Prometheus | Interno |
| 3100 | Loki | Interno |
| 3200 | Tempo | Interno |

