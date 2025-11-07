# 📚 Diferencias entre Tipos de Canal y Funcionamiento P2P

## Fecha: 2025-11-07

## 🎯 Tipos de Canal en el Sistema

El sistema maneja **3 tipos de canales** definidos en el enum `TipoCanal`:

```java
public enum TipoCanal {
    DIRECTO,    // Canales privados 1-a-1
    GRUPO,      // Canales grupales con múltiples miembros
    BROADCAST   // Canales de difusión del sistema
}
```

---

## 1️⃣ Canal DIRECTO (Privado 1-a-1)

### Características
- **Miembros**: Siempre 2 usuarios (conversación privada)
- **Invitaciones**: ❌ NO se permiten (validado en el código)
- **Creación**: Automática al iniciar chat con otro usuario
- **Nombre**: Automático "Directo: Alice - Bob"

### Creación
```java
// Cliente solicita chat con otro usuario
crearCanalDirecto(user1Id, user2Id)

// El sistema:
1. Busca si ya existe un canal entre estos usuarios
2. Si existe, lo retorna
3. Si no existe, lo crea automáticamente
4. Agrega a ambos usuarios como miembros ACTIVOS
```

### Funcionamiento P2P (AHORA IMPLEMENTADO ✅)

#### Escenario: Alice (Servidor 1) chatea con Bob (Servidor 2)

**PASO 1: Creación del canal**
```
Cliente Alice → "Iniciar chat con Bob"
    ↓
Servidor 1: crearCanalDirecto(alice, bob)
    ↓
ChannelService detecta que Bob está en Servidor 2
    ↓
Lanza FederationRequiredException
    ↓
ChatFachada captura la excepción
    ↓
Crea canal DIRECTO localmente con Alice
    ↓
✅ Canal listo para usar
```

**PASO 2: Primer mensaje**
```
Alice envía: "Hola Bob"
    ↓
Servidor 1 guarda el mensaje
    ↓
MessageService detecta que Bob está en Servidor 2
    ↓
Notifica automáticamente al Servidor 2
    ↓
Servidor 2 → Cliente Bob: Mensaje recibido
    ↓
✅ Bob ve el mensaje y puede responder
```

**PASO 3: Respuesta**
```
Bob responde: "Hola Alice"
    ↓
Servidor 2 guarda el mensaje
    ↓
MessageService detecta que Alice está en Servidor 1
    ↓
Notifica automáticamente al Servidor 1
    ↓
Servidor 1 → Cliente Alice: Mensaje recibido
    ↓
✅ Conversación fluida y transparente
```

### Logs Esperados (Canal DIRECTO Cross-Server)

**Servidor 1 (cuando Alice inicia chat con Bob remoto):**
```
→ [Federation] Verificando ubicación de usuarios...
   User1 (alice-id): LOCAL
   User2 (bob-id): REMOTO
→ [Federation] User2 es remoto. Se requiere federación P2P.
→ [ChatFachada] Canal directo requiere federación P2P
  User1: alice-id
  User2: bob-id
  Peer remoto: servidor-2-id
✓ [ChatFachada] Canal directo cross-server creado: canal-xxx
```

**Cuando Alice envía mensaje:**
```
→ [MessageService] Notificando mensaje a peer servidor-2-id con 1 miembros
✓ [MessageService] Mensaje notificado exitosamente a peer servidor-2-id
```

### Validación de Invitaciones
```java
// En invitarMiembro()
if (channel.getTipo() != TipoCanal.GRUPO) {
    throw new Exception("Solo se pueden enviar invitaciones a canales de tipo GRUPO.");
}
```

**Por qué:** Los canales DIRECTO son solo para 2 personas, no se pueden agregar más miembros.

---

## 2️⃣ Canal GRUPO (Grupal)

### Características
- **Miembros**: Múltiples usuarios (3 o más)
- **Invitaciones**: ✅ SÍ se permiten (solo el owner puede invitar)
- **Creación**: Manual, el usuario crea el grupo
- **Nombre**: Personalizado por el creador

