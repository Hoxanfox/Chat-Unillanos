**Respuesta Exitosa**:
```json
{
  "action": "startFileUpload",
  "status": "success",
  "data": {
    "uploadId": "UUID - ID de la sesión de subida"
  }
}
```

---

### 4.2 Iniciar Subida de Archivo para Registro (Sin Autenticación)

**Acción**: `uploadFileForRegistration`  
**Archivo**: `GestionArchivosImpl.java`  
**Ubicación**: `Negocio/GestionArchivos/src/main/java/gestionArchivos/`

**Payload (DTOStartUpload)**:
```json
{
  "action": "uploadFileForRegistration",
  "data": {
    "fileName": "String",
    "mimeType": "String",
    "totalChunks": "Integer"
  }
}
```

**Respuestas Esperadas**:
- Acción de respuesta: `uploadFileForRegistration`
- Status: `success` o `error`

**Respuesta Exitosa**:
```json
{
  "action": "uploadFileForRegistration",
  "status": "success",
  "data": {
    "uploadId": "UUID"
  }
}
```

---

### 4.3 Subir Chunk de Archivo

**Acción**: `uploadFileChunk`  
**Archivo**: `GestionArchivosImpl.java`  
**Ubicación**: `Negocio/GestionArchivos/src/main/java/gestionArchivos/`

**Payload (DTOUploadChunk)**:
```json
{
  "action": "uploadFileChunk",
  "data": {
    "uploadId": "UUID",
    "chunkNumber": "Integer - Número del chunk (1-based)",
    "chunkData": "String - Datos en Base64"
  }
}
```

**Respuestas Esperadas**:
- Acción de respuesta: `uploadFileChunk_{uploadId}_{chunkNumber}`
- Status: `success` o `error`

**Respuesta Exitosa**:
```json
{
  "action": "uploadFileChunk_<uploadId>_<chunkNumber>",
  "status": "success",
  "data": null
}
```

**Nota**: El cliente registra un manejador único por cada chunk para rastrear el progreso.

---

### 4.4 Finalizar Subida de Archivo

**Acción**: `endFileUpload`  
**Archivo**: `GestionArchivosImpl.java`  
**Ubicación**: `Negocio/GestionArchivos/src/main/java/gestionArchivos/`

**Payload (DTOEndUpload)**:
```json
{
  "action": "endFileUpload",
  "data": {
    "uploadId": "UUID",
    "fileHash": "String - Hash SHA-256 del archivo completo"
  }
}
```

**Respuestas Esperadas**:
- Acción de respuesta: `endFileUpload`
- Status: `success` o `error`

**Respuesta Exitosa**:
```json
{
  "action": "endFileUpload",
  "status": "success",
  "data": {
    "fileId": "UUID - ID del archivo en el servidor",
    "fileName": "String",
    "size": "Long - Tamaño en bytes"
  }
}
```

**Nota Importante**: El cliente usa el `fileId` para asociar archivos con mensajes.

---

### 4.5 Solicitar Chunk de Archivo (Descarga)

**Acción**: `requestFileChunk`  
**Archivo**: `GestionArchivosImpl.java`  
**Ubicación**: `Negocio/GestionArchivos/src/main/java/gestionArchivos/`

**Payload (DTORequestChunk)**:
```json
{
  "action": "requestFileChunk",
  "data": {
    "downloadId": "UUID - ID del archivo a descargar",
    "chunkNumber": "Integer - Número del chunk solicitado"
  }
}
```

**Respuestas Esperadas**:
- El servidor envía el chunk solicitado
- Status: `success` o `error`

---

## 5. GESTIÓN DE CANALES

### 5.1 Crear Canal

**Acción**: `crearCanal`  
**Archivo**: `CreadorCanal.java`  
**Ubicación**: `Negocio/GestionCanales/src/main/java/gestionCanales/nuevoCanal/`

**Payload (DTOCrearCanal)**:
```json
{
  "action": "crearCanal",
  "data": {
    "creadorId": "UUID",
    "nombre": "String - Nombre del canal",
    "descripcion": "String - Descripción (opcional)"
  }
}
```

**Respuestas Esperadas**:
- Acción de respuesta: `crearCanal`
- Status: `success` o `error`

**Respuesta Exitosa**:
```json
{
  "action": "crearCanal",
  "status": "success",
  "data": {
    "id": "UUID del canal",
    "nombre": "String",
    "descripcion": "String",
    "creadorId": "UUID",
    "fechaCreacion": "ISO-8601 DateTime"
  }
}
```

