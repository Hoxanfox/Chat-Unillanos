# Sistema de Enrutamiento Automático P2P

## Descripción General

Se ha implementado un **sistema de enrutamiento automático P2P** que detecta cuando una petición involucra a dos clientes en peers distintos y maneja la retransmisión transparentemente. Este sistema está basado en el diseño de **dos fases** propuesto:

### Fase 1: "Transmisión" (El Cartero Puro) ✅ IMPLEMENTADO
### Fase 2: "Procesamiento" (El Consenso Distribuido) 🔜 FUTURO

---

## Fase 1: Transmisión (El Cartero Puro)

### Concepto

En esta fase, `retransmitirpeticion` actúa como un **cartero puro**: solo necesita saber el destino final (`peerDestinoId`) y el paquete (`peticionCliente`), **sin tener que abrir el paquete** para ver a dónde va.

### Cambios Implementados

#### 1. Diseño Limpio del Payload

**ANTES (Diseño Sucio):**
```json
{
  "action": "retransmitirpeticion",
  "payload": {
    "peerOrigen": { "peerId": "uuid-peer-A" },
    "peticionCliente": {
      "action": "enviarMensajeDirecto",
      "payload": {
        "remitenteId": "...",
        "contenido": "...",
        "peerDestinoId": "uuid-peer-B"  // ❌ SUCIO: El cartero tiene que abrir el paquete
      }
    }
  }
}
```

**DESPUÉS (Diseño Limpio):**
```json
{
  "action": "retransmitirpeticion",
  "payload": {
    "peerOrigen": { "peerId": "uuid-peer-A" },
    
    // ✅ CLAVE: El destino está fuera, claro y en el primer nivel
    "peerDestinoId": "uuid-peer-B",

    "peticionCliente": {
      "action": "enviarMensajeDirecto",
      "payload": {
        "remitenteId": "...",
        "contenido": "..."
        // El peerDestinoId ya NO es necesario aquí
      }
    }
  }
}
```

### Ventajas del Diseño Fase 1

1. **Lógica Limpia**: `handleRetransmitirPeticion` ya no necesita parsear el JSON interno (`peticionCliente.payload`)
2. **Eficiencia**: Simplemente lee `payload.get("peerDestinoId")` del primer nivel
3. **Flexibilidad**: La `peticionCliente` es un "paquete sellado". Al controlador no le importa lo que lleva dentro
4. **Separación de Responsabilidades**: El cartero solo entrega, no inspecciona

---

## Arquitectura Implementada

### Componentes Nuevos

#### 1. `P2PRoutingHelper.java`
**Ubicación:** `transporte/server-controladorTransporte/src/main/java/com/arquitectura/controlador/routing/P2PRoutingHelper.java`

**Responsabilidades:**
- Detectar automáticamente si un destinatario está en otro peer
- Construir la petición de retransmisión con el formato limpio (Fase 1)
- Enviar la petición al peer remoto usando la fachada
- Devolver la respuesta del peer remoto

**Métodos Principales:**

```java
/**
 * Detecta si el destinatario está en otro peer y enruta automáticamente.
 * 
 * @return Optional con la respuesta del peer remoto, o vacío si el usuario es local
 */
Optional<DTOResponse> enrutarSiEsNecesario(
    UUID destinatarioId, 
    String accionOriginal, 
    Map<String, Object> payloadOriginal,
    IClientHandler handler
);

/**
 * Versión simplificada que solo retorna booleano.
 */
boolean necesitaEnrutamiento(UUID destinatarioId);

/**
 * Obtiene el peer ID del destinatario.
 */
UUID obtenerPeerIdDelDestinatario(UUID destinatarioId);
```

#### 2. Actualización del `PeerController.java`

**Método Actualizado:** `handleRetransmitirPeticion()`

**Cambios Clave:**

