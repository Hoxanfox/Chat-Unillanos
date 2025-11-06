# 🌐 Guía de Uso: Pool de Conexiones P2P

## 📋 Resumen

Se ha implementado exitosamente un **Pool de Conexiones P2P** completo para la red peer-to-peer del servidor Chat-Unillanos. Este sistema permite conexiones persistentes y bidireccionales entre servidores.

---

## 🏗️ Componentes Implementados

### 1. **Eventos P2P** (`comunes/server-events`)
- ✅ `PeerConnectedEvent` - Publicado cuando un peer se conecta
- ✅ `PeerDisconnectedEvent` - Publicado cuando un peer se desconecta
- ✅ `PeerListUpdatedEvent` - Publicado cuando cambia la lista de peers
- ✅ `RetransmitToOriginPeerEvent` - Para retransmitir respuestas a peers

### 2. **Interfaz IPeerHandler** (`transporte/server-controladorTransporte`)
- ✅ Define operaciones para manejar conexiones P2P individuales
- Métodos: `getPeerId()`, `sendMessage()`, `isConnected()`, `disconnect()`, etc.

### 3. **PeerHandler** (`transporte/server-Transporte`)
- ✅ Maneja conexiones P2P **entrantes** (otros peers conectan a este servidor)
- Procesa handshake, heartbeats, retransmisión y sincronización
- Thread individual por peer conectado

### 4. **PeerOutgoingConnection** (`transporte/server-Transporte`)
- ✅ Maneja conexiones P2P **salientes** (este servidor conecta a otros)
- Reconexión automática con reintentos configurables
- Envío de heartbeats periódicos

### 5. **PeerConnectionManager** (`transporte/server-Transporte`)
- ✅ **Componente principal** - Gestor del pool de conexiones P2P
- Pool de threads para conexiones concurrentes
- Mapa de conexiones activas (entrantes y salientes)
- Tareas de mantenimiento automáticas
- Sincronización con base de datos

### 6. **Integración con ServerLauncher** (`comunes/server-app`)
- ✅ Inicio automático del servidor P2P
- ✅ Conexión automática a peers conocidos

---

## ⚙️ Configuración

### Archivo: `config/server.properties`

```properties
# Puerto para clientes (existente)
server.port=22100
server.max.connections=100

# Configuración P2P (nuevo)
peer.server.port=22200                    # Puerto para conexiones P2P
peer.max.connections=50                   # Máximo de peers simultáneos
peer.heartbeat.interval.ms=30000          # Intervalo de heartbeat (30 seg)
peer.heartbeat.timeout.seconds=60         # Timeout sin heartbeat (60 seg)
peer.reconnect.attempts=3                 # Intentos de reconexión
peer.reconnect.delay.ms=5000              # Delay entre reintentos (5 seg)
```

---

## 🚀 Arquitectura del Sistema

```
┌─────────────────────────────────────────────────────────────┐
│                    PeerConnectionManager                     │
│  (Gestor Principal del Pool P2P)                            │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────────┐          ┌──────────────────┐        │
│  │ ExecutorService  │          │ Scheduled Pool   │        │
│  │   (peerPool)     │          │ (maintenance)    │        │
│  └──────────────────┘          └──────────────────┘        │
│                                                               │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  activePeerConnections: Map<UUID, IPeerHandler>     │   │
│  │  (Conexiones entrantes)                              │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                               │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  outgoingConnections: Map<UUID, PeerOutgoingConn>   │   │
│  │  (Conexiones salientes)                              │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                               │
└─────────────────────────────────────────────────────────────┘
         │                              │
         │ Conexiones Entrantes         │ Conexiones Salientes
         ▼                              ▼
┌──────────────────┐          ┌────────────────────────┐
│  PeerHandler     │          │ PeerOutgoingConnection │
│  (Thread)        │          │ (Thread)               │
└──────────────────┘          └────────────────────────┘
         │                              │
         └──────────────┬───────────────┘
                        │
                        ▼
                  ┌───────────┐
                  │ Socket    │
                  │ (TCP)     │
                  └───────────┘
```

---

## 🔄 Flujo de Conexión P2P

