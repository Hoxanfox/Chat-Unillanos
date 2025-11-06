# 🎯 PLAN DE IMPLEMENTACIÓN - PRIORIDAD 2
## Gestión de Canales

**Fecha de creación**: 5 de noviembre de 2025  
**Proyecto**: Chat-Unillanos - Servidor  
**Objetivo**: Implementar las 4 funcionalidades de gestión de canales

---

## 📋 ÍNDICE

1. [Visión General](#visión-general)
2. [Funcionalidad 1: Invitar Miembro a Canal](#funcionalidad-1-invitar-miembro-a-canal)
3. [Funcionalidad 2: Responder Invitación](#funcionalidad-2-responder-invitación)
4. [Funcionalidad 3: Ver Invitaciones Pendientes](#funcionalidad-3-ver-invitaciones-pendientes)
5. [Funcionalidad 4: Validar Permisos en Canales](#funcionalidad-4-validar-permisos-en-canales)
6. [Testing y Validación](#testing-y-validación)
7. [Checklist Final](#checklist-final)

---

## 🎯 VISIÓN GENERAL

### **Estado Actual del Proyecto**

Después de completar la Prioridad 1, el servidor ya tiene:
- ✅ Registro de usuarios
- ✅ Envío de mensajes de texto a canal
- ✅ Obtención de historial de canal
- ✅ Listado de miembros de canal

### **Arquitectura de Membresías**

El sistema usa la entidad `MembresiaCanal` con estados:
- **PENDIENTE**: Invitación enviada pero no respondida
- **ACTIVO**: Miembro aceptado y activo en el canal
- **RECHAZADO**: Invitación rechazada (se elimina)

### **Roles en Canales**

- **OWNER/ADMIN**: Creador del canal, puede invitar miembros
- **MIEMBRO**: Usuario aceptado, puede enviar mensajes

### **Archivos Clave**

```
Server-Nicolas/
├── datos/
│   └── server-dominio/
│       └── MembresiaCanal.java (Estados: PENDIENTE, ACTIVO)
├── negocio/
│   ├── server-LogicaCanales/
│   │   ├── IChannelService.java
│   │   └── ChannelServiceImpl.java
│   └── server-logicaFachada/
│       ├── IChatFachada.java
│       └── ChatFachadaImpl.java
└── transporte/
    └── server-controladorTransporte/
        └── RequestDispatcher.java
```

---


# FUNCIONALIDAD 1: INVITAR MIEMBRO A CANAL

## 📊 ANÁLISIS PREVIO

### **Estado Actual**

✅ **Lo que YA existe:**
```java
// 1. Entidad MembresiaCanal con estados
Server-Nicolas/datos/server-dominio/src/main/java/com/arquitectura/domain/MembresiaCanal.java

// 2. Repositorio
Server-Nicolas/datos/server-persistencia/src/main/java/com/arquitectura/persistence/repository/MembresiaCanalRepository.java

// 3. Servicio con lógica
Server-Nicolas/negocio/server-LogicaCanales/src/main/java/com/arquitectura/logicaCanales/ChannelServiceImpl.java
    → void invitarMiembro(InviteMemberRequestDto requestDto, UUID ownerId)

// 4. Fachada con método
Server-Nicolas/negocio/server-logicaFachada/src/main/java/com/arquitectura/fachada/ChatFachadaImpl.java
    → void invitarMiembro(InviteMemberRequestDto requestDto, UUID ownerId)

// 5. DTO de request
Server-Nicolas/comunes/Server-DTO/src/main/java/com/arquitectura/DTO/canales/InviteMemberRequestDto.java

// 6. Sistema de eventos
Server-Nicolas/comunes/server-events/src/main/java/com/arquitectura/events/UserInvitedEvent.java
```

❌ **Lo que FALTA:**
```java
// 1. Endpoint en RequestDispatcher
case "invitarmiembro": // NO EXISTE

// 2. Notificación push al usuario invitado
```

### **Flujo Esperado**

```
Cliente envía (Owner del canal):
{
  "action": "invitarMiembro",
  "payload": {
    "channelId": "uuid-del-canal",
    "userIdToInvite": "uuid-del-usuario-a-invitar"
  }
}

Servidor:
1. Valida que el usuario esté autenticado
2. Valida que el canal exista
3. Valida que el solicitante sea el OWNER del canal
4. Valida que el canal sea de tipo GRUPO
5. Valida que el usuario a invitar exista
6. Valida que no exista ya una membresía (activa o pendiente)
7. Crea MembresiaCanal con estado PENDIENTE
8. Publica UserInvitedEvent
9. ServerListener notifica al usuario invitado (si está conectado)

Servidor responde al owner:
{
  "action": "invitarMiembro",
  "status": "success",
  "message": "Invitación enviada exitosamente",
  "data": {
    "channelId": "uuid-del-canal",
    "invitedUserId": "uuid-del-usuario-invitado",
    "invitedUsername": "nombre-usuario"
  }
}

Servidor notifica al usuario invitado (PUSH):
{
  "action": "nuevaInvitacion",
  "status": "success",
  "message": "Has recibido una invitación a un canal",
  "data": {
    "channelId": "uuid-del-canal",
    "channelName": "Nombre del Canal",
    "channelType": "GRUPO",
    "owner": {
      "userId": "uuid-owner",
      "username": "nombre-owner"
    }
  }
}
```

---

## 🔧 IMPLEMENTACIÓN PASO A PASO

### **PASO 1: Verificar InviteMemberRequestDto**

**Ubicación**: `Server-Nicolas/comunes/Server-DTO/src/main/java/com/arquitectura/DTO/canales/InviteMemberRequestDto.java`

**Verificar que tenga estos campos:**
- `UUID channelId`
- `UUID userIdToInvite`

### **PASO 2: Verificar ChannelServiceImpl.invitarMiembro()**

**Ubicación**: `Server-Nicolas/negocio/server-LogicaCanales/src/main/java/com/arquitectura/logicaCanales/ChannelServiceImpl.java`

**Verificar que el método haga:**
1. ✅ Validar que el canal exista
2. ✅ Validar que el solicitante sea el owner
3. ✅ Validar que el canal sea tipo GRUPO
4. ✅ Validar que el usuario a invitar exista
5. ✅ Validar que no exista membresía previa
6. ✅ Crear MembresiaCanal con estado PENDIENTE
7. ✅ Publicar UserInvitedEvent

### **PASO 3: Verificar ChatFachadaImpl.invitarMiembro()**

**Ubicación**: `Server-Nicolas/negocio/server-logicaFachada/src/main/java/com/arquitectura/fachada/ChatFachadaImpl.java`

**Verificar que delegue correctamente al servicio.**

### **PASO 4: Agregar el Endpoint en RequestDispatcher**

**Ubicación**: `Server-Nicolas/transporte/server-controladorTransporte/src/main/java/com/arquitectura/controlador/RequestDispatcher.java`

**Agregar después de "listarmiembros":**

```java
case "invitarmiembro":
case "invitarusuario":
    // 1. Extraer payload
    Object inviteDataObj = request.getPayload();
    if (inviteDataObj == null) {
        sendJsonResponse(handler, "invitarMiembro", false, "Falta payload", null);
        return;
    }

    // 2. Convertir a JSON y extraer campos
    JsonObject inviteJson = gson.toJsonTree(inviteDataObj).getAsJsonObject();
    String inviteChannelIdStr = inviteJson.has("channelId") ? inviteJson.get("channelId").getAsString() : null;
    String inviteUserIdStr = inviteJson.has("userIdToInvite") ? inviteJson.get("userIdToInvite").getAsString() : null;

    // 3. Validar campos requeridos
    if (inviteChannelIdStr == null || inviteChannelIdStr.trim().isEmpty()) {
        sendJsonResponse(handler, "invitarMiembro", false, "El ID del canal es requerido",
            createErrorData("channelId", "Campo requerido"));
        return;
    }

    if (inviteUserIdStr == null || inviteUserIdStr.trim().isEmpty()) {
        sendJsonResponse(handler, "invitarMiembro", false, "El ID del usuario a invitar es requerido",
            createErrorData("userIdToInvite", "Campo requerido"));
        return;
    }

    try {
        // 4. Convertir a UUIDs
        UUID inviteChannelId = UUID.fromString(inviteChannelIdStr);
        UUID inviteUserId = UUID.fromString(inviteUserIdStr);

        // 5. Obtener ID del owner (usuario autenticado)
        UUID ownerId = handler.getAuthenticatedUser().getUserId();

        // 6. Crear DTO de request
        InviteMemberRequestDto inviteDto = new InviteMemberRequestDto(inviteChannelId, inviteUserId);

        // 7. Llamar a la fachada
        chatFachada.invitarMiembro(inviteDto, ownerId);

        // 8. Obtener información del usuario invitado para la respuesta
        Optional<UserResponseDto> invitedUser = chatFachada.buscarUsuarioPorId(inviteUserId);
        
        // 9. Construir respuesta exitosa
        Map<String, Object> inviteResponseData = new HashMap<>();
        inviteResponseData.put("channelId", inviteChannelIdStr);
        inviteResponseData.put("invitedUserId", inviteUserIdStr);
        if (invitedUser.isPresent()) {
            inviteResponseData.put("invitedUsername", invitedUser.get().getUsername());
        }

        sendJsonResponse(handler, "invitarMiembro", true, "Invitación enviada exitosamente", inviteResponseData);

    } catch (IllegalArgumentException e) {
        // Error de validación
        String errorMessage = e.getMessage();
        String campo = "general";
        
        if (errorMessage.contains("Canal")) {
            campo = "channelId";
        } else if (errorMessage.contains("propietario") || errorMessage.contains("owner")) {
            campo = "permisos";
        } else if (errorMessage.contains("Usuario") || errorMessage.contains("usuario")) {
            campo = "userIdToInvite";
        } else if (errorMessage.contains("miembro") || errorMessage.contains("invitación")) {
            campo = "membresía";
        }
        
        sendJsonResponse(handler, "invitarMiembro", false, errorMessage,
            createErrorData(campo, errorMessage));
            
    } catch (Exception e) {
        // Error inesperado
        System.err.println("Error al invitar miembro: " + e.getMessage());
        e.printStackTrace();
        sendJsonResponse(handler, "invitarMiembro", false, "Error interno del servidor al invitar miembro", null);
    }
    break;
```

### **PASO 5: Verificar el Sistema de Notificaciones**

**Ubicación**: `Server-Nicolas/transporte/server-Transporte/src/main/java/com/arquitectura/transporte/ServerListener.java`

**Verificar que existe el método `handleUserInvitedEvent()`:**

```java
@EventListener
public void handleUserInvitedEvent(UserInvitedEvent event) {
    UUID invitedUserId = event.getInvitedUserId();
    ChannelResponseDto channelDto = event.getChannelDto();
    
    // Construir notificación
    DTOResponse notification = new DTOResponse(
        "nuevaInvitacion",
        "success",
        "Has recibido una invitación a un canal",
        channelDto
    );
    
    String notificationJson = gson.toJson(notification);
    
    // Enviar al usuario invitado si está conectado
    List<IClientHandler> userSessions = activeClientsById.get(invitedUserId);
    if (userSessions != null) {
        userSessions.forEach(handler -> handler.sendMessage(notificationJson));
    }
}
```

### **PASO 6: Compilar y Probar**

```bash
cd Server-Nicolas
mvn clean install -DskipTests
```

---

## ✅ CHECKLIST - FUNCIONALIDAD 1

- [ ] `InviteMemberRequestDto` existe y tiene todos los campos
- [ ] `ChannelServiceImpl.invitarMiembro()` existe y está completo
- [ ] Validación de owner implementada
- [ ] Validación de tipo de canal (solo GRUPO)
- [ ] Validación de membresía previa
- [ ] `ChatFachadaImpl.invitarMiembro()` existe y delega correctamente
- [ ] Caso `"invitarmiembro"` agregado en `RequestDispatcher.dispatch()`
- [ ] Validaciones de campos requeridos implementadas
- [ ] Manejo de errores específicos
- [ ] Evento `UserInvitedEvent` se publica correctamente
- [ ] `ServerListener.handleUserInvitedEvent()` existe y funciona
- [ ] Proyecto compila sin errores
- [ ] Prueba manual exitosa (invitar usuario)
- [ ] Prueba manual exitosa (solo owner puede invitar)
- [ ] Verificación en BD (membresía con estado PENDIENTE)
- [ ] Notificación push funciona (usuario invitado recibe notificación)

---


# FUNCIONALIDAD 2: RESPONDER INVITACIÓN

## 📊 ANÁLISIS PREVIO

### **Estado Actual**

✅ **Lo que YA existe:**
```java
// 1. Servicio con lógica
Server-Nicolas/negocio/server-LogicaCanales/src/main/java/com/arquitectura/logicaCanales/ChannelServiceImpl.java
    → void responderInvitacion(RespondToInviteRequestDto requestDto, UUID userId)

// 2. Fachada con método
Server-Nicolas/negocio/server-logicaFachada/src/main/java/com/arquitectura/fachada/ChatFachadaImpl.java
    → void responderInvitacion(RespondToInviteRequestDto requestDto, UUID userId)

// 3. DTO de request
Server-Nicolas/comunes/Server-DTO/src/main/java/com/arquitectura/DTO/canales/RespondToInviteRequestDto.java
```

❌ **Lo que FALTA:**
```java
// 1. Endpoint en RequestDispatcher
case "responderinvitacion": // NO EXISTE
```

### **Flujo Esperado**

```
Cliente envía (Usuario invitado):
{
  "action": "responderInvitacion",
  "payload": {
    "channelId": "uuid-del-canal",
    "accepted": true  // o false para rechazar
  }
}

Servidor:
1. Valida que el usuario esté autenticado
2. Busca la membresía con estado PENDIENTE
3. Si accepted = true:
   - Cambia estado a ACTIVO
   - Guarda en BD
4. Si accepted = false:
   - Elimina la membresía
5. Retorna confirmación

Servidor responde (aceptada):
{
  "action": "responderInvitacion",
  "status": "success",
  "message": "Invitación aceptada. Ahora eres miembro del canal",
  "data": {
    "channelId": "uuid-del-canal",
    "channelName": "Nombre del Canal",
    "accepted": true
  }
}

Servidor responde (rechazada):
{
  "action": "responderInvitacion",
  "status": "success",
  "message": "Invitación rechazada",
  "data": {
    "channelId": "uuid-del-canal",
    "accepted": false
  }
}
```

---

## 🔧 IMPLEMENTACIÓN PASO A PASO

### **PASO 1: Verificar RespondToInviteRequestDto**

**Ubicación**: `Server-Nicolas/comunes/Server-DTO/src/main/java/com/arquitectura/DTO/canales/RespondToInviteRequestDto.java`

**Verificar que tenga estos campos:**
- `UUID channelId`
- `boolean accepted`

### **PASO 2: Verificar ChannelServiceImpl.responderInvitacion()**

**Ubicación**: `Server-Nicolas/negocio/server-LogicaCanales/src/main/java/com/arquitectura/logicaCanales/ChannelServiceImpl.java`

**Verificar que el método haga:**
1. ✅ Buscar la membresía por channelId y userId
2. ✅ Validar que exista una invitación
3. ✅ Validar que el estado sea PENDIENTE
4. ✅ Si accepted = true: cambiar estado a ACTIVO
5. ✅ Si accepted = false: eliminar la membresía

### **PASO 3: Agregar el Endpoint en RequestDispatcher**

**Ubicación**: `Server-Nicolas/transporte/server-controladorTransporte/src/main/java/com/arquitectura/controlador/RequestDispatcher.java`

**Agregar después de "invitarmiembro":**

```java
case "responderinvitacion":
case "aceptarinvitacion":
case "rechazarinvitacion":
    // 1. Extraer payload
    Object respondDataObj = request.getPayload();
    if (respondDataObj == null) {
        sendJsonResponse(handler, "responderInvitacion", false, "Falta payload", null);
        return;
    }

    // 2. Convertir a JSON y extraer campos
    JsonObject respondJson = gson.toJsonTree(respondDataObj).getAsJsonObject();
    String respondChannelIdStr = respondJson.has("channelId") ? respondJson.get("channelId").getAsString() : null;
    Boolean accepted = respondJson.has("accepted") ? respondJson.get("accepted").getAsBoolean() : null;

    // 3. Validar campos requeridos
    if (respondChannelIdStr == null || respondChannelIdStr.trim().isEmpty()) {
        sendJsonResponse(handler, "responderInvitacion", false, "El ID del canal es requerido",
            createErrorData("channelId", "Campo requerido"));
        return;
    }

    if (accepted == null) {
        sendJsonResponse(handler, "responderInvitacion", false, "Debes indicar si aceptas o rechazas la invitación",
            createErrorData("accepted", "Campo requerido"));
        return;
    }

    try {
        // 4. Convertir a UUID
        UUID respondChannelId = UUID.fromString(respondChannelIdStr);

        // 5. Obtener ID del usuario autenticado
        UUID userId = handler.getAuthenticatedUser().getUserId();

        // 6. Crear DTO de request
        RespondToInviteRequestDto respondDto = new RespondToInviteRequestDto(respondChannelId, accepted);

        // 7. Llamar a la fachada
        chatFachada.responderInvitacion(respondDto, userId);

        // 8. Obtener información del canal para la respuesta (opcional)
        // Aquí podrías buscar el canal si necesitas más detalles

        // 9. Construir respuesta exitosa
        Map<String, Object> respondResponseData = new HashMap<>();
        respondResponseData.put("channelId", respondChannelIdStr);
        respondResponseData.put("accepted", accepted);

        String message = accepted ? 
            "Invitación aceptada. Ahora eres miembro del canal" : 
            "Invitación rechazada";

        sendJsonResponse(handler, "responderInvitacion", true, message, respondResponseData);

    } catch (IllegalArgumentException e) {
        // Error de validación
        String errorMessage = e.getMessage();
        String campo = "general";
        
        if (errorMessage.contains("Canal")) {
            campo = "channelId";
        } else if (errorMessage.contains("invitación")) {
            campo = "invitación";
        }
        
        sendJsonResponse(handler, "responderInvitacion", false, errorMessage,
            createErrorData(campo, errorMessage));
            
    } catch (Exception e) {
        // Error inesperado
        System.err.println("Error al responder invitación: " + e.getMessage());
        e.printStackTrace();
        sendJsonResponse(handler, "responderInvitacion", false, "Error interno del servidor al responder invitación", null);
    }
    break;
```

### **PASO 4: Compilar y Probar**

```bash
cd Server-Nicolas
mvn clean install -DskipTests
```

---

## ✅ CHECKLIST - FUNCIONALIDAD 2

- [ ] `RespondToInviteRequestDto` existe y tiene todos los campos
- [ ] `ChannelServiceImpl.responderInvitacion()` existe y está completo
- [ ] Validación de invitación pendiente implementada
- [ ] Lógica de aceptar (cambiar a ACTIVO) implementada
- [ ] Lógica de rechazar (eliminar membresía) implementada
- [ ] `ChatFachadaImpl.responderInvitacion()` existe y delega correctamente
- [ ] Caso `"responderinvitacion"` agregado en `RequestDispatcher.dispatch()`
- [ ] Validaciones de campos requeridos implementadas
- [ ] Manejo de errores específicos
- [ ] Proyecto compila sin errores
- [ ] Prueba manual exitosa (aceptar invitación)
- [ ] Prueba manual exitosa (rechazar invitación)
- [ ] Verificación en BD (estado cambia a ACTIVO o se elimina)
- [ ] Usuario puede enviar mensajes después de aceptar

---


# FUNCIONALIDAD 3: VER INVITACIONES PENDIENTES

## 📊 ANÁLISIS PREVIO

### **Estado Actual**

✅ **Lo que YA existe:**
```java
// 1. Servicio con lógica
Server-Nicolas/negocio/server-LogicaCanales/src/main/java/com/arquitectura/logicaCanales/ChannelServiceImpl.java
    → List<ChannelResponseDto> getPendingInvitationsForUser(UUID userId)

// 2. Fachada con método
Server-Nicolas/negocio/server-logicaFachada/src/main/java/com/arquitectura/fachada/ChatFachadaImpl.java
    → List<ChannelResponseDto> getPendingInvitationsForUser(UUID userId)
```

❌ **Lo que FALTA:**
```java
// 1. Endpoint en RequestDispatcher
case "obtenerinvitaciones": // NO EXISTE
```

### **Flujo Esperado**

```
Cliente envía:
{
  "action": "obtenerInvitaciones",
  "payload": {
    "usuarioId": "uuid-del-usuario"
  }
}

Servidor:
1. Valida que el usuario esté autenticado
2. Valida que el usuario autenticado coincida con el solicitante
3. Busca todas las membresías con estado PENDIENTE del usuario
4. Convierte a ChannelResponseDto
5. Retorna lista de canales con invitaciones pendientes

Servidor responde:
{
  "action": "obtenerInvitaciones",
  "status": "success",
  "message": "Invitaciones obtenidas",
  "data": {
    "invitaciones": [
      {
        "channelId": "uuid-1",
        "channelName": "Canal de Trabajo",
        "channelType": "GRUPO",
        "owner": {
          "userId": "uuid-owner",
          "username": "jefe"
        },
        "peerId": "uuid-peer"
      },
      {
        "channelId": "uuid-2",
        "channelName": "Canal de Amigos",
        "channelType": "GRUPO",
        "owner": {
          "userId": "uuid-owner-2",
          "username": "amigo"
        },
        "peerId": "uuid-peer"
      }
    ],
    "totalInvitaciones": 2
  }
}
```

---

## 🔧 IMPLEMENTACIÓN PASO A PASO

### **PASO 1: Verificar ChannelServiceImpl.getPendingInvitationsForUser()**

**Ubicación**: `Server-Nicolas/negocio/server-LogicaCanales/src/main/java/com/arquitectura/logicaCanales/ChannelServiceImpl.java`

**Verificar que el método haga:**
1. ✅ Buscar membresías con estado PENDIENTE del usuario
2. ✅ Obtener los canales de esas membresías
3. ✅ Convertir a ChannelResponseDto
4. ✅ Retornar lista

### **PASO 2: Agregar el Endpoint en RequestDispatcher**

**Ubicación**: `Server-Nicolas/transporte/server-controladorTransporte/src/main/java/com/arquitectura/controlador/RequestDispatcher.java`

**Agregar después de "responderinvitacion":**

```java
case "obtenerinvitaciones":
case "listarinvitaciones":
case "invitacionespendientes":
    // 1. Extraer payload
    Object invitacionesDataObj = request.getPayload();
    if (invitacionesDataObj == null) {
        sendJsonResponse(handler, "obtenerInvitaciones", false, "Falta payload", null);
        return;
    }

    // 2. Convertir a JSON y extraer campos
    JsonObject invitacionesJson = gson.toJsonTree(invitacionesDataObj).getAsJsonObject();
    String invitUsuarioIdStr = invitacionesJson.has("usuarioId") ? invitacionesJson.get("usuarioId").getAsString() : null;

    // 3. Validar campos requeridos
    if (invitUsuarioIdStr == null || invitUsuarioIdStr.trim().isEmpty()) {
        sendJsonResponse(handler, "obtenerInvitaciones", false, "El ID del usuario es requerido",
            createErrorData("usuarioId", "Campo requerido"));
        return;
    }

    try {
        // 4. Convertir a UUID
        UUID invitUsuarioId = UUID.fromString(invitUsuarioIdStr);

        // 5. Validar que el usuario autenticado coincida con el solicitante (seguridad)
        if (!handler.getAuthenticatedUser().getUserId().equals(invitUsuarioId)) {
            sendJsonResponse(handler, "obtenerInvitaciones", false, "No autorizado para ver estas invitaciones",
                createErrorData("permisos", "Usuario no autorizado"));
            return;
        }

        // 6. Llamar a la fachada
        List<ChannelResponseDto> invitaciones = chatFachada.getPendingInvitationsForUser(invitUsuarioId);

        // 7. Construir lista de invitaciones para la respuesta
        List<Map<String, Object>> invitacionesData = new ArrayList<>();
        
        for (ChannelResponseDto canal : invitaciones) {
            Map<String, Object> canalMap = new HashMap<>();
            canalMap.put("channelId", canal.getChannelId().toString());
            canalMap.put("channelName", canal.getChannelName());
            canalMap.put("channelType", canal.getChannelType());
            
            if (canal.getOwner() != null) {
                canalMap.put("owner", Map.of(
                    "userId", canal.getOwner().getUserId().toString(),
                    "username", canal.getOwner().getUsername()
                ));
            }
            
            if (canal.getPeerId() != null) {
                canalMap.put("peerId", canal.getPeerId().toString());
            }
            
            invitacionesData.add(canalMap);
        }

        // 8. Construir respuesta exitosa
        Map<String, Object> invitacionesResponseData = new HashMap<>();
        invitacionesResponseData.put("invitaciones", invitacionesData);
        invitacionesResponseData.put("totalInvitaciones", invitaciones.size());

        sendJsonResponse(handler, "obtenerInvitaciones", true, "Invitaciones obtenidas", invitacionesResponseData);

    } catch (IllegalArgumentException e) {
        // Error de validación
        String errorMessage = e.getMessage();
        sendJsonResponse(handler, "obtenerInvitaciones", false, errorMessage,
            createErrorData("general", errorMessage));
            
    } catch (Exception e) {
        // Error inesperado
        System.err.println("Error al obtener invitaciones: " + e.getMessage());
        e.printStackTrace();
        sendJsonResponse(handler, "obtenerInvitaciones", false, "Error interno del servidor al obtener invitaciones", null);
    }
    break;
```

### **PASO 3: Compilar y Probar**

```bash
cd Server-Nicolas
mvn clean install -DskipTests
```

---

## ✅ CHECKLIST - FUNCIONALIDAD 3

- [ ] `ChannelServiceImpl.getPendingInvitationsForUser()` existe y está completo
- [ ] Búsqueda de membresías PENDIENTES implementada
- [ ] Conversión a ChannelResponseDto implementada
- [ ] `ChatFachadaImpl.getPendingInvitationsForUser()` existe y delega correctamente
- [ ] Caso `"obtenerinvitaciones"` agregado en `RequestDispatcher.dispatch()`
- [ ] Validaciones de campos requeridos implementadas
- [ ] Validación de autorización (usuario autenticado = solicitante)
- [ ] Construcción de respuesta con lista de invitaciones
- [ ] Manejo de errores específicos
- [ ] Proyecto compila sin errores
- [ ] Prueba manual exitosa (obtener invitaciones)
- [ ] Prueba manual exitosa (lista vacía si no hay invitaciones)
- [ ] Verificación en BD (solo invitaciones PENDIENTES)

---


# FUNCIONALIDAD 4: VALIDAR PERMISOS EN CANALES

## 📊 ANÁLISIS PREVIO

### **Estado Actual**

Esta funcionalidad es **transversal** y afecta a múltiples endpoints existentes.

✅ **Lo que YA existe:**
```java
// Validaciones básicas en algunos métodos:
- ChannelServiceImpl.invitarMiembro() valida que sea el owner
- MessageServiceImpl.enviarMensajeTexto() valida que sea miembro
```

❌ **Lo que FALTA:**
```java
// 1. Sistema centralizado de validación de permisos
// 2. Validaciones consistentes en todos los endpoints de canal
// 3. Manejo de roles (OWNER vs MIEMBRO)
```

### **Permisos por Rol**

#### **OWNER/ADMIN**
- ✅ Invitar miembros al canal
- ✅ Eliminar miembros del canal (futuro)
- ✅ Modificar información del canal (futuro)
- ✅ Eliminar el canal (futuro)
- ✅ Enviar mensajes
- ✅ Ver historial
- ✅ Ver miembros

#### **MIEMBRO**
- ❌ NO puede invitar miembros
- ❌ NO puede eliminar miembros
- ❌ NO puede modificar el canal
- ❌ NO puede eliminar el canal
- ✅ Enviar mensajes
- ✅ Ver historial
- ✅ Ver miembros
- ✅ Salir del canal (futuro)

---

## 🔧 IMPLEMENTACIÓN PASO A PASO

### **PASO 1: Crear Clase de Utilidad para Permisos**

**Ubicación**: `Server-Nicolas/comunes/server-Utils/src/main/java/com/arquitectura/utils/permissions/ChannelPermissionValidator.java`

**Crear nueva clase:**

```java
package com.arquitectura.utils.permissions;

import com.arquitectura.domain.Channel;
import com.arquitectura.domain.MembresiaCanal;
import com.arquitectura.domain.enums.EstadoMembresia;
import com.arquitectura.persistence.repository.ChannelRepository;
import com.arquitectura.persistence.repository.MembresiaCanalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ChannelPermissionValidator {

    private final ChannelRepository channelRepository;
    private final MembresiaCanalRepository membresiaCanalRepository;

    @Autowired
    public ChannelPermissionValidator(ChannelRepository channelRepository, 
                                     MembresiaCanalRepository membresiaCanalRepository) {
        this.channelRepository = channelRepository;
        this.membresiaCanalRepository = membresiaCanalRepository;
    }

    /**
     * Valida si un usuario es el owner de un canal
     */
    public boolean isOwner(UUID channelId, UUID userId) {
        return channelRepository.findById(channelId)
                .map(channel -> channel.getOwner().getUserId().equals(userId))
                .orElse(false);
    }

    /**
     * Valida si un usuario es miembro activo de un canal
     */
    public boolean isActiveMember(UUID channelId, UUID userId) {
        return membresiaCanalRepository
                .findAllByUsuarioUserIdAndEstado(userId, EstadoMembresia.ACTIVO)
                .stream()
                .anyMatch(m -> m.getCanal().getChannelId().equals(channelId));
    }

    /**
     * Valida si un usuario puede invitar miembros (solo owner)
     */
    public void validateCanInviteMembers(UUID channelId, UUID userId) {
        if (!isOwner(channelId, userId)) {
            throw new IllegalArgumentException("Solo el propietario del canal puede invitar miembros");
        }
    }

    /**
     * Valida si un usuario puede enviar mensajes (debe ser miembro activo)
     */
    public void validateCanSendMessages(UUID channelId, UUID userId) {
        if (!isActiveMember(channelId, userId)) {
            throw new IllegalArgumentException("No eres miembro de este canal");
        }
    }

    /**
     * Valida si un usuario puede ver el historial (debe ser miembro activo)
     */
    public void validateCanViewHistory(UUID channelId, UUID userId) {
        if (!isActiveMember(channelId, userId)) {
            throw new IllegalArgumentException("No eres miembro de este canal");
        }
    }

    /**
     * Valida si un usuario puede ver los miembros (debe ser miembro activo)
     */
    public void validateCanViewMembers(UUID channelId, UUID userId) {
        if (!isActiveMember(channelId, userId)) {
            throw new IllegalArgumentException("No eres miembro de este canal");
        }
    }

    /**
     * Obtiene el rol de un usuario en un canal
     */
    public String getUserRole(UUID channelId, UUID userId) {
        if (isOwner(channelId, userId)) {
            return "ADMIN";
        } else if (isActiveMember(channelId, userId)) {
            return "MIEMBRO";
        } else {
            return "NO_MIEMBRO";
        }
    }
}
```

### **PASO 2: Refactorizar ChannelServiceImpl para Usar el Validador**

**Ubicación**: `Server-Nicolas/negocio/server-LogicaCanales/src/main/java/com/arquitectura/logicaCanales/ChannelServiceImpl.java`

**Agregar el validador como dependencia:**

```java
@Service
public class ChannelServiceImpl implements IChannelService {

    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;
    private final MembresiaCanalRepository membresiaCanalRepository;
    private final PeerRepository peerRepository;
    private final NetworkUtils networkUtils;
    private final ApplicationEventPublisher eventPublisher;
    private final ChannelPermissionValidator permissionValidator; // NUEVO

    @Autowired
    public ChannelServiceImpl(ChannelRepository channelRepository, 
                              UserRepository userRepository,
                              MembresiaCanalRepository membresiaCanalRepository, 
                              PeerRepository peerRepository,
                              NetworkUtils networkUtils, 
                              ApplicationEventPublisher eventPublisher,
                              ChannelPermissionValidator permissionValidator) { // NUEVO
        this.channelRepository = channelRepository;
        this.userRepository = userRepository;
        this.membresiaCanalRepository = membresiaCanalRepository;
        this.peerRepository = peerRepository;
        this.networkUtils = networkUtils;
        this.eventPublisher = eventPublisher;
        this.permissionValidator = permissionValidator; // NUEVO
    }

    // ... resto del código
}
```

**Refactorizar método `invitarMiembro()`:**

```java
@Override
@Transactional
public void invitarMiembro(InviteMemberRequestDto requestDto, UUID ownerId) throws Exception {
    Channel channel = channelRepository.findById(requestDto.getChannelId())
            .orElseThrow(() -> new Exception("Canal no encontrado."));

    // USAR EL VALIDADOR
    permissionValidator.validateCanInviteMembers(channel.getChannelId(), ownerId);

    if (channel.getTipo() != TipoCanal.GRUPO) {
        throw new Exception("Solo se pueden enviar invitaciones a canales de tipo GRUPO.");
    }

    // ... resto del código
}
```

**Refactorizar método `obtenerMiembrosDeCanal()`:**

```java
@Override
@Transactional(readOnly = true)
public List<UserResponseDto> obtenerMiembrosDeCanal(UUID canalId, UUID solicitanteId) throws Exception {
    Channel channel = channelRepository.findById(canalId)
            .orElseThrow(() -> new IllegalArgumentException("Canal no encontrado"));

    // USAR EL VALIDADOR
    permissionValidator.validateCanViewMembers(canalId, solicitanteId);

    // ... resto del código
}
```

### **PASO 3: Refactorizar MessageServiceImpl para Usar el Validador**

**Ubicación**: `Server-Nicolas/negocio/server-LogicaMensajes/src/main/java/com/arquitectura/logicaMensajes/MessageServiceImpl.java`

**Agregar el validador como dependencia y refactorizar:**

```java
@Service
public class MessageServiceImpl implements IMessageService {

    // ... otras dependencias
    private final ChannelPermissionValidator permissionValidator; // NUEVO

    @Autowired
    public MessageServiceImpl(/* otras dependencias */,
                             ChannelPermissionValidator permissionValidator) { // NUEVO
        // ... asignaciones
        this.permissionValidator = permissionValidator;
    }

    @Override
    @Transactional
    public MessageResponseDto enviarMensajeTexto(SendMessageRequestDto requestDto, UUID autorId) throws Exception {
        User author = userRepository.findById(autorId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Channel channel = channelRepository.findById(requestDto.getChannelId())
                .orElseThrow(() -> new IllegalArgumentException("Canal no encontrado"));

        // USAR EL VALIDADOR
        permissionValidator.validateCanSendMessages(channel.getChannelId(), autorId);

        // ... resto del código (eliminar la validación manual anterior)
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponseDto> obtenerMensajesPorCanal(UUID canalId, UUID userId) throws Exception {
        Channel channel = channelRepository.findById(canalId)
                .orElseThrow(() -> new IllegalArgumentException("Canal no encontrado"));

        // USAR EL VALIDADOR
        permissionValidator.validateCanViewHistory(canalId, userId);

        // ... resto del código (eliminar la validación manual anterior)
    }
}
```

### **PASO 4: Agregar Endpoint para Verificar Permisos (Opcional)**

**Ubicación**: `Server-Nicolas/transporte/server-controladorTransporte/src/main/java/com/arquitectura/controlador/RequestDispatcher.java`

**Agregar después de "obtenerinvitaciones":**

```java
case "verificarpermisos":
case "obtenerrol":
    // 1. Extraer payload
    Object permisosDataObj = request.getPayload();
    if (permisosDataObj == null) {
        sendJsonResponse(handler, "verificarPermisos", false, "Falta payload", null);
        return;
    }

    // 2. Convertir a JSON y extraer campos
    JsonObject permisosJson = gson.toJsonTree(permisosDataObj).getAsJsonObject();
    String permCanalIdStr = permisosJson.has("channelId") ? permisosJson.get("channelId").getAsString() : null;

    // 3. Validar campos requeridos
    if (permCanalIdStr == null || permCanalIdStr.trim().isEmpty()) {
        sendJsonResponse(handler, "verificarPermisos", false, "El ID del canal es requerido",
            createErrorData("channelId", "Campo requerido"));
        return;
    }

    try {
        // 4. Convertir a UUID
        UUID permCanalId = UUID.fromString(permCanalIdStr);

        // 5. Obtener ID del usuario autenticado
        UUID userId = handler.getAuthenticatedUser().getUserId();

        // 6. Obtener rol del usuario (necesitarás agregar este método en la fachada)
        String rol = chatFachada.getUserRoleInChannel(permCanalId, userId);

        // 7. Construir respuesta exitosa
        Map<String, Object> permisosResponseData = new HashMap<>();
        permisosResponseData.put("channelId", permCanalIdStr);
        permisosResponseData.put("userId", userId.toString());
        permisosResponseData.put("rol", rol);
        permisosResponseData.put("canInviteMembers", "ADMIN".equals(rol));
        permisosResponseData.put("canSendMessages", "ADMIN".equals(rol) || "MIEMBRO".equals(rol));
        permisosResponseData.put("canViewHistory", "ADMIN".equals(rol) || "MIEMBRO".equals(rol));
        permisosResponseData.put("canViewMembers", "ADMIN".equals(rol) || "MIEMBRO".equals(rol));

        sendJsonResponse(handler, "verificarPermisos", true, "Permisos obtenidos", permisosResponseData);

    } catch (Exception e) {
        System.err.println("Error al verificar permisos: " + e.getMessage());
        e.printStackTrace();
        sendJsonResponse(handler, "verificarPermisos", false, "Error interno del servidor al verificar permisos", null);
    }
    break;
```

### **PASO 5: Compilar y Probar**

```bash
cd Server-Nicolas
mvn clean install -DskipTests
```

---

## ✅ CHECKLIST - FUNCIONALIDAD 4

- [ ] Clase `ChannelPermissionValidator` creada
- [ ] Método `isOwner()` implementado
- [ ] Método `isActiveMember()` implementado
- [ ] Método `validateCanInviteMembers()` implementado
- [ ] Método `validateCanSendMessages()` implementado
- [ ] Método `validateCanViewHistory()` implementado
- [ ] Método `validateCanViewMembers()` implementado
- [ ] Método `getUserRole()` implementado
- [ ] `ChannelServiceImpl` refactorizado para usar el validador
- [ ] `MessageServiceImpl` refactorizado para usar el validador
- [ ] Endpoint `"verificarpermisos"` agregado (opcional)
- [ ] Proyecto compila sin errores
- [ ] Prueba: Solo owner puede invitar miembros
- [ ] Prueba: Solo miembros pueden enviar mensajes
- [ ] Prueba: Solo miembros pueden ver historial
- [ ] Prueba: Solo miembros pueden ver lista de miembros
- [ ] Mensajes de error son claros y específicos

---


# TESTING Y VALIDACIÓN

## 🧪 PLAN DE PRUEBAS COMPLETO

### **Preparación del Entorno de Pruebas**

#### **1. Iniciar Base de Datos y Servidor**

```bash
cd Server-Nicolas
docker-compose up -d
java -jar comunes/server-app/target/server-app-1.0-SNAPSHOT-jar-with-dependencies.jar
```

#### **2. Crear Usuarios de Prueba**

```json
{"action":"registerUser","payload":{"username":"owner","email":"owner@test.com","password":"123456"}}
{"action":"registerUser","payload":{"username":"member1","email":"member1@test.com","password":"123456"}}
{"action":"registerUser","payload":{"username":"member2","email":"member2@test.com","password":"123456"}}
```

#### **3. Autenticar Usuarios**

```json
{"action":"authenticateUser","payload":{"nombreUsuario":"owner","password":"123456"}}
{"action":"authenticateUser","payload":{"nombreUsuario":"member1","password":"123456"}}
```

#### **4. Crear Canal de Prueba**

```json
{"action":"crearCanal","payload":{"nombre":"Canal Test","tipo":"GRUPO"}}
```

---

## 📝 CASOS DE PRUEBA

### **FUNCIONALIDAD 1: INVITAR MIEMBRO A CANAL**

#### **Caso 1.1: Invitación Exitosa (Owner)**

**Preparación**: Owner autenticado

**Entrada**:
```json
{
  "action": "invitarMiembro",
  "payload": {
    "channelId": "uuid-del-canal",
    "userIdToInvite": "uuid-member1"
  }
}
```

**Salida Esperada**:
```json
{
  "action": "invitarMiembro",
  "status": "success",
  "message": "Invitación enviada exitosamente",
  "data": {
    "channelId": "uuid-del-canal",
    "invitedUserId": "uuid-member1",
    "invitedUsername": "member1"
  }
}
```

**Verificación en BD**:
```sql
SELECT * FROM membresia_canal 
WHERE channel_id = 'uuid-del-canal' AND user_id = 'uuid-member1';
-- Debe tener estado = 'PENDIENTE'
```

---

#### **Caso 1.2: Usuario No Owner Intenta Invitar**

**Preparación**: member1 autenticado (no es owner)

**Entrada**:
```json
{
  "action": "invitarMiembro",
  "payload": {
    "channelId": "uuid-del-canal",
    "userIdToInvite": "uuid-member2"
  }
}
```

**Salida Esperada**:
```json
{
  "action": "invitarMiembro",
  "status": "error",
  "message": "Solo el propietario del canal puede enviar invitaciones",
  "data": {
    "campo": "permisos",
    "motivo": "Solo el propietario del canal puede enviar invitaciones"
  }
}
```

---

#### **Caso 1.3: Invitar Usuario Ya Invitado**

**Preparación**: Ya existe invitación pendiente

**Salida Esperada**:
```json
{
  "action": "invitarMiembro",
  "status": "error",
  "message": "El usuario ya es miembro o tiene una invitación pendiente",
  "data": {
    "campo": "membresía",
    "motivo": "El usuario ya es miembro o tiene una invitación pendiente"
  }
}
```

---

#### **Caso 1.4: Notificación Push al Usuario Invitado**

**Preparación**: member1 conectado y recibe invitación

**Notificación Esperada**:
```json
{
  "action": "nuevaInvitacion",
  "status": "success",
  "message": "Has recibido una invitación a un canal",
  "data": {
    "channelId": "uuid-del-canal",
    "channelName": "Canal Test",
    "channelType": "GRUPO",
    "owner": {
      "userId": "uuid-owner",
      "username": "owner"
    }
  }
}
```

---

### **FUNCIONALIDAD 2: RESPONDER INVITACIÓN**

#### **Caso 2.1: Aceptar Invitación**

**Preparación**: member1 tiene invitación pendiente

**Entrada**:
```json
{
  "action": "responderInvitacion",
  "payload": {
    "channelId": "uuid-del-canal",
    "accepted": true
  }
}
```

**Salida Esperada**:
```json
{
  "action": "responderInvitacion",
  "status": "success",
  "message": "Invitación aceptada. Ahora eres miembro del canal",
  "data": {
    "channelId": "uuid-del-canal",
    "accepted": true
  }
}
```

**Verificación en BD**:
```sql
SELECT * FROM membresia_canal 
WHERE channel_id = 'uuid-del-canal' AND user_id = 'uuid-member1';
-- Debe tener estado = 'ACTIVO'
```

---

#### **Caso 2.2: Rechazar Invitación**

**Preparación**: member2 tiene invitación pendiente

**Entrada**:
```json
{
  "action": "responderInvitacion",
  "payload": {
    "channelId": "uuid-del-canal",
    "accepted": false
  }
}
```

**Salida Esperada**:
```json
{
  "action": "responderInvitacion",
  "status": "success",
  "message": "Invitación rechazada",
  "data": {
    "channelId": "uuid-del-canal",
    "accepted": false
  }
}
```

**Verificación en BD**:
```sql
SELECT * FROM membresia_canal 
WHERE channel_id = 'uuid-del-canal' AND user_id = 'uuid-member2';
-- NO debe existir (fue eliminada)
```

---

#### **Caso 2.3: Responder Invitación Inexistente**

**Preparación**: Usuario sin invitación pendiente

**Salida Esperada**:
```json
{
  "action": "responderInvitacion",
  "status": "error",
  "message": "No se encontró una invitación para este usuario en este canal",
  "data": {
    "campo": "invitación",
    "motivo": "No se encontró una invitación para este usuario en este canal"
  }
}
```

---

### **FUNCIONALIDAD 3: VER INVITACIONES PENDIENTES**

#### **Caso 3.1: Obtener Invitaciones Pendientes**

**Preparación**: member1 tiene 2 invitaciones pendientes

**Entrada**:
```json
{
  "action": "obtenerInvitaciones",
  "payload": {
    "usuarioId": "uuid-member1"
  }
}
```

**Salida Esperada**:
```json
{
  "action": "obtenerInvitaciones",
  "status": "success",
  "message": "Invitaciones obtenidas",
  "data": {
    "invitaciones": [
      {
        "channelId": "uuid-canal-1",
        "channelName": "Canal Test 1",
        "channelType": "GRUPO",
        "owner": {
          "userId": "uuid-owner-1",
          "username": "owner1"
        }
      },
      {
        "channelId": "uuid-canal-2",
        "channelName": "Canal Test 2",
        "channelType": "GRUPO",
        "owner": {
          "userId": "uuid-owner-2",
          "username": "owner2"
        }
      }
    ],
    "totalInvitaciones": 2
  }
}
```

---

#### **Caso 3.2: Sin Invitaciones Pendientes**

**Salida Esperada**:
```json
{
  "action": "obtenerInvitaciones",
  "status": "success",
  "message": "Invitaciones obtenidas",
  "data": {
    "invitaciones": [],
    "totalInvitaciones": 0
  }
}
```

---

### **FUNCIONALIDAD 4: VALIDAR PERMISOS EN CANALES**

#### **Caso 4.1: Owner Puede Invitar**

**Preparación**: owner autenticado

**Resultado**: ✅ Invitación exitosa (ver Caso 1.1)

---

#### **Caso 4.2: Miembro NO Puede Invitar**

**Preparación**: member1 autenticado (miembro activo)

**Resultado**: ❌ Error de permisos (ver Caso 1.2)

---

#### **Caso 4.3: Solo Miembros Pueden Enviar Mensajes**

**Preparación**: Usuario no miembro intenta enviar mensaje

**Entrada**:
```json
{
  "action": "enviarMensajeCanal",
  "payload": {
    "canalId": "uuid-del-canal",
    "contenido": "Hola!"
  }
}
```

**Salida Esperada**:
```json
{
  "action": "enviarMensajeCanal",
  "status": "error",
  "message": "No eres miembro de este canal",
  "data": {
    "campo": "permisos",
    "motivo": "No eres miembro de este canal"
  }
}
```

---

#### **Caso 4.4: Verificar Rol de Usuario**

**Entrada**:
```json
{
  "action": "verificarPermisos",
  "payload": {
    "channelId": "uuid-del-canal"
  }
}
```

**Salida Esperada (Owner)**:
```json
{
  "action": "verificarPermisos",
  "status": "success",
  "message": "Permisos obtenidos",
  "data": {
    "channelId": "uuid-del-canal",
    "userId": "uuid-owner",
    "rol": "ADMIN",
    "canInviteMembers": true,
    "canSendMessages": true,
    "canViewHistory": true,
    "canViewMembers": true
  }
}
```

**Salida Esperada (Miembro)**:
```json
{
  "action": "verificarPermisos",
  "status": "success",
  "message": "Permisos obtenidos",
  "data": {
    "channelId": "uuid-del-canal",
    "userId": "uuid-member1",
    "rol": "MIEMBRO",
    "canInviteMembers": false,
    "canSendMessages": true,
    "canViewHistory": true,
    "canViewMembers": true
  }
}
```

---

## 🔄 PRUEBAS DE INTEGRACIÓN

### **Flujo Completo: Invitar → Aceptar → Enviar Mensaje**

#### **Paso 1: Owner Invita a Member1**
```json
{"action":"invitarMiembro","payload":{"channelId":"uuid-canal","userIdToInvite":"uuid-member1"}}
```

#### **Paso 2: Member1 Ve Sus Invitaciones**
```json
{"action":"obtenerInvitaciones","payload":{"usuarioId":"uuid-member1"}}
```

#### **Paso 3: Member1 Acepta la Invitación**
```json
{"action":"responderInvitacion","payload":{"channelId":"uuid-canal","accepted":true}}
```

#### **Paso 4: Member1 Envía un Mensaje**
```json
{"action":"enviarMensajeCanal","payload":{"canalId":"uuid-canal","contenido":"Hola a todos!"}}
```

#### **Paso 5: Member1 Ve el Historial**
```json
{"action":"solicitarHistorialCanal","payload":{"canalId":"uuid-canal","usuarioId":"uuid-member1"}}
```

#### **Paso 6: Member1 Ve los Miembros**
```json
{"action":"listarMiembros","payload":{"canalId":"uuid-canal","solicitanteId":"uuid-member1"}}
```

---

## ✅ CHECKLIST DE VALIDACIÓN FINAL

### **Compilación y Ejecución**
- [ ] Proyecto compila sin errores
- [ ] Servidor inicia correctamente
- [ ] MySQL está corriendo
- [ ] Servidor escucha en puerto 22100

### **Funcionalidad 1: Invitar Miembro**
- [ ] Owner puede invitar miembros
- [ ] Miembro NO puede invitar
- [ ] No se puede invitar a usuario ya invitado
- [ ] No se puede invitar a usuario ya miembro
- [ ] Invitación se guarda con estado PENDIENTE
- [ ] Usuario invitado recibe notificación push

### **Funcionalidad 2: Responder Invitación**
- [ ] Aceptar invitación cambia estado a ACTIVO
- [ ] Rechazar invitación elimina la membresía
- [ ] No se puede responder invitación inexistente
- [ ] Usuario puede enviar mensajes después de aceptar

### **Funcionalidad 3: Ver Invitaciones**
- [ ] Obtener invitaciones funciona
- [ ] Lista vacía si no hay invitaciones
- [ ] Solo se muestran invitaciones PENDIENTES
- [ ] Usuario solo ve sus propias invitaciones

### **Funcionalidad 4: Validar Permisos**
- [ ] Solo owner puede invitar miembros
- [ ] Solo miembros pueden enviar mensajes
- [ ] Solo miembros pueden ver historial
- [ ] Solo miembros pueden ver lista de miembros
- [ ] Roles se asignan correctamente (ADMIN/MIEMBRO)
- [ ] Mensajes de error son claros

### **Integración**
- [ ] Flujo completo funciona sin errores
- [ ] Notificaciones push funcionan
- [ ] Base de datos refleja los cambios
- [ ] Logs del servidor son claros

---


# CHECKLIST FINAL - PRIORIDAD 2

## 📋 RESUMEN DE IMPLEMENTACIÓN

### **Archivos Modificados/Creados**

#### **Nuevos Archivos**
- [ ] `Server-Nicolas/comunes/server-Utils/src/main/java/com/arquitectura/utils/permissions/ChannelPermissionValidator.java` (NUEVO)

#### **DTOs** (verificar que existan)
- [ ] `Server-Nicolas/comunes/Server-DTO/src/main/java/com/arquitectura/DTO/canales/InviteMemberRequestDto.java`
- [ ] `Server-Nicolas/comunes/Server-DTO/src/main/java/com/arquitectura/DTO/canales/RespondToInviteRequestDto.java`

#### **Servicios**
- [ ] `Server-Nicolas/negocio/server-LogicaCanales/src/main/java/com/arquitectura/logicaCanales/ChannelServiceImpl.java`
  - Método `invitarMiembro()` verificado/refactorizado
  - Método `responderInvitacion()` verificado
  - Método `getPendingInvitationsForUser()` verificado
  - Método `obtenerMiembrosDeCanal()` refactorizado
  
- [ ] `Server-Nicolas/negocio/server-LogicaMensajes/src/main/java/com/arquitectura/logicaMensajes/MessageServiceImpl.java`
  - Método `enviarMensajeTexto()` refactorizado
  - Método `obtenerMensajesPorCanal()` refactorizado

#### **Fachada**
- [ ] `Server-Nicolas/negocio/server-logicaFachada/src/main/java/com/arquitectura/fachada/ChatFachadaImpl.java`
  - Método `invitarMiembro()` verificado
  - Método `responderInvitacion()` verificado
  - Método `getPendingInvitationsForUser()` verificado
  - Método `getUserRoleInChannel()` agregado (opcional)

#### **Controlador**
- [ ] `Server-Nicolas/transporte/server-controladorTransporte/src/main/java/com/arquitectura/controlador/RequestDispatcher.java`
  - Caso `"invitarmiembro"` agregado
  - Caso `"responderinvitacion"` agregado
  - Caso `"obtenerinvitaciones"` agregado
  - Caso `"verificarpermisos"` agregado (opcional)

#### **Eventos**
- [ ] `Server-Nicolas/transporte/server-Transporte/src/main/java/com/arquitectura/transporte/ServerListener.java`
  - Método `handleUserInvitedEvent()` verificado

---

## 🎯 FUNCIONALIDADES IMPLEMENTADAS

### **✅ Funcionalidad 1: Invitar Miembro a Canal**
- [ ] Endpoint `invitarmiembro` funcional
- [ ] Validación de owner implementada
- [ ] Validación de tipo de canal (solo GRUPO)
- [ ] Validación de membresía previa
- [ ] Creación de membresía con estado PENDIENTE
- [ ] Publicación de UserInvitedEvent
- [ ] Notificación push al usuario invitado
- [ ] Respuestas de error descriptivas

### **✅ Funcionalidad 2: Responder Invitación**
- [ ] Endpoint `responderinvitacion` funcional
- [ ] Validación de invitación pendiente
- [ ] Lógica de aceptar (cambiar a ACTIVO)
- [ ] Lógica de rechazar (eliminar membresía)
- [ ] Respuestas de error descriptivas

### **✅ Funcionalidad 3: Ver Invitaciones Pendientes**
- [ ] Endpoint `obtenerinvitaciones` funcional
- [ ] Búsqueda de membresías PENDIENTES
- [ ] Conversión a ChannelResponseDto
- [ ] Validación de autorización
- [ ] Respuestas de error descriptivas

### **✅ Funcionalidad 4: Validar Permisos en Canales**
- [ ] Clase `ChannelPermissionValidator` creada
- [ ] Validación de owner implementada
- [ ] Validación de miembro activo implementada
- [ ] Métodos de validación específicos
- [ ] Integración en ChannelServiceImpl
- [ ] Integración en MessageServiceImpl
- [ ] Endpoint `verificarpermisos` funcional (opcional)
- [ ] Mensajes de error claros y específicos

---

## 📊 MÉTRICAS DE IMPLEMENTACIÓN

### **Líneas de Código Agregadas** (aproximado)
- ChannelPermissionValidator: ~150 líneas
- Endpoints en RequestDispatcher: ~300 líneas
- Refactorizaciones: ~50 líneas
- **Total**: ~500 líneas

### **Endpoints Agregados**
- `invitarmiembro` (autenticado, solo owner)
- `responderinvitacion` (autenticado)
- `obtenerinvitaciones` (autenticado)
- `verificarpermisos` (autenticado, opcional)
- **Total**: 3-4 endpoints

### **Clases Nuevas**
- `ChannelPermissionValidator` (utilidad)
- **Total**: 1 clase

---

## 🚀 PRÓXIMOS PASOS

### **Prioridad 3: Mensajes Privados**
1. Crear/obtener canal directo
2. Enviar mensaje privado
3. Historial privado

### **Prioridad 4: Gestión Avanzada de Canales**
1. Crear canal (endpoint) - Ya existe, solo verificar
2. Eliminar miembro del canal
3. Salir del canal
4. Eliminar canal
5. Modificar información del canal

### **Mejoras Opcionales**
1. Roles personalizados (MODERADOR, etc.)
2. Permisos granulares por rol
3. Logs de auditoría de cambios en canales
4. Límite de miembros por canal
5. Canales privados vs públicos

---

## 📝 NOTAS IMPORTANTES

### **Seguridad**
- ✅ Validación de owner antes de invitar
- ✅ Validación de miembro antes de acciones
- ✅ Validación de autorización en endpoints
- ✅ Sistema centralizado de permisos
- ⚠️ No hay rate limiting en invitaciones (aceptable para proyecto académico)

### **Rendimiento**
- ✅ Consultas optimizadas con JPA
- ✅ Validaciones en memoria cuando es posible
- ✅ Uso de índices en BD
- ⚠️ Sin caché de permisos (aceptable para proyecto académico)

### **Arquitectura**
- ✅ Separación de responsabilidades clara
- ✅ Validador de permisos reutilizable
- ✅ Eventos para notificaciones
- ✅ DTOs para transferencia de datos
- ✅ Manejo de errores consistente

### **Buenas Prácticas**
- ✅ Validación de entrada
- ✅ Manejo de errores específicos
- ✅ Logs informativos
- ✅ Nombres descriptivos
- ✅ Código reutilizable (ChannelPermissionValidator)
- ✅ Transacciones en operaciones de BD

---

## 🎓 APRENDIZAJES DEL PROYECTO

### **Patrones de Diseño Aplicados**
1. **Validator Pattern**: ChannelPermissionValidator centraliza validaciones
2. **Observer Pattern**: Eventos de Spring para notificaciones
3. **Facade Pattern**: ChatFachadaImpl coordina servicios
4. **Repository Pattern**: Acceso a datos con Spring Data JPA
5. **DTO Pattern**: Transferencia de datos entre capas

### **Conceptos de Seguridad**
1. **Autorización basada en roles**: OWNER vs MIEMBRO
2. **Validación de permisos**: Antes de cada acción sensible
3. **Validación de identidad**: Usuario autenticado = solicitante
4. **Principio de mínimo privilegio**: Solo permisos necesarios

### **Gestión de Estados**
1. **Estados de membresía**: PENDIENTE → ACTIVO o eliminada
2. **Transiciones de estado**: Controladas y validadas
3. **Consistencia de datos**: Transacciones para operaciones críticas

---

## ✅ FIRMA DE COMPLETITUD

**Funcionalidades de Prioridad 2 Completadas**: 4/4

- [ ] Invitar Miembro a Canal
- [ ] Responder Invitación
- [ ] Ver Invitaciones Pendientes
- [ ] Validar Permisos en Canales

**Estado del Proyecto**: ⏳ **EN PROGRESO**

**Fecha de Completitud**: _________________

**Desarrollador**: _________________

**Revisor**: _________________

---

## 📚 REFERENCIAS

### **Documentación del Proyecto**
- `PLAN_IMPLEMENTACION_PRIORIDAD_1.md` - Funcionalidades básicas implementadas
- `ANALISIS_COMPLETO_PROYECTO.md` - Análisis exhaustivo del sistema
- `Server-Nicolas/PEER_IMPLEMENTATION_REVIEW.md` - Implementación de Peers

### **Código Fuente Clave**
- `Server-Nicolas/transporte/server-controladorTransporte/src/main/java/com/arquitectura/controlador/RequestDispatcher.java`
- `Server-Nicolas/negocio/server-logicaFachada/src/main/java/com/arquitectura/fachada/ChatFachadaImpl.java`
- `Server-Nicolas/negocio/server-LogicaCanales/src/main/java/com/arquitectura/logicaCanales/ChannelServiceImpl.java`
- `Server-Nicolas/transporte/server-Transporte/src/main/java/com/arquitectura/transporte/ServerListener.java`

### **Recursos Externos**
- Spring Framework: https://spring.io/projects/spring-framework
- Spring Data JPA: https://spring.io/projects/spring-data-jpa
- Spring Events: https://spring.io/guides/gs/messaging-with-redis/

---

**FIN DEL DOCUMENTO**

---
