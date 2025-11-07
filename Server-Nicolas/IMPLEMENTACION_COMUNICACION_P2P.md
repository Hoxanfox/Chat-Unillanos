# Implementación de Comunicación P2P Transparente

## Fecha de Implementación
2025-11-07

## Descripción General

Se ha implementado un sistema completo de comunicación transparente entre servidores (peers) en la red P2P del chat. Los clientes no se dan cuenta de que están comunicándose con usuarios de otros servidores, todo el proceso es transparente.

## 🎯 Características Implementadas

### 1. **Notificación de Mensajes entre Peers**
Cuando un usuario en el Servidor A envía un mensaje a un canal que tiene miembros en el Servidor B, el mensaje se retransmite automáticamente al Servidor B para que sus usuarios lo reciban en tiempo real.

### 2. **Invitaciones a Canales Cross-Server**
Un usuario en el Servidor A puede invitar a un usuario del Servidor B a un canal, y la invitación se notifica automáticamente al servidor remoto.

### 3. **Canales Privados (1 a 1) entre Servidores**
Los usuarios pueden chatear privadamente aunque estén en servidores diferentes.

### 4. **Consultas de Información entre Servidores**
Los servidores pueden solicitar información de usuarios y canales de otros servidores.

## 📁 Archivos Creados/Modificados

### Nuevos Archivos

1. **IPeerNotificationService.java** 
   - Ubicación: `negocio/server-LogicaPeers/src/main/java/com/arquitectura/logicaPeers/`
   - Interfaz para el servicio de notificaciones P2P

2. **PeerNotificationServiceImpl.java**
   - Ubicación: `negocio/server-LogicaPeers/src/main/java/com/arquitectura/logicaPeers/`
   - Implementación del servicio de notificaciones P2P
   - Maneja la comunicación TCP entre servidores

3. **P2PNotificationController.java**
   - Ubicación: `transporte/server-controladorTransporte/src/main/java/com/arquitectura/controlador/controllers/`
   - Controlador que maneja las notificaciones P2P entrantes de otros servidores

4. **ApplicationContextProvider.java**
   - Ubicación: `comunes/server-Utils/src/main/java/org/springframework/context/`
   - Helper para acceder al contexto de Spring desde cualquier parte

5. **PeersReportPanel.java**
   - Ubicación: `vista/server-vista/src/main/java/com/arquitectura/vista/`
   - Panel GUI para visualizar peers conectados

### Archivos Modificados

1. **MessageServiceImpl.java**
   - Se agregó inyección de `IPeerNotificationService`
   - Se agregó método `notificarMensajeAPeers()` que detecta usuarios remotos y notifica a sus servidores
   - Los mensajes ahora se retransmiten automáticamente a peers

2. **ServerViewController.java**
   - Se agregaron métodos `obtenerPeersDisponibles()` y `obtenerPeersActivos()`

3. **ServerMainWindow.java**
   - Se agregó botón "Ver Peers" en el GUI
   - Se integró el nuevo panel de visualización de peers

4. **pom.xml de server-LogicaMensajes**
   - Se agregó dependencia a `server-LogicaPeers`

## 🔄 Flujo de Comunicación P2P

### Escenario: Usuario en Servidor A envía mensaje a canal con miembros en Servidor B

```
1. Cliente A → Servidor A: Enviar mensaje
   ↓
2. Servidor A: Guarda mensaje en BD local
   ↓
3. Servidor A: Detecta que el canal tiene miembros en Servidor B
   ↓
4. Servidor A → Servidor B: Notificación P2P (TCP)
   {
     action: "notificarMensaje",
     payload: {
       messageId: "...",
       channelId: "...",
       authorId: "...",
       content: "...",
       ...
     }
   }
   ↓
5. Servidor B: Recibe notificación en P2PNotificationController
   ↓
6. Servidor B: Publica evento local NewMessageEvent
   ↓
7. Servidor B → Clientes B: Mensaje retransmitido en tiempo real
```

## 🛠️ Arquitectura Técnica

### Componentes Principales