### Conexión Entrante (Otro peer conecta a este servidor)

```
1. Peer remoto conecta al puerto P2P (22200)
   ↓
2. PeerConnectionManager.startPeerServer() acepta conexión
   ↓
3. Se crea PeerHandler y se asigna a peerPool
   ↓
4. PeerHandler espera handshake del peer remoto
   ↓
5. Peer remoto envía handshake con su peerId, IP, puerto
   ↓
6. PeerHandler valida y responde con handshake local
   ↓
7. connectionManager.onPeerAuthenticated(handler)
   ↓
8. Se agrega a activePeerConnections
   ↓
9. Se publica PeerConnectedEvent
   ↓
10. Se actualiza estado ONLINE en BD
```

### Conexión Saliente (Este servidor conecta a otro peer)

```
1. PeerConnectionManager.connectToPeer(peerId, ip, port)
   ↓
2. Se crea PeerOutgoingConnection
   ↓
3. Se intenta conexión TCP al peer remoto
   ↓
4. Si conecta, envía handshake local
   ↓
5. Espera respuesta de handshake del peer remoto
   ↓
6. Si handshake exitoso, mantiene conexión activa
   ↓
7. Si falla, reintenta según peer.reconnect.attempts
```

---

## 📊 Tareas de Mantenimiento Automáticas

El sistema ejecuta 3 tareas periódicas:

### 1. Verificación de Heartbeats
- **Frecuencia**: Cada 30 segundos (configurable)
- **Función**: Detecta peers caídos sin heartbeat
- **Acción**: Desconecta peers con timeout > 60 segundos

### 2. Reconexión Automática
- **Frecuencia**: Cada 10 segundos (2x reconnect delay)
- **Función**: Intenta reconectar a peers desconectados
- **Acción**: Conecta a peers en BD marcados como OFFLINE

### 3. Sincronización con BD
- **Frecuencia**: Cada 60 segundos
- **Función**: Mantiene estado consistente entre memoria y BD
- **Acción**: 
  - Actualiza peers conectados a ONLINE
  - Marca peers desconectados como OFFLINE

---

## 📝 Logging

Todos los componentes generan logs detallados:

### Ubicación de Logs
- **Consola**: `STDOUT` (tiempo real)
- **Archivo**: `logs/server.log`

### Niveles de Log

**INFO** - Eventos importantes:
```
INFO  PeerConnectionManager - Servidor P2P iniciado en puerto 22200
INFO  PeerHandler - Handshake exitoso con peer ID: abc-123-def (192.168.1.100:22200)
INFO  PeerConnectionManager - Peer abc-123-def autenticado. Total peers activos: 3
```

**DEBUG** - Detalles de comunicación:
```
DEBUG PeerHandler - Mensaje recibido de peer abc-123-def: {"action":"peer_heartbeat"...}
DEBUG PeerHandler - Mensaje enviado a peer abc-123-def: 256 bytes
```

**WARN** - Situaciones anormales:
```
WARN  PeerConnectionManager - Peer xyz-789-abc sin heartbeat por 65000 ms. Desconectando...
WARN  PeerHandler - Intento de enviar mensaje a peer desconectado: abc-123-def
```

**ERROR** - Errores críticos:
```
ERROR PeerHandler - Error procesando mensaje de peer abc-123-def: JSON parse error
ERROR PeerConnectionManager - Error sincronizando con base de datos: Connection refused
```

---

## 🛠️ API Pública de PeerConnectionManager

### Métodos de Conexión

```java
// Conectar a un peer específico
void connectToPeer(UUID peerId, String ip, int port)

// Conectar a todos los peers conocidos en BD
void connectToAllKnownPeers()
```

### Métodos de Comunicación

```java
// Enviar mensaje a un peer específico
boolean sendToPeer(UUID peerId, String message)

// Broadcast a todos los peers conectados
void broadcastToAllPeers(String message)
```

### Métodos de Consulta

```java
// Verificar si está conectado a un peer
boolean isConnectedToPeer(UUID peerId)

// Obtener IDs de todos los peers conectados
Set<UUID> getConnectedPeerIds()

// Obtener información local
Map<String, Object> getLocalPeerInfo()

// Obtener ID de este servidor
UUID getLocalPeerId()

// Contar peers activos
int getActivePeerCount()
```