### Creación
```java
// Cliente crea grupo
CreateChannelRequestDto requestDto = new CreateChannelRequestDto("Mi Grupo", "GRUPO");
crearCanal(requestDto, ownerId)

// El sistema:
1. Valida que el tipo sea GRUPO (no DIRECTO)
2. Crea el canal con el nombre especificado
3. Agrega al creador como owner y miembro ACTIVO
```

### Funcionamiento P2P (COMPLETO ✅)

#### Escenario: Alice (Servidor 1) invita a Bob (Servidor 2) a un grupo

**PASO 1: Creación del grupo**
```
Alice crea grupo "Amigos"
    ↓
Servidor 1: crearCanal("Amigos", GRUPO)
    ↓
Canal creado con Alice como owner
    ↓
✅ Grupo listo
```

**PASO 2: Invitación cross-server**
```
Alice invita a Bob al grupo "Amigos"
    ↓
Servidor 1: invitarMiembro(bob)
    ↓
ChannelInvitationP2PService.procesarInvitacion()
    ↓
Detecta que Bob está en Servidor 2
    ↓
Guarda invitación PENDIENTE en BD local
    ↓
Notifica al Servidor 2 vía TCP P2P
    ↓
Servidor 2: Recibe notificación
    ↓
Guarda invitación PENDIENTE en BD
    ↓
Servidor 2 → Cliente Bob: "Tienes invitación a 'Amigos'"
    ↓
✅ Bob ve la invitación
```

**PASO 3: Aceptación**
```
Bob acepta la invitación
    ↓
Servidor 2: responderInvitacion(ACEPTAR)
    ↓
Marca invitación como ACTIVA
    ↓
ChannelInvitationP2PService.procesarAceptacionInvitacion()
    ↓
Notifica aceptación al Servidor 1
    ↓
Servidor 1: Actualiza estado de Bob a ACTIVO
    ↓
✅ Bob ahora es miembro del grupo
```

**PASO 4: Mensajes del grupo**
```
Alice envía: "Bienvenido Bob al grupo"
    ↓
MessageService detecta miembros en múltiples servidores
    ↓
Notifica a Servidor 2 que tiene 1 miembro (Bob)
    ↓
Bob recibe el mensaje
    ↓
✅ Comunicación grupal cross-server funcional
```

### Logs Esperados (Canal GRUPO Cross-Server)

**Servidor 1 (cuando Alice invita a Bob):**
```
→ [ChannelInvitation] Invitación guardada en BD: Bob al canal Amigos
→ [ChannelInvitation] Notificando invitación a peer servidor-2-id
  Canal: Amigos (canal-xxx)
  Usuario invitado: Bob
✓ [ChannelInvitation] Invitación notificada exitosamente
✓ [ChannelService] Invitación procesada: Bob al canal Amigos
```

**Servidor 2 (cuando Bob acepta):**
```
✓ [ChannelService] Invitación aceptada por bob-id
→ [ChannelInvitation] Procesando aceptación de invitación
→ [ChannelInvitation] Notificando aceptación al peer del canal: servidor-1-id
✓ [ChannelInvitation] Aceptación notificada exitosamente
```

---

## 3️⃣ Canal BROADCAST (Difusión del Sistema)

### Características
- **Miembros**: Todos los usuarios del sistema
- **Invitaciones**: ❌ No aplica (todos son miembros automáticamente)
- **Creación**: Automática al iniciar el servidor
- **Uso**: Mensajes administrativos y anuncios del sistema

### Funcionamiento
```java
// Solo el servidor puede enviar mensajes broadcast
enviarMensajeBroadcast("Mantenimiento programado a las 3 AM", adminId)

// El mensaje se envía a TODOS los usuarios conectados
```

### P2P
- No requiere notificaciones P2P específicas
- Cada servidor maneja su propio canal broadcast
- Los mensajes broadcast son locales a cada servidor

---

## 📊 Tabla Comparativa

| Característica | DIRECTO | GRUPO | BROADCAST |
|----------------|---------|-------|-----------|
| Número de miembros | 2 fijo | 3+ variable | Todos |
| Permite invitaciones | ❌ No | ✅ Sí | ❌ No |
| Creación | Automática | Manual | Automática |
| Nombre | Automático | Personalizado | "Broadcast" |
| P2P implementado | ✅ Sí | ✅ Sí | ➖ N/A |
| Notificación cross-server | ✅ Mensajes | ✅ Invitaciones + Mensajes | ➖ Local |

