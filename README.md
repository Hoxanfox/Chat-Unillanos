# Chat-Unillanos

Chat-Unillanos es una plataforma de comunicación distribuida que implementa una arquitectura híbrida Cliente-Servidor y P2P (Peer-to-Peer). El sistema incluye capacidades avanzadas de observabilidad, un dashboard web de monitoreo y un cliente de escritorio.

## 🏗️ Arquitectura del Proyecto

El proyecto está dividido en los siguientes componentes principales:

### 1. Servidor (`Servidor(p2p-clienteServidor)`)
El núcleo del sistema. Implementa la lógica de negocio, la comunicación P2P entre nodos y la gestión de clientes.
- **Tecnología:** Java 17, Maven.
- **Características:** Arquitectura en capas (Infraestructura, Negocio, Persistencia, Presentación), integración con OpenTelemetry.

### 2. Cliente de Escritorio (`Cliente_Escritorio`)
La interfaz principal para los usuarios finales.
- **Tecnología:** Java 17, JavaFX, Maven.
- **Características:** Interfaz gráfica moderna, comunicación en tiempo real, soporte multiplataforma (Windows, Linux, Mac).

### 3. API Gateway (`ApiGateway`)
Un servicio intermediario que expone datos de la red P2P y logs a la aplicación web.
- **Tecnología:** Go, Gin .
- **Características:** Recolección de logs vía SSE, endpoints REST para estado de la red, Dockerizado.

### 4. Dashboard Web (`App_Web`)
Panel de control para visualizar el estado de la red y los logs.
- **Tecnología:** React, Vite.
- **Características:** Gráficos en tiempo real, visualización de logs, monitoreo de salud de los nodos.

### 5. Módulo de Observabilidad (`modulo-observability`)
Stack completo de monitoreo para trazas, métricas y logs.
- **Tecnología:** Grafana, Prometheus, Loki, Tempo, OpenTelemetry.
- **Documentación:** Ver [OBSERVABILIDAD.md](./OBSERVABILIDAD.md) para detalles completos.

---

## � Arquitectura API REST

El sistema utiliza un **API Gateway** en Go para unificar el acceso a la red P2P. A continuación se explica el flujo de comunicación y descubrimiento:

### 1. Descubrimiento de Nodos (Service Discovery)
El API Gateway no conoce inicialmente a todos los nodos. Utiliza un mecanismo de "Seed Peer" para descubrir la red.

1.  **Seed Peer:** El Gateway se configura con la URL de un nodo semilla (por defecto `http://host.docker.internal:7000`).
2.  **Consulta de Topología:** El Gateway hace una petición `GET /api/network/peers` al nodo semilla.
3.  **Respuesta:** El servidor (vía `NetworkRestController`) retorna la lista de todos los peers conocidos y su estado (ONLINE/OFFLINE).
4.  **Actualización:** El Gateway actualiza su lista interna de nodos activos para enrutar las siguientes peticiones.

### 2. Comunicación Gateway -> Servidores
Una vez descubiertos los nodos, el Gateway se comunica con ellos mediante endpoints REST y SSE:

-   **Logs en Tiempo Real (SSE):** El Gateway se suscribe al endpoint `/api/logs/stream` de cada nodo activo. Esto permite recibir logs en vivo sin necesidad de hacer polling constante.
-   **Agregación de Datos:** Para endpoints como `/gateway/logs` o `/gateway/network`, el Gateway consulta a todos los nodos activos en paralelo, agrega las respuestas y las devuelve al cliente web como una sola estructura unificada.
-   **Manejo de Docker:** El Gateway detecta si un peer reporta su IP como `localhost` y la traduce automáticamente a `host.docker.internal` para garantizar la conectividad dentro de la red de contenedores.

---

## �📋 Requisitos Previos

- **Java JDK 17+**
- **Docker** y **Docker Compose**
- **Go** (para desarrollo local del Gateway)
- **Node.js** y **npm** (para el Dashboard Web)
- **Maven**

---

## 🚀 Guía de Inicio Rápido

### 1. Iniciar el Stack de Observabilidad
Es recomendable iniciar primero el sistema de monitoreo.

```bash
cd modulo-observability
docker-compose up -d
```
Esto levantará Grafana en `http://localhost:3000` (admin/admin).

### 2. Iniciar el Servidor (Nodo P2P)
El servidor requiere una base de datos MySQL. Puedes usar el `docker-compose.yml` dentro de la carpeta del servidor o asegurarte de tener una instancia MySQL corriendo.

**Configuración:** Revisa `Servidor(p2p-clienteServidor)/configuracion.txt` y `init.sql`.

**Ejecución con Observabilidad:**
```bash
cd "Servidor(p2p-clienteServidor)"
# Windows
.\iniciar-servidor-otel.bat
# Linux/Mac
./iniciar-servidor-otel.sh
```

### 3. Iniciar el Cliente de Escritorio
```bash
cd Cliente_Escritorio
# Windows
.\iniciar-cliente-otel.bat
# Linux/Mac
./iniciar-cliente-otel.sh
```

### 4. Iniciar API Gateway y Dashboard Web
El archivo `docker-compose.yml` del API Gateway levanta tanto el servicio de gateway como el dashboard web.

```bash
cd ApiGateway
docker-compose up -d --build
```

- **API Gateway:** Puerto `8080`
- **Dashboard Web:** `http://localhost:3001`

### 5. Desarrollo Local del Dashboard (Opcional)
Si deseas ejecutar el dashboard fuera de Docker para desarrollo:

```bash
cd App_Web
npm install
npm run dev
```
Por defecto en desarrollo correrá en el puerto `5173`.

---

## 🔧 Configuración Adicional

### Variables de Entorno
Cada componente soporta configuración mediante archivos `.env` o variables de entorno del sistema. Revisa los archivos `docker-compose.yml` de cada módulo para ver las variables disponibles.

### Base de Datos
El servidor y el cliente pueden requerir inicialización de base de datos. Los scripts `init.sql` se encuentran en sus respectivas carpetas raíz.

---

## 📚 Documentación
- **Observabilidad:** [OBSERVABILIDAD.md](./OBSERVABILIDAD.md)
- **Configuración Vosk (Voz):** `Servidor(p2p-clienteServidor)/CONFIGURACION_VOSK.md`