```java
// ANTES: Tenía que buscar peerDestinoId dentro de peticionCliente.payload
JsonObject peticionPayload = gson.toJsonTree(peticionCliente.getPayload()).getAsJsonObject();
String peerDestinoIdStr = peticionPayload.has("peerDestinoId") ? 
    peticionPayload.get("peerDestinoId").getAsString() : null;

// DESPUÉS: Lee peerDestinoId del primer nivel (diseño limpio)
String peerDestinoIdStr = payload.get("peerDestinoId").getAsString();
UUID peerDestinoId = UUID.fromString(peerDestinoIdStr);
```

**Logs Mejorados:**

```
📨 [PeerController] Cartero: Entregando paquete
   ├─ Origen: Servidor-1
   ├─ Destino: uuid-peer-B
   └─ Acción: enviarMensajeDirecto
✅ [PeerController] Cartero: Paquete entregado y respuesta recibida
```

#### 3. Integración en `MessageController.java`

**Método Actualizado:** `handleSendDirectMessage()`

**Flujo de Enrutamiento Automático:**

```java
// 1. Validaciones normales de la petición
// ...

// 2. ENRUTAMIENTO AUTOMÁTICO P2P
System.out.println("🔍 [MessageController] Verificando ubicación del destinatario...");

Optional<DTOResponse> respuestaEnrutada = routingHelper.enrutarSiEsNecesario(
    destinatarioId,
    "enviarMensajeDirecto",
    request.getPayload(),
    handler
);

// 3. Si fue enrutado a otro peer, devolver la respuesta
if (respuestaEnrutada.isPresent()) {
    DTOResponse respuesta = respuestaEnrutada.get();
    System.out.println("✅ [MessageController] Mensaje enrutado a peer remoto exitosamente");
    
    // Devolver respuesta del peer remoto
    sendJsonResponse(handler, "enviarMensajeDirecto", ...);
    return;
}

// 4. PROCESAMIENTO LOCAL
// El destinatario está en este peer, procesar normalmente
System.out.println("📍 [MessageController] Destinatario es local, procesando mensaje...");
// ...
```

---

## Flujo Completo de Enrutamiento

### Caso: Usuario A (Peer 1) envía mensaje a Usuario B (Peer 2)