---

## 🔄 Flujo Completo de Comunicación P2P

### Canal DIRECTO (1-a-1)
```
1. Alice inicia chat con Bob
2. Se crea canal DIRECTO automáticamente
3. Los mensajes se retransmiten vía P2P
4. ✅ Conversación privada transparente
```

### Canal GRUPO (Grupal)
```
1. Alice crea grupo "Amigos"
2. Alice invita a Bob (Servidor 2)
3. Invitación se sincroniza vía P2P
4. Bob acepta (notificación P2P de vuelta)
5. Los mensajes del grupo se retransmiten a todos los peers con miembros
6. ✅ Chat grupal cross-server funcional
```

---

## ✅ Estado de Implementación

### Canal DIRECTO
- ✅ Creación cross-server (ChatFachada maneja FederationRequiredException)
- ✅ Mensajes cross-server (MessageService notifica automáticamente)
- ✅ Totalmente funcional y transparente

### Canal GRUPO
- ✅ Invitaciones cross-server (ChannelInvitationP2PService)
- ✅ Aceptación de invitaciones con notificación bidireccional
- ✅ Mensajes grupales cross-server (MessageService)
- ✅ Totalmente funcional y transparente

### Canal BROADCAST
- ✅ Funcional localmente en cada servidor
- ➖ No requiere sincronización P2P

---

## 🎯 Resumen de Cambios Implementados

### 1. ChatFachadaImpl.java (NUEVO)
```java
@Override
public ChannelResponseDto crearCanalDirecto(UUID user1Id, UUID user2Id) throws Exception {
    try {
        return channelService.crearCanalDirecto(user1Id, user2Id);
    } catch (FederationRequiredException e) {
        // Maneja canales directos cross-server
        Channel channel = channelService.obtenerOCrearCanalDirecto(user1Id, user2Id);
        return mapChannelToDto(channel);
    }
}
```

### 2. ChannelServiceImpl.java (YA IMPLEMENTADO)
```java
@Override
public void invitarMiembro(...) {
    // Validación de tipo de canal
    if (channel.getTipo() != TipoCanal.GRUPO) {
        throw new Exception("Solo GRUPO permite invitaciones");
    }
    
    // Usa servicio P2P para invitaciones cross-server
    channelInvitationP2PService.procesarInvitacion(...);
}
```

### 3. MessageServiceImpl.java (YA IMPLEMENTADO)
```java
private void notificarMensajeAPeers(...) {
    // Detecta miembros en otros servidores
    // Notifica automáticamente a esos peers
    // Funciona para DIRECTO y GRUPO
}
```

---

## 🧪 Cómo Probar Cada Tipo

### Probar Canal DIRECTO Cross-Server
```
1. Servidor 1: Usuario Alice
2. Servidor 2: Usuario Bob
3. Alice → "Iniciar chat con Bob"
4. Alice envía: "Hola Bob"
5. ✅ Bob recibe mensaje en tiempo real
6. Bob responde: "Hola Alice"
7. ✅ Alice recibe respuesta
```

### Probar Canal GRUPO Cross-Server
```
1. Servidor 1: Usuario Alice
2. Servidor 2: Usuario Bob
3. Alice crea grupo "TestGrupo"
4. Alice invita a Bob
5. ✅ Bob ve invitación pendiente
6. Bob acepta invitación
7. ✅ Bob es miembro del grupo
8. Alice envía mensaje al grupo
9. ✅ Bob recibe mensaje
10. Bob responde en el grupo
11. ✅ Alice recibe respuesta
```

---

## 🎉 Conclusión

**Ahora el sistema soporta completamente:**

1. ✅ **Canales DIRECTO cross-server**: Conversaciones privadas transparentes entre usuarios de diferentes servidores
2. ✅ **Canales GRUPO cross-server**: Grupos con miembros distribuidos en múltiples servidores
3. ✅ **Mensajes en tiempo real**: Retransmisión automática a peers relevantes
4. ✅ **Invitaciones sincronizadas**: Sistema bidireccional de invitaciones entre servidores

**Todo es completamente transparente para los clientes** - ellos no saben ni les importa en qué servidor están sus contactos.

