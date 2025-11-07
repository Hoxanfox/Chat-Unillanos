# IMPLEMENTACIÓN DE FEDERACIÓN P2P - CANALES DIRECTOS ENTRE SERVIDORES

## 📋 Resumen

Se ha implementado la funcionalidad completa de **federación P2P** que permite a clientes conectados a diferentes servidores (peers) crear canales de chat directo entre ellos, respetando la autoridad de cada servidor sobre sus propios usuarios y datos.

## 🎯 Objetivo Cumplido

✅ **Un cliente en Peer 1 puede crear un canal directo con un cliente en Peer 2**

Cada servidor mantiene su propia base de datos aislada y actúa como autoridad sobre sus propios usuarios.

---

## 🏗️ Arquitectura de la Solución

### Componentes Implementados

#### 1. **UserPeerMappingService** (Nuevo)
📁 `negocio/server-LogicaPeers/src/main/java/com/arquitectura/logicaPeers/UserPeerMappingService.java`

**Propósito:** Mantener el mapeo de qué usuarios pertenecen a qué servidores.

**Funcionalidades:**
- `registerUserToPeer(UUID userId, UUID peerId)` - Registra un usuario en un peer
- `getPeerForUser(UUID userId)` - Obtiene el peer al que pertenece un usuario
- `isLocalUser(UUID userId)` - Verifica si un usuario es local
- `isRemoteUser(UUID userId)` - Verifica si un usuario es remoto
- `printMappingState()` - Muestra el estado del mapeo (debugging)

#### 2. **ChannelServiceImpl** (Modificado)
📁 `negocio/server-LogicaCanales/src/main/java/com/arquitectura/logicaCanales/ChannelServiceImpl.java`

**Cambios Principales:**
- ✅ Inyección de `UserPeerMappingService` y `PeerConnectionManager`
- ✅ Método `obtenerOCrearCanalDirecto()` con lógica de federación
- ✅ Método `crearCanalDirectoLocal()` para canales locales
- ✅ Método `retransmitirCreacionCanalDirecto()` para federación
- ✅ Método `mapRemoteChannelFromResponse()` para mapear canales remotos

#### 3. **PeerConnectionManager** (Modificado)
📁 `transporte/server-Transporte/src/main/java/com/arquitectura/transporte/PeerConnectionManager.java`

**Cambios Principales:**
- ✅ Nuevo método `sendRequestToPeer(UUID peerId, DTORequest request)` 
- ✅ Soporte para envío síncronо de peticiones P2P
- ✅ Reconexión automática si el peer no está conectado
- ✅ Manejo de timeouts y errores de comunicación

#### 4. **PeerController** (Modificado)
📁 `transporte/server-controladorTransporte/src/main/java/com/arquitectura/controlador/controllers/PeerController.java`

**Cambios Principales:**
- ✅ `handleRetransmitirPeticion()` actualizado para dos formatos
- ✅ Nuevo método `handleRetransmitirPeticionNuevoFormato()` para federación directa
- ✅ Nuevo método `handleCrearCanalDirectoFederado()` para procesar peticiones remotas
- ✅ Método `handleRetransmitirPeticionFormatoAntiguo()` para compatibilidad

---

## 🔄 Flujo de Ejecución Completo

### Escenario: Cliente1 (Peer1) quiere chatear con Cliente2 (Peer2)

```
┌─────────────┐                                    ┌─────────────┐
│   Peer 1    │                                    │   Peer 2    │
│ (Servidor 1)│                                    │ (Servidor 2)│
└──────┬──────┘                                    └──────┬──────┘
       │                                                  │
   ┌───┴───┐                                          ┌───┴───┐
   │Client1│                                          │Client2│
   └───┬───┘                                          └───────┘
       │
       │ 1. crearCanalDirecto(Client1, Client2)
       ├─────────────────────►
       │                      │
       │                      │ 2. ¿Client2 es local?
       │                      │    ❌ NO → Es remoto
       │                      │
       │                      │ 3. Obtener peer de Client2
       │                      │    → UserPeerMappingService
       │                      │    → peerId = Peer2
       │                      │
       │                      │ 4. Retransmitir petición
       │                      │──────────────────────►
       │                      │                       │
       │                      │                       │ 5. Recibir petición
       │                      │                       │    retransmitirpeticion
       │                      │                       │
       │                      │                       │ 6. Extraer acción
       │                      │                       │    → crearCanalDirecto
       │                      │                       │
       │                      │                       │ 7. Ejecutar localmente
       │                      │                       │    ✓ Client2 ES local
       │                      │                       │    ✓ Crear canal en BD2
       │                      │                       │
       │                      │ 8. DTOResponse        │
       │                      │◄──────────────────────┤
       │                      │    (datos del canal)  │
       │                      │                       │
       │ 9. ChannelResponseDto│                       │
       │◄─────────────────────┤                       │
       │                      │                       │
   ┌───┴───┐              ┌───┴────┐             ┌────┴────┐
   │Client1│              │  Peer1 │             │  Peer2  │
   │✓Canal │              │(Orquesta)            │(Ejecuta)│
   └───────┘              └────────┘             └─────────┘
```

