# 📢 Documentación de Notificaciones Push del Servidor

## 📋 Resumen
Este documento describe las notificaciones push que el servidor envía proactivamente a los clientes conectados, sin que el cliente las solicite explícitamente.

## 🎯 Notificaciones Push Implementadas

### 1. 📢 solicitarListaContactos (Push de Actualización de Contactos)

**Propósito:** Informar a todos los usuarios conectados de un cambio en la lista de contactos (por ejemplo, cuando alguien se conecta o se desconecta).

**Trigger (Disparador):** Es disparado internamente por el servidor cuando un usuario cambia de estado. El servidor llama al método `broadcastContactListUpdate(Object contactListData)`.

**¿Quién la recibe?** TODOS los clientes conectados (`activeClientsById.values()`).

#### JSON de Push (Lo que recibe el cliente)
El cliente NO envía nada. El cliente simplemente recibe este JSON del servidor de forma proactiva:

```json
{
  "action": "solicitarListaContactos",
  "status": "success",
  "message": "Lista de contactos obtenida exitosamente",
  "data": [
    {
      "id": "uuid-del-contacto-1",
      "peerid": "peer-id-del-contacto-1",
      "nombre": "Nombre del Contacto 1",
      "email": "contacto1@email.com",
      "imagenBase64": "data:image/png;base64,...",
      "imagenId": "ruta/o/id_de_imagen_1.jpg",
      "conectado": "ONLINE"
    },
    {
      "id": "uuid-del-contacto-2",
      "peerid": null,
      "nombre": "Nombre del Contacto 2",
      "email": "contacto2@email.com",
      "imagenBase64": null,
      "imagenId": "ruta/o/id_de_imagen_2.png",
      "conectado": "OFFLINE"
    }
  ]
}
```

**Campos del objeto contacto:**
- `id`: UUID del contacto
- `peerid`: ID del peer WebRTC (puede ser `null` si está desconectado)
- `nombre`: Nombre completo del contacto
- `email`: Correo electrónico del contacto
- `imagenBase64`: Imagen en Base64 (puede ser `null`)
- `imagenId`: Ruta o ID de la imagen almacenada (puede ser `null`)
- `conectado`: Estado de conexión (`"ONLINE"` o `"OFFLINE"`)

#### Implementación en el Cliente
**Archivo:** `GestionContactosImpl.java`

```java
// Manejador registrado
this.gestorRespuesta.registrarManejador("solicitarListaContactos", this::manejarPushActualizacionContactos);

// Método manejador
private void manejarPushActualizacionContactos(DTOResponse respuesta) {
    System.out.println("📥 [GestionContactos][PUSH]: Notificación de actualización recibida");
    if (respuesta.fueExitoso()) {
        procesarListaContactos(respuesta, "PUSH");
    }
}
```

---

### 2. 💬 nuevoMensajeDirecto (Push de Mensaje de Texto)

**Propósito:** Entregar un mensaje de texto a un usuario en tiempo real.

**Trigger (Disparador):** La Fachada, después de procesar un `enviarMensajeDirecto` o `enviarMensajeTexto`, dispara un `NewMessageEvent`. El `handleNewMessageEvent` lo captura.

**¿Quién la recibe?** El destinatario del mensaje (o todos los miembros de un canal, excepto el remitente original).

#### JSON de Push (Lo que recibe el destinatario)
El cliente destinatario recibe este JSON sin haberlo solicitado:

```json
{
  "action": "nuevoMensajeDirecto",
  "status": "success",
  "message": "Nuevo mensaje recibido",
  "data": {
    "mensajeId": "uuid-del-mensaje-creado",
    "remitenteId": "uuid-del-que-envio-el-mensaje",
    "remitenteNombre": "NombreDelRemitente",
    "peerRemitenteId": "peer-id-del-remitente",
    "peerDestinoId": null,
    "tipo": "texto",
    "contenido": "Este es el contenido del mensaje",
    "fechaEnvio": "2025-11-06T22:30:00Z",
    "destinatarioId": "uuid-del-que-recibe-el-mensaje"
  }
}
```

**Campos del objeto mensaje:**
- `mensajeId`: UUID del mensaje
- `remitenteId`: UUID del usuario que envió el mensaje
- `remitenteNombre`: Nombre del remitente
- `peerRemitenteId`: ID del peer WebRTC del remitente
- `peerDestinoId`: ID del peer WebRTC del destinatario (puede ser `null`)
- `tipo`: Tipo de mensaje (`"texto"` en minúsculas)
- `contenido`: Texto del mensaje
- `fechaEnvio`: Timestamp ISO 8601
- `destinatarioId`: UUID del destinatario (tu ID)

#### Implementación en el Cliente
**Archivo:** `GestionMensajesImpl.java`

