# Observabilidad - Chat Unillanos

Sistema de monitoreo completo para la red P2P usando **Grafana**, **Prometheus**, **Loki** y **Tempo**.

## Arquitectura

```
                    ┌─────────────────────────────────────┐
                    │         GRAFANA (:3000)             │
                    │    ┌───────┴───────┐                │
                    │    ▼       ▼       ▼                │
                    │ Prometheus Loki  Tempo              │
                    │  (:9090)  (:3100) (:3200)           │
                    │    ▲       ▲       ▲                │
                    │    └───────┼───────┘                │
                    │            ▼                        │
                    │   OpenTelemetry Collector           │
                    │        (:4317/:4318)                │
                    └────────────┬────────────────────────┘
                                 │
          ┌──────────────────────┼──────────────────────┐
          ▼                      ▼                      ▼
   ┌──────────────┐      ┌──────────────┐      ┌──────────────┐
   │ SERVIDOR P2P │      │ SERVIDOR P2P │      │   CLIENTE    │
   │   + OTel     │◄────►│   + OTel     │      │   + OTel     │
   │    Agent     │      │    Agent     │      │    Agent     │
   └──────────────┘      └──────────────┘      └──────────────┘
```

---

## Requisitos Previos

- **Docker** y **Docker Compose**
- **Java JDK 17+**
- **Maven**

---

## 1. Descargar OpenTelemetry Java Agent

El agente de OpenTelemetry se adjunta a la JVM y automáticamente instrumenta la aplicación sin modificar código.

### Windows (PowerShell)

```powershell
# Crear directorios
New-Item -ItemType Directory -Force -Path "ServidorDeivid\otel"
New-Item -ItemType Directory -Force -Path "Cliente\otel"

# Descargar el agente (versión 2.1.0)
$url = "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.1.0/opentelemetry-javaagent.jar"

Invoke-WebRequest -Uri $url -OutFile "opentelemetry-javaagent.jar"

# Copiar a las carpetas correspondientes
Copy-Item "opentelemetry-javaagent.jar" -Destination "ServidorDeivid\otel\"
Copy-Item "opentelemetry-javaagent.jar" -Destination "Cliente\otel\"

# Verificar
Get-ChildItem "ServidorDeivid\otel\*.jar"
Get-ChildItem "Cliente\otel\*.jar"

# Limpiar archivo temporal
Remove-Item "opentelemetry-javaagent.jar"
```

**Comando en una sola línea:**
```powershell
$url="https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.1.0/opentelemetry-javaagent.jar"; New-Item -ItemType Directory -Force -Path "ServidorDeivid\otel","Cliente\otel"; Invoke-WebRequest -Uri $url -OutFile "ServidorDeivid\otel\opentelemetry-javaagent.jar"; Copy-Item "ServidorDeivid\otel\opentelemetry-javaagent.jar" -Destination "Cliente\otel\"
```

### Linux / macOS (Bash)

```bash
# Crear directorios
mkdir -p ServidorDeivid/otel Cliente/otel

# Descargar el agente (versión 2.1.0)
curl -L -o opentelemetry-javaagent.jar \
  https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.1.0/opentelemetry-javaagent.jar

# Copiar a las carpetas correspondientes
cp opentelemetry-javaagent.jar ServidorDeivid/otel/
cp opentelemetry-javaagent.jar Cliente/otel/

# Verificar
ls -la ServidorDeivid/otel/*.jar
ls -la Cliente/otel/*.jar

# Limpiar archivo temporal
rm opentelemetry-javaagent.jar
```

**Comando en una sola línea:**
```bash
mkdir -p ServidorDeivid/otel Cliente/otel && curl -L -o ServidorDeivid/otel/opentelemetry-javaagent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.1.0/opentelemetry-javaagent.jar && cp ServidorDeivid/otel/opentelemetry-javaagent.jar Cliente/otel/
```

---

## 2. Levantar el Stack de Observabilidad

```bash
cd observability
docker-compose up -d
```

Verificar que todo esté corriendo:
```bash
docker-compose ps
```

### Servicios disponibles:

| Servicio | URL | Descripción |
|----------|-----|-------------|
| Grafana | http://localhost:3000 | Dashboards (admin/admin) |
| Prometheus | http://localhost:9090 | Métricas |
| Loki | http://localhost:3100 | Logs |
| Tempo | http://localhost:3200 | Trazas |

---

## 3. Iniciar Servidor con Observabilidad

### Windows
```powershell
cd ServidorDeivid
.\iniciar-servidor-otel.bat
```