---

### 5.2 Listar Canales del Usuario

**Acción**: `listarCanales`  
**Archivo**: `ListadorCanales.java`  
**Ubicación**: `Negocio/GestionCanales/src/main/java/gestionCanales/listarCanales/`

**Payload (DTOListarCanales)**:
```json
{
  "action": "listarCanales",
  "data": {
    "usuarioId": "UUID",
    "limite": 100,
    "offset": 0
  }
}
```

**Respuestas Esperadas**:
- Acción de respuesta: `listarCanales`
- Status: `success` or `error`

**Respuesta Exitosa**:
```json
{
  "action": "listarCanales",
  "status": "success",
  "data": [
    {
      "id": "UUID",
      "nombre": "String",
      "creadorId": "UUID (opcional)"
    }
  ]
}
```

---

### 5.3 Invitar Miembro a Canal

**Acción**: `gestionarMiembro`  
**Archivo**: `InvitadorMiembro.java`  
**Ubicación**: `Negocio/GestionCanales/src/main/java/gestionCanales/invitarMiembro/`

**Payload (DTOGestionarMiembro)**:
```json
{
  "action": "gestionarMiembro",
  "data": {
    "adminId": "UUID - ID del administrador",
    "canalId": "UUID",
    "contactoId": "UUID - ID del usuario a invitar",
    "operacion": "AGREGAR"
  }
}
```

**Respuestas Esperadas**:
- Acción de respuesta: `gestionarMiembro`
- Status: `success` o `error`

**Respuesta Exitosa**:
```json
{
  "action": "gestionarMiembro",
  "status": "success",
  "data": null
}
```

**Nota**: Esta acción solo envía la invitación. El usuario no se agrega al canal hasta que acepte. El servidor enviará una notificación `nuevoMiembro` cuando esto suceda.

---

### 5.4 Listar Miembros de un Canal

**Acción**: `listarMiembros`  
**Archivo**: `ListadorMiembros.java`  
**Ubicación**: `Negocio/GestionCanales/src/main/java/gestionCanales/listarMiembros/`

**Payload (DTOListarMiembros)**:
```json
{
  "action": "listarMiembros",
  "data": {
    "canalId": "UUID",
    "solicitanteId": "UUID"
  }
}
```

**Respuestas Esperadas**:
- Acción de respuesta: `listarMiembros`
- Status: `success` o `error`

**Respuesta Exitosa**:
```json
{
  "action": "listarMiembros",
  "status": "success",
  "data": [
    {
      "usuarioId": "UUID",
      "nombre": "String",
      "rol": "ADMIN/MEMBER",
      "photoId": "String (UUID)"
    }
  ]
}
```

---

### 5.5 Unirse a Canal (Aceptar Invitación)

**Acción**: `unirseCanal`  
**Archivo**: `AceptadorInvitacion.java`  
**Ubicación**: `Negocio/GestionCanales/src/main/java/gestionCanales/aceptarInvitacion/`

**Payload**:
```json
{
  "action": "unirseCanal",
  "data": {
    "usuarioId": "UUID",
    "canalId": "UUID"
  }
}
```

**Respuestas Esperadas**:
- Acción de respuesta: `unirseCanal`
- Status: `success` o `error`

**Respuesta Exitosa**:
```json
{
  "action": "unirseCanal",
  "status": "success",
  "data": {
    "canalId": "UUID",
    "usuarioId": "UUID"
  }
}
```

---

## 6. MENSAJERÍA EN CANALES

### 6.1 Enviar Mensaje a Canal

**Acción**: `enviarMensajeCanal`  
**Archivo**: `GestorMensajesCanalImpl.java`  
**Ubicación**: `Negocio/GestionCanales/src/main/java/gestionCanales/mensajes/`

**Payload**:
```json
{
  "action": "enviarMensajeCanal",
  "data": {
    "canalId": "UUID",
    "remitenteId": "UUID",
    "tipo": "TEXTO/AUDIO/IMAGEN/ARCHIVO",
    "contenido": "String"
  }
}
```

**Respuestas Esperadas**:
- Acción de respuesta: `enviarMensajeCanal`
- Status: `success` o `error`