```java
// Manejador registrado
this.gestorRespuesta.registrarManejador("nuevoMensajeDirecto", this::manejarNuevoMensajePush);

// Método manejador
private void manejarNuevoMensajePush(DTOResponse r) {
    System.out.println("📥 [GestionMensajes]: Recibido PUSH de nuevo mensaje directo");
    
    if (!r.fueExitoso()) {
        notificarObservadores("ERROR_NOTIFICACION_MENSAJE", r.getMessage());
        return;
    }

    DTOMensaje mensaje = mapearMensajeDesdeServidor(r.getData());
    mensaje.setEsMio(myUserId.equals(mensaje.getRemitenteId()));
    
    notificarObservadores("NUEVO_MENSAJE_PRIVADO", mensaje);
}
```

---

### 3. 🎧 nuevoMensajeDirectoAudio (Push de Mensaje de Audio)

**Propósito:** Entregar un mensaje de audio a un usuario en tiempo real.

**Trigger (Disparador):** Idéntico al anterior. La Fachada dispara un `NewMessageEvent` (con `messageType: "AUDIO"`) y el `handleNewMessageEvent` lo captura.

**¿Quién la recibe?** El destinatario del mensaje (o todos los miembros de un canal, excepto el remitente original).

#### JSON de Push (Lo que recibe el destinatario)
El cliente destinatario recibe este JSON:

```json
{
  "action": "nuevoMensajeDirectoAudio",
  "status": "success",
  "message": "Nuevo mensaje de audio recibido",
  "data": {
    "mensajeId": "uuid-del-mensaje-de-audio",
    "remitenteId": "uuid-del-que-envio-el-audio",
    "remitenteNombre": "NombreDelRemitente",
    "peerRemitenteId": "peer-id-del-remitente",
    "peerDestinoId": null,
    "tipo": "audio",
    "contenido": "data:audio/webm;base64,.....",
    "fechaEnvio": "2025-11-06T22:31:00Z",
    "destinatarioId": "uuid-del-que-recibe-el-mensaje"
  }
}
```

**¡Dato Clave!** Fíjate en el `contenido`: El servidor llama a `requestDispatcher.enrichOutgoingMessage(originalDto)`. Esto significa que, a diferencia de la petición `enviarmensajedirectoaudio` (que usaba una ruta), esta notificación "push" ya trae el archivo de audio codificado en **Base64**, listo para ser reproducido por el cliente.

**Formato del contenido:**
```
data:audio/webm;base64,GkXfo59ChoEBQveBAULygQRC84EIQoKEd2VibUKHgQRChYECGFOAZwH/////////FU...
```

#### Implementación en el Cliente
**Archivo:** `GestionMensajesImpl.java`

```java
// Manejador registrado
this.gestorRespuesta.registrarManejador("nuevoMensajeDirectoAudio", this::manejarNuevoMensajeAudioPush);

// Método manejador
private void manejarNuevoMensajeAudioPush(DTOResponse r) {
    System.out.println("📥 [GestionMensajes]: Recibido PUSH de nuevo mensaje de audio");
    
    if (!r.fueExitoso()) {
        notificarObservadores("ERROR_NOTIFICACION_MENSAJE_AUDIO", r.getMessage());
        return;
    }

    DTOMensaje mensaje = mapearMensajeDesdeServidor(r.getData());
    mensaje.setEsMio(myUserId.equals(mensaje.getRemitenteId()));
    
    // El contenido viene en Base64 (data:audio/webm;base64,...)
    if (mensaje.getContenido() != null && mensaje.getContenido().startsWith("data:audio")) {
        System.out.println("   → Audio Base64: Sí (listo para reproducir)");
    }
    
    notificarObservadores("NUEVO_MENSAJE_AUDIO_PRIVADO", mensaje);
}
```

---

## 🔄 Diferencias Clave entre Petición y Push

### Mensajes de Audio: Petición vs Push

| Aspecto | Petición (enviarmensajedirectoaudio) | Push (nuevoMensajeDirectoAudio) |
|---------|--------------------------------------|----------------------------------|
| **Formato de contenido** | Ruta/URL del archivo | Base64 completo |
| **Ejemplo** | `"ruta/al/archivo.webm"` | `"data:audio/webm;base64,..."` |
| **¿Listo para reproducir?** | No, requiere descarga | Sí, reproducir directamente |
| **Tamaño** | Pequeño (solo ruta) | Grande (audio completo) |

### Por qué esta diferencia:
1. **En la petición:** El cliente envía la ruta porque el archivo ya está en el servidor
2. **En el push:** El servidor enriquece el mensaje con el audio completo para que el destinatario pueda reproducirlo inmediatamente sin hacer otra petición

---

## 📊 Mapeo de Campos del Servidor al Cliente

### Estructura del Push del Servidor
```json
{
  "mensajeId": "...",
  "remitenteId": "...",
  "remitenteNombre": "...",
  "peerRemitenteId": "...",
  "peerDestinoId": "...",
  "tipo": "texto",           // ← minúsculas
  "contenido": "...",
  "fechaEnvio": "...",
  "destinatarioId": "..."
}
```