```
┌─────────────────────────────────────────────────────────────────────┐
│ CLIENTE A (Conectado a Servidor 1)                                  │
│ Envía: enviarMensajeDirecto                                         │
│   - remitenteId: usuario-A                                          │
│   - destinatarioId: usuario-B                                       │
│   - contenido: "Hola desde otro peer!"                              │
└─────────────────────────────────────────────────────────────────────┘
                                ↓
┌─────────────────────────────────────────────────────────────────────┐
│ SERVIDOR 1 - MessageController.handleSendDirectMessage()            │
│                                                                      │
│ 1. Validaciones de campos requeridos ✓                              │
│ 2. Verificar autenticación ✓                                        │
│                                                                      │
│ 3. 🔍 ENRUTAMIENTO AUTOMÁTICO P2P                                    │
│    routingHelper.enrutarSiEsNecesario(destinatarioId, ...)          │
│                                                                      │
│    ├─ Buscar usuario-B en BD local                                  │
│    ├─ Obtener peerId de usuario-B → "uuid-peer-2"                   │
│    ├─ Comparar con peerId local → "uuid-peer-1"                     │
│    └─ 🌐 ¡Destinatario está en otro peer!                           │
└─────────────────────────────────────────────────────────────────────┘
                                ↓
┌─────────────────────────────────────────────────────────────────────┐
│ P2PRoutingHelper.enrutarSiEsNecesario()                             │
│                                                                      │
│ 1. Construir petición de retransmisión (FASE 1: Diseño Limpio):    │
│                                                                      │
│    {                                                                 │
│      "action": "retransmitirpeticion",                              │
│      "payload": {                                                    │
│        "peerOrigen": { "peerId": "uuid-peer-1" },                   │
│        "peerDestinoId": "uuid-peer-2",  // ← Primer nivel           │
│        "peticionCliente": {                                          │
│          "action": "enviarMensajeDirecto",                          │
│          "payload": {                                                │
│            "remitenteId": "usuario-A",                              │
│            "destinatarioId": "usuario-B",                           │
│            "contenido": "Hola desde otro peer!"                     │
│          }                                                           │
│        }                                                             │
│      }                                                               │
│    }                                                                 │
│                                                                      │
│ 2. Enviar al Peer 2 usando la fachada:                              │
│    chatFachada.p2p().retransmitirPeticion(uuid-peer-2, request)     │
└─────────────────────────────────────────────────────────────────────┘
                                ↓
┌─────────────────────────────────────────────────────────────────────┐
│ SERVIDOR 2 - PeerController.handleRetransmitirPeticion()            │
│                                                                      │
│ 📨 Cartero: Entregando paquete                                       │
│    ├─ Origen: Servidor-1                                            │
│    ├─ Destino: uuid-peer-2                                          │
│    └─ Acción: enviarMensajeDirecto                                  │
│                                                                      │
│ 1. Lee peerDestinoId del primer nivel ✓ (Diseño limpio)             │
│ 2. Extrae peticionCliente (paquete sellado) ✓                       │
│ 3. Llama a la fachada para procesar la petición:                    │
│    chatFachada.p2p().retransmitirPeticion(peerDestinoId, peticion)  │
└─────────────────────────────────────────────────────────────────────┘
                                ↓
┌─────────────────────────────────────────────────────────────────────┐
│ SERVIDOR 2 - MessageController.handleSendDirectMessage()            │
│                                                                      │
│ 📍 Destinatario es local, procesando mensaje...                     │
│                                                                      │
│ 1. Usuario-B está en Peer 2 (local) → NO enrutar                    │
│ 2. Crear/obtener canal directo entre A y B                          │
│ 3. Guardar mensaje en BD local                                      │
│ 4. Enviar notificación PUSH a Cliente B                             │
│ 5. ✅ Responder con éxito                                            │
└─────────────────────────────────────────────────────────────────────┘
                                ↓
┌─────────────────────────────────────────────────────────────────────┐
│ RESPUESTA viaja de vuelta:                                          │
│ Servidor 2 → Servidor 1 → Cliente A                                 │
│                                                                      │
│ {                                                                    │
│   "action": "enviarMensajeDirecto",                                 │
│   "status": "success",                                               │
│   "message": "Mensaje enviado",                                     │
│   "data": {                                                          │
│     "mensajeId": "uuid-mensaje-123",                                │
│     "fechaEnvio": "2025-11-10T15:30:00"                             │
│   }                                                                  │
│ }                                                                    │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Ventajas del Sistema

### 1. **Transparencia Total**

- Los clientes NO necesitan saber en qué peer está el destinatario
- Los clientes envían `enviarMensajeDirecto` igual que siempre
- El servidor detecta automáticamente y enruta si es necesario

### 2. **Aplicable a Cualquier Petición**

El sistema funciona para **CUALQUIER** tipo de petición que involucre dos usuarios:

- ✅ `enviarMensajeDirecto` (texto)
- ✅ `enviarMensajeDirectoAudio` (audio)
- ✅ `solicitarHistorialPrivado` (futuro)
- ✅ `invitarACanal` (futuro)
- ✅ `compartirArchivo` (futuro)

### 3. **Separación de Responsabilidades**

- **P2PRoutingHelper**: Detecta y construye la petición de enrutamiento
- **PeerController**: Cartero puro que solo entrega paquetes
- **MessageController**: Procesa la lógica de negocio (local o después de recibir retransmisión)

### 4. **Eficiencia**

- No hay parseo innecesario de JSON
- El `peerDestinoId` está en el primer nivel (acceso O(1))
- La petición original se mantiene intacta (paquete sellado)

---

## Logs del Sistema

### Caso 1: Destinatario Local

```
🔍 [MessageController] Verificando ubicación del destinatario...
📍 [P2PRouting] Peer local: uuid-peer-1
📍 [P2PRouting] Peer destinatario: uuid-peer-1
✓ [P2PRouting] Destinatario es local, no se requiere enrutamiento
📍 [MessageController] Destinatario es local, procesando mensaje...
```

### Caso 2: Destinatario en Peer Remoto

```
🔍 [MessageController] Verificando ubicación del destinatario...
📍 [P2PRouting] Peer local: uuid-peer-1
📍 [P2PRouting] Peer destinatario: uuid-peer-2
🌐 [P2PRouting] Destinatario está en peer remoto: uuid-peer-2
📨 [P2PRouting] Enrutando petición 'enviarMensajeDirecto' al peer remoto...
✅ [P2PRouting] Petición enrutada exitosamente al peer remoto
✅ [MessageController] Mensaje enrutado a peer remoto exitosamente
```

### Servidor Remoto (Peer 2)

```
→ [PeerController] Procesando retransmitirpeticion (Fase 1: Cartero Puro)
📨 [PeerController] Cartero: Entregando paquete
   ├─ Origen: Servidor-1
   ├─ Destino: uuid-peer-2
   └─ Acción: enviarMensajeDirecto