**Respuesta Exitosa**:
```json
{
  "action": "enviarMensajeCanal",
  "status": "success",
  "data": {
    "mensajeId": "UUID",
    "canalId": "UUID",
    "remitenteId": "UUID",
    "remitenteNombre": "String",
    "tipo": "TEXTO",
    "contenido": "String",
    "timestamp": "ISO-8601 DateTime"
  }
}
```

---

### 6.2 Solicitar Historial de Canal

**Acción**: `solicitarHistorialCanal`  
**Archivo**: `GestorMensajesCanalImpl.java`  
**Ubicación**: `Negocio/GestionCanales/src/main/java/gestionCanales/mensajes/`

**Payload**:
```json
{
  "action": "solicitarHistorialCanal",
  "data": {
    "canalId": "UUID",
    "usuarioId": "UUID",
    "limite": 50,
    "offset": 0
  }
}
```

**Respuestas Esperadas**:
- Acción de respuesta: `solicitarHistorialCanal`
- Status: `success` o `error`

**Respuesta Exitosa**:
```json
{
  "action": "solicitarHistorialCanal",
  "status": "success",
  "data": [
    {
      "mensajeId": "UUID",
      "canalId": "UUID",
      "remitenteId": "UUID",
      "remitenteNombre": "String",
      "tipo": "TEXTO/AUDIO/IMAGEN/ARCHIVO",
      "contenido": "String",
      "timestamp": "ISO-8601 DateTime"
    }
  ]
}
```

---

## 7. NOTIFICACIONES

### 7.1 Obtener Notificaciones

**Acción**: `obtenerNotificaciones`  
**Archivo**: `GestorNotificaciones.java`  
**Ubicación**: `Negocio/GestionNotificaciones/src/main/java/gestionNotificaciones/`

**Payload**:
```json
{
  "action": "obtenerNotificaciones",
  "data": {
    "usuarioId": "UUID"
  }
}
```

**Respuestas Esperadas**:
- Acción de respuesta: `obtenerNotificaciones`
- Status: `success` o `error`

**Respuesta Exitosa**:
```json
{
  "action": "obtenerNotificaciones",
  "status": "success",
  "data": [
    {
      "notificacionId": "UUID",
      "usuarioId": "UUID",
      "tipo": "SOLICITUD_AMISTAD/INVITACION_CANAL/MENSAJE/...",
      "contenido": "String",
      "leida": boolean,
      "fechaCreacion": "ISO-8601 DateTime"
    }
  ]
}
```

---

### 7.2 Marcar Notificación como Leída

**Acción**: `marcarNotificacionLeida`  
**Archivo**: `GestorNotificaciones.java`  
**Ubicación**: `Negocio/GestionNotificaciones/src/main/java/gestionNotificaciones/`

**Payload**:
```json
{
  "action": "marcarNotificacionLeida",
  "data": {
    "usuarioId": "UUID",
    "notificacionId": "UUID"
  }
}
```

**Respuestas Esperadas**:
- Acción de respuesta: `marcarNotificacionLeida`
- Status: `success` o `error`

---

### 7.3 Marcar Todas las Notificaciones como Leídas

**Acción**: `marcarTodasNotificacionesLeidas`  
**Archivo**: `GestorNotificaciones.java`  
**Ubicación**: `Negocio/GestionNotificaciones/src/main/java/gestionNotificaciones/`

**Payload**:
```json
{
  "action": "marcarTodasNotificacionesLeidas",
  "data": {
    "usuarioId": "UUID"
  }
}
```

**Respuestas Esperadas**:
- Acción de respuesta: `marcarTodasNotificacionesLeidas`
- Status: `success` o `error`

---

### 7.4 Responder Solicitud de Amistad

**Acción**: `responderSolicitudAmistad`  
**Archivo**: `GestorNotificaciones.java`  
**Ubicación**: `Negocio/GestionNotificaciones/src/main/java/gestionNotificaciones/`

**Payload (Aceptar)**:
```json
{
  "action": "responderSolicitudAmistad",
  "data": {
    "usuarioId": "UUID",
    "solicitanteId": "UUID",
    "notificacionId": "UUID",
    "aceptar": true
  }
}
```

**Payload (Rechazar)**:
```json
{
  "action": "responderSolicitudAmistad",
  "data": {
    "usuarioId": "UUID",
    "solicitanteId": "UUID",
    "notificacionId": "UUID",
    "aceptar": false
  }
}
```

