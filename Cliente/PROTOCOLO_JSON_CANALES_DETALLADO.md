                
            case "HISTORIAL_CANAL_RECIBIDO":
                List<DTOMensajeCanal> historial = (List<DTOMensajeCanal>) datos;
                cargarHistorial(historial);
                break;
        }
    });
}
```

---

## 🎯 PUNTOS CLAVE

1. **Todas las peticiones usan `DTORequest`** con un `action` y `data`
2. **Todas las respuestas usan `DTOResponse`** con `status`, `message` y `data`
3. **Las notificaciones push** son respuestas no solicitadas del servidor
4. **Los mensajes se guardan localmente** antes y después de enviar
5. **El patrón Observer notifica a la UI** automáticamente

---

📅 **Fecha de creación**: 16 de Octubre, 2025  
👨‍💻 **Proyecto**: Chat Unillanos - Sistema de Gestión de Canales  
📡 **Versión del Protocolo**: 1.0
# 📡 PROTOCOLO JSON - SISTEMA DE GESTIÓN DE CANALES

## 📋 Tabla de Contenidos
- [1. Crear Canal](#1-crear-canal)
- [2. Listar Canales](#2-listar-canales)
- [3. Invitar Miembro](#3-invitar-miembro)
- [4. Aceptar Invitación](#4-aceptar-invitación)
- [5. Listar Miembros](#5-listar-miembros)
- [6. Enviar Mensaje de Texto](#6-enviar-mensaje-de-texto)
- [7. Enviar Mensaje de Audio](#7-enviar-mensaje-de-audio)
- [8. Solicitar Historial](#8-solicitar-historial)
- [Notificaciones Push del Servidor](#notificaciones-push-del-servidor)

---

## 1. CREAR CANAL

### 📤 Petición del Cliente al Servidor

```json
{
  "action": "crearCanal",
  "data": {
    "creadorId": "123e4567-e89b-12d3-a456-426614174000",
    "nombre": "Canal General",
    "descripcion": "Canal para discusiones generales del equipo"
  }
}
```

**Clase Java**: `DTOCrearCanal`
```java
new DTOCrearCanal(
    creadorId: "123e4567-e89b-12d3-a456-426614174000",
    nombre: "Canal General",
    descripcion: "Canal para discusiones generales"
)
```

---

### ✅ Respuesta Exitosa del Servidor

```json
{
  "status": "success",
  "message": "Canal creado exitosamente",
  "data": {
    "id": "987fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d",
    "nombre": "Canal General",
    "creadorId": "123e4567-e89b-12d3-a456-426614174000",
    "fechaCreacion": "2025-10-16T10:30:00Z"
  }
}
```

**Procesamiento en el Cliente**:
```java
// En CreadorCanal.java
gestorRespuesta.registrarManejador("crearCanal", (respuesta) -> {
    if (respuesta.fueExitoso()) {
        Map<String, Object> data = (Map<String, Object>) respuesta.getData();
        Canal canal = new Canal(
            UUID.fromString((String) data.get("id")),
            (String) data.get("nombre"),
            UUID.fromString((String) data.get("creadorId"))
        );
        // Guardar en base de datos local
        repositorioCanal.guardar(canal);
    }
});
```

---

### ❌ Respuesta de Error del Servidor

```json
{
  "status": "error",
  "message": "El nombre del canal ya existe",
  "data": null
}
```

**Errores posibles**:
- `"El nombre del canal ya existe"`
- `"Nombre de canal inválido"`
- `"Usuario no autenticado"`
- `"Permisos insuficientes"`

---

## 2. LISTAR CANALES

### 📤 Petición del Cliente al Servidor

```json
{
  "action": "listarCanales",
  "data": {
    "usuarioId": "123e4567-e89b-12d3-a456-426614174000",
    "limite": 100,
    "offset": 0
  }
}
```

**Clase Java**: `DTOListarCanales`
```java
new DTOListarCanales(
    usuarioId: "123e4567-e89b-12d3-a456-426614174000",
    limite: 100,
    offset: 0
)
```

---

### ✅ Respuesta Exitosa del Servidor

```json
{
  "status": "success",
  "message": "Lista de canales obtenida",
  "data": [
    {
      "id": "987fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d",
      "nombre": "Canal General",
      "creadorId": "123e4567-e89b-12d3-a456-426614174000",
      "cantidadMiembros": 25
    },
    {
      "id": "111fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d",
      "nombre": "Canal de Proyectos",
      "creadorId": "456e7890-e89b-12d3-a456-426614174111",
      "cantidadMiembros": 12
    },
    {
      "id": "222fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d",
      "nombre": "Canal de Soporte",
      "creadorId": "789e0123-e89b-12d3-a456-426614174222",
      "cantidadMiembros": 8
    }
  ]
}
```

**Procesamiento en el Cliente**:
```java
// En ListadorCanales.java
Type tipoLista = new TypeToken<ArrayList<Map<String, Object>>>() {}.getType();
List<Map<String, Object>> listaDeMapas = gson.fromJson(
    gson.toJson(respuesta.getData()), 
    tipoLista
);