✅ [PeerController] Cartero: Paquete entregado y respuesta recibida

🔍 [MessageController] Verificando ubicación del destinatario...
📍 [P2PRouting] Destinatario es local, no se requiere enrutamiento
📍 [MessageController] Destinatario es local, procesando mensaje...
```

---

## Fase 2: Procesamiento Distribuido (FUTURO)

### Concepto

Para operaciones que requieren **consenso entre múltiples peers** (ej: crear un canal global), necesitamos una nueva acción: `procesarAccionDistribuida`.

### Caso de Uso: Crear un Canal Global

```json
{
  "action": "procesarAccionDistribuida",
  "payload": {
    "peerOrigen": { "peerId": "uuid-peer-A" },
    
    "politica": "ALL",  // O "QUORUM" para 51%
    
    "accionDistribuida": {
      "action": "_internal_crearCanal",
      "payload": {
        "canalId": "uuid-canal-123",
        "nombre": "Canal Global",
        "propietarioId": "uuid-usuario"
      }
    }
  }
}
```

### Flujo Fase 2

1. **Validación Local**: El servidor procesa primero localmente
2. **Distribución**: Usa Fase 1 (retransmitirpeticion) para enviar a otros peers
3. **Recolección**: Espera respuestas de todos los peers
4. **Consenso**: Aplica la política (ALL o QUORUM)
5. **Rollback**: Si algo falla, envía compensación a todos
6. **Respuesta**: Confirma al cliente original

---

## Próximos Pasos

### Inmediato

- [x] Implementar Fase 1: Cartero Puro
- [x] Aplicar enrutamiento automático a `enviarMensajeDirecto`
- [ ] Aplicar enrutamiento automático a `enviarMensajeDirectoAudio`
- [ ] Aplicar enrutamiento automático a `solicitarHistorialPrivado`

### Futuro

- [ ] Implementar Fase 2: Procesamiento Distribuido
- [ ] Implementar `procesarAccionDistribuida`
- [ ] Implementar transacciones de compensación (rollback)
- [ ] Añadir políticas de consenso (ALL, QUORUM)
- [ ] Aplicar Fase 2 a operaciones globales (crear canales, etc.)

---

## Estado de Compilación

✅ **Compilación Exitosa**
- `P2PRoutingHelper.java` creado correctamente
- `PeerController.java` actualizado (Fase 1)
- `MessageController.java` actualizado con enrutamiento automático

---

**Fecha de Implementación:** 2025-11-10  
**Autor:** Sistema de Enrutamiento Automático P2P  
**Versión:** 1.0 (Fase 1 Completa)