**Respuestas Esperadas**:
- Acción de respuesta: `responderSolicitudAmistad`
- Status: `success` o `error`

---

### 7.5 Responder Invitación a Canal

**Acción**: `responderInvitacionCanal`  
**Archivo**: `GestorNotificaciones.java`  
**Ubicación**: `Negocio/GestionNotificaciones/src/main/java/gestionNotificaciones/`

**Payload (Aceptar)**:
```json
{
  "action": "responderInvitacionCanal",
  "data": {
    "usuarioId": "UUID",
    "canalId": "UUID",
    "notificacionId": "UUID",
    "aceptar": true
  }
}
```

**Payload (Rechazar)**:
```json
{
  "action": "responderInvitacionCanal",
  "data": {
    "usuarioId": "UUID",
    "canalId": "UUID",
    "notificacionId": "UUID",
    "aceptar": false
  }
}
```

**Respuestas Esperadas**:
- Acción de respuesta: `responderInvitacionCanal`
- Status: `success` o `error`

---

## 8. RESPUESTAS DEL SERVIDOR (PUSH)

### 8.1 Nuevo Mensaje Directo (Push)

**Acción Recibida**: `nuevoMensajeDirecto`  
**Archivo**: `GestionMensajesImpl.java`  
**Ubicación**: `Negocio/GestionContactos/src/main/java/gestionContactos/mensajes/`

**Descripción**: El servidor envía esta notificación cuando otro usuario envía un mensaje al cliente actual.

**Formato**:
```json
{
  "action": "nuevoMensajeDirecto",
  "status": "success",
  "data": {
    "mensajeId": "UUID",
    "remitenteId": "UUID",
    "remitenteNombre": "String",
    "destinatarioId": "UUID",
    "tipo": "TEXTO/AUDIO/IMAGEN/ARCHIVO",
    "contenido": "String",
    "timestamp": "ISO-8601 DateTime",
    "leido": false
  }
}
```

**Manejador**: `manejarNuevoMensajePush()`  
**Notificación UI**: `"NUEVO_MENSAJE_PRIVADO"`

---

### 8.2 Nuevo Mensaje en Canal (Push)

**Acción Recibida**: `nuevoMensajeCanal`  
**Archivo**: `GestorMensajesCanalImpl.java`  
**Ubicación**: `Negocio/GestionCanales/src/main/java/gestionCanales/mensajes/`

**Descripción**: El servidor envía esta notificación cuando se publica un mensaje en un canal del que el usuario es miembro.

**Formato**:
```json
{
  "action": "nuevoMensajeCanal",
  "status": "success",
  "data": {
    "mensajeId": "UUID",
    "canalId": "UUID",
    "remitenteId": "UUID",
    "remitenteNombre": "String",
    "tipo": "TEXTO",
    "contenido": "String",
    "timestamp": "ISO-8601 DateTime"
  }
}
```

---

### 8.3 Actualización de Lista de Contactos (Push)

**Acción Recibida**: `actualizarListaContactos`  
**Archivo**: `GestionContactosImpl.java`  
**Ubicación**: `Negocio/GestionContactos/src/main/java/gestionContactos/actualizacion/`

**Descripción**: El servidor puede enviar actualizaciones proactivas de la lista de contactos.

---

### 8.4 Nuevo Miembro en Canal (Push)

**Descripción**: Cuando un usuario acepta una invitación, el servidor notifica a todos los miembros del canal.

**Formato esperado**:
```json
{
  "action": "nuevoMiembro",
  "status": "success",
  "data": {
    "canalId": "UUID",
    "usuarioId": "UUID",
    "nombre": "String",
    "rol": "MEMBER"
  }
}
```

---

## 9. RESUMEN DE ACCIONES POR MÓDULO

### 📝 Módulo de Autenticación
- `registerUser` - Registrar nuevo usuario
- `authenticateUser` - Autenticar usuario existente

### 👥 Módulo de Contactos
- `solicitarListaContactos` - Obtener lista de contactos

### 💬 Módulo de Mensajería Directa
- `enviarMensajePrivado` - Enviar mensaje privado
- `solicitarHistorialPrivado` - Obtener historial de mensajes

### 📎 Módulo de Archivos
- `startFileUpload` - Iniciar subida (autenticado)
- `uploadFileForRegistration` - Iniciar subida (registro)
- `uploadFileChunk` - Subir chunk de archivo
- `endFileUpload` - Finalizar subida
- `requestFileChunk` - Solicitar chunk (descarga)