List<Canal> canales = new ArrayList<>();
for (Map<String, Object> mapa : listaDeMapas) {
    Canal canal = new Canal(
        UUID.fromString((String) mapa.get("id")),
        (String) mapa.get("nombre"),
        UUID.fromString((String) mapa.get("creadorId"))
    );
    canales.add(canal);
}

// Sincronizar con BD local
repositorioCanal.sincronizarCanales(canales);

// Notificar a la UI
notificarObservadores("CANALES_ACTUALIZADOS", canales);
```

---

## 3. INVITAR MIEMBRO

### 📤 Petición del Cliente al Servidor

```json
{
  "action": "invitarMiembroCanal",
  "data": {
    "canalId": "987fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d",
    "usuarioIdInvitador": "123e4567-e89b-12d3-a456-426614174000",
    "usuarioIdInvitado": "789e4567-e89b-12d3-a456-426614174222"
  }
}
```

**Clase Java**: `DTOGestionarMiembro`
```java
new DTOGestionarMiembro(
    canalId: "987fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d",
    usuarioIdInvitador: "123e4567-e89b-12d3-a456-426614174000",
    usuarioIdInvitado: "789e4567-e89b-12d3-a456-426614174222"
)
```

---

### ✅ Respuesta Exitosa del Servidor

```json
{
  "status": "success",
  "message": "Invitación enviada exitosamente",
  "data": {
    "invitacionId": "aaa1234b-51a2-43f8-9c7d-8e9f1a2b3c4d",
    "canalId": "987fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d",
    "usuarioInvitado": "789e4567-e89b-12d3-a456-426614174222",
    "estado": "pendiente",
    "fechaInvitacion": "2025-10-16T10:35:00Z"
  }
}
```

---

### 🔔 Notificación Push al Usuario Invitado

**El servidor envía automáticamente esta notificación al usuario invitado**:

```json
{
  "action": "notificacionInvitacionCanal",
  "status": "success",
  "message": "Has sido invitado a un canal",
  "data": {
    "invitacionId": "aaa1234b-51a2-43f8-9c7d-8e9f1a2b3c4d",
    "canalId": "987fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d",
    "nombreCanal": "Canal General",
    "invitadoPor": "Juan Pérez",
    "invitadoPorId": "123e4567-e89b-12d3-a456-426614174000",
    "fechaInvitacion": "2025-10-16T10:35:00Z"
  }
}
```

**Procesamiento en el Cliente**:
```java
// En GestorNotificacionesCanal.java
gestorRespuesta.registrarManejador("notificacionInvitacionCanal", (respuesta) -> {
    Map<String, Object> data = (Map<String, Object>) respuesta.getData();
    
    // Notificar a la UI para mostrar la invitación
    notificarObservadores("NUEVA_INVITACION_CANAL", data);
    
    // La UI puede mostrar una notificación:
    // "Juan Pérez te ha invitado a unirte al canal 'Canal General'"
});
```

---

## 4. ACEPTAR INVITACIÓN

### 📤 Petición del Cliente al Servidor

```json
{
  "action": "aceptarInvitacionCanal",
  "data": {
    "invitacionId": "aaa1234b-51a2-43f8-9c7d-8e9f1a2b3c4d",
    "usuarioId": "789e4567-e89b-12d3-a456-426614174222"
  }
}
```

**Clase Java**: `DTOUnirseCanal`
```java
new DTOUnirseCanal(
    invitacionId: "aaa1234b-51a2-43f8-9c7d-8e9f1a2b3c4d",
    usuarioId: "789e4567-e89b-12d3-a456-426614174222"
)
```

---

### ✅ Respuesta Exitosa del Servidor

```json
{
  "status": "success",
  "message": "Te has unido al canal exitosamente",
  "data": {
    "canalId": "987fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d",
    "nombreCanal": "Canal General",
    "rol": "miembro",
    "fechaUnion": "2025-10-16T10:40:00Z"
  }
}
```

---

### 🔔 Notificación Push a Todos los Miembros del Canal

**El servidor notifica a todos los miembros del canal sobre el nuevo integrante**:

```json
{
  "action": "nuevoMiembro",
  "status": "success",
  "message": "Nuevo miembro se ha unido al canal",
  "data": {
    "canalId": "987fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d",
    "usuarioId": "789e4567-e89b-12d3-a456-426614174222",
    "nombreUsuario": "María García",
    "rol": "miembro",
    "fechaUnion": "2025-10-16T10:40:00Z"
  }
}
```

**Procesamiento en el Cliente**:
```java
// En GestorNotificacionesCanal.java
gestorRespuesta.registrarManejador("nuevoMiembro", (respuesta) -> {
    Map<String, Object> data = (Map<String, Object>) respuesta.getData();
    
    String nombreUsuario = (String) data.get("nombreUsuario");
    String canalId = (String) data.get("canalId");
    
    // Notificar a la UI
    notificarObservadores("NUEVO_MIEMBRO_EN_CANAL", data);
    
    // Si es el canal actual, mostrar mensaje en el chat:
    // "María García se ha unido al canal"
});
```

---

## 5. LISTAR MIEMBROS

### 📤 Petición del Cliente al Servidor

```json
{
  "action": "listarMiembrosCanal",
  "data": {
    "canalId": "987fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d",
    "usuarioId": "123e4567-e89b-12d3-a456-426614174000"
  }
}
```

**Clase Java**: Se usa directamente un Map o un DTO simple

---

### ✅ Respuesta Exitosa del Servidor

```json
{
  "status": "success",
  "message": "Lista de miembros obtenida",
  "data": [
    {
      "usuarioId": "123e4567-e89b-12d3-a456-426614174000",
      "nombre": "Juan Pérez",
      "email": "juan.perez@example.com",
      "rol": "administrador",
      "estado": "activo",
      "fechaUnion": "2025-10-15T09:00:00Z"
    },
    {
      "usuarioId": "789e4567-e89b-12d3-a456-426614174222",
      "nombre": "María García",
      "email": "maria.garcia@example.com",
      "rol": "miembro",
      "estado": "activo",
      "fechaUnion": "2025-10-16T10:40:00Z"
    },
    {
      "usuarioId": "456e7890-e89b-12d3-a456-426614174333",
      "nombre": "Carlos López",
      "email": "carlos.lopez@example.com",
      "rol": "miembro",
      "estado": "activo",
      "fechaUnion": "2025-10-16T11:15:00Z"
    }
  ]
}
```

**Procesamiento en el Cliente**:
```java
// En ListadorMiembros.java
Type tipoLista = new TypeToken<ArrayList<DTOMiembroCanal>>() {}.getType();
List<DTOMiembroCanal> miembros = gson.fromJson(
    gson.toJson(respuesta.getData()), 
    tipoLista
);