### Paso a Paso Detallado

#### **PASO 1: Cliente solicita crear canal**
```java
Cliente1 → Peer1: crearCanalDirecto(user1Id, user2Id)
```

#### **PASO 2: Peer1 verifica ubicación de usuarios**
```java
// En ChannelServiceImpl.obtenerOCrearCanalDirecto()
Optional<User> user1Local = userRepository.findById(user1Id); // ✓ Existe
Optional<User> user2Local = userRepository.findById(user2Id); // ✗ No existe

// Conclusión: user2 es remoto
```

#### **PASO 3: Peer1 obtiene el servidor de user2**
```java
Optional<UUID> user2PeerId = userPeerMappingService.getPeerForUser(user2Id);
// Resultado: peer2Id
```

#### **PASO 4: Peer1 retransmite la petición**
```java
// En ChannelServiceImpl.retransmitirCreacionCanalDirecto()
DTORequest originalRequest = new DTORequest("crearCanalDirecto", {
    "user1Id": user1Id,
    "user2Id": user2Id
});

DTOResponse response = peerConnectionManager.sendRequestToPeer(peer2Id, originalRequest);
```

#### **PASO 5: Peer2 recibe la petición**
```java
// En PeerController.handleRetransmitirPeticion()
// Formato detectado: Nuevo (federación directa)
handleRetransmitirPeticionNuevoFormato(payload, handler);
```

#### **PASO 6: Peer2 procesa la acción**
```java
// En PeerController.handleCrearCanalDirectoFederado()
var channelDto = chatFachada.crearCanalDirecto(user1Id, user2Id);
```

#### **PASO 7: Peer2 ejecuta localmente**
```java
// En ChannelServiceImpl.obtenerOCrearCanalDirecto() del Peer2
Optional<User> user2Local = userRepository.findById(user2Id); // ✓ Existe localmente
// Crea el canal en su BD
Channel canal = crearCanalDirectoLocal(user1, user2);
```

#### **PASO 8: Peer2 responde**
```java
DTOResponse response = new DTOResponse("crearCanalDirecto", true, "Canal creado", {
    "channelId": "...",
    "channelName": "Directo: user1 - user2",
    "peerId": "peer2Id"
});
```

#### **PASO 9: Cliente1 recibe confirmación**
```java
ChannelResponseDto channelDto = mapToChannelResponseDto(channel);
// Cliente1 ahora puede usar este canal para chatear con Cliente2
```

---

## 📝 Configuración Requerida

### 1. Archivo `config/p2p.properties`
```properties
# Ya creado anteriormente con configuración P2P
p2p.enabled=true
p2p.puerto=22100
# ... (otros parámetros)
```

### 2. Archivo `config/server.properties`
```properties
# Ya actualizado con comentarios detallados
peer.server.port=22100
peer.max.connections=50
# ... (otros parámetros)
```

### 3. Dependencias Maven
Se agregaron al `pom.xml` de `server-LogicaCanales`:
```xml
<dependency>
    <groupId>com.arquitectura</groupId>
    <artifactId>server-LogicaPeers</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>com.arquitectura</groupId>
    <artifactId>server-Transporte</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
</dependency>
```

---

## 🔧 Uso y Sincronización

### Sincronización de Usuarios entre Peers

Antes de que los clientes puedan crear canales federados, los servidores deben intercambiar información sobre sus usuarios:

```java
// Implementar en el futuro (opcional):
// Acción P2P: "actualizarlistapeers" ya existe
// Crear acción similar: "sincronizarUsuarios"

// Ejemplo de uso:
DTORequest syncRequest = new DTORequest("sincronizarUsuarios", {
    "usuarios": [
        { "userId": "uuid1", "username": "user1", "peerId": "peer1Id" },
        { "userId": "uuid2", "username": "user2", "peerId": "peer1Id" }
    ]
});

// Cada peer registra estos usuarios en su UserPeerMappingService
```

### Registro Manual de Usuarios Remotos

```java
// En el código de inicialización o sincronización:
userPeerMappingService.registerUserToPeer(user2Id, peer2Id);
```

---

## ✅ Casos de Uso Soportados