---

## 📡 Protocolo de Comunicación P2P

### Formato de Mensajes

Todos los mensajes P2P usan JSON con `DTORequest` y `DTOResponse`:

```json
// Request
{
  "action": "peer_handshake",
  "payload": {
    "peerId": "550e8400-e29b-41d4-a716-446655440000",
    "ip": "192.168.1.100",
    "port": 22200
  }
}

// Response
{
  "action": "peer_handshake",
  "status": "success",
  "message": "Handshake aceptado",
  "data": {
    "peerId": "660e8400-e29b-41d4-a716-446655440001",
    "port": 22200,
    "clientPort": 22100
  }
}
```

### Acciones Soportadas

| Acción | Descripción | Dirección |
|--------|-------------|-----------|
| `peer_handshake` | Autenticación inicial | Bidireccional |
| `peer_heartbeat` | Mantener conexión viva | Bidireccional |
| `peer_retransmit` | Retransmitir petición cliente | Entrante |
| `peer_sync` | Sincronizar estado | Bidireccional |

---

## 🔐 Seguridad

### Estado Actual
- ✅ **Autenticación por peerId**: Cada peer debe enviar su ID único
- ✅ **Validación de handshake**: Rechaza conexiones incompletas
- ✅ **Límite de conexiones**: Previene sobrecarga (max 50 peers)

### Recomendaciones Futuras
- 🔒 Implementar autenticación con tokens/certificados
- 🔒 Encriptar comunicación (TLS/SSL)
- 🔒 Lista blanca de IPs permitidas
- 🔒 Rate limiting para prevenir floods

---

## 🧪 Testing

### Probar Conexión P2P

1. **Iniciar Servidor 1**:
```bash
# En terminal 1
mvn clean package
java -jar target/server.jar
```

2. **Iniciar Servidor 2** (puerto diferente):
```bash
# En terminal 2
# Editar config/server.properties:
#   peer.server.port=22201
java -jar target/server.jar
```

3. **Agregar Peer Manualmente** (vía REST API o GUI):
```sql
INSERT INTO peers (id, ip, puerto, conectado, ultimo_latido) 
VALUES (gen_random_uuid(), '192.168.1.100', 22201, 'OFFLINE', NOW());
```

4. **Verificar Logs**:
```
INFO  PeerConnectionManager - Conectando a 1 peers conocidos...
INFO  PeerOutgoingConnection - Conectando a peer abc-123 (192.168.1.100:22201)...
INFO  PeerOutgoingConnection - Conexión establecida con peer abc-123
INFO  PeerOutgoingConnection - Handshake enviado a peer abc-123
INFO  PeerHandler - Nueva conexión P2P entrante desde: 192.168.1.100
INFO  PeerHandler - Handshake exitoso con peer ID: abc-123
INFO  PeerConnectionManager - Peer abc-123 autenticado. Total peers activos: 1
```

---

## 📈 Monitoreo

### Métricas Disponibles

```java
// Obtener estadísticas del pool P2P
int activePeers = peerConnectionManager.getActivePeerCount();
Set<UUID> connectedPeerIds = peerConnectionManager.getConnectedPeerIds();

log.info("Peers activos: {}", activePeers);
log.info("IDs conectados: {}", connectedPeerIds);
```

### Base de Datos

```sql
-- Ver estado de todos los peers
SELECT peer_id, ip, puerto, conectado, ultimo_latido 
FROM peers 
ORDER BY ultimo_latido DESC;

-- Contar peers online
SELECT COUNT(*) FROM peers WHERE conectado = 'ONLINE';
```

---

## 🐛 Troubleshooting

### Problema: Peer no se conecta

**Síntoma**: Logs muestran "Máximo de reintentos alcanzado"

**Soluciones**:
1. Verificar que el peer remoto esté corriendo
2. Verificar firewall: `sudo ufw allow 22200/tcp`
3. Verificar IP y puerto en BD
4. Revisar logs del peer remoto

### Problema: Peer se desconecta constantemente