// Sincronizar con BD local
repositorioCanal.sincronizarMiembros(canalId, miembros);

// Notificar a la UI
notificarObservadores("MIEMBROS_ACTUALIZADOS", miembros);
```

---

## 6. ENVIAR MENSAJE DE TEXTO

### 📤 Petición del Cliente al Servidor

```json
{
  "action": "enviarMensajeCanal",
  "data": {
    "remitenteId": "123e4567-e89b-12d3-a456-426614174000",
    "canalId": "987fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d",
    "tipo": "texto",
    "contenido": "Hola a todos, ¿cómo están?"
  }
}
```

**Clase Java**: `DTOEnviarMensajeCanal`
```java
DTOEnviarMensajeCanal.deTexto(
    remitenteId: "123e4567-e89b-12d3-a456-426614174000",
    canalId: "987fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d",
    contenido: "Hola a todos, ¿cómo están?"
)
```

**Código en el Cliente**:
```java
// En GestorMensajesCanalImpl.java
gestorMensajes.enviarMensajeTexto(canalId, "Hola a todos, ¿cómo están?")
    .thenAccept(resultado -> {
        System.out.println("Mensaje enviado exitosamente");
    })
    .exceptionally(ex -> {
        System.err.println("Error al enviar: " + ex.getMessage());
        return null;
    });