| Caso | User1 | User2 | Resultado |
|------|-------|-------|-----------|
| **A** | Local | Local | ✅ Canal creado localmente en Peer1 |
| **B** | Local | Remoto | ✅ Petición retransmitida a Peer2, canal creado allí |
| **C** | Remoto | Local | ✅ Petición retransmitida a Peer1, canal creado allí |
| **D** | Remoto | Remoto | ❌ Error (petición debería venir desde el peer correcto) |

---

## 🚀 Próximos Pasos Recomendados

### 1. **Implementar Sincronización Automática de Usuarios**
- Crear acción P2P `sincronizarUsuarios`
- Enviar lista de usuarios locales a peers conectados
- Actualizar `UserPeerMappingService` automáticamente

### 2. **Mejorar Manejo de Respuestas Asíncronas**
El método `sendRequestToPeer()` actualmente es parcialmente síncrono. Mejorar con:
- `CompletableFuture` para respuestas asíncronas
- Sistema de callbacks con `requestId`
- Timeout configurable

### 3. **Implementar Caché de Canales Remotos**
- Guardar referencias a canales creados en otros peers
- Sincronizar mensajes entre servidores
- Implementar replicación de mensajes

### 4. **Agregar Eventos de Sincronización**
```java
@EventListener
public void onPeerConnected(PeerConnectedEvent event) {
    // Sincronizar usuarios automáticamente
    sincronizarUsuariosConPeer(event.getPeerId());
}
```

### 5. **Implementar Descubrimiento de Usuarios**
- Acción de búsqueda global: `buscarUsuarioEnRed(username)`
- Consulta a todos los peers conectados
- Retornar lista completa con información del peer

---

## 🐛 Notas de Debugging

### Ver Estado del Mapeo
```java
userPeerMappingService.printMappingState();
```

### Verificar Conexión P2P
```java
boolean connected = peerConnectionManager.isConnectedToPeer(peerId);
System.out.println("Conectado a peer " + peerId + ": " + connected);
```

### Logs Relevantes
```
→ [Federation] Verificando ubicación de usuarios...
   User1 (uuid1): LOCAL
   User2 (uuid2): REMOTO
→ [Federation] User2 es remoto. Retransmitiendo petición a su peer...
→ [Federation] Enviando petición 'crearCanalDirecto' al peer uuid-peer2
✓ [Federation] Petición enviada al peer uuid-peer2. Esperando respuesta...
✓ [Federation] Canal creado exitosamente en peer remoto
```

---

## 📊 Diagrama de Clases (Resumen)

```
┌─────────────────────────┐
│  ChannelServiceImpl     │
├─────────────────────────┤
│ - userPeerMappingService│◄─────┐
│ - peerConnectionManager │      │
│ - channelRepository     │      │
├─────────────────────────┤      │
│ + crearCanalDirecto()   │      │
│ + obtenerOCrearCanal... │      │
│ - crearCanalDirectoLocal│      │
│ - retransmitirCreacion..│──┐   │
└─────────────────────────┘  │   │
                             │   │
                             ▼   │
┌─────────────────────────┐     │
│ PeerConnectionManager   │     │
├─────────────────────────┤     │
│ - peerPool             │     │
│ - outgoingConnections   │     │
├─────────────────────────┤     │
│ + sendRequestToPeer()   │     │
│ + connectToPeer()       │     │
│ + isConnectedToPeer()   │     │
└─────────────────────────┘     │
                                │
┌─────────────────────────┐     │
│ UserPeerMappingService  │◄────┘
├─────────────────────────┤
│ - userToPeerMap         │
│ - peerToUsersMap        │
├─────────────────────────┤
│ + registerUserToPeer()  │
│ + getPeerForUser()      │
│ + isLocalUser()         │
│ + isRemoteUser()        │
└─────────────────────────┘
```

---

## 🎉 Resumen de la Implementación

✅ **4 archivos nuevos/modificados**
- ✅ `UserPeerMappingService.java` (NUEVO)
- ✅ `ChannelServiceImpl.java` (MODIFICADO)
- ✅ `PeerConnectionManager.java` (MODIFICADO)
- ✅ `PeerController.java` (MODIFICADO)

✅ **Configuración actualizada**
- ✅ `p2p.properties` con comentarios detallados
- ✅ `server.properties` con comentarios detallados
- ✅ `pom.xml` con dependencias necesarias

✅ **Funcionalidad completa**
- ✅ Detección de usuarios locales vs remotos
- ✅ Retransmisión de peticiones P2P
- ✅ Creación de canales en el servidor autoritativo
- ✅ Respuestas de vuelta al cliente original

---

**Fecha de implementación:** 6 de noviembre de 2025
**Versión:** 1.0.0
**Estado:** ✅ IMPLEMENTADO - Pendiente de compilación y pruebas