```
┌─────────────────────────────────────────────────────────┐
│                     SERVIDOR A                          │
├─────────────────────────────────────────────────────────┤
│  MessageServiceImpl                                     │
│    ├─ enviarMensajeTexto()                             │
│    ├─ getMessageResponseDto()                          │
│    └─ notificarMensajeAPeers() ──────────┐            │
│                                            │            │
│  PeerNotificationService                   │            │
│    ├─ notificarNuevoMensaje() ────────────┼──────┐     │
│    ├─ notificarInvitacionCanal()          │      │     │
│    └─ enviarPeticionAPeer() ──────────────┼──────┼─┐   │
└────────────────────────────────────────────┼──────┼─┼───┘
                                             │      │ │
                                        TCP  │      │ │
                                             ▼      ▼ ▼
┌─────────────────────────────────────────────────────────┐
│                     SERVIDOR B                          │
├─────────────────────────────────────────────────────────┤
│  PeerConnectionManager                                  │
│    └─ ServerSocket (puerto P2P)                        │
│              │                                          │
│              ▼                                          │
│  P2PNotificationController                              │
│    ├─ handleNotificarMensaje()                         │
│    ├─ handleNotificarInvitacionCanal()                 │
│    └─ handleObtenerInfoUsuario()                       │
│              │                                          │
│              ▼                                          │
│  EventPublisher → NewMessageEvent                       │
│              │                                          │
│              ▼                                          │
│  Clientes conectados en Servidor B                     │
└─────────────────────────────────────────────────────────┘
```

### Métodos del Servicio de Notificaciones P2P

```java
// Notificaciones
boolean notificarNuevoMensaje(UUID peerDestinoId, MessageResponseDto mensaje)
boolean notificarInvitacionCanal(UUID peerDestinoId, UUID canalId, UUID usuarioInvitadoId, UUID usuarioInvitadorId)
boolean notificarAceptacionInvitacion(UUID peerDestinoId, UUID canalId, UUID usuarioId)

// Consultas
UserResponseDto solicitarInfoUsuario(UUID peerDestinoId, UUID usuarioId)
ChannelResponseDto solicitarInfoCanal(UUID peerDestinoId, UUID canalId)

// Utilidades
UUID obtenerPeerDeUsuario(UUID usuarioId)
UUID obtenerPeerDeCanal(UUID canalId)
DTOResponse enviarPeticionAPeer(UUID peerDestinoId, DTORequest peticion)
```

## 🔧 Configuración

### Propiedades del Servidor (server.properties)

```properties
# Puerto para clientes
server.port=8080

# Puerto P2P (comunicación entre servidores)
peer.server.port=9090

# Configuración P2P
peer.max.connections=10
peer.heartbeat.interval.ms=30000
peer.heartbeat.timeout.seconds=60
peer.reconnect.attempts=3
peer.reconnect.delay.ms=5000

# Peers de arranque (opcional, separados por coma)
# peer.bootstrap.nodes=192.168.1.100:9090,192.168.1.101:9090
```

### Base de Datos

Las tablas `users` y `channels` ya tienen la columna `servidor_padre` que referencia al peer:

```sql
ALTER TABLE users ADD COLUMN servidor_padre UUID;
ALTER TABLE channels ADD COLUMN servidor_padre UUID;

ALTER TABLE users ADD FOREIGN KEY (servidor_padre) REFERENCES peers(id);
ALTER TABLE channels ADD FOREIGN KEY (servidor_padre) REFERENCES peers(id);
```

## 📊 Panel de Visualización de Peers en GUI

Se agregó un nuevo panel en el GUI del servidor que muestra:
- ID del Peer
- Nombre del Servidor
- IP
- Puerto
- Estado (ONLINE/OFFLINE/DESCONOCIDO)
- Último Latido (Heartbeat)

### Acceso
1. Inicia el servidor
2. Abre el GUI
3. Clic en "Ver Peers" en el menú lateral

## 🚀 Cómo Compilar y Ejecutar

### Compilación

```bash
cd "Server-Nicolas"
mvn clean install -DskipTests
```

### Ejecución