### 📢 Módulo de Canales
- `crearCanal` - Crear nuevo canal
- `listarCanales` - Listar canales del usuario
- `gestionarMiembro` - Invitar miembro
- `listarMiembros` - Listar miembros del canal
- `unirseCanal` - Aceptar invitación

### 💬 Módulo de Mensajería en Canales
- `enviarMensajeCanal` - Enviar mensaje a canal
- `solicitarHistorialCanal` - Obtener historial del canal

### 🔔 Módulo de Notificaciones
- `obtenerNotificaciones` - Obtener notificaciones
- `marcarNotificacionLeida` - Marcar como leída
- `marcarTodasNotificacionesLeidas` - Marcar todas como leídas
- `responderSolicitudAmistad` - Aceptar/rechazar amistad
- `responderInvitacionCanal` - Aceptar/rechazar invitación canal

---

## 10. NOTAS TÉCNICAS

### 10.1 Estructura del DTORequest

Todas las peticiones siguen este formato base:
```java
public class DTORequest {
    private String action;  // Nombre de la acción
    private Object data;    // Payload específico
}
```

### 10.2 Estructura del DTOResponse

Todas las respuestas siguen este formato base:
```java
public class DTOResponse {
    private String action;   // Acción de la respuesta
    private String status;   // "success" o "error"
    private String message;  // Mensaje descriptivo (opcional)
    private Object data;     // Datos de respuesta
}
```

### 10.3 Manejo de Estados de Usuario

**Estados del Servidor**: `ONLINE`, `OFFLINE`, `BANNED`  
**Estados de BD Local**: `activo`, `inactivo`, `baneado`

Mapeo:
- `ONLINE` → `activo`
- `OFFLINE` → `inactivo`
- `BANNED` → `baneado`

### 10.4 Gestión de Sesión

La clase `GestorSesionUsuario` mantiene:
- `userId`: UUID del usuario autenticado
- `usuarioLogueado`: Objeto Usuario completo

### 10.5 Patrón Observador

Todos los componentes de negocio implementan el patrón Observador para notificar a la UI:

**Tipos de Notificaciones**:
- `AUTENTICACION_INICIADA`
- `AUTENTICACION_EXITOSA`
- `USUARIO_BANEADO`
- `REGISTRO_INICIADO`
- `REGISTRO_EXITOSO`
- `REGISTRO_ERROR`
- `ACTUALIZAR_CONTACTOS`
- `NUEVO_MENSAJE_PRIVADO`
- `MENSAJE_ENVIADO_EXITOSO`
- `HISTORIAL_MENSAJES`
- `CANALES_ACTUALIZADOS`
- `CANAL_CREACION_INICIADA`
- `CANAL_ERROR`
- `MIEMBROS_ACTUALIZADOS`

### 10.6 Chunk Size

El cliente utiliza un tamaño de chunk de **256 KB** para la transferencia de archivos.

```java
private static final int CHUNK_SIZE = 256 * 1024; // 256 KB
```

### 10.7 Hash de Archivos

Los archivos se verifican usando **SHA-256** para garantizar integridad.

---

## 11. INTEGRACIÓN CON EL SERVIDOR

### 11.1 Registro de Manejadores

El cliente utiliza `GestorRespuesta` (Singleton) para registrar manejadores de respuestas:

```java
gestorRespuesta.registrarManejador("nombreAccion", (DTOResponse res) -> {
    // Procesamiento de la respuesta
});
```

### 11.2 Envío de Peticiones

El cliente utiliza `EnviadorPeticiones` para enviar peticiones:

```java
DTORequest request = new DTORequest("accion", payload);
enviadorPeticiones.enviar(request);
```

### 11.3 Flujo de Comunicación

1. Cliente crea un `DTORequest` con la acción y payload
2. Cliente registra un manejador para procesar la respuesta
3. Cliente envía la petición a través del socket
4. Servidor procesa y responde con un `DTOResponse`
5. `GestorRespuesta` despacha la respuesta al manejador apropiado
6. El manejador procesa la respuesta y notifica a los observadores

---

## 12. CHECKLIST DE IMPLEMENTACIÓN EN EL SERVIDOR

Para cada ruta del cliente, el servidor debe:

- [ ] Implementar el endpoint con el nombre de acción correcto
- [ ] Validar el payload recibido
- [ ] Procesar la petición
- [ ] Responder con la estructura `DTOResponse` correcta
- [ ] Usar el mismo `action` en la respuesta
- [ ] Incluir `status: "success"` o `status: "error"`
- [ ] Incluir los datos esperados en el campo `data`
- [ ] Implementar notificaciones push donde sea necesario

---

**Fin de la Documentación**

# 📡 DOCUMENTACIÓN DE RUTAS DEL CLIENTE

**Fecha**: Octubre 2025  
**Proyecto**: Chat Unillanos - Cliente  
**Propósito**: Documentar todas las rutas (acciones) que el cliente envía al servidor para facilitar la integración

---

## 📋 ÍNDICE

1. [Autenticación y Registro](#1-autenticación-y-registro)
2. [Gestión de Contactos](#2-gestión-de-contactos)
3. [Mensajería Directa (Privada)](#3-mensajería-directa-privada)
4. [Gestión de Archivos](#4-gestión-de-archivos)
5. [Gestión de Canales](#5-gestión-de-canales)
6. [Mensajería en Canales](#6-mensajería-en-canales)
7. [Notificaciones](#7-notificaciones)
8. [Respuestas del Servidor (Push)](#8-respuestas-del-servidor-push)

---

## 1. AUTENTICACIÓN Y REGISTRO

### 1.1 Registro de Usuario

**Acción**: `registerUser`  
**Archivo**: `RegistroUsuarioImpl.java`  
**Ubicación**: `Negocio/GestionUsuario/src/main/java/gestionUsuario/registro/`

**Payload (DTORegistro)**:
```json
{
  "action": "registerUser",
  "data": {
    "name": "String - Nombre del usuario",
    "email": "String - Email único",
    "password": "String - Contraseña",
    "ip": "String - IP del cliente",
    "photoId": "String - ID de la foto subida (opcional)"
  }
}
```

**Respuestas Esperadas**:
- Acción de respuesta: `register` o `registro`
- Status: `success` o `error`

**Respuesta Exitosa**:
```json
{
  "action": "registro",
  "status": "success",
  "data": {
    "userId": "UUID",
    "id": "UUID",
    "fechaRegistro": "ISO-8601 DateTime",
    "photoId": "String (UUID)",
    "estado": "ONLINE/OFFLINE/BANNED"
  }
}
```

---

### 1.2 Autenticación de Usuario

**Acción**: `authenticateUser`  
**Archivo**: `AutenticarUsuario.java`  
**Ubicación**: `Negocio/GestionUsuario/src/main/java/gestionUsuario/autenticacion/`

**Payload (DTOAutenticacion)**:
```json
{
  "action": "authenticateUser",
  "data": {
    "email": "String - Email del usuario",
    "password": "String - Contraseña"
  }
}
```

**Respuestas Esperadas**:
- Acción de respuesta: `login`
- Status: `success` o `error`

**Respuesta Exitosa**:
```json
{
  "action": "login",
  "status": "success",
  "data": {
    "id": "UUID del usuario",
    "nombre": "String",
    "email": "String",
    "photoId": "String (UUID)",
    "estado": "ONLINE/OFFLINE/BANNED",
    "fechaRegistro": "ISO-8601 DateTime"
  }
}
```

**Nota**: Si el estado es `BANNED`, el cliente notifica al usuario y no permite el acceso.

---

## 2. GESTIÓN DE CONTACTOS

### 2.1 Solicitar Lista de Contactos

**Acción**: `solicitarListaContactos`  
**Archivo**: `GestionContactosImpl.java`  
**Ubicación**: `Negocio/GestionContactos/src/main/java/gestionContactos/actualizacion/`

**Payload**:
```json
{
  "action": "solicitarListaContactos",
  "data": null
}
```

**Respuestas Esperadas**:
- Acción de respuesta: `actualizarListaContactos` o `solicitarListaContactos`
- Status: `success` o `error`

**Respuesta Exitosa**:
```json
{
  "action": "actualizarListaContactos",
  "status": "success",
  "data": [
    {
      "id": "UUID",
      "nombre": "String",
      "email": "String",
      "photoId": "String (UUID)",
      "estado": "ONLINE/OFFLINE/BANNED"
    }
  ]
}
```

---

## 3. MENSAJERÍA DIRECTA (PRIVADA)

### 3.1 Enviar Mensaje Privado

**Acción**: `enviarMensajePrivado`  
**Archivo**: `GestionMensajesImpl.java`  
**Ubicación**: `Negocio/GestionContactos/src/main/java/gestionContactos/mensajes/`

**Payload (DTOEnviarMensaje)**:

#### 3.1.1 Mensaje de Texto
```json
{
  "action": "enviarMensajePrivado",
  "data": {
    "remitenteId": "UUID",
    "destinatarioId": "UUID",
    "tipo": "TEXTO",
    "contenido": "String - Texto del mensaje"
  }
}
```

#### 3.1.2 Mensaje de Audio
```json
{
  "action": "enviarMensajePrivado",
  "data": {
    "remitenteId": "UUID",
    "destinatarioId": "UUID",
    "tipo": "AUDIO",
    "contenido": "UUID - ID del audio subido",
    "audioFileId": "UUID - ID del archivo de audio"
  }
}
```

#### 3.1.3 Mensaje con Imagen
```json
{
  "action": "enviarMensajePrivado",
  "data": {
    "remitenteId": "UUID",
    "destinatarioId": "UUID",
    "tipo": "IMAGEN",
    "contenido": "String - Texto opcional",
    "imageFileId": "UUID - ID de la imagen subida",
    "fileName": "String - Nombre del archivo"
  }
}
```

#### 3.1.4 Mensaje con Archivo
```json
{
  "action": "enviarMensajePrivado",
  "data": {
    "remitenteId": "UUID",
    "destinatarioId": "UUID",
    "tipo": "ARCHIVO",
    "contenido": "String - Descripción opcional",
    "fileId": "UUID - ID del archivo subido",
    "fileName": "String - Nombre del archivo"
  }
}
```

**Respuestas Esperadas**:
- Acción de respuesta: `enviarMensajePrivado` o `enviarMensajeDirecto`
- Status: `success` o `error`

**Respuesta Exitosa**:
```json
{
  "action": "enviarMensajeDirecto",
  "status": "success",
  "data": {
    "mensajeId": "UUID",
    "remitenteId": "UUID",
    "destinatarioId": "UUID",
    "tipo": "TEXTO/AUDIO/IMAGEN/ARCHIVO",
    "contenido": "String",
    "timestamp": "ISO-8601 DateTime",
    "leido": false
  }
}
```

---

### 3.2 Solicitar Historial de Mensajes Privados

**Acción**: `solicitarHistorialPrivado`  
**Archivo**: `GestionMensajesImpl.java`  
**Ubicación**: `Negocio/GestionContactos/src/main/java/gestionContactos/mensajes/`

**Payload (DTOSolicitarHistorial)**:
```json
{
  "action": "solicitarHistorialPrivado",
  "data": {
    "userId": "UUID - ID del usuario que solicita",
    "contactoId": "UUID - ID del contacto"
  }
}
```

**Respuestas Esperadas**:
- Acción de respuesta: `solicitarHistorialPrivado`
- Status: `success` o `error`

**Respuesta Exitosa**:
```json
{
  "action": "solicitarHistorialPrivado",
  "status": "success",
  "data": {
    "mensajes": [
      {
        "mensajeId": "UUID",
        "remitenteId": "UUID",
        "remitenteNombre": "String",
        "destinatarioId": "UUID",
        "tipo": "TEXTO/AUDIO/IMAGEN/ARCHIVO",
        "contenido": "String",
        "timestamp": "ISO-8601 DateTime",
        "leido": boolean
      }
    ],
    "tieneMas": boolean,
    "contactoNombre": "String"
  }
}
```

**Nota**: El cliente marca cada mensaje como `esMio` comparando `remitenteId` con el `userId` de la sesión.

---

## 4. GESTIÓN DE ARCHIVOS

### 4.1 Iniciar Subida de Archivo (Autenticado)

**Acción**: `startFileUpload`  
**Archivo**: `GestionArchivosImpl.java`  
**Ubicación**: `Negocio/GestionArchivos/src/main/java/gestionArchivos/`

**Payload (DTOStartUpload)**:
```json
{
  "action": "startFileUpload",
  "data": {
    "fileName": "String - Nombre del archivo",
    "mimeType": "String - Tipo MIME",
    "totalChunks": "Integer - Total de chunks"
  }
}
```

**Respuestas Esperadas**:
- Acción de respuesta: `startFileUpload`
- Status: `success` o `error`