```

---

### ✅ Respuesta Exitosa del Servidor

```json
{
  "status": "success",
  "message": "Mensaje enviado",
  "data": {
    "mensajeId": "msg-123abc-456def-789ghi",
    "fechaEnvio": "2025-10-16T10:45:00Z"
  }
}
```

---

### 🔔 Notificación Push a Todos los Miembros del Canal

**El servidor envía esta notificación a TODOS los miembros del canal (excepto el remitente)**:

```json
{
  "action": "nuevoMensajeCanal",
  "status": "success",
  "message": "Nuevo mensaje en canal",
  "data": {
    "mensajeId": "msg-123abc-456def-789ghi",
    "canalId": "987fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d",
    "remitenteId": "123e4567-e89b-12d3-a456-426614174000",
    "nombreRemitente": "Juan Pérez",
    "tipo": "texto",
    "contenido": "Hola a todos, ¿cómo están?",
    "fechaEnvio": "2025-10-16T10:45:00Z"
  }
}
```

**Procesamiento en el Cliente**:
```java
// En GestorMensajesCanalImpl.java
gestorRespuesta.registrarManejador("nuevoMensajeCanal", (respuesta) -> {
    Map<String, Object> data = (Map<String, Object>) respuesta.getData();
    
    DTOMensajeCanal mensaje = new DTOMensajeCanal();
    mensaje.setMensajeId((String) data.get("mensajeId"));
    mensaje.setCanalId((String) data.get("canalId"));
    mensaje.setRemitenteId((String) data.get("remitenteId"));
    mensaje.setNombreRemitente((String) data.get("nombreRemitente"));
    mensaje.setTipo((String) data.get("tipo"));
    mensaje.setContenido((String) data.get("contenido"));
    mensaje.setFechaEnvio(LocalDateTime.parse((String) data.get("fechaEnvio")));
    
    // Guardar en BD local
    repositorioMensajes.guardarMensajeRecibido(convertir(mensaje));
    
    // Notificar a la UI
    notificarObservadores("MENSAJE_CANAL_RECIBIDO", mensaje);
});
```

---

## 7. ENVIAR MENSAJE DE AUDIO

### 📤 Petición del Cliente al Servidor

```json
{
  "action": "enviarMensajeCanal",
  "data": {
    "remitenteId": "123e4567-e89b-12d3-a456-426614174000",
    "canalId": "987fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d",
    "tipo": "audio",
    "fileId": "audio_abc123xyz789def456ghi"
  }
}
```

**Clase Java**: `DTOEnviarMensajeCanal`
```java
DTOEnviarMensajeCanal.deAudio(
    remitenteId: "123e4567-e89b-12d3-a456-426614174000",
    canalId: "987fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d",
    audioFileId: "audio_abc123xyz789def456ghi"
)
```

**Flujo completo en el Cliente**:
```java
// 1. Grabar el audio
File archivoAudio = grabarAudio();

// 2. Subir el archivo al servidor
gestionArchivos.subirArchivo(archivoAudio)
    .thenAccept(fileId -> {
        // 3. Enviar el mensaje con el fileId
        gestorMensajes.enviarMensajeAudio(canalId, fileId)
            .thenAccept(resultado -> {
                System.out.println("Audio enviado exitosamente");
            });
    });