### Linux/macOS
```bash
cd ServidorDeivid
chmod +x iniciar-servidor-otel.sh
./iniciar-servidor-otel.sh
```

### Múltiples Peers P2P

Para simular una red P2P con múltiples servidores, cada peer debe tener un `SERVICE_NAME` único:

**Peer 1:**
```powershell
set SERVICE_NAME=chat-servidor-peer1
.\iniciar-servidor-otel.bat
```

**Peer 2:**
```powershell
set SERVICE_NAME=chat-servidor-peer2
.\iniciar-servidor-otel.bat
```

**Peer 3:**
```powershell
set SERVICE_NAME=chat-servidor-peer3
.\iniciar-servidor-otel.bat
```

---

## 4. Iniciar Cliente con Observabilidad

### Windows
```powershell
cd Cliente
.\iniciar-cliente-otel.bat
```

### Linux/macOS
```bash
cd Cliente
chmod +x iniciar-cliente-otel.sh
./iniciar-cliente-otel.sh
```

---

## 5. Configuración

### Archivo de configuración del Servidor (`ServidorDeivid/configuracion.txt`)

```properties
# Observabilidad
otel.collector.host=localhost
otel.collector.port=4318
otel.service.name=chat-servidor-peer1
```

### Archivo de configuración del Cliente (`Cliente/configuracion.txt`)

```properties
# Observabilidad
otel.collector.host=localhost
otel.collector.port=4318
otel.service.name=chat-cliente
```

### Variables de entorno (opcional)

| Variable | Descripción | Default |
|----------|-------------|---------|
| `OTEL_COLLECTOR_HOST` | IP del collector | localhost |
| `OTEL_COLLECTOR_PORT` | Puerto OTLP HTTP | 4318 |
| `SERVICE_NAME` | Nombre único del servicio | chat-servidor-peer1 |

---

## 6. Dashboards en Grafana

Accede a http://localhost:3000 (usuario: `admin`, contraseña: `admin`)

Los dashboards están en la carpeta **Chat-Unillanos**:

### Vista General
- Servicios activos en la red
- Memoria Heap JVM por servicio
- Threads activos
- Uso de CPU
- Garbage Collection

### Logs
- Logs en tiempo real de toda la red
- Filtrado por errores y warnings
- Búsqueda de excepciones

### Trazas
- Estadísticas del collector
- Exploración de trazas en Tempo
- Queries a base de datos

---

## 7. Detener el Stack

```bash
cd observability
docker-compose down
```

Para eliminar todos los datos:
```bash
docker-compose down -v
```

---

## Estructura de Archivos

```
Chat-Unillanos/
├── observability/
│   ├── docker-compose.yml
│   ├── otel-collector-config.yaml
│   ├── prometheus.yml
│   ├── loki-config.yaml
│   ├── tempo-config.yaml
│   ├── README.md
│   └── grafana/
│       └── provisioning/
│           ├── datasources/
│           │   └── datasources.yaml
│           └── dashboards/
│               ├── dashboards.yaml
│               ├── chat-unillanos-overview.json
│               ├── chat-unillanos-logs.json
│               └── chat-unillanos-traces.json
├── ServidorDeivid/
│   ├── otel/
│   │   └── opentelemetry-javaagent.jar  (descargar)
│   ├── iniciar-servidor-otel.bat
│   ├── iniciar-servidor-otel.sh
│   └── configuracion.txt
├── Cliente/
│   ├── otel/
│   │   └── opentelemetry-javaagent.jar  (descargar)
│   ├── iniciar-cliente-otel.bat
│   ├── iniciar-cliente-otel.sh
│   └── configuracion.txt
└── OBSERVABILIDAD.md  (este archivo)
```

---

## Troubleshooting

### No aparecen datos en Grafana
1. Verifica que el servidor esté corriendo con el script `*-otel.bat/sh`
2. Revisa los logs del collector: `docker-compose logs otel-collector`
3. Verifica métricas en Prometheus: http://localhost:9090

### Error "No se encontró el agente OpenTelemetry"
Ejecuta los comandos de la sección 1 para descargar el JAR.

### Múltiples peers muestran como uno solo
Cada peer debe tener un `SERVICE_NAME` único. Usa la variable de entorno antes de iniciar.

---

## Referencias

- [OpenTelemetry Java Agent](https://github.com/open-telemetry/opentelemetry-java-instrumentation)
- [Grafana](https://grafana.com/docs/)
- [Prometheus](https://prometheus.io/docs/)
- [Loki](https://grafana.com/docs/loki/)
- [Tempo](https://grafana.com/docs/tempo/)