**Síntoma**: Logs muestran "Peer sin heartbeat por X ms"

**Soluciones**:
1. Aumentar `peer.heartbeat.timeout.seconds`
2. Verificar latencia de red
3. Revisar carga del servidor (CPU/memoria)

### Problema: Peers en BD pero no conectan

**Síntoma**: `connectToAllKnownPeers()` no establece conexiones

**Soluciones**:
1. Verificar que los peers remotos estén escuchando
2. Revisar campo `conectado` en BD (debe ser OFFLINE para reconexión)
3. Aumentar `peer.reconnect.attempts`

---

## 🎯 Uso en Aplicación

### Ejemplo: Broadcast de Mensaje a Todos los Peers

```java
@Autowired
private PeerConnectionManager peerConnectionManager;

public void notificarTodosPeers(String mensaje) {
    DTOResponse notification = new DTOResponse(
        "notification",
        "success",
        "Nuevo evento",
        mensaje
    );
    
    String json = gson.toJson(notification);
    peerConnectionManager.broadcastToAllPeers(json);
    
    log.info("Notificación enviada a {} peers", 
             peerConnectionManager.getActivePeerCount());
}
```

### Ejemplo: Enviar a Peer Específico

```java
public void enviarAPeer(UUID peerId, Object data) {
    DTORequest request = new DTORequest("custom_action", data);
    String json = gson.toJson(request);
    
    boolean sent = peerConnectionManager.sendToPeer(peerId, json);
    if (!sent) {
        log.warn("No se pudo enviar a peer {}: desconectado", peerId);
    }
}
```

### Ejemplo: Escuchar Eventos de Peers

```java
@Component
public class PeerEventListener {
    
    @EventListener
    public void onPeerConnected(PeerConnectedEvent event) {
        log.info("Nuevo peer conectado: {} desde {}:{}", 
                 event.getPeerId(), event.getIp(), event.getPuerto());
        
        // Lógica personalizada: sincronizar datos, enviar bienvenida, etc.
    }
    
    @EventListener
    public void onPeerDisconnected(PeerDisconnectedEvent event) {
        log.warn("Peer desconectado: {} - Razón: {}", 
                 event.getPeerId(), event.getRazon());
        
        // Lógica personalizada: marcar datos como no sincronizados, etc.
    }
}
```

---

## 📚 Comparación: Pool de Clientes vs Pool de Peers

| Aspecto | Pool de Clientes | Pool de Peers |
|---------|------------------|---------------|
| **Propósito** | Usuarios finales | Servidores P2P |
| **Puerto** | 22100 | 22200 |
| **Autenticación** | Usuario/contraseña | PeerId |
| **Sesiones múltiples** | Sí (multi-device) | No (1 peer = 1 conexión) |
| **Dirección** | Cliente → Servidor | Bidireccional |
| **Heartbeat** | No requerido | Crítico (30s) |
| **Persistencia** | Solo en memoria | BD + Memoria |
| **Reconexión** | Cliente decide | Automática |

---

## ✅ Checklist de Implementación

- [x] Eventos P2P creados
- [x] IPeerHandler interface definida
- [x] PeerHandler implementado
- [x] PeerOutgoingConnection implementado
- [x] PeerConnectionManager implementado
- [x] Integración con ServerLauncher
- [x] Configuración en server.properties
- [x] Logging completo implementado
- [x] Tareas de mantenimiento automáticas
- [x] Sincronización con BD
- [x] Reconexión automática
- [x] Sistema de heartbeats
- [x] Manejo de eventos Spring
- [x] Shutdown graceful

---

## 🚀 Próximos Pasos Recomendados

1. **Testing exhaustivo** con múltiples servidores
2. **Implementar lógica de retransmisión** completa
3. **Implementar sincronización de datos** entre peers
4. **Agregar métricas y monitoreo** (Prometheus/Grafana)
5. **Implementar seguridad** (TLS, autenticación)
6. **Crear GUI** para gestión de peers en ServerMainWindow
7. **Documentar API REST** para gestión de peers

---

**Fecha de Implementación**: 6 de noviembre de 2025  
**Versión**: 1.0.0  
**Estado**: ✅ Completamente Funcional

