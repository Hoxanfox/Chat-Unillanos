# 🎯 PLAN DE IMPLEMENTACIÓN - PRIORIDAD 4
## Extras y Mejoras

**Fecha de creación**: 5 de noviembre de 2025  
**Proyecto**: Chat-Unillanos - Servidor  
**Objetivo**: Implementar funcionalidades adicionales y mejoras de arquitectura

---

## 📋 ÍNDICE

1. [Visión General](#visión-general)
2. [Funcionalidad 1: Enviar Audio a Canal](#funcionalidad-1-enviar-audio-a-canal)
3. [Funcionalidad 2: Transcripciones Automáticas](#funcionalidad-2-transcripciones-automáticas)
4. [Funcionalidad 3: Refactorización de Controladores](#funcionalidad-3-refactorización-de-controladores)
5. [Testing y Validación](#testing-y-validación)
6. [Checklist Final](#checklist-final)

---

## 🎯 VISIÓN GENERAL

### **Estado Actual del Proyecto**

Después de completar las Prioridades 1, 2 y 3, el servidor ya tiene:
- ✅ Sistema completo de usuarios (registro, autenticación)
- ✅ Sistema de canales GRUPO (crear, invitar, gestionar)
- ✅ Sistema de mensajes de texto
- ✅ Sistema de mensajes privados (canales DIRECTO)
- ✅ Sistema de invitaciones y membresías
- ✅ Sistema de notificaciones push

### **¿Qué incluye la Prioridad 4?**

Esta prioridad incluye funcionalidades extras y mejoras de arquitectura:

1. **Enviar Audio a Canal**: Sistema completo de mensajes de voz
2. **Transcripciones Automáticas**: Convertir audio a texto automáticamente
3. **Refactorización**: Separar el RequestDispatcher en controladores específicos

### **Archivos Clave**

```
Server-Nicolas/
├── negocio/
│   ├── server-LogicaMensajes/
│   │   ├── IMessageService.java
│   │   ├── MessageServiceImpl.java (enviarMensajeAudio ya existe)
│   │   └── transcripcionAudio/
│   │       └── IAudioTranscriptionService.java
│   └── server-logicaFachada/
│       ├── IChatFachada.java
│       └── ChatFachadaImpl.java
└── transporte/
    └── server-controladorTransporte/
        ├── RequestDispatcher.java (refactorizar)
        └── controllers/ (nuevo)
            ├── UserController.java (nuevo)
            ├── ChannelController.java (nuevo)
            ├── MessageController.java (nuevo)
            └── FileController.java (nuevo)
```

---


# FUNCIONALIDAD 1: ENVIAR AUDIO A CANAL

## 📊 ANÁLISIS PREVIO

### **Estado Actual**

✅ **Lo que YA existe:**
```java
// 1. Método en MessageServiceImpl
Server-Nicolas/negocio/server-LogicaMensajes/src/main/java/com/arquitectura/logicaMensajes/MessageServiceImpl.java
    → MessageResponseDto enviarMensajeAudio(SendMessageRequestDto requestDto, UUID autorId)

// 2. Método en ChatFachadaImpl
Server-Nicolas/negocio/server-logicaFachada/src/main/java/com/arquitectura/fachada/ChatFachadaImpl.java
    → MessageResponseDto enviarMensajeAudio(SendMessageRequestDto requestDto, UUID autorId)

// 3. Sistema de transferencia de archivos por chunks
Server-Nicolas/comunes/server-Utils/src/main/java/com/arquitectura/utils/chunkManager/FileChunkManager.java
    → startUpload(), processChunk(), endUpload()

// 4. Endpoints de transferencia de archivos
case "startfileupload": // YA EXISTE
case "uploadfilechunk": // YA EXISTE
case "endfileupload": // YA EXISTE
```

❌ **Lo que FALTA:**
```java
// 1. Endpoint específico para enviar audio
case "enviarmensajeaudio": // NO EXISTE (opcional, puede usar el flujo existente)

// 2. Documentación del flujo completo para el cliente
```

### **Flujo Esperado**

#### **Opción A: Usar Flujo de Chunks Existente (Recomendado)**

```
Cliente:
1. Graba audio y lo tiene en memoria
2. Inicia upload: startfileupload
3. Envía chunks: uploadfilechunk (múltiples veces)
4. Finaliza upload: endfileupload (obtiene fileId)
5. Envía mensaje: enviarMensajeCanal con messageType="AUDIO" y content=fileId

Servidor:
1. Recibe chunks y ensambla el archivo
2. Guarda el archivo en audio_files/
3. Retorna fileId al cliente
4. Cliente envía mensaje con el fileId
5. Servidor guarda mensaje con tipo AUDIO
6. Notifica a los miembros del canal
```

#### **Opción B: Endpoint Directo con Base64 (Más Simple)**

```
Cliente envía:
{
  "action": "enviarMensajeAudio",
  "payload": {
    "canalId": "uuid-del-canal",
    "audioBase64": "data:audio/webm;base64,GkXfo59ChoEBQveBAULygQRC...",
    "duration": 5.2,
    "format": "webm"
  }
}

Servidor:
1. Valida que el usuario sea miembro del canal
2. Decodifica el Base64
3. Guarda el archivo en audio_files/
4. Crea mensaje con tipo AUDIO
5. Notifica a los miembros del canal

Servidor responde:
{
  "action": "enviarMensajeAudio",
  "status": "success",
  "message": "Audio enviado",
  "data": {
    "messageId": "uuid-del-mensaje",
    "channelId": "uuid-del-canal",
    "author": {
      "userId": "uuid-autor",
      "username": "nombre-autor"
    },
    "timestamp": "2025-11-05T20:00:00",
    "messageType": "AUDIO",
    "content": "audio_files/uuid-autor_timestamp.webm",
    "duration": 5.2
  }
}
```

---

## 🔧 IMPLEMENTACIÓN PASO A PASO

### **PASO 1: Decidir el Enfoque**

**Opción A (Chunks):**
- ✅ Mejor para archivos grandes
- ✅ Ya está implementado
- ❌ Más complejo para el cliente (3 pasos)

**Opción B (Base64):**
- ✅ Más simple para el cliente (1 paso)
- ✅ Mejor para audios cortos (< 1MB)
- ❌ Requiere nuevo endpoint

**Recomendación:** Implementar Opción B para simplicidad, pero documentar Opción A como alternativa.

### **PASO 2: Verificar MessageServiceImpl.enviarMensajeAudio()**

**Ubicación**: `Server-Nicolas/negocio/server-LogicaMensajes/src/main/java/com/arquitectura/logicaMensajes/MessageServiceImpl.java`

**Verificar que el método haga:**
1. ✅ Validar que el usuario sea miembro del canal
2. ✅ Guardar el archivo de audio
3. ✅ Crear mensaje con tipo AUDIO
4. ✅ Publicar evento NewMessageEvent
5. ✅ Retornar MessageResponseDto

### **PASO 3: Agregar Endpoint en RequestDispatcher (Opción B)**

**Ubicación**: `Server-Nicolas/transporte/server-controladorTransporte/src/main/java/com/arquitectura/controlador/RequestDispatcher.java`

**Agregar después de "enviarmensajecanal":**

```java
case "enviarmensajeaudio":
case "enviaraudio":
    // 1. Extraer payload
    Object audioDataObj = request.getPayload();
    if (audioDataObj == null) {
        sendJsonResponse(handler, "enviarMensajeAudio", false, "Falta payload", null);
        return;
    }

    // 2. Convertir a JSON y extraer campos
    JsonObject audioJson = gson.toJsonTree(audioDataObj).getAsJsonObject();
    String audioCanalIdStr = audioJson.has("canalId") ? audioJson.get("canalId").getAsString() : null;
    String audioBase64 = audioJson.has("audioBase64") ? audioJson.get("audioBase64").getAsString() : null;
    Double duration = audioJson.has("duration") ? audioJson.get("duration").getAsDouble() : null;
    String format = audioJson.has("format") ? audioJson.get("format").getAsString() : "webm";

    // 3. Validar campos requeridos
    if (audioCanalIdStr == null || audioCanalIdStr.trim().isEmpty()) {
        sendJsonResponse(handler, "enviarMensajeAudio", false, "El ID del canal es requerido",
            createErrorData("canalId", "Campo requerido"));
        return;
    }

    if (audioBase64 == null || audioBase64.trim().isEmpty()) {
        sendJsonResponse(handler, "enviarMensajeAudio", false, "El audio es requerido",
            createErrorData("audioBase64", "Campo requerido"));
        return;
    }

    try {
        // 4. Convertir canalId a UUID
        UUID audioCanalId = UUID.fromString(audioCanalIdStr);

        // 5. Obtener ID del usuario autenticado
        UUID autorId = handler.getAuthenticatedUser().getUserId();

        // 6. Guardar el archivo de audio
        String fileName = "audio_" + System.currentTimeMillis() + "." + format;
        String audioFilePath = chatFachada.guardarArchivoDeAudio(fileName, audioBase64, autorId);

        // 7. Crear DTO de request
        SendMessageRequestDto sendAudioDto = new SendMessageRequestDto(
            audioCanalId,
            "AUDIO",
            audioFilePath
        );

        // 8. Llamar a la fachada
        MessageResponseDto audioResponse = chatFachada.enviarMensajeAudio(sendAudioDto, autorId);

        // 9. Construir respuesta exitosa
        Map<String, Object> audioResponseData = new HashMap<>();
        audioResponseData.put("messageId", audioResponse.getMessageId().toString());
        audioResponseData.put("channelId", audioResponse.getChannelId().toString());
        audioResponseData.put("author", Map.of(
            "userId", audioResponse.getAuthor().getUserId().toString(),
            "username", audioResponse.getAuthor().getUsername()
        ));
        audioResponseData.put("timestamp", audioResponse.getTimestamp().toString());
        audioResponseData.put("messageType", audioResponse.getMessageType());
        audioResponseData.put("content", audioResponse.getContent());
        if (duration != null) {
            audioResponseData.put("duration", duration);
        }

        sendJsonResponse(handler, "enviarMensajeAudio", true, "Audio enviado", audioResponseData);

    } catch (IllegalArgumentException e) {
        // Error de validación
        String errorMessage = e.getMessage();
        String campo = "general";
        
        if (errorMessage.contains("Canal") || errorMessage.contains("canal")) {
            campo = "canalId";
        } else if (errorMessage.contains("miembro")) {
            campo = "permisos";
        } else if (errorMessage.contains("audio")) {
            campo = "audioBase64";
        }
        
        sendJsonResponse(handler, "enviarMensajeAudio", false, errorMessage,
            createErrorData(campo, errorMessage));
            
    } catch (Exception e) {
        // Error inesperado
        System.err.println("Error al enviar audio: " + e.getMessage());
        e.printStackTrace();
        sendJsonResponse(handler, "enviarMensajeAudio", false, "Error interno del servidor al enviar audio", null);
    }
    break;
```

### **PASO 4: Compilar y Probar**

```bash
cd Server-Nicolas
mvn clean install -DskipTests
```

---

## ✅ CHECKLIST - FUNCIONALIDAD 1

- [ ] Decidir enfoque (Chunks vs Base64)
- [ ] `MessageServiceImpl.enviarMensajeAudio()` existe y está completo
- [ ] Validación de membresía implementada
- [ ] Guardado de archivo implementado
- [ ] Creación de mensaje con tipo AUDIO implementada
- [ ] Sistema de notificaciones funciona
- [ ] Caso `"enviarmensajeaudio"` agregado en `RequestDispatcher.dispatch()` (si Opción B)
- [ ] Validaciones de campos requeridos implementadas
- [ ] Manejo de errores específicos
- [ ] Proyecto compila sin errores
- [ ] Prueba manual exitosa (enviar audio)
- [ ] Verificación en BD (mensaje con tipo AUDIO)
- [ ] Notificación push funciona
- [ ] Audio se puede descargar y reproducir

---



# FUNCIONALIDAD 2: TRANSCRIPCIONES AUTOMÁTICAS

## 📊 ANÁLISIS PREVIO

### **Estado Actual**

✅ **Lo que YA existe:**
```java
// 1. Servicio de transcripción
Server-Nicolas/negocio/server-LogicaMensajes/src/main/java/com/arquitectura/logicaMensajes/transcripcionAudio/IAudioTranscriptionService.java
Server-Nicolas/negocio/server-LogicaMensajes/src/main/java/com/arquitectura/logicaMensajes/transcripcionAudio/AudioTranscriptionServiceImpl.java

// 2. Método en ChatFachadaImpl
Server-Nicolas/negocio/server-logicaFachada/src/main/java/com/arquitectura/fachada/ChatFachadaImpl.java
    → List<TranscriptionResponseDto> obtenerTranscripciones()

// 3. DTO de transcripción
Server-Nicolas/comunes/Server-DTO/src/main/java/com/arquitectura/DTO/Mensajes/TranscriptionResponseDto.java
```

❌ **Lo que FALTA:**
```java
// 1. Endpoint para obtener transcripciones
case "obtenertranscripciones": // NO EXISTE

// 2. Lógica automática de transcripción al recibir audio
// (Actualmente se debe llamar manualmente)

// 3. Integración con API de transcripción (Google Speech-to-Text, Whisper, etc.)
```

### **Flujo Esperado**

#### **Transcripción Automática**

```
Servidor (al recibir mensaje de audio):
1. Usuario envía mensaje de audio
2. Servidor guarda el mensaje
3. Servidor publica evento de transcripción (asíncrono)
4. Worker de transcripción procesa el audio
5. Guarda la transcripción en BD
6. Notifica al canal (opcional)

Cliente (para ver transcripciones):
{
  "action": "obtenerTranscripciones",
  "payload": {
    "messageId": "uuid-del-mensaje-audio"  // Opcional
  }
}

Servidor responde:
{
  "action": "obtenerTranscripciones",
  "status": "success",
  "message": "Transcripciones obtenidas",
  "data": {
    "transcripciones": [
      {
        "transcriptionId": "uuid-transcripcion",
        "messageId": "uuid-mensaje",
        "text": "Hola, ¿cómo estás? Espero que bien.",
        "language": "es-ES",
        "confidence": 0.95,
        "timestamp": "2025-11-05T20:01:00",
        "status": "COMPLETED"
      }
    ],
    "totalTranscripciones": 1
  }
}
```

---

## 🔧 IMPLEMENTACIÓN PASO A PASO

### **PASO 1: Verificar AudioTranscriptionServiceImpl**

**Ubicación**: `Server-Nicolas/negocio/server-LogicaMensajes/src/main/java/com/arquitectura/logicaMensajes/transcripcionAudio/AudioTranscriptionServiceImpl.java`

**Verificar que tenga:**
1. ✅ Método para transcribir audio
2. ✅ Integración con API de transcripción (o mock)
3. ✅ Guardado de transcripción en BD
4. ✅ Manejo de errores

### **PASO 2: Agregar Transcripción Automática**

**Opción A: Event-Driven (Recomendado)**

Crear un evento `AudioMessageCreatedEvent` que se publique cuando se crea un mensaje de audio:

```java
// 1. Crear evento
public class AudioMessageCreatedEvent extends ApplicationEvent {
    private final UUID messageId;
    private final String audioFilePath;
    
    public AudioMessageCreatedEvent(Object source, UUID messageId, String audioFilePath) {
        super(source);
        this.messageId = messageId;
        this.audioFilePath = audioFilePath;
    }
    // getters
}

// 2. Publicar evento en MessageServiceImpl.enviarMensajeAudio()
eventPublisher.publishEvent(new AudioMessageCreatedEvent(this, message.getMessageId(), audioFilePath));

// 3. Crear listener en AudioTranscriptionServiceImpl
@EventListener
@Async
public void handleAudioMessageCreated(AudioMessageCreatedEvent event) {
    try {
        transcribeAudio(event.getMessageId(), event.getAudioFilePath());
    } catch (Exception e) {
        log.error("Error al transcribir audio: {}", e.getMessage());
    }
}
```

**Opción B: Síncrono (Más Simple)**

Llamar directamente al servicio de transcripción después de guardar el mensaje:

```java
// En MessageServiceImpl.enviarMensajeAudio()
Message savedMessage = messageRepository.save(message);

// Transcribir automáticamente (asíncrono)
CompletableFuture.runAsync(() -> {
    try {
        transcriptionService.transcribeAudio(savedMessage.getMessageId(), audioFilePath);
    } catch (Exception e) {
        log.error("Error al transcribir audio: {}", e.getMessage());
    }
});
```

### **PASO 3: Agregar Endpoint para Obtener Transcripciones**

**Ubicación**: `Server-Nicolas/transporte/server-controladorTransporte/src/main/java/com/arquitectura/controlador/RequestDispatcher.java`

```java
case "obtenertranscripciones":
case "vertranscripciones":
    // 1. Extraer payload (opcional)
    Object transcripcionDataObj = request.getPayload();
    UUID messageIdFilter = null;
    
    if (transcripcionDataObj != null) {
        JsonObject transcripcionJson = gson.toJsonTree(transcripcionDataObj).getAsJsonObject();
        String messageIdStr = transcripcionJson.has("messageId") ? transcripcionJson.get("messageId").getAsString() : null;
        if (messageIdStr != null && !messageIdStr.trim().isEmpty()) {
            messageIdFilter = UUID.fromString(messageIdStr);
        }
    }

    try {
        // 2. Obtener transcripciones
        List<TranscriptionResponseDto> transcripciones;
        
        if (messageIdFilter != null) {
            // Filtrar por mensaje específico
            transcripciones = chatFachada.obtenerTranscripciones().stream()
                .filter(t -> t.getMessageId().equals(messageIdFilter))
                .collect(Collectors.toList());
        } else {
            // Todas las transcripciones
            transcripciones = chatFachada.obtenerTranscripciones();
        }

        // 3. Construir respuesta
        List<Map<String, Object>> transcripcionesData = new ArrayList<>();
        
        for (TranscriptionResponseDto transcripcion : transcripciones) {
            Map<String, Object> transcripcionMap = new HashMap<>();
            transcripcionMap.put("transcriptionId", transcripcion.getTranscriptionId().toString());
            transcripcionMap.put("messageId", transcripcion.getMessageId().toString());
            transcripcionMap.put("text", transcripcion.getText());
            transcripcionMap.put("language", transcripcion.getLanguage());
            transcripcionMap.put("confidence", transcripcion.getConfidence());
            transcripcionMap.put("timestamp", transcripcion.getTimestamp().toString());
            transcripcionMap.put("status", transcripcion.getStatus());
            
            transcripcionesData.add(transcripcionMap);
        }

        Map<String, Object> transcripcionesResponseData = new HashMap<>();
        transcripcionesResponseData.put("transcripciones", transcripcionesData);
        transcripcionesResponseData.put("totalTranscripciones", transcripciones.size());

        sendJsonResponse(handler, "obtenerTranscripciones", true, "Transcripciones obtenidas", transcripcionesResponseData);

    } catch (Exception e) {
        System.err.println("Error al obtener transcripciones: " + e.getMessage());
        e.printStackTrace();
        sendJsonResponse(handler, "obtenerTranscripciones", false, "Error interno del servidor al obtener transcripciones", null);
    }
    break;
```

### **PASO 4: Configurar API de Transcripción**

**Opciones de APIs:**

1. **Google Cloud Speech-to-Text** (Recomendado)
   - Muy preciso
   - Soporta múltiples idiomas
   - Requiere cuenta de Google Cloud

2. **OpenAI Whisper** (Alternativa)
   - Código abierto
   - Muy preciso
   - Puede correr localmente

3. **Mock para Testing**
   - Retorna texto simulado
   - Útil para desarrollo

**Configuración en application.properties:**

```properties
# Transcripción
transcription.enabled=true
transcription.provider=google  # google, whisper, mock
transcription.google.api-key=YOUR_API_KEY
transcription.language=es-ES
```

---

## ✅ CHECKLIST - FUNCIONALIDAD 2

- [ ] `AudioTranscriptionServiceImpl` existe y está completo
- [ ] Integración con API de transcripción configurada
- [ ] Transcripción automática implementada (Event-Driven o Síncrono)
- [ ] Evento `AudioMessageCreatedEvent` creado (si Event-Driven)
- [ ] Listener para transcripción automática implementado
- [ ] Caso `"obtenertranscripciones"` agregado en `RequestDispatcher.dispatch()`
- [ ] Filtrado por messageId implementado
- [ ] Manejo de errores específicos
- [ ] Configuración de API en properties
- [ ] Proyecto compila sin errores
- [ ] Prueba manual exitosa (transcripción automática)
- [ ] Prueba manual exitosa (obtener transcripciones)
- [ ] Verificación en BD (transcripción guardada)

---



# FUNCIONALIDAD 3: REFACTORIZACIÓN DE CONTROLADORES

## 📊 ANÁLISIS PREVIO

### **Estado Actual**

❌ **Problema Actual:**

El archivo `RequestDispatcher.java` tiene actualmente:
- ~900+ líneas de código
- Todos los endpoints en un solo switch gigante
- Difícil de mantener y extender
- Viola el principio de responsabilidad única

```java
// RequestDispatcher.java (ACTUAL)
public class RequestDispatcher {
    public void dispatch(String requestJson, IClientHandler handler) {
        switch (action) {
            case "authenticateuser": // 50 líneas
            case "registeruser": // 80 líneas
            case "listarcanales": // 60 líneas
            case "enviarmensajecanal": // 70 líneas
            case "invitarmiembro": // 80 líneas
            // ... 15+ casos más
        }
    }
}
```

✅ **Solución Propuesta:**

Separar en controladores específicos por dominio:

```
Server-Nicolas/transporte/server-controladorTransporte/src/main/java/com/arquitectura/controlador/
├── RequestDispatcher.java (coordinador principal)
├── IController.java (interfaz común)
└── controllers/
    ├── UserController.java (autenticación, registro, logout)
    ├── ChannelController.java (canales, invitaciones, miembros)
    ├── MessageController.java (mensajes de texto y audio)
    └── FileController.java (subida y descarga de archivos)
```

---

## 🔧 IMPLEMENTACIÓN PASO A PASO

### **PASO 1: Crear Interfaz Común IController**

**Ubicación**: `Server-Nicolas/transporte/server-controladorTransporte/src/main/java/com/arquitectura/controlador/IController.java`

```java
package com.arquitectura.controlador;

public interface IController {
    /**
     * Maneja una acción específica del controlador
     * @param action La acción a ejecutar
     * @param request El request completo
     * @param handler El handler del cliente
     * @return true si la acción fue manejada, false si no corresponde a este controlador
     */
    boolean handleAction(String action, DTORequest request, IClientHandler handler);
    
    /**
     * Retorna las acciones que este controlador puede manejar
     * @return Set de acciones soportadas
     */
    Set<String> getSupportedActions();
}
```

### **PASO 2: Crear UserController**

**Ubicación**: `Server-Nicolas/transporte/server-controladorTransporte/src/main/java/com/arquitectura/controlador/controllers/UserController.java`

```java
package com.arquitectura.controlador.controllers;

import com.arquitectura.controlador.IClientHandler;
import com.arquitectura.controlador.IController;
import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.fachada.IChatFachada;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class UserController implements IController {
    
    private final IChatFachada chatFachada;
    private final Gson gson;
    
    private static final Set<String> SUPPORTED_ACTIONS = Set.of(
        "authenticateuser",
        "registeruser",
        "logoutuser",
        "listarcontactos"
    );
    
    @Autowired
    public UserController(IChatFachada chatFachada, Gson gson) {
        this.chatFachada = chatFachada;
        this.gson = gson;
    }
    
    @Override
    public boolean handleAction(String action, DTORequest request, IClientHandler handler) {
        if (!SUPPORTED_ACTIONS.contains(action.toLowerCase())) {
            return false;
        }
        
        switch (action.toLowerCase()) {
            case "authenticateuser":
                handleAuthenticate(request, handler);
                break;
            case "registeruser":
                handleRegister(request, handler);
                break;
            case "logoutuser":
                handleLogout(request, handler);
                break;
            case "listarcontactos":
                handleListContacts(request, handler);
                break;
            default:
                return false;
        }
        
        return true;
    }
    
    @Override
    public Set<String> getSupportedActions() {
        return SUPPORTED_ACTIONS;
    }
    
    private void handleAuthenticate(DTORequest request, IClientHandler handler) {
        // Mover lógica de authenticateuser aquí
    }
    
    private void handleRegister(DTORequest request, IClientHandler handler) {
        // Mover lógica de registeruser aquí
    }
    
    private void handleLogout(DTORequest request, IClientHandler handler) {
        // Mover lógica de logoutuser aquí
    }
    
    private void handleListContacts(DTORequest request, IClientHandler handler) {
        // Mover lógica de listarcontactos aquí
    }
    
    // Métodos auxiliares (sendJsonResponse, createErrorData, etc.)
}
```

### **PASO 3: Crear ChannelController**

**Ubicación**: `Server-Nicolas/transporte/server-controladorTransporte/src/main/java/com/arquitectura/controlador/controllers/ChannelController.java`

```java
package com.arquitectura.controlador.controllers;

@Component
public class ChannelController implements IController {
    
    private static final Set<String> SUPPORTED_ACTIONS = Set.of(
        "listarcanales",
        "crearcanaldirecto",
        "invitarmiembro",
        "responderinvitacion",
        "obtenerinvitaciones",
        "listarmiembros"
    );
    
    @Override
    public boolean handleAction(String action, DTORequest request, IClientHandler handler) {
        switch (action.toLowerCase()) {
            case "listarcanales":
                handleListChannels(request, handler);
                break;
            case "crearcanaldirecto":
            case "iniciarchat":
            case "obtenerchatprivado":
                handleCreateDirectChannel(request, handler);
                break;
            case "invitarmiembro":
            case "invitarusuario":
                handleInviteMember(request, handler);
                break;
            case "responderinvitacion":
            case "aceptarinvitacion":
            case "rechazarinvitacion":
                handleRespondInvitation(request, handler);
                break;
            case "obtenerinvitaciones":
            case "listarinvitaciones":
            case "invitacionespendientes":
                handleGetInvitations(request, handler);
                break;
            case "listarmiembros":
            case "obtenermiembroscanal":
                handleListMembers(request, handler);
                break;
            default:
                return false;
        }
        return true;
    }
    
    // Métodos privados para cada acción
}
```

### **PASO 4: Crear MessageController**

**Ubicación**: `Server-Nicolas/transporte/server-controladorTransporte/src/main/java/com/arquitectura/controlador/controllers/MessageController.java`

```java
package com.arquitectura.controlador.controllers;

@Component
public class MessageController implements IController {
    
    private static final Set<String> SUPPORTED_ACTIONS = Set.of(
        "enviarmensajecanal",
        "enviarmensajetexto",
        "enviarmensajeaudio",
        "enviaraudio",
        "solicitarhistorialcanal",
        "obtenermensajescanal",
        "obtenertranscripciones",
        "vertranscripciones"
    );
    
    @Override
    public boolean handleAction(String action, DTORequest request, IClientHandler handler) {
        switch (action.toLowerCase()) {
            case "enviarmensajecanal":
            case "enviarmensajetexto":
                handleSendTextMessage(request, handler);
                break;
            case "enviarmensajeaudio":
            case "enviaraudio":
                handleSendAudioMessage(request, handler);
                break;
            case "solicitarhistorialcanal":
            case "obtenermensajescanal":
                handleGetHistory(request, handler);
                break;
            case "obtenertranscripciones":
            case "vertranscripciones":
                handleGetTranscriptions(request, handler);
                break;
            default:
                return false;
        }
        return true;
    }
    
    // Métodos privados para cada acción
}
```

### **PASO 5: Crear FileController**

**Ubicación**: `Server-Nicolas/transporte/server-controladorTransporte/src/main/java/com/arquitectura/controlador/controllers/FileController.java`

```java
package com.arquitectura.controlador.controllers;

@Component
public class FileController implements IController {
    
    private static final Set<String> SUPPORTED_ACTIONS = Set.of(
        "startfileupload",
        "uploadfileforregistration",
        "uploadfilechunk",
        "endfileupload",
        "startfiledownload",
        "requestfilechunk"
    );
    
    @Override
    public boolean handleAction(String action, DTORequest request, IClientHandler handler) {
        switch (action.toLowerCase()) {
            case "startfileupload":
            case "uploadfileforregistration":
                handleStartUpload(request, handler);
                break;
            case "uploadfilechunk":
                handleUploadChunk(request, handler);
                break;
            case "endfileupload":
                handleEndUpload(request, handler);
                break;
            case "startfiledownload":
                handleStartDownload(request, handler);
                break;
            case "requestfilechunk":
                handleRequestChunk(request, handler);
                break;
            default:
                return false;
        }
        return true;
    }
    
    // Métodos privados para cada acción
}
```

### **PASO 6: Refactorizar RequestDispatcher**

**Ubicación**: `Server-Nicolas/transporte/server-controladorTransporte/src/main/java/com/arquitectura/controlador/RequestDispatcher.java`

```java
@Component
public class RequestDispatcher {

    private final List<IController> controllers;
    private final Gson gson;
    private static final Set<String> ACCIONES_PUBLICAS = Set.of(
            "authenticateuser",
            "registeruser",
            "uploadfileforregistration",
            "uploadfilechunk",
            "endfileupload"
    );

    @Autowired
    public RequestDispatcher(List<IController> controllers, Gson gson) {
        this.controllers = controllers;
        this.gson = gson;
    }

    public void dispatch(String requestJson, IClientHandler handler) {
        DTORequest request;
        String action = "unknown";
        
        try {
            request = gson.fromJson(requestJson, DTORequest.class);
            action = request.getAction() != null ? request.getAction().toLowerCase() : "unknown";

            // Validar sesión
            if (!ACCIONES_PUBLICAS.contains(action) && !handler.isAuthenticated()) {
                sendJsonResponse(handler, action, false, "Debes iniciar sesión para realizar esta acción.", null);
                return;
            }

            // Delegar a los controladores
            boolean handled = false;
            for (IController controller : controllers) {
                if (controller.handleAction(action, request, handler)) {
                    handled = true;
                    break;
                }
            }

            if (!handled) {
                sendJsonResponse(handler, action, false, "Comando desconocido: " + action, null);
            }

        } catch (Exception e) {
            sendJsonResponse(handler, action, false, "Error interno del servidor", null);
            e.printStackTrace();
        }
    }

    // Métodos auxiliares compartidos
    private void sendJsonResponse(IClientHandler handler, String action, boolean success, String message, Object data) {
        String status = success ? "success" : "error";
        DTOResponse response = new DTOResponse(action, status, message, data);
        String jsonResponse = gson.toJson(response);
        handler.sendMessage(jsonResponse);
    }
}
```

### **PASO 7: Crear Clase Base para Controladores (Opcional)**

**Ubicación**: `Server-Nicolas/transporte/server-controladorTransporte/src/main/java/com/arquitectura/controlador/controllers/BaseController.java`

```java
package com.arquitectura.controlador.controllers;

public abstract class BaseController implements IController {
    
    protected final IChatFachada chatFachada;
    protected final Gson gson;
    
    public BaseController(IChatFachada chatFachada, Gson gson) {
        this.chatFachada = chatFachada;
        this.gson = gson;
    }
    
    // Métodos auxiliares compartidos
    protected void sendJsonResponse(IClientHandler handler, String action, boolean success, String message, Object data) {
        String status = success ? "success" : "error";
        DTOResponse response = new DTOResponse(action, status, message, data);
        String jsonResponse = gson.toJson(response);
        handler.sendMessage(jsonResponse);
    }
    
    protected Map<String, String> createErrorData(String campo, String motivo) {
        Map<String, String> errorData = new HashMap<>();
        errorData.put("campo", campo);
        errorData.put("motivo", motivo);
        return errorData;
    }
}
```

---

## ✅ CHECKLIST - FUNCIONALIDAD 3

- [ ] Interfaz `IController` creada
- [ ] Clase `BaseController` creada (opcional)
- [ ] `UserController` creado y funcionando
- [ ] `ChannelController` creado y funcionando
- [ ] `MessageController` creado y funcionando
- [ ] `FileController` creado y funcionando
- [ ] `RequestDispatcher` refactorizado
- [ ] Todos los endpoints migrados a controladores
- [ ] Métodos auxiliares compartidos extraídos
- [ ] Proyecto compila sin errores
- [ ] Todas las pruebas manuales pasan
- [ ] Código más limpio y mantenible
- [ ] Documentación actualizada

---

## 📊 BENEFICIOS DE LA REFACTORIZACIÓN

### **Antes:**
- ❌ 1 archivo con 900+ líneas
- ❌ Difícil de mantener
- ❌ Difícil de testear
- ❌ Viola principio de responsabilidad única

### **Después:**
- ✅ 5 archivos con ~200 líneas cada uno
- ✅ Fácil de mantener
- ✅ Fácil de testear (cada controlador independiente)
- ✅ Cumple principio de responsabilidad única
- ✅ Fácil agregar nuevos controladores
- ✅ Código más limpio y organizado

---



# TESTING Y VALIDACIÓN

## 🧪 PLAN DE PRUEBAS COMPLETO

### **Preparación del Entorno de Pruebas**

#### **1. Iniciar Base de Datos y Servidor**

```bash
cd Server-Nicolas
docker-compose up -d
mvn clean install -DskipTests
java -jar comunes/server-app/target/server-app-1.0-SNAPSHOT-jar-with-dependencies.jar
```

#### **2. Crear Usuarios de Prueba**

```json
{"action":"registerUser","payload":{"username":"alice","email":"alice@test.com","password":"123456"}}
{"action":"registerUser","payload":{"username":"bob","email":"bob@test.com","password":"123456"}}
```

#### **3. Autenticar y Crear Canal**

```json
{"action":"authenticateUser","payload":{"nombreUsuario":"alice","password":"123456"}}
{"action":"crearCanal","payload":{"nombre":"Canal Test","tipo":"GRUPO"}}
```

---

## 📝 CASOS DE PRUEBA

### **FUNCIONALIDAD 1: ENVIAR AUDIO A CANAL**

#### **Caso 1.1: Enviar Audio con Base64**

**Preparación**: Alice autenticada, miembro del canal

**Entrada**:
```json
{
  "action": "enviarMensajeAudio",
  "payload": {
    "canalId": "uuid-del-canal",
    "audioBase64": "data:audio/webm;base64,GkXfo59ChoEBQveBAULygQRC...",
    "duration": 5.2,
    "format": "webm"
  }
}
```

**Salida Esperada**:
```json
{
  "action": "enviarMensajeAudio",
  "status": "success",
  "message": "Audio enviado",
  "data": {
    "messageId": "uuid-mensaje",
    "channelId": "uuid-canal",
    "author": {
      "userId": "uuid-alice",
      "username": "alice"
    },
    "timestamp": "2025-11-05T20:00:00",
    "messageType": "AUDIO",
    "content": "audio_files/alice_1730851200000.webm",
    "duration": 5.2
  }
}
```

**Verificación en BD**:
```sql
SELECT * FROM message WHERE message_id = 'uuid-mensaje';
-- Debe tener message_type = 'AUDIO'
-- content debe apuntar al archivo guardado
```

---

#### **Caso 1.2: Enviar Audio con Chunks**

**Preparación**: Alice autenticada, miembro del canal

**Paso 1: Iniciar Upload**
```json
{"action":"startfileupload","payload":{"fileName":"audio.webm","fileSize":50000,"mimeType":"audio/webm"}}
```

**Paso 2: Enviar Chunks**
```json
{"action":"uploadfilechunk","payload":{"uploadId":"uuid-upload","chunkNumber":0,"chunkData":"base64..."}}
{"action":"uploadfilechunk","payload":{"uploadId":"uuid-upload","chunkNumber":1,"chunkData":"base64..."}}
```

**Paso 3: Finalizar Upload**
```json
{"action":"endfileupload","payload":{"uploadId":"uuid-upload"}}
```

**Paso 4: Enviar Mensaje**
```json
{"action":"enviarMensajeCanal","payload":{"canalId":"uuid-canal","contenido":"audio_files/file.webm"}}
```

---

### **FUNCIONALIDAD 2: TRANSCRIPCIONES AUTOMÁTICAS**

#### **Caso 2.1: Transcripción Automática**

**Preparación**: Alice envía audio

**Entrada**:
```json
{"action":"enviarMensajeAudio","payload":{"canalId":"uuid-canal","audioBase64":"..."}}
```

**Verificación**:
1. Mensaje se guarda inmediatamente
2. Transcripción se procesa en background
3. Después de unos segundos, verificar en BD:

```sql
SELECT * FROM transcription WHERE message_id = 'uuid-mensaje';
-- Debe existir una transcripción con status = 'COMPLETED'
```

---

#### **Caso 2.2: Obtener Transcripciones**

**Preparación**: Existen transcripciones en BD

**Entrada (todas las transcripciones)**:
```json
{"action":"obtenerTranscripciones","payload":{}}
```

**Entrada (filtrar por mensaje)**:
```json
{"action":"obtenerTranscripciones","payload":{"messageId":"uuid-mensaje"}}
```

**Salida Esperada**:
```json
{
  "action": "obtenerTranscripciones",
  "status": "success",
  "message": "Transcripciones obtenidas",
  "data": {
    "transcripciones": [
      {
        "transcriptionId": "uuid-transcripcion",
        "messageId": "uuid-mensaje",
        "text": "Hola, ¿cómo estás?",
        "language": "es-ES",
        "confidence": 0.95,
        "timestamp": "2025-11-05T20:01:00",
        "status": "COMPLETED"
      }
    ],
    "totalTranscripciones": 1
  }
}
```

---

### **FUNCIONALIDAD 3: REFACTORIZACIÓN**

#### **Caso 3.1: Verificar Todos los Endpoints Funcionan**

**Pruebas de Regresión**: Ejecutar todas las pruebas de Prioridades 1, 2 y 3

**Endpoints a Verificar:**
- ✅ authenticateUser
- ✅ registerUser
- ✅ logoutUser
- ✅ listarContactos
- ✅ listarCanales
- ✅ crearCanalDirecto
- ✅ invitarMiembro
- ✅ responderInvitacion
- ✅ obtenerInvitaciones
- ✅ listarMiembros
- ✅ enviarMensajeCanal
- ✅ solicitarHistorialCanal
- ✅ startfileupload
- ✅ uploadfilechunk
- ✅ endfileupload
- ✅ startfiledownload
- ✅ requestfilechunk

**Resultado Esperado**: Todos los endpoints deben funcionar exactamente igual que antes.

---

#### **Caso 3.2: Verificar Organización del Código**

**Verificar estructura de archivos:**
```
controllers/
├── UserController.java (~200 líneas)
├── ChannelController.java (~250 líneas)
├── MessageController.java (~200 líneas)
└── FileController.java (~150 líneas)

RequestDispatcher.java (~100 líneas)
```

**Verificar que cada controlador:**
- ✅ Implementa IController
- ✅ Tiene métodos privados bien organizados
- ✅ Maneja solo sus acciones específicas
- ✅ Tiene buena separación de responsabilidades

---

## 🔄 PRUEBAS DE INTEGRACIÓN

### **Flujo Completo: Audio + Transcripción**

#### **Paso 1: Alice Envía Audio**
```json
{"action":"enviarMensajeAudio","payload":{"canalId":"uuid-canal","audioBase64":"...","duration":5.2}}
```

#### **Paso 2: Bob Recibe Notificación**
```
Automático - Bob ve el mensaje de audio en tiempo real
```

#### **Paso 3: Transcripción Automática**
```
Servidor procesa el audio en background (5-10 segundos)
```

#### **Paso 4: Alice Ve la Transcripción**
```json
{"action":"obtenerTranscripciones","payload":{"messageId":"uuid-mensaje"}}
```

#### **Paso 5: Bob Descarga el Audio**
```json
{"action":"startfiledownload","payload":{"fileId":"audio_files/file.webm"}}
{"action":"requestfilechunk","payload":{"downloadId":"uuid-download","chunkNumber":0}}
```

---

## ✅ CHECKLIST DE VALIDACIÓN FINAL

### **Compilación y Ejecución**
- [ ] Proyecto compila sin errores
- [ ] Servidor inicia correctamente
- [ ] MySQL está corriendo
- [ ] Servidor escucha en puerto 22100

### **Funcionalidad 1: Enviar Audio**
- [ ] Enviar audio con Base64 funciona
- [ ] Enviar audio con chunks funciona
- [ ] Solo miembros pueden enviar audio
- [ ] Audio se guarda correctamente
- [ ] Notificación push funciona
- [ ] Audio se puede descargar

### **Funcionalidad 2: Transcripciones**
- [ ] Transcripción automática funciona
- [ ] API de transcripción configurada
- [ ] Transcripción se guarda en BD
- [ ] Obtener transcripciones funciona
- [ ] Filtrar por mensaje funciona
- [ ] Manejo de errores funciona

### **Funcionalidad 3: Refactorización**
- [ ] Todos los controladores creados
- [ ] RequestDispatcher refactorizado
- [ ] Todos los endpoints funcionan
- [ ] Código más limpio y organizado
- [ ] Fácil agregar nuevos endpoints
- [ ] Pruebas de regresión pasan

### **Integración**
- [ ] Flujo completo funciona sin errores
- [ ] Notificaciones push funcionan
- [ ] Base de datos refleja los cambios
- [ ] Logs del servidor son claros

---


# CHECKLIST FINAL - PRIORIDAD 4

## 📋 RESUMEN DE IMPLEMENTACIÓN

### **Archivos Modificados/Creados**

#### **Nuevos Archivos**
- [ ] `IController.java` (interfaz común)
- [ ] `BaseController.java` (clase base opcional)
- [ ] `controllers/UserController.java`
- [ ] `controllers/ChannelController.java`
- [ ] `controllers/MessageController.java`
- [ ] `controllers/FileController.java`
- [ ] `AudioMessageCreatedEvent.java` (evento para transcripción)

#### **Archivos Modificados**
- [ ] `RequestDispatcher.java` (refactorizado)
- [ ] `MessageServiceImpl.java` (transcripción automática)
- [ ] `AudioTranscriptionServiceImpl.java` (listener de eventos)
- [ ] `application.properties` (configuración de transcripción)

---

## 🎯 FUNCIONALIDADES IMPLEMENTADAS

### **✅ Funcionalidad 1: Enviar Audio a Canal**
- [ ] Endpoint `enviarMensajeAudio` funcional (Opción B)
- [ ] Documentación de flujo con chunks (Opción A)
- [ ] Validación de membresía implementada
- [ ] Guardado de archivo implementado
- [ ] Notificaciones push funcionan
- [ ] Audio se puede descargar

### **✅ Funcionalidad 2: Transcripciones Automáticas**
- [ ] Transcripción automática implementada
- [ ] Evento `AudioMessageCreatedEvent` creado
- [ ] Listener para transcripción implementado
- [ ] Endpoint `obtenerTranscripciones` funcional
- [ ] Integración con API de transcripción
- [ ] Configuración en properties

### **✅ Funcionalidad 3: Refactorización**
- [ ] Interfaz `IController` creada
- [ ] 4 controladores creados y funcionando
- [ ] `RequestDispatcher` refactorizado
- [ ] Código más limpio y mantenible
- [ ] Pruebas de regresión pasan

---

## 📊 MÉTRICAS DE IMPLEMENTACIÓN

### **Líneas de Código**
- Antes: RequestDispatcher.java (~900 líneas)
- Después: 
  - RequestDispatcher.java (~100 líneas)
  - UserController.java (~200 líneas)
  - ChannelController.java (~250 líneas)
  - MessageController.java (~200 líneas)
  - FileController.java (~150 líneas)
  - **Total**: ~900 líneas (mismo total, mejor organizado)

### **Endpoints Agregados**
- `enviarMensajeAudio` (nuevo)
- `obtenerTranscripciones` (nuevo)
- **Total**: 2 endpoints nuevos

### **Mejoras de Arquitectura**
- ✅ Separación de responsabilidades
- ✅ Código más mantenible
- ✅ Fácil agregar nuevos controladores
- ✅ Mejor testabilidad
- ✅ Cumple principios SOLID

---

## 🚀 PRÓXIMOS PASOS

### **Mejoras Futuras (Opcional)**

1. **Testing Unitario**
   - Tests para cada controlador
   - Tests de integración
   - Cobertura de código > 80%

2. **Documentación API**
   - Swagger/OpenAPI
   - Postman Collection
   - Ejemplos de uso

3. **Optimizaciones**
   - Caché de transcripciones
   - Compresión de audio
   - Rate limiting

4. **Monitoreo**
   - Métricas de uso
   - Logs estructurados
   - Alertas de errores

---

## 📝 NOTAS IMPORTANTES

1. **Transcripción de Audio**:
   - Requiere API key de Google Cloud o configuración de Whisper
   - Proceso asíncrono (no bloquea el envío del mensaje)
   - Puede tardar 5-10 segundos dependiendo del tamaño del audio

2. **Refactorización**:
   - No cambia la funcionalidad, solo la organización
   - Todos los endpoints deben seguir funcionando igual
   - Facilita el mantenimiento futuro

3. **Compatibilidad**:
   - Cliente no necesita cambios (mismos endpoints)
   - Solo mejoras internas del servidor
   - Backward compatible

---

## ✅ FIRMA DE COMPLETITUD

**Funcionalidades de Prioridad 4 Completadas**: 0/3

- [ ] Enviar Audio a Canal
- [ ] Transcripciones Automáticas
- [ ] Refactorización de Controladores

**Estado del Proyecto**: ⏳ **PENDIENTE**

**Fecha de Completitud**: _________________

**Desarrollador**: _________________

---

## 📚 REFERENCIAS

### **Documentación del Proyecto**
- `PLAN_IMPLEMENTACION_PRIORIDAD_1.md` - Funcionalidades básicas
- `PLAN_IMPLEMENTACION_PRIORIDAD_2.md` - Gestión de canales
- `PLAN_IMPLEMENTACION_PRIORIDAD_3.md` - Mensajes privados

### **APIs de Transcripción**
- Google Cloud Speech-to-Text: https://cloud.google.com/speech-to-text
- OpenAI Whisper: https://github.com/openai/whisper
- Azure Speech Services: https://azure.microsoft.com/en-us/services/cognitive-services/speech-to-text/

### **Patrones de Diseño**
- Strategy Pattern (Controladores)
- Chain of Responsibility (RequestDispatcher)
- Observer Pattern (Eventos de transcripción)

---

**FIN DEL DOCUMENTO**

