# 🎯 PLAN DE IMPLEMENTACIÓN - PRIORIDAD 1
## Funcionalidad Básica del Servidor

**Fecha de creación**: 5 de noviembre de 2025  
**Proyecto**: Chat-Unillanos - Servidor  
**Objetivo**: Implementar las 4 funcionalidades críticas faltantes en el servidor

---

## 📋 ÍNDICE

1. [Visión General](#visión-general)
2. [Funcionalidad 1: Registro de Usuarios](#funcionalidad-1-registro-de-usuarios)
3. [Funcionalidad 2: Enviar Mensaje de Texto a Canal](#funcionalidad-2-enviar-mensaje-de-texto-a-canal)
4. [Funcionalidad 3: Obtener Historial de Canal](#funcionalidad-3-obtener-historial-de-canal)
5. [Funcionalidad 4: Listar Miembros de Canal](#funcionalidad-4-listar-miembros-de-canal)
6. [Testing y Validación](#testing-y-validación)
7. [Checklist Final](#checklist-final)

---

## 🎯 VISIÓN GENERAL

### **Estado Actual del Proyecto**

El servidor tiene la siguiente estructura:

```
Server-Nicolas/
├── comunes/
│   ├── server-app/          # Configuración y lanzamiento
│   ├── Server-DTO/          # DTOs compartidos
│   ├── server-Utils/        # Utilidades
│   └── server-events/       # Eventos de Spring
├── datos/
│   ├── server-dominio/      # Entidades JPA
│   └── server-persistencia/ # Repositorios
├── negocio/
│   ├── server-logicaFachada/    # ChatFachadaImpl
│   ├── server-logicaUsuarios/   # UserServiceImpl
│   ├── server-LogicaCanales/    # ChannelServiceImpl
│   └── server-LogicaMensajes/   # MessageServiceImpl
├── transporte/
│   ├── server-Transporte/           # ServerListener, ClientHandler
│   └── server-controladorTransporte/ # RequestDispatcher
└── vista/
    ├── server-vista/            # UI Swing
    └── server-controladorVista/ # Controlador UI
```

### **Punto de Entrada de Peticiones**

**TODAS** las peticiones del cliente pasan por:
```
ClientHandler.run() 
    ↓
RequestDispatcher.dispatch(String requestJson, IClientHandler handler)
    ↓
switch (action) { ... }
```

### **Flujo Estándar de una Petición**

```
1. Cliente envía JSON: {"action":"nombreAccion","payload":{...}}
2. ClientHandler recibe y llama a RequestDispatcher.dispatch()
3. RequestDispatcher:
   - Parsea JSON a DTORequest
   - Normaliza action a lowercase
   - Valida autenticación (si es necesario)
   - Extrae payload
   - Llama a ChatFachadaImpl
4. ChatFachadaImpl delega a servicio específico
5. Servicio ejecuta lógica de negocio
6. Servicio retorna DTO o lanza excepción
7. RequestDispatcher construye DTOResponse
8. ClientHandler envía JSON de respuesta
```

### **Archivos que Modificaremos**

Para cada funcionalidad necesitaremos tocar:

1. **DTOs** (si no existen): `Server-Nicolas/comunes/Server-DTO/`
2. **RequestDispatcher**: `Server-Nicolas/transporte/server-controladorTransporte/src/main/java/com/arquitectura/controlador/RequestDispatcher.java`
3. **Servicios** (si falta lógica): `Server-Nicolas/negocio/server-logica*/`
4. **Fachada** (si falta método): `Server-Nicolas/negocio/server-logicaFachada/`

---


# FUNCIONALIDAD 1: REGISTRO DE USUARIOS

## 📊 ANÁLISIS PREVIO

### **Estado Actual**

✅ **Lo que YA existe:**
```java
// 1. Entidad de dominio
Server-Nicolas/datos/server-dominio/src/main/java/com/arquitectura/domain/User.java

// 2. Repositorio
Server-Nicolas/datos/server-persistencia/src/main/java/com/arquitectura/persistence/repository/UserRepository.java

// 3. Servicio con lógica
Server-Nicolas/negocio/server-logicaUsuarios/src/main/java/com/arquitectura/logicaUsuarios/UserServiceImpl.java
    → void registrarUsuario(UserRegistrationRequestDto requestDto, String ipAddress)

// 4. Fachada con método
Server-Nicolas/negocio/server-logicaFachada/src/main/java/com/arquitectura/fachada/ChatFachadaImpl.java
    → void registrarUsuario(UserRegistrationRequestDto requestDto, String ipAddress)

// 5. DTO de request
Server-Nicolas/comunes/Server-DTO/src/main/java/com/arquitectura/DTO/usuarios/UserRegistrationRequestDto.java
```

❌ **Lo que FALTA:**
```java
// 1. Endpoint en RequestDispatcher
case "registeruser": // NO EXISTE

// 2. Validación de email único antes de registrar
// 3. Manejo de errores específicos (email duplicado, etc.)
```

### **Flujo Esperado**

```
Cliente envía:
{
  "action": "registerUser",
  "payload": {
    "username": "juan",
    "email": "juan@mail.com",
    "password": "123456",
    "photoFileId": "user_photos/juan.jpg" (opcional)
  }
}

Servidor responde (éxito):
{
  "action": "registerUser",
  "status": "success",
  "message": "Usuario registrado exitosamente",
  "data": {
    "userId": "uuid-generado",
    "username": "juan",
    "email": "juan@mail.com"
  }
}

Servidor responde (error - email duplicado):
{
  "action": "registerUser",
  "status": "error",
  "message": "El email ya está registrado",
  "data": {
    "campo": "email",
    "motivo": "Email duplicado"
  }
}
```

---

## 🔧 IMPLEMENTACIÓN PASO A PASO

### **PASO 1: Verificar el DTO UserRegistrationRequestDto**

**Ubicación**: `Server-Nicolas/comunes/Server-DTO/src/main/java/com/arquitectura/DTO/usuarios/UserRegistrationRequestDto.java`

**Acción**: Abrir el archivo y verificar que tenga estos campos:

```java
public class UserRegistrationRequestDto {
    private String username;
    private String email;
    private String password;
    private String photoFileId; // Opcional
    
    // Constructor, getters, setters
}
```

**Si NO existe el archivo**, crearlo con este contenido:

```java
package com.arquitectura.DTO.usuarios;

public class UserRegistrationRequestDto {
    private String username;
    private String email;
    private String password;
    private String photoFileId; // Ruta del archivo de foto (opcional)

    public UserRegistrationRequestDto() {
    }

    public UserRegistrationRequestDto(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public UserRegistrationRequestDto(String username, String email, String password, String photoFileId) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.photoFileId = photoFileId;
    }

    // Getters y Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhotoFileId() {
        return photoFileId;
    }

    public void setPhotoFileId(String photoFileId) {
        this.photoFileId = photoFileId;
    }

    @Override
    public String toString() {
        return "UserRegistrationRequestDto{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", photoFileId='" + photoFileId + '\'' +
                '}';
    }
}
```

---

### **PASO 2: Verificar UserServiceImpl.registrarUsuario()**

**Ubicación**: `Server-Nicolas/negocio/server-logicaUsuarios/src/main/java/com/arquitectura/logicaUsuarios/UserServiceImpl.java`

**Acción**: Abrir el archivo y buscar el método `registrarUsuario()`.

**Verificar que haga lo siguiente:**

1. ✅ Validar que el email no exista
2. ✅ Hashear la contraseña con BCrypt
3. ✅ Crear entidad User
4. ✅ Asignar Peer (servidor padre)
5. ✅ Guardar en BD
6. ✅ Asignar foto de perfil (si se proporcionó)

**Código esperado** (aproximado):

```java
@Override
@Transactional
public void registrarUsuario(UserRegistrationRequestDto requestDto, String ipAddress) throws Exception {
    // 1. Validar que el email no exista
    if (userRepository.findByEmail(requestDto.getEmail()).isPresent()) {
        throw new IllegalArgumentException("El email ya está registrado");
    }

    // 2. Validar que el username no exista
    if (userRepository.findByUsername(requestDto.getUsername()).isPresent()) {
        throw new IllegalArgumentException("El nombre de usuario ya está en uso");
    }

    // 3. Hashear la contraseña
    String hashedPassword = passwordEncoder.encode(requestDto.getPassword());

    // 4. Crear entidad User
    User newUser = new User(
        requestDto.getUsername(),
        requestDto.getEmail(),
        hashedPassword,
        ipAddress
    );

    // 5. Asignar Peer (servidor padre)
    String serverPeerAddress = networkUtils.getServerIPAddress();
    Peer currentPeer = peerRepository.findByIp(serverPeerAddress)
            .orElseGet(() -> peerRepository.save(new Peer(serverPeerAddress)));
    newUser.setPeerId(currentPeer);

    // 6. Asignar foto de perfil (si se proporcionó)
    if (requestDto.getPhotoFileId() != null && !requestDto.getPhotoFileId().isEmpty()) {
        newUser.setPhotoAddress(requestDto.getPhotoFileId());
    }

    // 7. Guardar en BD
    userRepository.save(newUser);
    
    log.info("Usuario registrado exitosamente: {}", newUser.getUsername());
}
```

**Si el método NO existe o está incompleto**, necesitarás agregarlo o completarlo.

---

### **PASO 3: Verificar ChatFachadaImpl.registrarUsuario()**

**Ubicación**: `Server-Nicolas/negocio/server-logicaFachada/src/main/java/com/arquitectura/fachada/ChatFachadaImpl.java`

**Acción**: Abrir el archivo y buscar el método `registrarUsuario()`.

**Código esperado**:

```java
@Override
public void registrarUsuario(UserRegistrationRequestDto requestDto, String ipAddress) throws Exception {
    userService.registrarUsuario(requestDto, ipAddress);
}
```

**Si NO existe**, agregarlo en la sección de "Métodos de Usuario".

---

### **PASO 4: Agregar el Endpoint en RequestDispatcher**

**Ubicación**: `Server-Nicolas/transporte/server-controladorTransporte/src/main/java/com/arquitectura/controlador/RequestDispatcher.java`

**Acción**: Abrir el archivo y buscar el método `dispatch()`.

**Encontrar el switch statement**:

```java
switch (action) {
    case "authenticateuser":
        // ... código existente ...
        break;
    
    case "logoutuser":
        // ... código existente ...
        break;
    
    // AQUÍ AGREGAREMOS EL NUEVO CASO
}
```

**Agregar el nuevo caso DESPUÉS de "logoutuser" y ANTES de "listarcontactos"**:

```java
case "registeruser":
    // 1. Extraer payload
    Object registerDataObj = request.getPayload();
    if (registerDataObj == null) {
        sendJsonResponse(handler, "registerUser", false, "Falta payload", null);
        return;
    }

    // 2. Convertir a JSON y extraer campos
    JsonObject registerJson = gson.toJsonTree(registerDataObj).getAsJsonObject();
    String regUsername = registerJson.has("username") ? registerJson.get("username").getAsString() : null;
    String regEmail = registerJson.has("email") ? registerJson.get("email").getAsString() : null;
    String regPassword = registerJson.has("password") ? registerJson.get("password").getAsString() : null;
    String regPhotoFileId = registerJson.has("photoFileId") ? registerJson.get("photoFileId").getAsString() : null;

    // 3. Validar campos requeridos
    if (regUsername == null || regUsername.trim().isEmpty()) {
        sendJsonResponse(handler, "registerUser", false, "El nombre de usuario es requerido",
            createErrorData("username", "Campo requerido"));
        return;
    }

    if (regEmail == null || regEmail.trim().isEmpty()) {
        sendJsonResponse(handler, "registerUser", false, "El email es requerido",
            createErrorData("email", "Campo requerido"));
        return;
    }

    if (regPassword == null || regPassword.trim().isEmpty()) {
        sendJsonResponse(handler, "registerUser", false, "La contraseña es requerida",
            createErrorData("password", "Campo requerido"));
        return;
    }

    // 4. Validar formato de email (básico)
    if (!regEmail.contains("@") || !regEmail.contains(".")) {
        sendJsonResponse(handler, "registerUser", false, "Formato de email inválido",
            createErrorData("email", "Formato inválido"));
        return;
    }

    // 5. Validar longitud de contraseña
    if (regPassword.length() < 6) {
        sendJsonResponse(handler, "registerUser", false, "La contraseña debe tener al menos 6 caracteres",
            createErrorData("password", "Mínimo 6 caracteres"));
        return;
    }

    try {
        // 6. Crear DTO
        UserRegistrationRequestDto registrationDto = new UserRegistrationRequestDto(
            regUsername,
            regEmail,
            regPassword,
            regPhotoFileId
        );

        // 7. Llamar a la fachada
        chatFachada.registrarUsuario(registrationDto, handler.getClientIpAddress());

        // 8. Construir respuesta exitosa
        Map<String, Object> registerResponseData = new HashMap<>();
        registerResponseData.put("username", regUsername);
        registerResponseData.put("email", regEmail);
        registerResponseData.put("message", "Usuario registrado exitosamente. Ahora puedes iniciar sesión.");

        sendJsonResponse(handler, "registerUser", true, "Registro exitoso", registerResponseData);

    } catch (IllegalArgumentException e) {
        // Error de validación (email duplicado, username duplicado, etc.)
        String errorMessage = e.getMessage();
        String campo = "general";
        
        if (errorMessage.contains("email")) {
            campo = "email";
        } else if (errorMessage.contains("username") || errorMessage.contains("usuario")) {
            campo = "username";
        }
        
        sendJsonResponse(handler, "registerUser", false, errorMessage,
            createErrorData(campo, errorMessage));
            
    } catch (Exception e) {
        // Error inesperado
        log.error("Error al registrar usuario: {}", e.getMessage(), e);
        sendJsonResponse(handler, "registerUser", false, "Error interno del servidor al registrar usuario", null);
    }
    break;
```

---

### **PASO 5: Compilar y Probar**

**Compilar el proyecto**:

```bash
cd Server-Nicolas
mvn clean install -DskipTests
```

**Verificar que no haya errores de compilación**.

---

### **PASO 6: Probar con Cliente o Postman**

**Opción A: Usar el cliente existente**

Si el cliente ya tiene la funcionalidad de registro, simplemente ejecutar y probar.

**Opción B: Probar manualmente con telnet/netcat**

```bash
# Conectar al servidor
telnet localhost 22100

# Enviar JSON (todo en una línea)
{"action":"registerUser","payload":{"username":"testuser","email":"test@mail.com","password":"123456"}}
```

**Respuesta esperada (éxito)**:
```json
{
  "action":"registerUser",
  "status":"success",
  "message":"Registro exitoso",
  "data":{
    "username":"testuser",
    "email":"test@mail.com",
    "message":"Usuario registrado exitosamente. Ahora puedes iniciar sesión."
  }
}
```

**Respuesta esperada (email duplicado)**:
```json
{
  "action":"registerUser",
  "status":"error",
  "message":"El email ya está registrado",
  "data":{
    "campo":"email",
    "motivo":"El email ya está registrado"
  }
}
```

---

### **PASO 7: Verificar en Base de Datos**

**Conectar a MySQL**:

```bash
mysql -u root -p
# Password: root1234

USE chat_db;
SELECT * FROM users ORDER BY fecha_registro DESC LIMIT 5;
```

**Verificar que el usuario se haya creado con**:
- ✅ `user_id` (UUID)
- ✅ `username`
- ✅ `email`
- ✅ `hashed_password` (debe ser un hash BCrypt, no la contraseña en texto plano)
- ✅ `ip_address`
- ✅ `conectado` = false
- ✅ `fecha_registro` = timestamp actual
- ✅ `servidor_padre` = UUID del peer

---

## ✅ CHECKLIST - FUNCIONALIDAD 1

- [ ] DTO `UserRegistrationRequestDto` existe y tiene todos los campos
- [ ] `UserServiceImpl.registrarUsuario()` existe y está completo
- [ ] `ChatFachadaImpl.registrarUsuario()` existe y delega correctamente
- [ ] Caso `"registeruser"` agregado en `RequestDispatcher.dispatch()`
- [ ] Validaciones de campos requeridos implementadas
- [ ] Validación de formato de email implementada
- [ ] Validación de longitud de contraseña implementada
- [ ] Manejo de error para email duplicado
- [ ] Manejo de error para username duplicado
- [ ] Proyecto compila sin errores
- [ ] Prueba manual exitosa (registro nuevo usuario)
- [ ] Prueba manual exitosa (email duplicado retorna error)
- [ ] Verificación en BD (usuario creado correctamente)
- [ ] Contraseña hasheada con BCrypt (no texto plano)
- [ ] Peer asignado correctamente

---


# FUNCIONALIDAD 2: ENVIAR MENSAJE DE TEXTO A CANAL

## 📊 ANÁLISIS PREVIO

### **Estado Actual**

✅ **Lo que YA existe:**
```java
// 1. Entidades de dominio
Server-Nicolas/datos/server-dominio/src/main/java/com/arquitectura/domain/Message.java (abstracta)
Server-Nicolas/datos/server-dominio/src/main/java/com/arquitectura/domain/TextMessage.java

// 2. Repositorio
Server-Nicolas/datos/server-persistencia/src/main/java/com/arquitectura/persistence/repository/MessageRepository.java

// 3. Servicio con lógica
Server-Nicolas/negocio/server-LogicaMensajes/src/main/java/com/arquitectura/logicaMensajes/MessageServiceImpl.java
    → MessageResponseDto enviarMensajeTexto(SendMessageRequestDto requestDto, UUID autorId)

// 4. Fachada con método
Server-Nicolas/negocio/server-logicaFachada/src/main/java/com/arquitectura/fachada/ChatFachadaImpl.java
    → MessageResponseDto enviarMensajeTexto(SendMessageRequestDto requestDto, UUID autorId)

// 5. Sistema de eventos
Server-Nicolas/comunes/server-events/src/main/java/com/arquitectura/events/NewMessageEvent.java
Server-Nicolas/transporte/server-Transporte/src/main/java/com/arquitectura/transporte/ServerListener.java
    → @EventListener handleNewMessageEvent()
```

❌ **Lo que FALTA:**
```java
// 1. Endpoint en RequestDispatcher
case "enviarmensajecanal": // NO EXISTE
case "enviarmensajetexto": // NO EXISTE

// 2. DTO SendMessageRequestDto (verificar que exista)
```

### **Flujo Esperado**

```
Cliente envía:
{
  "action": "enviarMensajeCanal",
  "payload": {
    "canalId": "uuid-del-canal",
    "contenido": "Hola a todos!",
    "tipo": "TEXT"
  }
}

Servidor:
1. Valida que el usuario esté autenticado
2. Valida que el canal exista
3. Valida que el usuario sea miembro del canal
4. Crea TextMessage
5. Guarda en BD
6. Publica NewMessageEvent
7. ServerListener notifica a todos los miembros conectados

Servidor responde al remitente:
{
  "action": "enviarMensajeCanal",
  "status": "success",
  "message": "Mensaje enviado",
  "data": {
    "messageId": "uuid-del-mensaje",
    "channelId": "uuid-del-canal",
    "author": {
      "userId": "uuid-autor",
      "username": "juan"
    },
    "timestamp": "2025-11-05T10:30:00",
    "messageType": "TEXT",
    "content": "Hola a todos!"
  }
}

Servidor notifica a otros miembros (PUSH):
{
  "action": "nuevoMensajeCanal",
  "status": "success",
  "message": "Nuevo mensaje recibido",
  "data": {
    "messageId": "uuid-del-mensaje",
    "channelId": "uuid-del-canal",
    "author": {
      "userId": "uuid-autor",
      "username": "juan"
    },
    "timestamp": "2025-11-05T10:30:00",
    "messageType": "TEXT",
    "content": "Hola a todos!"
  }
}
```

---

## 🔧 IMPLEMENTACIÓN PASO A PASO

### **PASO 1: Verificar el DTO SendMessageRequestDto**

**Ubicación**: `Server-Nicolas/comunes/Server-DTO/src/main/java/com/arquitectura/DTO/Mensajes/SendMessageRequestDto.java`

**Acción**: Abrir el archivo y verificar que tenga estos campos:

```java
public class SendMessageRequestDto {
    private UUID channelId;  // o String channelId
    private String content;
    private String messageType; // "TEXT" o "AUDIO"
    
    // Constructor, getters, setters
}
```

**Si NO existe el archivo**, crearlo con este contenido:

```java
package com.arquitectura.DTO.Mensajes;

import java.util.UUID;

public class SendMessageRequestDto {
    private UUID channelId;
    private String content;
    private String messageType; // "TEXT" o "AUDIO"

    public SendMessageRequestDto() {
    }

    public SendMessageRequestDto(UUID channelId, String content, String messageType) {
        this.channelId = channelId;
        this.content = content;
        this.messageType = messageType;
    }

    // Getters y Setters
    public UUID getChannelId() {
        return channelId;
    }

    public void setChannelId(UUID channelId) {
        this.channelId = channelId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    @Override
    public String toString() {
        return "SendMessageRequestDto{" +
                "channelId=" + channelId +
                ", messageType='" + messageType + '\'' +
                ", contentLength=" + (content != null ? content.length() : 0) +
                '}';
    }
}
```

---

### **PASO 2: Verificar MessageServiceImpl.enviarMensajeTexto()**

**Ubicación**: `Server-Nicolas/negocio/server-LogicaMensajes/src/main/java/com/arquitectura/logicaMensajes/MessageServiceImpl.java`

**Acción**: Abrir el archivo y buscar el método `enviarMensajeTexto()`.

**Verificar que haga lo siguiente:**

1. ✅ Buscar el autor (User) por ID
2. ✅ Buscar el canal (Channel) por ID
3. ✅ Validar que el usuario sea miembro del canal
4. ✅ Crear TextMessage
5. ✅ Guardar en BD
6. ✅ Publicar NewMessageEvent
7. ✅ Retornar MessageResponseDto

**Código esperado** (aproximado):

```java
@Override
@Transactional
public MessageResponseDto enviarMensajeTexto(SendMessageRequestDto requestDto, UUID autorId) throws Exception {
    // 1. Buscar autor
    User author = userRepository.findById(autorId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

    // 2. Buscar canal
    Channel channel = channelRepository.findById(requestDto.getChannelId())
            .orElseThrow(() -> new IllegalArgumentException("Canal no encontrado"));

    // 3. Validar que el usuario sea miembro del canal
    boolean isMember = membresiaCanalRepository
            .findAllByUsuarioUserIdAndEstado(autorId, EstadoMembresia.ACEPTADO)
            .stream()
            .anyMatch(m -> m.getCanal().getChannelId().equals(requestDto.getChannelId()));

    if (!isMember) {
        throw new IllegalArgumentException("No eres miembro de este canal");
    }

    // 4. Crear TextMessage
    TextMessage message = new TextMessage(author, channel, requestDto.getContent());

    // 5. Guardar en BD
    TextMessage savedMessage = (TextMessage) messageRepository.save(message);

    // 6. Crear DTO de respuesta
    MessageResponseDto responseDto = new MessageResponseDto(
            savedMessage.getIdMensaje(),
            channel.getChannelId(),
            new UserResponseDto(author), // Simplificado
            savedMessage.getTimestamp(),
            "TEXT",
            savedMessage.getContent()
    );

    // 7. Obtener IDs de miembros del canal para notificar
    List<UUID> memberIds = membresiaCanalRepository
            .findAllByCanal_ChannelIdAndEstado(channel.getChannelId(), EstadoMembresia.ACEPTADO)
            .stream()
            .map(m -> m.getUsuario().getUserId())
            .filter(id -> !id.equals(autorId)) // Excluir al autor
            .collect(Collectors.toList());

    // 8. Publicar evento para notificar a otros miembros
    eventPublisher.publishEvent(new NewMessageEvent(this, responseDto, memberIds));

    log.info("Mensaje de texto enviado al canal {} por usuario {}", channel.getName(), author.getUsername());

    return responseDto;
}
```

**Si el método NO existe o está incompleto**, necesitarás agregarlo o completarlo.

---

### **PASO 3: Verificar ChatFachadaImpl.enviarMensajeTexto()**

**Ubicación**: `Server-Nicolas/negocio/server-logicaFachada/src/main/java/com/arquitectura/fachada/ChatFachadaImpl.java`

**Acción**: Abrir el archivo y buscar el método `enviarMensajeTexto()`.

**Código esperado**:

```java
@Override
public MessageResponseDto enviarMensajeTexto(SendMessageRequestDto requestDto, UUID autorId) throws Exception {
    return messageService.enviarMensajeTexto(requestDto, autorId);
}
```

**Si NO existe**, agregarlo en la sección de "Métodos de Mensajes".

---

### **PASO 4: Agregar el Endpoint en RequestDispatcher**

**Ubicación**: `Server-Nicolas/transporte/server-controladorTransporte/src/main/java/com/arquitectura/controlador/RequestDispatcher.java`

**Acción**: Agregar el nuevo caso en el switch statement.

**Agregar DESPUÉS de "listarcanales"**:

```java
case "enviarmensajecanal":
case "enviarmensajetexto":
    // 1. Validar autenticación (ya se hace arriba)
    
    // 2. Extraer payload
    Object mensajeDataObj = request.getPayload();
    if (mensajeDataObj == null) {
        sendJsonResponse(handler, "enviarMensajeCanal", false, "Falta payload", null);
        return;
    }

    // 3. Convertir a JSON y extraer campos
    JsonObject mensajeJson = gson.toJsonTree(mensajeDataObj).getAsJsonObject();
    String canalIdStr = mensajeJson.has("canalId") ? mensajeJson.get("canalId").getAsString() : null;
    String contenido = mensajeJson.has("contenido") ? mensajeJson.get("contenido").getAsString() : null;

    // 4. Validar campos requeridos
    if (canalIdStr == null || canalIdStr.trim().isEmpty()) {
        sendJsonResponse(handler, "enviarMensajeCanal", false, "El ID del canal es requerido",
            createErrorData("canalId", "Campo requerido"));
        return;
    }

    if (contenido == null || contenido.trim().isEmpty()) {
        sendJsonResponse(handler, "enviarMensajeCanal", false, "El contenido del mensaje es requerido",
            createErrorData("contenido", "Campo requerido"));
        return;
    }

    // 5. Validar longitud del mensaje (opcional pero recomendado)
    if (contenido.length() > 5000) {
        sendJsonResponse(handler, "enviarMensajeCanal", false, "El mensaje es demasiado largo (máximo 5000 caracteres)",
            createErrorData("contenido", "Máximo 5000 caracteres"));
        return;
    }

    try {
        // 6. Convertir canalId a UUID
        UUID canalId = UUID.fromString(canalIdStr);

        // 7. Obtener ID del usuario autenticado
        UUID autorId = handler.getAuthenticatedUser().getUserId();

        // 8. Crear DTO de request
        SendMessageRequestDto sendMessageDto = new SendMessageRequestDto(
            canalId,
            contenido,
            "TEXT"
        );

        // 9. Llamar a la fachada
        MessageResponseDto messageResponse = chatFachada.enviarMensajeTexto(sendMessageDto, autorId);

        // 10. Construir respuesta exitosa
        Map<String, Object> mensajeResponseData = new HashMap<>();
        mensajeResponseData.put("messageId", messageResponse.getMessageId().toString());
        mensajeResponseData.put("channelId", messageResponse.getChannelId().toString());
        mensajeResponseData.put("author", Map.of(
            "userId", messageResponse.getAuthor().getUserId().toString(),
            "username", messageResponse.getAuthor().getUsername()
        ));
        mensajeResponseData.put("timestamp", messageResponse.getTimestamp().toString());
        mensajeResponseData.put("messageType", messageResponse.getMessageType());
        mensajeResponseData.put("content", messageResponse.getContent());

        sendJsonResponse(handler, "enviarMensajeCanal", true, "Mensaje enviado", mensajeResponseData);

    } catch (IllegalArgumentException e) {
        // Error de validación (canal no existe, no es miembro, etc.)
        String errorMessage = e.getMessage();
        String campo = "general";
        
        if (errorMessage.contains("Canal")) {
            campo = "canalId";
        } else if (errorMessage.contains("miembro")) {
            campo = "permisos";
        }
        
        sendJsonResponse(handler, "enviarMensajeCanal", false, errorMessage,
            createErrorData(campo, errorMessage));
            
    } catch (Exception e) {
        // Error inesperado
        log.error("Error al enviar mensaje: {}", e.getMessage(), e);
        sendJsonResponse(handler, "enviarMensajeCanal", false, "Error interno del servidor al enviar mensaje", null);
    }
    break;
```

---

### **PASO 5: Verificar el Sistema de Notificaciones**

**Ubicación**: `Server-Nicolas/transporte/server-Transporte/src/main/java/com/arquitectura/transporte/ServerListener.java`

**Acción**: Verificar que existe el método `handleNewMessageEvent()`.

**Código esperado**:

```java
@EventListener
public void handleNewMessageEvent(NewMessageEvent event) {
    MessageResponseDto originalDto = event.getMessageDto();
    log.info("Nuevo mensaje en canal {}. Propagando a los miembros conectados.", originalDto.getChannelId());
    List<UUID> memberIds = event.getRecipientUserIds();
    
    // Para mensajes de texto, no necesitamos enriquecer
    // Para audio, sí (ya está implementado)
    MessageResponseDto dtoParaPropagar = requestDispatcher.enrichOutgoingMessage(originalDto);

    DTOResponse response = new DTOResponse("nuevoMensajeCanal", "success", "Nuevo mensaje recibido", dtoParaPropagar);
    String notification = gson.toJson(response);

    memberIds.forEach(memberId -> {
        List<IClientHandler> userSessions = activeClientsById.get(memberId);
        if (userSessions != null) {
            userSessions.forEach(handler -> handler.sendMessage(notification));
        }
    });
}
```

**Este método YA EXISTE**, solo verificar que esté presente.

---

### **PASO 6: Compilar y Probar**

**Compilar el proyecto**:

```bash
cd Server-Nicolas
mvn clean install -DskipTests
```

---

### **PASO 7: Probar con Cliente o Manualmente**

**Preparación**:
1. Tener un usuario autenticado
2. Tener un canal creado
3. El usuario debe ser miembro del canal

**Enviar mensaje**:

```json
{
  "action": "enviarMensajeCanal",
  "payload": {
    "canalId": "uuid-del-canal",
    "contenido": "Hola a todos!"
  }
}
```

**Respuesta esperada (éxito)**:
```json
{
  "action": "enviarMensajeCanal",
  "status": "success",
  "message": "Mensaje enviado",
  "data": {
    "messageId": "uuid-generado",
    "channelId": "uuid-del-canal",
    "author": {
      "userId": "uuid-autor",
      "username": "juan"
    },
    "timestamp": "2025-11-05T10:30:00",
    "messageType": "TEXT",
    "content": "Hola a todos!"
  }
}
```

**Respuesta esperada (no es miembro)**:
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

### **PASO 8: Verificar en Base de Datos**

```sql
USE chat_db;
SELECT * FROM messages ORDER BY timestamp DESC LIMIT 5;
```

**Verificar que el mensaje se haya creado con**:
- ✅ `id_mensaje` (UUID)
- ✅ `author_id` (UUID del usuario)
- ✅ `channel_id` (UUID del canal)
- ✅ `timestamp` (timestamp actual)
- ✅ `message_type` = 'TEXT'
- ✅ `content` = el texto del mensaje

---

### **PASO 9: Verificar Notificación Push**

**Si tienes 2 clientes conectados al mismo canal**:

1. Cliente A envía mensaje
2. Cliente A recibe respuesta de confirmación
3. Cliente B recibe notificación push con el mensaje

**Verificar en logs del servidor**:
```
INFO  Nuevo mensaje en canal NombreCanal. Propagando a los miembros conectados.
```

---

## ✅ CHECKLIST - FUNCIONALIDAD 2

- [ ] DTO `SendMessageRequestDto` existe y tiene todos los campos
- [ ] `MessageServiceImpl.enviarMensajeTexto()` existe y está completo
- [ ] Validación de membresía implementada
- [ ] `ChatFachadaImpl.enviarMensajeTexto()` existe y delega correctamente
- [ ] Caso `"enviarmensajecanal"` agregado en `RequestDispatcher.dispatch()`
- [ ] Validaciones de campos requeridos implementadas
- [ ] Validación de longitud de mensaje implementada
- [ ] Manejo de error para canal no encontrado
- [ ] Manejo de error para usuario no miembro
- [ ] Evento `NewMessageEvent` se publica correctamente
- [ ] `ServerListener.handleNewMessageEvent()` existe y funciona
- [ ] Proyecto compila sin errores
- [ ] Prueba manual exitosa (enviar mensaje)
- [ ] Prueba manual exitosa (no miembro retorna error)
- [ ] Verificación en BD (mensaje creado correctamente)
- [ ] Notificación push funciona (otros miembros reciben el mensaje)

---


# FUNCIONALIDAD 3: OBTENER HISTORIAL DE CANAL

## 📊 ANÁLISIS PREVIO

### **Estado Actual**

✅ **Lo que YA existe:**
```java
// 1. Repositorio con método
Server-Nicolas/datos/server-persistencia/src/main/java/com/arquitectura/persistence/repository/MessageRepository.java
    → List<Message> findByChannel_ChannelIdOrderByTimestampAsc(UUID channelId)

// 2. Servicio con lógica
Server-Nicolas/negocio/server-LogicaMensajes/src/main/java/com/arquitectura/logicaMensajes/MessageServiceImpl.java
    → List<MessageResponseDto> obtenerMensajesPorCanal(UUID canalId, UUID userId)

// 3. Fachada con método
Server-Nicolas/negocio/server-logicaFachada/src/main/java/com/arquitectura/fachada/ChatFachadaImpl.java
    → List<MessageResponseDto> obtenerMensajesDeCanal(UUID canalId, UUID userId)
```

❌ **Lo que FALTA:**
```java
// 1. Endpoint en RequestDispatcher
case "solicitarhistorialcanal": // NO EXISTE
case "obtenermensajescanal": // NO EXISTE
```

### **Flujo Esperado**

```
Cliente envía:
{
  "action": "solicitarHistorialCanal",
  "payload": {
    "canalId": "uuid-del-canal",
    "usuarioId": "uuid-del-usuario"
  }
}

Servidor:
1. Valida que el usuario esté autenticado
2. Valida que el canal exista
3. Valida que el usuario sea miembro del canal
4. Obtiene todos los mensajes del canal (ordenados por timestamp)
5. Para mensajes de audio, codifica el contenido a Base64
6. Retorna lista de mensajes

Servidor responde:
{
  "action": "solicitarHistorialCanal",
  "status": "success",
  "message": "Historial obtenido",
  "data": {
    "mensajes": [
      {
        "messageId": "uuid-1",
        "channelId": "uuid-del-canal",
        "author": {
          "userId": "uuid-autor-1",
          "username": "juan"
        },
        "timestamp": "2025-11-05T10:00:00",
        "messageType": "TEXT",
        "content": "Hola!"
      },
      {
        "messageId": "uuid-2",
        "channelId": "uuid-del-canal",
        "author": {
          "userId": "uuid-autor-2",
          "username": "maria"
        },
        "timestamp": "2025-11-05T10:05:00",
        "messageType": "TEXT",
        "content": "Hola Juan!"
      },
      {
        "messageId": "uuid-3",
        "channelId": "uuid-del-canal",
        "author": {
          "userId": "uuid-autor-1",
          "username": "juan"
        },
        "timestamp": "2025-11-05T10:10:00",
        "messageType": "AUDIO",
        "content": "base64-encoded-audio-data..."
      }
    ],
    "totalMensajes": 3
  }
}
```

---

## 🔧 IMPLEMENTACIÓN PASO A PASO

### **PASO 1: Verificar MessageRepository**

**Ubicación**: `Server-Nicolas/datos/server-persistencia/src/main/java/com/arquitectura/persistence/repository/MessageRepository.java`

**Acción**: Abrir el archivo y verificar que tenga este método:

```java
@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    
    // Método para obtener mensajes de un canal ordenados por timestamp
    List<Message> findByChannel_ChannelIdOrderByTimestampAsc(UUID channelId);
    
    // O si prefieres descendente (más recientes primero):
    // List<Message> findByChannel_ChannelIdOrderByTimestampDesc(UUID channelId);
}
```

**Si NO existe**, agregarlo.

---

### **PASO 2: Verificar MessageServiceImpl.obtenerMensajesPorCanal()**

**Ubicación**: `Server-Nicolas/negocio/server-LogicaMensajes/src/main/java/com/arquitectura/logicaMensajes/MessageServiceImpl.java`

**Acción**: Abrir el archivo y buscar el método `obtenerMensajesPorCanal()`.

**Verificar que haga lo siguiente:**

1. ✅ Validar que el canal exista
2. ✅ Validar que el usuario sea miembro del canal
3. ✅ Obtener mensajes del repositorio
4. ✅ Convertir cada Message a MessageResponseDto
5. ✅ Para mensajes de audio, incluir la ruta del archivo (NO el Base64 aquí)
6. ✅ Retornar lista de DTOs

**Código esperado** (aproximado):

```java
@Override
@Transactional(readOnly = true)
public List<MessageResponseDto> obtenerMensajesPorCanal(UUID canalId, UUID userId) throws Exception {
    // 1. Validar que el canal exista
    Channel channel = channelRepository.findById(canalId)
            .orElseThrow(() -> new IllegalArgumentException("Canal no encontrado"));

    // 2. Validar que el usuario sea miembro del canal
    boolean isMember = membresiaCanalRepository
            .findAllByUsuarioUserIdAndEstado(userId, EstadoMembresia.ACEPTADO)
            .stream()
            .anyMatch(m -> m.getCanal().getChannelId().equals(canalId));

    if (!isMember) {
        throw new IllegalArgumentException("No eres miembro de este canal");
    }

    // 3. Obtener mensajes del repositorio (ordenados por timestamp)
    List<Message> messages = messageRepository.findByChannel_ChannelIdOrderByTimestampAsc(canalId);

    // 4. Convertir a DTOs
    List<MessageResponseDto> messageDtos = new ArrayList<>();
    
    for (Message message : messages) {
        MessageResponseDto dto;
        
        if (message instanceof TextMessage) {
            TextMessage textMsg = (TextMessage) message;
            dto = new MessageResponseDto(
                textMsg.getIdMensaje(),
                channel.getChannelId(),
                convertUserToDto(textMsg.getAuthor()),
                textMsg.getTimestamp(),
                "TEXT",
                textMsg.getContent()
            );
        } else if (message instanceof AudioMessage) {
            AudioMessage audioMsg = (AudioMessage) message;
            // Para audio, guardamos la ruta del archivo
            // El RequestDispatcher se encargará de codificar a Base64 si es necesario
            dto = new MessageResponseDto(
                audioMsg.getIdMensaje(),
                channel.getChannelId(),
                convertUserToDto(audioMsg.getAuthor()),
                audioMsg.getTimestamp(),
                "AUDIO",
                audioMsg.getAudioUrl() // Ruta relativa del archivo
            );
        } else {
            // Tipo de mensaje desconocido, saltar
            continue;
        }
        
        messageDtos.add(dto);
    }

    log.info("Historial de canal {} obtenido: {} mensajes", channel.getName(), messageDtos.size());

    return messageDtos;
}

// Método auxiliar para convertir User a UserResponseDto
private UserResponseDto convertUserToDto(User user) {
    UserResponseDto dto = new UserResponseDto();
    dto.setUserId(user.getUserId());
    dto.setUsername(user.getUsername());
    dto.setEmail(user.getEmail());
    dto.setPhotoAddress(user.getPhotoAddress());
    dto.setEstado(user.getConectado());
    return dto;
}
```

**Si el método NO existe o está incompleto**, necesitarás agregarlo o completarlo.

---

### **PASO 3: Verificar ChatFachadaImpl.obtenerMensajesDeCanal()**

**Ubicación**: `Server-Nicolas/negocio/server-logicaFachada/src/main/java/com/arquitectura/fachada/ChatFachadaImpl.java`

**Acción**: Abrir el archivo y buscar el método `obtenerMensajesDeCanal()`.

**Código esperado**:

```java
@Override
public List<MessageResponseDto> obtenerMensajesDeCanal(UUID canalId, UUID userId) throws Exception {
    return messageService.obtenerMensajesPorCanal(canalId, userId);
}
```

**Si NO existe**, agregarlo en la sección de "Métodos de Mensajes".

---

### **PASO 4: Agregar el Endpoint en RequestDispatcher**

**Ubicación**: `Server-Nicolas/transporte/server-controladorTransporte/src/main/java/com/arquitectura/controlador/RequestDispatcher.java`

**Acción**: Agregar el nuevo caso en el switch statement.

**Agregar DESPUÉS de "enviarmensajecanal"**:

```java
case "solicitarhistorialcanal":
case "obtenermensajescanal":
    // 1. Validar autenticación (ya se hace arriba)
    
    // 2. Extraer payload
    Object historialDataObj = request.getPayload();
    if (historialDataObj == null) {
        sendJsonResponse(handler, "solicitarHistorialCanal", false, "Falta payload", null);
        return;
    }

    // 3. Convertir a JSON y extraer campos
    JsonObject historialJson = gson.toJsonTree(historialDataObj).getAsJsonObject();
    String histCanalIdStr = historialJson.has("canalId") ? historialJson.get("canalId").getAsString() : null;
    String histUsuarioIdStr = historialJson.has("usuarioId") ? historialJson.get("usuarioId").getAsString() : null;

    // 4. Validar campos requeridos
    if (histCanalIdStr == null || histCanalIdStr.trim().isEmpty()) {
        sendJsonResponse(handler, "solicitarHistorialCanal", false, "El ID del canal es requerido",
            createErrorData("canalId", "Campo requerido"));
        return;
    }

    if (histUsuarioIdStr == null || histUsuarioIdStr.trim().isEmpty()) {
        sendJsonResponse(handler, "solicitarHistorialCanal", false, "El ID del usuario es requerido",
            createErrorData("usuarioId", "Campo requerido"));
        return;
    }

    try {
        // 5. Convertir a UUIDs
        UUID histCanalId = UUID.fromString(histCanalIdStr);
        UUID histUsuarioId = UUID.fromString(histUsuarioIdStr);

        // 6. Validar que el usuario autenticado coincida con el solicitante (seguridad)
        if (!handler.getAuthenticatedUser().getUserId().equals(histUsuarioId)) {
            sendJsonResponse(handler, "solicitarHistorialCanal", false, "No autorizado para ver este historial",
                createErrorData("permisos", "Usuario no autorizado"));
            return;
        }

        // 7. Llamar a la fachada
        List<MessageResponseDto> mensajes = chatFachada.obtenerMensajesDeCanal(histCanalId, histUsuarioId);

        // 8. Enriquecer mensajes de audio con Base64
        List<Map<String, Object>> mensajesEnriquecidos = new ArrayList<>();
        
        for (MessageResponseDto mensaje : mensajes) {
            Map<String, Object> mensajeMap = new HashMap<>();
            mensajeMap.put("messageId", mensaje.getMessageId().toString());
            mensajeMap.put("channelId", mensaje.getChannelId().toString());
            mensajeMap.put("author", Map.of(
                "userId", mensaje.getAuthor().getUserId().toString(),
                "username", mensaje.getAuthor().getUsername()
            ));
            mensajeMap.put("timestamp", mensaje.getTimestamp().toString());
            mensajeMap.put("messageType", mensaje.getMessageType());
            
            // Para mensajes de audio, codificar a Base64
            if ("AUDIO".equals(mensaje.getMessageType())) {
                try {
                    String base64Content = chatFachada.getFileAsBase64(mensaje.getContent());
                    mensajeMap.put("content", base64Content);
                } catch (Exception e) {
                    log.error("Error al codificar audio a Base64: {}", e.getMessage());
                    mensajeMap.put("content", null);
                    mensajeMap.put("error", "Audio no disponible");
                }
            } else {
                // Para mensajes de texto, usar el contenido directamente
                mensajeMap.put("content", mensaje.getContent());
            }
            
            mensajesEnriquecidos.add(mensajeMap);
        }

        // 9. Construir respuesta exitosa
        Map<String, Object> historialResponseData = new HashMap<>();
        historialResponseData.put("mensajes", mensajesEnriquecidos);
        historialResponseData.put("totalMensajes", mensajes.size());

        sendJsonResponse(handler, "solicitarHistorialCanal", true, "Historial obtenido", historialResponseData);

    } catch (IllegalArgumentException e) {
        // Error de validación (canal no existe, no es miembro, etc.)
        String errorMessage = e.getMessage();
        String campo = "general";
        
        if (errorMessage.contains("Canal")) {
            campo = "canalId";
        } else if (errorMessage.contains("miembro")) {
            campo = "permisos";
        }
        
        sendJsonResponse(handler, "solicitarHistorialCanal", false, errorMessage,
            createErrorData(campo, errorMessage));
            
    } catch (Exception e) {
        // Error inesperado
        log.error("Error al obtener historial: {}", e.getMessage(), e);
        sendJsonResponse(handler, "solicitarHistorialCanal", false, "Error interno del servidor al obtener historial", null);
    }
    break;
```

---

### **PASO 5: Verificar el Método getFileAsBase64() en ChatFachadaImpl**

**Ubicación**: `Server-Nicolas/negocio/server-logicaFachada/src/main/java/com/arquitectura/fachada/ChatFachadaImpl.java`

**Acción**: Verificar que existe el método `getFileAsBase64()`.

**Código esperado**:

```java
@Override
public String getFileAsBase64(String relativePath) {
    try {
        return fileStorageService.readFileAsBase64(relativePath);
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
}
```

**Este método YA EXISTE**, solo verificar que esté presente.

---

### **PASO 6: Compilar y Probar**

**Compilar el proyecto**:

```bash
cd Server-Nicolas
mvn clean install -DskipTests
```

---

### **PASO 7: Probar con Cliente o Manualmente**

**Preparación**:
1. Tener un usuario autenticado
2. Tener un canal con mensajes
3. El usuario debe ser miembro del canal

**Solicitar historial**:

```json
{
  "action": "solicitarHistorialCanal",
  "payload": {
    "canalId": "uuid-del-canal",
    "usuarioId": "uuid-del-usuario"
  }
}
```

**Respuesta esperada (éxito con mensajes)**:
```json
{
  "action": "solicitarHistorialCanal",
  "status": "success",
  "message": "Historial obtenido",
  "data": {
    "mensajes": [
      {
        "messageId": "uuid-1",
        "channelId": "uuid-del-canal",
        "author": {
          "userId": "uuid-autor-1",
          "username": "juan"
        },
        "timestamp": "2025-11-05T10:00:00",
        "messageType": "TEXT",
        "content": "Hola!"
      },
      {
        "messageId": "uuid-2",
        "channelId": "uuid-del-canal",
        "author": {
          "userId": "uuid-autor-2",
          "username": "maria"
        },
        "timestamp": "2025-11-05T10:05:00",
        "messageType": "TEXT",
        "content": "Hola Juan!"
      }
    ],
    "totalMensajes": 2
  }
}
```

**Respuesta esperada (canal vacío)**:
```json
{
  "action": "solicitarHistorialCanal",
  "status": "success",
  "message": "Historial obtenido",
  "data": {
    "mensajes": [],
    "totalMensajes": 0
  }
}
```

**Respuesta esperada (no es miembro)**:
```json
{
  "action": "solicitarHistorialCanal",
  "status": "error",
  "message": "No eres miembro de este canal",
  "data": {
    "campo": "permisos",
    "motivo": "No eres miembro de este canal"
  }
}
```

---

### **PASO 8: Verificar Orden de Mensajes**

**Los mensajes deben estar ordenados cronológicamente** (del más antiguo al más reciente).

Si quieres cambiar el orden (más recientes primero), modifica el método del repositorio:

```java
// En MessageRepository.java
List<Message> findByChannel_ChannelIdOrderByTimestampDesc(UUID channelId);
```

---

### **PASO 9: Probar con Mensajes de Audio**

**Si el canal tiene mensajes de audio**:

1. Verificar que el campo `content` contenga datos Base64
2. Verificar que el cliente pueda decodificar y reproducir el audio

---

## ✅ CHECKLIST - FUNCIONALIDAD 3

- [ ] `MessageRepository.findByChannel_ChannelIdOrderByTimestampAsc()` existe
- [ ] `MessageServiceImpl.obtenerMensajesPorCanal()` existe y está completo
- [ ] Validación de membresía implementada
- [ ] Conversión de Message a MessageResponseDto implementada
- [ ] Manejo de TextMessage y AudioMessage implementado
- [ ] `ChatFachadaImpl.obtenerMensajesDeCanal()` existe y delega correctamente
- [ ] `ChatFachadaImpl.getFileAsBase64()` existe y funciona
- [ ] Caso `"solicitarhistorialcanal"` agregado en `RequestDispatcher.dispatch()`
- [ ] Validaciones de campos requeridos implementadas
- [ ] Validación de autorización (usuario autenticado = usuario solicitante)
- [ ] Enriquecimiento de mensajes de audio con Base64
- [ ] Manejo de error para canal no encontrado
- [ ] Manejo de error para usuario no miembro
- [ ] Proyecto compila sin errores
- [ ] Prueba manual exitosa (obtener historial con mensajes)
- [ ] Prueba manual exitosa (obtener historial vacío)
- [ ] Prueba manual exitosa (no miembro retorna error)
- [ ] Mensajes ordenados cronológicamente
- [ ] Mensajes de audio codificados correctamente en Base64

---


# FUNCIONALIDAD 4: LISTAR MIEMBROS DE CANAL

## 📊 ANÁLISIS PREVIO

### **Estado Actual**

✅ **Lo que YA existe:**
```java
// 1. Entidad de relación
Server-Nicolas/datos/server-dominio/src/main/java/com/arquitectura/domain/MembresiaCanal.java

// 2. Repositorio
Server-Nicolas/datos/server-persistencia/src/main/java/com/arquitectura/persistence/repository/MembresiaCanalRepository.java
    → List<MembresiaCanal> findAllByCanal_ChannelIdAndEstado(UUID channelId, EstadoMembresia estado)

// 3. Relación en Channel
Server-Nicolas/datos/server-dominio/src/main/java/com/arquitectura/domain/Channel.java
    → Set<MembresiaCanal> getMembresias()
```

❌ **Lo que FALTA:**
```java
// 1. Método en ChannelService
List<UserResponseDto> obtenerMiembrosDeCanal(UUID canalId, UUID solicitanteId)

// 2. Método en ChatFachada
List<UserResponseDto> obtenerMiembrosDeCanal(UUID canalId, UUID solicitanteId)

// 3. Endpoint en RequestDispatcher
case "listarmiembros": // NO EXISTE
case "obtenermiembroscanal": // NO EXISTE
```

### **Flujo Esperado**

```
Cliente envía:
{
  "action": "listarMiembros",
  "payload": {
    "canalId": "uuid-del-canal",
    "solicitanteId": "uuid-del-usuario"
  }
}

Servidor:
1. Valida que el usuario esté autenticado
2. Valida que el canal exista
3. Valida que el usuario sea miembro del canal
4. Obtiene todas las membresías ACEPTADAS del canal
5. Convierte cada membresía a UserResponseDto
6. Retorna lista de miembros

Servidor responde:
{
  "action": "listarMiembros",
  "status": "success",
  "message": "Miembros obtenidos",
  "data": {
    "miembros": [
      {
        "userId": "uuid-1",
        "username": "juan",
        "email": "juan@mail.com",
        "photoAddress": "user_photos/juan.jpg",
        "conectado": true,
        "rol": "ADMIN"
      },
      {
        "userId": "uuid-2",
        "username": "maria",
        "email": "maria@mail.com",
        "photoAddress": "user_photos/maria.jpg",
        "conectado": false,
        "rol": "MIEMBRO"
      }
    ],
    "totalMiembros": 2,
    "canalId": "uuid-del-canal"
  }
}
```

---

## 🔧 IMPLEMENTACIÓN PASO A PASO

### **PASO 1: Verificar MembresiaCanalRepository**

**Ubicación**: `Server-Nicolas/datos/server-persistencia/src/main/java/com/arquitectura/persistence/repository/MembresiaCanalRepository.java`

**Acción**: Abrir el archivo y verificar que tenga este método:

```java
@Repository
public interface MembresiaCanalRepository extends JpaRepository<MembresiaCanal, MembresiaCanalId> {
    
    // Obtener membresías de un usuario con un estado específico
    List<MembresiaCanal> findAllByUsuarioUserIdAndEstado(UUID userId, EstadoMembresia estado);
    
    // Obtener membresías de un canal con un estado específico
    List<MembresiaCanal> findAllByCanal_ChannelIdAndEstado(UUID channelId, EstadoMembresia estado);
}
```

**Este método YA EXISTE**, solo verificar que esté presente.

---

### **PASO 2: Agregar Método en IChannelService**

**Ubicación**: `Server-Nicolas/negocio/server-LogicaCanales/src/main/java/com/arquitectura/logicaCanales/IChannelService.java`

**Acción**: Abrir el archivo y agregar la firma del método.

**Agregar al final de la interfaz**:

```java
/**
 * Obtiene la lista de miembros de un canal.
 * @param canalId El ID del canal.
 * @param solicitanteId El ID del usuario que solicita la lista.
 * @return Lista de usuarios que son miembros del canal.
 * @throws Exception si el canal no existe o el solicitante no es miembro.
 */
List<UserResponseDto> obtenerMiembrosDeCanal(UUID canalId, UUID solicitanteId) throws Exception;
```

---

### **PASO 3: Implementar Método en ChannelServiceImpl**

**Ubicación**: `Server-Nicolas/negocio/server-LogicaCanales/src/main/java/com/arquitectura/logicaCanales/ChannelServiceImpl.java`

**Acción**: Abrir el archivo y agregar la implementación del método.

**Agregar al final de la clase**:

```java
@Override
@Transactional(readOnly = true)
public List<UserResponseDto> obtenerMiembrosDeCanal(UUID canalId, UUID solicitanteId) throws Exception {
    // 1. Validar que el canal exista
    Channel channel = channelRepository.findById(canalId)
            .orElseThrow(() -> new IllegalArgumentException("Canal no encontrado"));

    // 2. Validar que el solicitante sea miembro del canal
    boolean isMember = membresiaCanalRepository
            .findAllByUsuarioUserIdAndEstado(solicitanteId, EstadoMembresia.ACEPTADO)
            .stream()
            .anyMatch(m -> m.getCanal().getChannelId().equals(canalId));

    if (!isMember) {
        throw new IllegalArgumentException("No eres miembro de este canal");
    }

    // 3. Obtener todas las membresías ACEPTADAS del canal
    List<MembresiaCanal> membresias = membresiaCanalRepository
            .findAllByCanal_ChannelIdAndEstado(canalId, EstadoMembresia.ACEPTADO);

    // 4. Convertir a UserResponseDto
    List<UserResponseDto> miembros = new ArrayList<>();
    
    for (MembresiaCanal membresia : membresias) {
        User usuario = membresia.getUsuario();
        
        UserResponseDto dto = new UserResponseDto();
        dto.setUserId(usuario.getUserId());
        dto.setUsername(usuario.getUsername());
        dto.setEmail(usuario.getEmail());
        dto.setPhotoAddress(usuario.getPhotoAddress());
        dto.setEstado(usuario.getConectado());
        
        // Determinar el rol (si es el owner del canal, es ADMIN)
        if (channel.getOwner().getUserId().equals(usuario.getUserId())) {
            dto.setRol("ADMIN");
        } else {
            dto.setRol("MIEMBRO");
        }
        
        miembros.add(dto);
    }

    log.info("Miembros del canal {} obtenidos: {} miembros", channel.getName(), miembros.size());

    return miembros;
}
```

**Nota**: Asegúrate de que `UserResponseDto` tenga el campo `rol`. Si no lo tiene, agrégalo:

```java
// En UserResponseDto.java
private String rol; // "ADMIN" o "MIEMBRO"

public String getRol() {
    return rol;
}

public void setRol(String rol) {
    this.rol = rol;
}
```

---

### **PASO 4: Agregar Método en IChatFachada**

**Ubicación**: `Server-Nicolas/negocio/server-logicaFachada/src/main/java/com/arquitectura/fachada/IChatFachada.java`

**Acción**: Abrir el archivo y agregar la firma del método.

**Agregar en la sección de "Métodos de Canal"**:

```java
/**
 * Obtiene la lista de miembros de un canal.
 * @param canalId El ID del canal.
 * @param solicitanteId El ID del usuario que solicita la lista.
 * @return Lista de usuarios que son miembros del canal.
 * @throws Exception si el canal no existe o el solicitante no es miembro.
 */
List<UserResponseDto> obtenerMiembrosDeCanal(UUID canalId, UUID solicitanteId) throws Exception;
```

---

### **PASO 5: Implementar Método en ChatFachadaImpl**

**Ubicación**: `Server-Nicolas/negocio/server-logicaFachada/src/main/java/com/arquitectura/fachada/ChatFachadaImpl.java`

**Acción**: Abrir el archivo y agregar la implementación del método.

**Agregar en la sección de "Métodos de Canales"**:

```java
@Override
public List<UserResponseDto> obtenerMiembrosDeCanal(UUID canalId, UUID solicitanteId) throws Exception {
    return channelService.obtenerMiembrosDeCanal(canalId, solicitanteId);
}
```

---

### **PASO 6: Agregar el Endpoint en RequestDispatcher**

**Ubicación**: `Server-Nicolas/transporte/server-controladorTransporte/src/main/java/com/arquitectura/controlador/RequestDispatcher.java`

**Acción**: Agregar el nuevo caso en el switch statement.

**Agregar DESPUÉS de "solicitarhistorialcanal"**:

```java
case "listarmiembros":
case "obtenermiembroscanal":
    // 1. Validar autenticación (ya se hace arriba)
    
    // 2. Extraer payload
    Object miembrosDataObj = request.getPayload();
    if (miembrosDataObj == null) {
        sendJsonResponse(handler, "listarMiembros", false, "Falta payload", null);
        return;
    }

    // 3. Convertir a JSON y extraer campos
    JsonObject miembrosJson = gson.toJsonTree(miembrosDataObj).getAsJsonObject();
    String miembrosCanalIdStr = miembrosJson.has("canalId") ? miembrosJson.get("canalId").getAsString() : null;
    String solicitanteIdStr = miembrosJson.has("solicitanteId") ? miembrosJson.get("solicitanteId").getAsString() : null;

    // 4. Validar campos requeridos
    if (miembrosCanalIdStr == null || miembrosCanalIdStr.trim().isEmpty()) {
        sendJsonResponse(handler, "listarMiembros", false, "El ID del canal es requerido",
            createErrorData("canalId", "Campo requerido"));
        return;
    }

    if (solicitanteIdStr == null || solicitanteIdStr.trim().isEmpty()) {
        sendJsonResponse(handler, "listarMiembros", false, "El ID del solicitante es requerido",
            createErrorData("solicitanteId", "Campo requerido"));
        return;
    }

    try {
        // 5. Convertir a UUIDs
        UUID miembrosCanalId = UUID.fromString(miembrosCanalIdStr);
        UUID solicitanteId = UUID.fromString(solicitanteIdStr);

        // 6. Validar que el usuario autenticado coincida con el solicitante (seguridad)
        if (!handler.getAuthenticatedUser().getUserId().equals(solicitanteId)) {
            sendJsonResponse(handler, "listarMiembros", false, "No autorizado para ver estos miembros",
                createErrorData("permisos", "Usuario no autorizado"));
            return;
        }

        // 7. Llamar a la fachada
        List<UserResponseDto> miembros = chatFachada.obtenerMiembrosDeCanal(miembrosCanalId, solicitanteId);

        // 8. Construir lista de miembros para la respuesta
        List<Map<String, Object>> miembrosData = new ArrayList<>();
        
        for (UserResponseDto miembro : miembros) {
            Map<String, Object> miembroMap = new HashMap<>();
            miembroMap.put("userId", miembro.getUserId().toString());
            miembroMap.put("username", miembro.getUsername());
            miembroMap.put("email", miembro.getEmail());
            miembroMap.put("photoAddress", miembro.getPhotoAddress());
            miembroMap.put("conectado", miembro.getEstado());
            miembroMap.put("rol", miembro.getRol() != null ? miembro.getRol() : "MIEMBRO");
            
            miembrosData.add(miembroMap);
        }

        // 9. Construir respuesta exitosa
        Map<String, Object> miembrosResponseData = new HashMap<>();
        miembrosResponseData.put("miembros", miembrosData);
        miembrosResponseData.put("totalMiembros", miembros.size());
        miembrosResponseData.put("canalId", miembrosCanalIdStr);

        sendJsonResponse(handler, "listarMiembros", true, "Miembros obtenidos", miembrosResponseData);

    } catch (IllegalArgumentException e) {
        // Error de validación (canal no existe, no es miembro, etc.)
        String errorMessage = e.getMessage();
        String campo = "general";
        
        if (errorMessage.contains("Canal")) {
            campo = "canalId";
        } else if (errorMessage.contains("miembro")) {
            campo = "permisos";
        }
        
        sendJsonResponse(handler, "listarMiembros", false, errorMessage,
            createErrorData(campo, errorMessage));
            
    } catch (Exception e) {
        // Error inesperado
        log.error("Error al listar miembros: {}", e.getMessage(), e);
        sendJsonResponse(handler, "listarMiembros", false, "Error interno del servidor al listar miembros", null);
    }
    break;
```

---

### **PASO 7: Compilar y Probar**

**Compilar el proyecto**:

```bash
cd Server-Nicolas
mvn clean install -DskipTests
```

---

### **PASO 8: Probar con Cliente o Manualmente**

**Preparación**:
1. Tener un usuario autenticado
2. Tener un canal con varios miembros
3. El usuario debe ser miembro del canal

**Solicitar lista de miembros**:

```json
{
  "action": "listarMiembros",
  "payload": {
    "canalId": "uuid-del-canal",
    "solicitanteId": "uuid-del-usuario"
  }
}
```

**Respuesta esperada (éxito)**:
```json
{
  "action": "listarMiembros",
  "status": "success",
  "message": "Miembros obtenidos",
  "data": {
    "miembros": [
      {
        "userId": "uuid-1",
        "username": "juan",
        "email": "juan@mail.com",
        "photoAddress": "user_photos/juan.jpg",
        "conectado": true,
        "rol": "ADMIN"
      },
      {
        "userId": "uuid-2",
        "username": "maria",
        "email": "maria@mail.com",
        "photoAddress": "user_photos/maria.jpg",
        "conectado": false,
        "rol": "MIEMBRO"
      }
    ],
    "totalMiembros": 2,
    "canalId": "uuid-del-canal"
  }
}
```

**Respuesta esperada (no es miembro)**:
```json
{
  "action": "listarMiembros",
  "status": "error",
  "message": "No eres miembro de este canal",
  "data": {
    "campo": "permisos",
    "motivo": "No eres miembro de este canal"
  }
}
```

---

### **PASO 9: Verificar en Base de Datos**

```sql
USE chat_db;

-- Ver membresías de un canal
SELECT 
    mc.user_id,
    u.username,
    u.email,
    mc.estado,
    c.channel_name
FROM membresia_canal mc
JOIN users u ON mc.user_id = u.user_id
JOIN channels c ON mc.channel_id = c.channel_id
WHERE mc.channel_id = 'uuid-del-canal'
  AND mc.estado = 'ACEPTADO';
```

---

### **PASO 10: Verificar Roles**

**El owner del canal debe aparecer con rol "ADMIN"**:

```sql
-- Ver owner del canal
SELECT 
    c.channel_id,
    c.channel_name,
    c.owner_id,
    u.username as owner_username
FROM channels c
JOIN users u ON c.owner_id = u.user_id
WHERE c.channel_id = 'uuid-del-canal';
```

---

## ✅ CHECKLIST - FUNCIONALIDAD 4

- [ ] `MembresiaCanalRepository.findAllByCanal_ChannelIdAndEstado()` existe
- [ ] Método `obtenerMiembrosDeCanal()` agregado en `IChannelService`
- [ ] Método `obtenerMiembrosDeCanal()` implementado en `ChannelServiceImpl`
- [ ] Validación de membresía del solicitante implementada
- [ ] Conversión de MembresiaCanal a UserResponseDto implementada
- [ ] Determinación de rol (ADMIN/MIEMBRO) implementada
- [ ] Campo `rol` agregado en `UserResponseDto` (si no existía)
- [ ] Método `obtenerMiembrosDeCanal()` agregado en `IChatFachada`
- [ ] Método `obtenerMiembrosDeCanal()` implementado en `ChatFachadaImpl`
- [ ] Caso `"listarmiembros"` agregado en `RequestDispatcher.dispatch()`
- [ ] Validaciones de campos requeridos implementadas
- [ ] Validación de autorización (usuario autenticado = solicitante)
- [ ] Construcción de respuesta con lista de miembros
- [ ] Manejo de error para canal no encontrado
- [ ] Manejo de error para usuario no miembro
- [ ] Proyecto compila sin errores
- [ ] Prueba manual exitosa (listar miembros)
- [ ] Prueba manual exitosa (no miembro retorna error)
- [ ] Verificación en BD (membresías correctas)
- [ ] Owner del canal aparece con rol "ADMIN"
- [ ] Otros miembros aparecen con rol "MIEMBRO"

---


# TESTING Y VALIDACIÓN

## 🧪 PLAN DE PRUEBAS COMPLETO

### **Preparación del Entorno de Pruebas**

#### **1. Iniciar Base de Datos**

```bash
cd Server-Nicolas
docker-compose up -d
```

**Verificar que MySQL esté corriendo**:
```bash
docker ps
# Debe aparecer: chat-db
```

#### **2. Compilar el Servidor**

```bash
cd Server-Nicolas
mvn clean install -DskipTests
```

#### **3. Iniciar el Servidor**

**Opción A: Desde IDE**
- Ejecutar `ServerLauncher.java` como aplicación Java

**Opción B: Desde línea de comandos**
```bash
cd Server-Nicolas/comunes/server-app
mvn exec:java -Dexec.mainClass="com.arquitectura.app.ServerLauncher"
```

**Verificar que el servidor esté escuchando**:
```
INFO  Servidor de Chat iniciado en el puerto 22100 con un límite de 100 conexiones.
```

---

## 📝 CASOS DE PRUEBA

### **FUNCIONALIDAD 1: REGISTRO DE USUARIOS**

#### **Caso 1.1: Registro Exitoso**

**Entrada**:
```json
{
  "action": "registerUser",
  "payload": {
    "username": "testuser1",
    "email": "test1@mail.com",
    "password": "123456"
  }
}
```

**Salida Esperada**:
```json
{
  "action": "registerUser",
  "status": "success",
  "message": "Registro exitoso",
  "data": {
    "username": "testuser1",
    "email": "test1@mail.com",
    "message": "Usuario registrado exitosamente. Ahora puedes iniciar sesión."
  }
}
```

**Verificación en BD**:
```sql
SELECT * FROM users WHERE email = 'test1@mail.com';
-- Debe existir el usuario con contraseña hasheada
```

---

#### **Caso 1.2: Email Duplicado**

**Entrada**:
```json
{
  "action": "registerUser",
  "payload": {
    "username": "testuser2",
    "email": "test1@mail.com",
    "password": "123456"
  }
}
```

**Salida Esperada**:
```json
{
  "action": "registerUser",
  "status": "error",
  "message": "El email ya está registrado",
  "data": {
    "campo": "email",
    "motivo": "El email ya está registrado"
  }
}
```

---

#### **Caso 1.3: Username Duplicado**

**Entrada**:
```json
{
  "action": "registerUser",
  "payload": {
    "username": "testuser1",
    "email": "test2@mail.com",
    "password": "123456"
  }
}
```

**Salida Esperada**:
```json
{
  "action": "registerUser",
  "status": "error",
  "message": "El nombre de usuario ya está en uso",
  "data": {
    "campo": "username",
    "motivo": "El nombre de usuario ya está en uso"
  }
}
```

---

#### **Caso 1.4: Contraseña Corta**

**Entrada**:
```json
{
  "action": "registerUser",
  "payload": {
    "username": "testuser3",
    "email": "test3@mail.com",
    "password": "123"
  }
}
```

**Salida Esperada**:
```json
{
  "action": "registerUser",
  "status": "error",
  "message": "La contraseña debe tener al menos 6 caracteres",
  "data": {
    "campo": "password",
    "motivo": "Mínimo 6 caracteres"
  }
}
```

---

#### **Caso 1.5: Email Inválido**

**Entrada**:
```json
{
  "action": "registerUser",
  "payload": {
    "username": "testuser4",
    "email": "emailinvalido",
    "password": "123456"
  }
}
```

**Salida Esperada**:
```json
{
  "action": "registerUser",
  "status": "error",
  "message": "Formato de email inválido",
  "data": {
    "campo": "email",
    "motivo": "Formato inválido"
  }
}
```

---

### **FUNCIONALIDAD 2: ENVIAR MENSAJE DE TEXTO A CANAL**

#### **Preparación**:
1. Autenticar usuario
2. Crear canal
3. Agregar usuario como miembro

#### **Caso 2.1: Envío Exitoso**

**Entrada**:
```json
{
  "action": "enviarMensajeCanal",
  "payload": {
    "canalId": "uuid-del-canal",
    "contenido": "Hola a todos!"
  }
}
```

**Salida Esperada**:
```json
{
  "action": "enviarMensajeCanal",
  "status": "success",
  "message": "Mensaje enviado",
  "data": {
    "messageId": "uuid-generado",
    "channelId": "uuid-del-canal",
    "author": {
      "userId": "uuid-autor",
      "username": "testuser1"
    },
    "timestamp": "2025-11-05T...",
    "messageType": "TEXT",
    "content": "Hola a todos!"
  }
}
```

**Verificación en BD**:
```sql
SELECT * FROM messages WHERE channel_id = 'uuid-del-canal' ORDER BY timestamp DESC LIMIT 1;
-- Debe existir el mensaje con message_type = 'TEXT'
```

---

#### **Caso 2.2: Usuario No Miembro**

**Entrada**: Usuario autenticado pero NO miembro del canal

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

#### **Caso 2.3: Mensaje Vacío**

**Entrada**:
```json
{
  "action": "enviarMensajeCanal",
  "payload": {
    "canalId": "uuid-del-canal",
    "contenido": ""
  }
}
```

**Salida Esperada**:
```json
{
  "action": "enviarMensajeCanal",
  "status": "error",
  "message": "El contenido del mensaje es requerido",
  "data": {
    "campo": "contenido",
    "motivo": "Campo requerido"
  }
}
```

---

#### **Caso 2.4: Notificación Push**

**Preparación**: Tener 2 clientes conectados al mismo canal

**Acción**: Cliente A envía mensaje

**Verificación**:
1. Cliente A recibe respuesta de confirmación
2. Cliente B recibe notificación push:

```json
{
  "action": "nuevoMensajeCanal",
  "status": "success",
  "message": "Nuevo mensaje recibido",
  "data": {
    "messageId": "uuid-del-mensaje",
    "channelId": "uuid-del-canal",
    "author": {
      "userId": "uuid-cliente-a",
      "username": "clienteA"
    },
    "timestamp": "2025-11-05T...",
    "messageType": "TEXT",
    "content": "Hola a todos!"
  }
}
```

---

### **FUNCIONALIDAD 3: OBTENER HISTORIAL DE CANAL**

#### **Preparación**:
1. Tener canal con varios mensajes
2. Usuario autenticado y miembro del canal

#### **Caso 3.1: Obtener Historial Exitoso**

**Entrada**:
```json
{
  "action": "solicitarHistorialCanal",
  "payload": {
    "canalId": "uuid-del-canal",
    "usuarioId": "uuid-del-usuario"
  }
}
```

**Salida Esperada**:
```json
{
  "action": "solicitarHistorialCanal",
  "status": "success",
  "message": "Historial obtenido",
  "data": {
    "mensajes": [
      {
        "messageId": "uuid-1",
        "channelId": "uuid-del-canal",
        "author": {
          "userId": "uuid-autor-1",
          "username": "juan"
        },
        "timestamp": "2025-11-05T10:00:00",
        "messageType": "TEXT",
        "content": "Primer mensaje"
      },
      {
        "messageId": "uuid-2",
        "channelId": "uuid-del-canal",
        "author": {
          "userId": "uuid-autor-2",
          "username": "maria"
        },
        "timestamp": "2025-11-05T10:05:00",
        "messageType": "TEXT",
        "content": "Segundo mensaje"
      }
    ],
    "totalMensajes": 2
  }
}
```

**Verificación**:
- Mensajes ordenados cronológicamente (más antiguos primero)
- Todos los campos presentes

---

#### **Caso 3.2: Canal Vacío**

**Entrada**: Canal sin mensajes

**Salida Esperada**:
```json
{
  "action": "solicitarHistorialCanal",
  "status": "success",
  "message": "Historial obtenido",
  "data": {
    "mensajes": [],
    "totalMensajes": 0
  }
}
```

---

#### **Caso 3.3: Usuario No Miembro**

**Entrada**: Usuario autenticado pero NO miembro del canal

**Salida Esperada**:
```json
{
  "action": "solicitarHistorialCanal",
  "status": "error",
  "message": "No eres miembro de este canal",
  "data": {
    "campo": "permisos",
    "motivo": "No eres miembro de este canal"
  }
}
```

---

### **FUNCIONALIDAD 4: LISTAR MIEMBROS DE CANAL**

#### **Preparación**:
1. Tener canal con varios miembros
2. Usuario autenticado y miembro del canal

#### **Caso 4.1: Listar Miembros Exitoso**

**Entrada**:
```json
{
  "action": "listarMiembros",
  "payload": {
    "canalId": "uuid-del-canal",
    "solicitanteId": "uuid-del-usuario"
  }
}
```

**Salida Esperada**:
```json
{
  "action": "listarMiembros",
  "status": "success",
  "message": "Miembros obtenidos",
  "data": {
    "miembros": [
      {
        "userId": "uuid-1",
        "username": "juan",
        "email": "juan@mail.com",
        "photoAddress": "user_photos/juan.jpg",
        "conectado": true,
        "rol": "ADMIN"
      },
      {
        "userId": "uuid-2",
        "username": "maria",
        "email": "maria@mail.com",
        "photoAddress": null,
        "conectado": false,
        "rol": "MIEMBRO"
      }
    ],
    "totalMiembros": 2,
    "canalId": "uuid-del-canal"
  }
}
```

**Verificación**:
- Owner del canal tiene rol "ADMIN"
- Otros miembros tienen rol "MIEMBRO"
- Estado de conexión correcto

---

#### **Caso 4.2: Usuario No Miembro**

**Entrada**: Usuario autenticado pero NO miembro del canal

**Salida Esperada**:
```json
{
  "action": "listarMiembros",
  "status": "error",
  "message": "No eres miembro de este canal",
  "data": {
    "campo": "permisos",
    "motivo": "No eres miembro de este canal"
  }
}
```

---

## 🔄 PRUEBAS DE INTEGRACIÓN

### **Flujo Completo: Registro → Login → Crear Canal → Enviar Mensaje → Ver Historial**

#### **Paso 1: Registrar Usuario**
```json
{"action":"registerUser","payload":{"username":"juan","email":"juan@test.com","password":"123456"}}
```

#### **Paso 2: Autenticar Usuario**
```json
{"action":"authenticateUser","payload":{"nombreUsuario":"juan","password":"123456"}}
```

#### **Paso 3: Crear Canal** (requiere implementación de Prioridad 2)
```json
{"action":"crearCanal","payload":{"nombre":"Canal Test","descripcion":"Test"}}
```

#### **Paso 4: Enviar Mensaje**
```json
{"action":"enviarMensajeCanal","payload":{"canalId":"uuid-del-canal","contenido":"Hola!"}}
```

#### **Paso 5: Obtener Historial**
```json
{"action":"solicitarHistorialCanal","payload":{"canalId":"uuid-del-canal","usuarioId":"uuid-juan"}}
```

#### **Paso 6: Listar Miembros**
```json
{"action":"listarMiembros","payload":{"canalId":"uuid-del-canal","solicitanteId":"uuid-juan"}}
```

---

## 🛠️ HERRAMIENTAS DE PRUEBA

### **Opción 1: Telnet (Básico)**

```bash
telnet localhost 22100
# Pegar JSON y presionar Enter
```

### **Opción 2: Netcat (Mejor)**

```bash
echo '{"action":"registerUser","payload":{"username":"test","email":"test@mail.com","password":"123456"}}' | nc localhost 22100
```

### **Opción 3: Script Python (Recomendado)**

Crear archivo `test_server.py`:

```python
import socket
import json

def send_request(action, payload):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.connect(('localhost', 22100))
    
    request = {
        "action": action,
        "payload": payload
    }
    
    sock.send((json.dumps(request) + '\n').encode())
    response = sock.recv(4096).decode()
    sock.close()
    
    return json.loads(response)

# Prueba de registro
response = send_request("registerUser", {
    "username": "testuser",
    "email": "test@mail.com",
    "password": "123456"
})

print(json.dumps(response, indent=2))
```

Ejecutar:
```bash
python test_server.py
```

---

## ✅ CHECKLIST DE VALIDACIÓN FINAL

### **Compilación y Ejecución**
- [ ] Proyecto compila sin errores
- [ ] Servidor inicia correctamente
- [ ] MySQL está corriendo
- [ ] Servidor escucha en puerto 22100

### **Funcionalidad 1: Registro**
- [ ] Registro exitoso funciona
- [ ] Email duplicado retorna error
- [ ] Username duplicado retorna error
- [ ] Contraseña corta retorna error
- [ ] Email inválido retorna error
- [ ] Usuario se guarda en BD con contraseña hasheada
- [ ] Peer se asigna correctamente

### **Funcionalidad 2: Enviar Mensaje**
- [ ] Envío exitoso funciona
- [ ] Usuario no miembro retorna error
- [ ] Mensaje vacío retorna error
- [ ] Mensaje se guarda en BD
- [ ] Notificación push funciona
- [ ] Otros miembros reciben el mensaje

### **Funcionalidad 3: Historial**
- [ ] Obtener historial funciona
- [ ] Canal vacío retorna lista vacía
- [ ] Usuario no miembro retorna error
- [ ] Mensajes ordenados cronológicamente
- [ ] Mensajes de audio codificados en Base64

### **Funcionalidad 4: Listar Miembros**
- [ ] Listar miembros funciona
- [ ] Usuario no miembro retorna error
- [ ] Owner tiene rol "ADMIN"
- [ ] Otros tienen rol "MIEMBRO"
- [ ] Estado de conexión correcto

### **Integración**
- [ ] Flujo completo funciona sin errores
- [ ] Logs del servidor son claros
- [ ] No hay excepciones en consola
- [ ] Base de datos refleja los cambios

---


# CHECKLIST FINAL - PRIORIDAD 1

## 📋 RESUMEN DE IMPLEMENTACIÓN

### **Archivos Modificados/Creados**

#### **DTOs** (si no existían)
- [ ] `Server-Nicolas/comunes/Server-DTO/src/main/java/com/arquitectura/DTO/usuarios/UserRegistrationRequestDto.java`
- [ ] `Server-Nicolas/comunes/Server-DTO/src/main/java/com/arquitectura/DTO/Mensajes/SendMessageRequestDto.java`
- [ ] Campo `rol` agregado en `UserResponseDto.java`

#### **Servicios**
- [ ] `Server-Nicolas/negocio/server-logicaUsuarios/src/main/java/com/arquitectura/logicaUsuarios/UserServiceImpl.java`
  - Método `registrarUsuario()` verificado/completado
  
- [ ] `Server-Nicolas/negocio/server-LogicaMensajes/src/main/java/com/arquitectura/logicaMensajes/MessageServiceImpl.java`
  - Método `enviarMensajeTexto()` verificado/completado
  - Método `obtenerMensajesPorCanal()` verificado/completado
  
- [ ] `Server-Nicolas/negocio/server-LogicaCanales/src/main/java/com/arquitectura/logicaCanales/IChannelService.java`
  - Método `obtenerMiembrosDeCanal()` agregado
  
- [ ] `Server-Nicolas/negocio/server-LogicaCanales/src/main/java/com/arquitectura/logicaCanales/ChannelServiceImpl.java`
  - Método `obtenerMiembrosDeCanal()` implementado

#### **Fachada**
- [ ] `Server-Nicolas/negocio/server-logicaFachada/src/main/java/com/arquitectura/fachada/IChatFachada.java`
  - Método `obtenerMiembrosDeCanal()` agregado
  
- [ ] `Server-Nicolas/negocio/server-logicaFachada/src/main/java/com/arquitectura/fachada/ChatFachadaImpl.java`
  - Método `registrarUsuario()` verificado
  - Método `enviarMensajeTexto()` verificado
  - Método `obtenerMensajesDeCanal()` verificado
  - Método `obtenerMiembrosDeCanal()` implementado

#### **Controlador**
- [ ] `Server-Nicolas/transporte/server-controladorTransporte/src/main/java/com/arquitectura/controlador/RequestDispatcher.java`
  - Caso `"registeruser"` agregado
  - Caso `"enviarmensajecanal"` agregado
  - Caso `"solicitarhistorialcanal"` agregado
  - Caso `"listarmiembros"` agregado

---

## 🎯 FUNCIONALIDADES IMPLEMENTADAS

### **✅ Funcionalidad 1: Registro de Usuarios**
- [x] Endpoint `registeruser` funcional
- [x] Validación de campos requeridos
- [x] Validación de formato de email
- [x] Validación de longitud de contraseña
- [x] Detección de email duplicado
- [x] Detección de username duplicado
- [x] Contraseña hasheada con BCrypt
- [x] Asignación de Peer automática
- [x] Respuestas de error descriptivas

### **✅ Funcionalidad 2: Enviar Mensaje de Texto a Canal**
- [x] Endpoint `enviarmensajecanal` funcional
- [x] Validación de membresía
- [x] Creación de TextMessage
- [x] Guardado en base de datos
- [x] Publicación de NewMessageEvent
- [x] Notificaciones push a otros miembros
- [x] Respuestas de error descriptivas

### **✅ Funcionalidad 3: Obtener Historial de Canal**
- [x] Endpoint `solicitarhistorialcanal` funcional
- [x] Validación de membresía
- [x] Obtención de mensajes ordenados
- [x] Soporte para mensajes de texto
- [x] Soporte para mensajes de audio (Base64)
- [x] Manejo de canales vacíos
- [x] Respuestas de error descriptivas

### **✅ Funcionalidad 4: Listar Miembros de Canal**
- [x] Endpoint `listarmiembros` funcional
- [x] Validación de membresía
- [x] Obtención de lista de miembros
- [x] Determinación de roles (ADMIN/MIEMBRO)
- [x] Inclusión de estado de conexión
- [x] Respuestas de error descriptivas

---

## 🧪 PRUEBAS REALIZADAS

### **Pruebas Unitarias (Manuales)**
- [ ] Registro de usuario nuevo
- [ ] Registro con email duplicado
- [ ] Registro con username duplicado
- [ ] Registro con contraseña corta
- [ ] Registro con email inválido
- [ ] Envío de mensaje exitoso
- [ ] Envío de mensaje sin ser miembro
- [ ] Envío de mensaje vacío
- [ ] Obtener historial exitoso
- [ ] Obtener historial sin ser miembro
- [ ] Obtener historial de canal vacío
- [ ] Listar miembros exitoso
- [ ] Listar miembros sin ser miembro

### **Pruebas de Integración**
- [ ] Flujo completo: Registro → Login → Mensaje → Historial
- [ ] Notificaciones push funcionan
- [ ] Múltiples clientes conectados simultáneamente
- [ ] Mensajes se persisten correctamente en BD
- [ ] Roles se asignan correctamente

### **Verificaciones en Base de Datos**
- [ ] Usuarios se crean con contraseña hasheada
- [ ] Peer se asigna correctamente
- [ ] Mensajes se guardan con tipo correcto
- [ ] Membresías se registran correctamente
- [ ] Timestamps son correctos

---

## 📊 MÉTRICAS DE IMPLEMENTACIÓN

### **Líneas de Código Agregadas** (aproximado)
- DTOs: ~100 líneas
- Servicios: ~200 líneas
- Fachada: ~50 líneas
- RequestDispatcher: ~400 líneas
- **Total**: ~750 líneas

### **Endpoints Agregados**
- `registeruser` (público)
- `enviarmensajecanal` (autenticado)
- `solicitarhistorialcanal` (autenticado)
- `listarmiembros` (autenticado)
- **Total**: 4 endpoints

### **Métodos de Servicio Agregados/Verificados**
- `UserServiceImpl.registrarUsuario()`
- `MessageServiceImpl.enviarMensajeTexto()`
- `MessageServiceImpl.obtenerMensajesPorCanal()`
- `ChannelServiceImpl.obtenerMiembrosDeCanal()` (nuevo)
- **Total**: 4 métodos

---

## 🚀 PRÓXIMOS PASOS

### **Prioridad 2: Gestión de Canales**
1. Crear canal (endpoint)
2. Invitar miembro a canal
3. Responder invitación
4. Ver invitaciones pendientes
5. Validar permisos en canales

### **Prioridad 3: Mensajes Privados**
1. Crear/obtener canal directo
2. Enviar mensaje privado
3. Historial privado

### **Mejoras Opcionales**
1. Paginación en historial de mensajes
2. Búsqueda de mensajes
3. Editar/eliminar mensajes
4. Reacciones a mensajes
5. Mensajes fijados

---

## 📝 NOTAS IMPORTANTES

### **Seguridad**
- ✅ Contraseñas hasheadas con BCrypt
- ✅ Validación de autenticación en endpoints
- ✅ Validación de permisos (membresía)
- ✅ Validación de autorización (usuario = solicitante)
- ⚠️ No hay rate limiting (aceptable para proyecto académico)
- ⚠️ No hay encriptación E2E (aceptable para proyecto académico)

### **Rendimiento**
- ✅ Consultas optimizadas con JPA
- ✅ Uso de índices en BD (automático con JPA)
- ✅ Lazy loading en relaciones
- ⚠️ Sin paginación en historial (agregar si hay muchos mensajes)
- ⚠️ Sin caché (aceptable para proyecto académico)

### **Arquitectura**
- ✅ Separación de capas clara
- ✅ Patrón Facade implementado
- ✅ Inyección de dependencias con Spring
- ✅ Eventos para notificaciones
- ✅ DTOs para transferencia de datos
- ⚠️ RequestDispatcher muy grande (refactorizar en Prioridad 4)

### **Buenas Prácticas**
- ✅ Validación de entrada
- ✅ Manejo de errores consistente
- ✅ Logs informativos
- ✅ Nombres descriptivos
- ✅ Comentarios en código complejo
- ✅ Transacciones en operaciones de BD

---

## 🎓 APRENDIZAJES DEL PROYECTO

### **Patrones de Diseño Aplicados**
1. **Facade**: ChatFachadaImpl coordina todos los servicios
2. **Repository**: Acceso a datos con Spring Data JPA
3. **Service Layer**: Lógica de negocio separada
4. **DTO**: Transferencia de datos entre capas
5. **Observer**: Eventos de Spring para notificaciones
6. **Dependency Injection**: Spring Framework
7. **Strategy**: Diferentes servicios para diferentes dominios

### **Tecnologías Utilizadas**
1. **Spring Framework 6.2**: IoC, DI, Events
2. **Spring Data JPA**: Persistencia
3. **Hibernate 6.2**: ORM
4. **MySQL 8.0**: Base de datos
5. **Gson**: Serialización JSON
6. **BCrypt**: Hash de contraseñas
7. **SLF4J**: Logging
8. **Maven**: Gestión de dependencias

### **Arquitectura Implementada**
```
Presentación (Swing Admin)
    ↓
Transporte (TCP/IP + JSON)
    ↓
Controlador (RequestDispatcher)
    ↓
Fachada (ChatFachadaImpl)
    ↓
Servicios (UserService, ChannelService, MessageService)
    ↓
Repositorios (Spring Data JPA)
    ↓
Base de Datos (MySQL)
```

---

## ✅ FIRMA DE COMPLETITUD

**Funcionalidades de Prioridad 1 Completadas**: 4/4

- [x] Registro de Usuarios
- [x] Enviar Mensaje de Texto a Canal
- [x] Obtener Historial de Canal
- [x] Listar Miembros de Canal

**Estado del Proyecto**: ✅ **PRIORIDAD 1 COMPLETADA**

**Fecha de Completitud**: _________________

**Desarrollador**: _________________

**Revisor**: _________________

---

## 📚 REFERENCIAS

### **Documentación del Proyecto**
- `ANALISIS_COMPLETO_PROYECTO.md` - Análisis exhaustivo del sistema
- `Cliente/BUGFIX_FILE_DOWNLOAD.md` - Bug de case sensitivity
- `Server-Nicolas/PEER_IMPLEMENTATION_REVIEW.md` - Implementación de Peers
- `Cliente/Negocio/GestionArchivos/README_ARCHIVO_SERVICE.md` - Gestión de archivos
- `Cliente/Negocio/GestionCanales/DOCUMENTACION_FLUJO_GESTION_CANALES.md` - Flujo de canales

### **Código Fuente Clave**
- `Server-Nicolas/transporte/server-controladorTransporte/src/main/java/com/arquitectura/controlador/RequestDispatcher.java`
- `Server-Nicolas/negocio/server-logicaFachada/src/main/java/com/arquitectura/fachada/ChatFachadaImpl.java`
- `Server-Nicolas/transporte/server-Transporte/src/main/java/com/arquitectura/transporte/ServerListener.java`

### **Recursos Externos**
- Spring Framework: https://spring.io/projects/spring-framework
- Spring Data JPA: https://spring.io/projects/spring-data-jpa
- Hibernate: https://hibernate.org/
- MySQL: https://dev.mysql.com/doc/

---

**FIN DEL DOCUMENTO**

---