```

---

### ✅ Respuesta Exitosa del Servidor

```json
{
  "status": "success",
  "message": "Mensaje de audio enviado",
  "data": {
    "mensajeId": "msg-audio-789xyz-123abc",
    "fechaEnvio": "2025-10-16T10:50:00Z",
    "duracion": 15
  }
}
```

---

### 🔔 Notificación Push a Miembros del Canal

```json
{
  "action": "nuevoMensajeCanal",
  "status": "success",
  "message": "Nuevo mensaje de audio en canal",
  "data": {
    "mensajeId": "msg-audio-789xyz-123abc",
    "canalId": "987fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d",
    "remitenteId": "123e4567-e89b-12d3-a456-426614174000",
    "nombreRemitente": "Juan Pérez",
    "tipo": "audio",
    "fileId": "audio_abc123xyz789def456ghi",
    "duracion": 15,
    "fechaEnvio": "2025-10-16T10:50:00Z"
  }
}
```

---

## 8. SOLICITAR HISTORIAL

### 📤 Petición del Cliente al Servidor

```json
{
  "action": "solicitarHistorialCanal",
  "data": {
    "canalId": "987fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d",
    "usuarioId": "123e4567-e89b-12d3-a456-426614174000",
    "limite": 50,
    "offset": 0
  }
}
```

**Clase Java**: `DTOSolicitarHistorialCanal`
```java
new DTOSolicitarHistorialCanal(
    canalId: "987fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d",
    usuarioId: "123e4567-e89b-12d3-a456-426614174000",
    limite: 50,
    offset: 0
)
```

**Código en el Cliente**:
```java
// En GestorMensajesCanalImpl.java
gestorMensajes.solicitarHistorialCanal(canalId, 50);
```

---

### ✅ Respuesta Exitosa del Servidor

```json
{
  "status": "success",
  "message": "Historial de mensajes obtenido",
  "data": [
    {
      "mensajeId": "msg-001",
      "remitenteId": "123e4567-e89b-12d3-a456-426614174000",
      "nombreRemitente": "Juan Pérez",
      "tipo": "texto",
      "contenido": "Bienvenidos al canal",
      "fechaEnvio": "2025-10-16T09:00:00Z"
    },
    {
      "mensajeId": "msg-002",
      "remitenteId": "789e4567-e89b-12d3-a456-426614174222",
      "nombreRemitente": "María García",
      "tipo": "texto",
      "contenido": "Gracias por la invitación",
      "fechaEnvio": "2025-10-16T09:05:00Z"
    },
    {
      "mensajeId": "msg-003",
      "remitenteId": "123e4567-e89b-12d3-a456-426614174000",
      "nombreRemitente": "Juan Pérez",
      "tipo": "audio",
      "fileId": "audio_abc123xyz789",
      "duracion": 10,
      "fechaEnvio": "2025-10-16T09:10:00Z"
    },
    {
      "mensajeId": "msg-004",
      "remitenteId": "456e7890-e89b-12d3-a456-426614174333",
      "nombreRemitente": "Carlos López",
      "tipo": "texto",
      "contenido": "¿Cuándo es la próxima reunión?",
      "fechaEnvio": "2025-10-16T09:15:00Z"
    }
  ]
}
```

**Procesamiento en el Cliente**:
```java
// En GestorMensajesCanalImpl.java
Type tipoLista = new TypeToken<ArrayList<Map<String, Object>>>() {}.getType();
List<Map<String, Object>> listaDeMapas = gson.fromJson(
    gson.toJson(respuesta.getData()), 
    tipoLista
);

List<DTOMensajeCanal> historial = new ArrayList<>();
for (Map<String, Object> mapa : listaDeMapas) {
    DTOMensajeCanal mensaje = construirDTOMensajeDesdeMap(mapa);
    historial.add(mensaje);
}

// Sincronizar con BD local
repositorioMensajes.sincronizarHistorial(canalId, historial);