```bash
# Servidor 1 (Puerto 8080 para clientes, 9090 para P2P)
mvn spring-boot:run

# Servidor 2 (en otra terminal, con diferentes puertos)
# Edita config/server.properties para cambiar los puertos
mvn spring-boot:run
```

## 🧪 Pruebas

### Escenario 1: Mensaje entre servidores

1. **Servidor A**: Usuario1 crea un canal "General"
2. **Servidor A**: Usuario1 invita a Usuario2 (del Servidor B)
3. **Servidor B**: Usuario2 acepta la invitación
4. **Servidor A**: Usuario1 envía mensaje "Hola desde Servidor A"
5. **Resultado**: Usuario2 recibe el mensaje en tiempo real

### Escenario 2: Chat privado cross-server

1. **Servidor A**: Usuario1 inicia chat con Usuario2 (Servidor B)
2. **Servidor A**: Usuario1 envía "Mensaje privado"
3. **Resultado**: Usuario2 (Servidor B) recibe el mensaje

### Escenario 3: Visualización de peers

1. Abre GUI del servidor
2. Clic en "Ver Peers"
3. Ver listado de servidores conectados con su estado

## ⚙️ Configuración de Múltiples Servidores

### Servidor 1 (config/server.properties)
```properties
server.port=8080
peer.server.port=9090
peer.nombre.servidor=Servidor Principal
```

### Servidor 2 (config/server.properties)
```properties
server.port=8081
peer.server.port=9091
peer.nombre.servidor=Servidor Secundario
peer.bootstrap.nodes=localhost:9090
```

## 📝 Acciones P2P Soportadas

### Notificaciones (de servidor a servidor)
- `notificarMensaje` - Notifica nuevo mensaje
- `notificarInvitacionCanal` - Notifica invitación a canal
- `notificarAceptacionInvitacion` - Notifica aceptación de invitación

### Consultas (request/response)
- `obtenerInfoUsuario` - Solicita información de un usuario
- `obtenerInfoCanal` - Solicita información de un canal

## 🔐 Seguridad

- Las conexiones P2P usan el mismo protocolo TCP que los clientes
- Cada peer verifica que tiene la información solicitada antes de responder
- Los mensajes solo se retransmiten a peers que tienen usuarios miembros del canal
- Timeout de conexión: 5 segundos

## 🐛 Debug y Logging

Los logs P2P incluyen:
```
→ [MessageService] Notificando mensaje a peer {id} con {n} miembros
✓ [MessageService] Mensaje notificado exitosamente a peer {id}
✗ [MessageService] Fallo al notificar mensaje a peer {id}

→ [PeerNotificationService] Notificando nuevo mensaje al peer {id}
→ [PeerNotificationService] Enviando petición '{action}' al peer {id}
✓ [PeerNotificationService] Respuesta recibida con status: {status}

→ [P2PNotificationController] Procesando acción P2P: {action}
✓ [P2PNotificationController] Mensaje retransmitido a usuarios locales
```

## ⏭️ Próximas Mejoras (TODO)

1. **Sincronización de Invitaciones**: Implementar sincronización completa de invitaciones pendientes
2. **Caché de Peers**: Implementar caché para reducir consultas a BD
3. **Compresión**: Agregar compresión a mensajes P2P grandes
4. **Encriptación**: Implementar TLS para comunicación P2P
5. **Descubrimiento Automático**: Implementar descubrimiento automático de peers en LAN
6. **Balanceo de Carga**: Distribuir carga entre peers disponibles
7. **Persistencia de Mensajes Remotos**: Decidir si guardar mensajes remotos en BD local

## 📚 Referencias

- Documentos relacionados:
  - `EXPLICACION_CONEXION_PEERS.md`
  - `GUIA_USO_POOL_P2P.md`
  - `PEER_IMPLEMENTATION_REVIEW.md`
  - `GUIA_PANEL_PEERS.md`

---

**Nota**: Esta implementación permite que el chat funcione como una red distribuida donde los clientes no necesitan saber en qué servidor están sus contactos. Todo el enrutamiento y retransmisión es transparente y automático.