### Mapeo en el Cliente (DTOMensaje)
```java
mensaje.setMensajeId(map.get("mensajeId"));
mensaje.setRemitenteId(map.get("remitenteId"));
mensaje.setRemitenteNombre(map.get("remitenteNombre"));
mensaje.setPeerRemitenteId(map.get("peerRemitenteId"));
mensaje.setPeerDestinoId(map.get("peerDestinoId"));
mensaje.setTipo(map.get("tipo").toUpperCase()); // Convertir a "TEXTO" o "AUDIO"
mensaje.setContenido(map.get("contenido"));
mensaje.setFechaEnvio(map.get("fechaEnvio"));
mensaje.setDestinatarioId(map.get("destinatarioId"));
```

**Nota:** El servidor envía `tipo` en minúsculas (`"texto"`, `"audio"`), pero el cliente lo convierte a mayúsculas (`"TEXTO"`, `"AUDIO"`).

---

## 🔍 Filtrado de Mensajes por Peer

El cliente implementa un filtro para ignorar mensajes que no están dirigidos a él:

```java
// Marcar si el mensaje es mío
boolean esMio = myUserId != null && myUserId.equals(mensaje.getRemitenteId());
mensaje.setEsMio(esMio);

// Null-safe peer destination filter - solo filtrar si NO es mío
if (!esMio && myPeerId != null && mensaje.getPeerDestinoId() != null &&
        !myPeerId.equals(mensaje.getPeerDestinoId())) {
    System.out.println("⏩ [GestionMensajes]: Ignorando mensaje dirigido a otro peer");
    return;
}
```

**Lógica:**
1. Si el mensaje es mío (yo soy el remitente), NO filtrar
2. Si el mensaje NO es mío y tiene `peerDestinoId` diferente a mi `peerId`, ignorarlo
3. Si el mensaje NO tiene `peerDestinoId` (`null`), procesarlo (broadcast)

---

## 🧪 Pruebas Recomendadas

### 1. Push de Lista de Contactos
- [ ] Conectar un usuario A
- [ ] Conectar un usuario B
- [ ] Verificar que el usuario A reciba el push con B como "ONLINE"
- [ ] Desconectar el usuario B
- [ ] Verificar que el usuario A reciba el push con B como "OFFLINE"

### 2. Push de Mensaje de Texto
- [ ] Usuario A envía mensaje de texto a usuario B
- [ ] Verificar que B reciba el push con el mensaje completo
- [ ] Verificar que el campo `tipo` sea "texto" (minúsculas del servidor)
- [ ] Verificar que el cliente lo convierta a "TEXTO" (mayúsculas)

### 3. Push de Mensaje de Audio
- [ ] Usuario A envía mensaje de audio a usuario B
- [ ] Verificar que B reciba el push con el audio en Base64
- [ ] Verificar que el campo `contenido` empiece con "data:audio/webm;base64,"
- [ ] Verificar que el cliente pueda reproducir el audio directamente

### 4. Filtrado por Peer
- [ ] Usuario A (con peerId1) envía mensaje a B
- [ ] Usuario C (con peerId2) NO debe procesar el mensaje
- [ ] Verificar logs: "Ignorando mensaje dirigido a otro peer"

---

## 📝 Notas Importantes

### ✅ Características de los Push
- **No requieren petición:** El servidor los envía proactivamente
- **Son inmediatos:** Se entregan en tiempo real
- **Son broadcast o unicast:** Algunos van a todos, otros solo al destinatario
- **No tienen respuesta:** El cliente solo los procesa

### ⚠️ Consideraciones
1. **Audio en Push viene en Base64:** Listo para reproducir, no requiere descarga adicional
2. **Contactos en Push es broadcast:** TODOS los clientes conectados lo reciben
3. **Mensajes en Push son unicast:** Solo el destinatario (o miembros del canal) lo reciben
4. **El campo `tipo` viene en minúsculas:** El cliente debe convertirlo a mayúsculas
5. **El filtrado por peer es importante:** Evita procesar mensajes de otros usuarios en multi-sesión

### 🔄 Conversión de Tipos
| Servidor | Cliente |
|----------|---------|
| `"texto"` | `"TEXTO"` |
| `"audio"` | `"AUDIO"` |

---

## 📅 Fecha de Documentación
7 de noviembre de 2025

## 👤 Estado
✅ **Completado y Alineado con la API del Servidor**

---

## 📌 Resumen de Manejadores Registrados

### En GestionContactosImpl
```java
// PUSH: Actualización de lista de contactos
this.gestorRespuesta.registrarManejador("solicitarListaContactos", this::manejarPushActualizacionContactos);
```

### En GestionMensajesImpl
```java
// PUSH: Nuevos mensajes
this.gestorRespuesta.registrarManejador("nuevoMensajeDirecto", this::manejarNuevoMensajePush);
this.gestorRespuesta.registrarManejador("nuevoMensajeDirectoAudio", this::manejarNuevoMensajeAudioPush);
```

**Total de notificaciones push implementadas: 3**