// Notificar a la UI
notificarObservadores("HISTORIAL_CANAL_RECIBIDO", historial);
```

---

## 🔔 NOTIFICACIONES PUSH DEL SERVIDOR

El servidor envía notificaciones push automáticamente sin que el cliente las solicite. Estas son **eventos en tiempo real**.

### Resumen de Notificaciones Push:

| Acción | Cuándo se envía | Destinatarios |
|--------|-----------------|---------------|
| `notificacionInvitacionCanal` | Al invitar a un usuario | Usuario invitado |
| `nuevoMiembro` | Al aceptar invitación | Todos los miembros del canal |
| `nuevoMensajeCanal` | Al enviar un mensaje | Todos los miembros (excepto remitente) |

---

## 📊 TABLA RESUMEN DE ACCIONES

| # | Acción | Petición | Respuesta | Notificación Push |
|---|--------|----------|-----------|-------------------|
| 1 | Crear Canal | `crearCanal` | Canal creado | No |
| 2 | Listar Canales | `listarCanales` | Lista de canales | No |
| 3 | Invitar Miembro | `invitarMiembroCanal` | Invitación enviada | Sí → Usuario invitado |
| 4 | Aceptar Invitación | `aceptarInvitacionCanal` | Unión exitosa | Sí → Todos los miembros |
| 5 | Listar Miembros | `listarMiembrosCanal` | Lista de miembros | No |
| 6 | Enviar Mensaje Texto | `enviarMensajeCanal` | Mensaje enviado | Sí → Todos los miembros |
| 7 | Enviar Mensaje Audio | `enviarMensajeCanal` | Mensaje enviado | Sí → Todos los miembros |
| 8 | Solicitar Historial | `solicitarHistorialCanal` | Historial | No |

---

## 🔄 FLUJO COMPLETO: Enviar y Recibir Mensaje

### Paso a Paso con JSON

#### 1️⃣ Usuario A envía un mensaje

**Cliente A → Servidor**:
```json
{
  "action": "enviarMensajeCanal",
  "data": {
    "remitenteId": "user-a-123",
    "canalId": "canal-001",
    "tipo": "texto",
    "contenido": "¿Alguien disponible para la reunión?"
  }
}
```

#### 2️⃣ Servidor confirma al Usuario A

**Servidor → Cliente A**:
```json
{
  "status": "success",
  "message": "Mensaje enviado",
  "data": {
    "mensajeId": "msg-12345",
    "fechaEnvio": "2025-10-16T10:45:00Z"
  }
}
```

#### 3️⃣ Servidor notifica a Usuario B (push)

**Servidor → Cliente B**:
```json
{
  "action": "nuevoMensajeCanal",
  "status": "success",
  "message": "Nuevo mensaje en canal",
  "data": {
    "mensajeId": "msg-12345",
    "canalId": "canal-001",
    "remitenteId": "user-a-123",
    "nombreRemitente": "Juan Pérez",
    "tipo": "texto",
    "contenido": "¿Alguien disponible para la reunión?",
    "fechaEnvio": "2025-10-16T10:45:00Z"
  }
}
```

#### 4️⃣ Cliente B procesa y muestra el mensaje

```java
// En la UI del Cliente B
@Override
public void actualizar(String tipoDeDato, Object datos) {
    if ("MENSAJE_CANAL_RECIBIDO".equals(tipoDeDato)) {
        DTOMensajeCanal mensaje = (DTOMensajeCanal) datos;
        
        Platform.runLater(() -> {
            // Agregar mensaje al chat
            listViewMensajes.getItems().add(mensaje);
            
            // Reproducir sonido de notificación
            reproducirSonido();
            
            // Mostrar badge si el canal no está activo
            if (!esCanalActivo(mensaje.getCanalId())) {
                mostrarBadge(mensaje.getCanalId());
            }
        });
    }
}
```

---

## 💡 EJEMPLOS DE USO EN EL CÓDIGO

### Enviar Mensaje desde la UI

```java
// En VentanaChat.java
@FXML
private void btnEnviar_Click() {
    String contenido = txtMensaje.getText();
    String canalId = canalSeleccionado.getId();
    
    gestorMensajes.enviarMensajeTexto(canalId, contenido)
        .thenAccept(v -> {
            Platform.runLater(() -> {
                txtMensaje.clear();
                System.out.println("✓ Mensaje enviado");
            });
        })
        .exceptionally(ex -> {
            Platform.runLater(() -> {
                mostrarError("Error al enviar: " + ex.getMessage());
            });
            return null;
        });
}
```

### Recibir y Mostrar Mensajes

```java
// En VentanaChat.java (implementa IObservador)
@Override
public void actualizar(String tipoDeDato, Object datos) {
    Platform.runLater(() -> {
        switch (tipoDeDato) {
            case "MENSAJE_CANAL_RECIBIDO":
                DTOMensajeCanal mensaje = (DTOMensajeCanal) datos;
                if (mensaje.getCanalId().equals(canalActual)) {
                    agregarMensajeAlChat(mensaje);
                }
                break;

