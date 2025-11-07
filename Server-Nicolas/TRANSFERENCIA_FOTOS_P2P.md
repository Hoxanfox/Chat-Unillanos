# Transferencia de Fotos de Perfil entre Servidores P2P

## 📋 Problema Identificado

Cuando se solicitaba información de un usuario de otro servidor, **solo se enviaba la ruta del archivo** (`photoAddress`), pero esa ruta es local al servidor de origen y **no es accesible** desde otros servidores.

**Ejemplo del problema:**
```
Servidor A: user.photoAddress = "user_photos/abc-123.jpg"
Servidor B solicita info del usuario
Servidor B recibe: "user_photos/abc-123.jpg"
❌ Servidor B no puede acceder a ese archivo (está en Servidor A)
```

## ✅ Solución Implementada

Ahora se utiliza el campo `imagenBase64` del `UserResponseDto` para transferir la foto codificada en Base64 junto con la información del usuario.

## 🔧 Cambios Realizados

### 1. **UserServiceImpl.buscarPorUsername()** - Modificado

**Antes:**
```java
public Optional<UserResponseDto> buscarPorUsername(String username) {
    return userRepository.findByUsername(username)
        .map(user -> new UserResponseDto(
            user.getUserId(),
            user.getUsername(),
            user.getEmail(),
            user.getPhotoAddress(), // ❌ Solo la ruta
            user.getFechaRegistro(),
            user.getConectado() ? "ONLINE" : "OFFLINE"
        ));
}
```

**Después:**
```java
public Optional<UserResponseDto> buscarPorUsername(String username) {
    return userRepository.findByUsername(username)
        .map(user -> {
            // Obtener la imagen en Base64 si existe
            String imagenBase64 = getImagenBase64(user);
            
            return new UserResponseDto(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhotoAddress(),
                imagenBase64, // ✅ Imagen codificada en Base64
                user.getFechaRegistro(),
                user.getConectado() ? "ONLINE" : "OFFLINE"
            );
        });
}
```

### 2. **P2PNotificationController.handleObtenerInfoUsuario()** - Modificado

Ahora incluye la imagen en Base64 en la respuesta:

```java
private void handleObtenerInfoUsuario(DTORequest request, IClientHandler handler) {
    // ...buscar usuario...
    
    if (usuarioOpt.isPresent()) {
        UserResponseDto user = usuarioOpt.get();
        
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", user.getUserId().toString());
        userData.put("username", user.getUsername());
        userData.put("email", user.getEmail());
        userData.put("photoAddress", user.getPhotoAddress());
        
        // ✅ NUEVO: Incluir la foto en Base64 si existe
        if (user.getImagenBase64() != null && !user.getImagenBase64().isEmpty()) {
            userData.put("imagenBase64", user.getImagenBase64());
            System.out.println("  → Incluyendo foto de perfil en Base64");
        } else {
            userData.put("imagenBase64", null);
        }
        
        // ...enviar respuesta...
    }
}
```

### 3. **PeerNotificationServiceImpl.solicitarInfoUsuario()** - Modificado

Ahora guarda la imagen localmente cuando se recibe:

```java
public UserResponseDto solicitarInfoUsuario(UUID peerDestinoId, UUID usuarioId) {
    // ...solicitar info al peer...
    
    UserResponseDto user = gson.fromJson(jsonData, UserResponseDto.class);
    
    // ✅ Si el usuario tiene imagen en Base64, guardarla localmente
    if (user.getImagenBase64() != null && !user.getImagenBase64().isEmpty()) {
        try {
            log.info("→ Guardando foto de perfil de usuario remoto: {}", user.getUsername());
            
            // Decodificar y guardar la imagen localmente
            byte[] imageBytes = Base64.getDecoder().decode(user.getImagenBase64());
            
            // Determinar extensión del archivo
            String extension = ".jpg";
            if (user.getPhotoAddress() != null && user.getPhotoAddress().contains(".")) {
                extension = user.getPhotoAddress().substring(
                    user.getPhotoAddress().lastIndexOf("."));
            }
            
            String fileName = user.getUserId().toString() + extension;
            
            // Guardar en storage/user_photos/
            String localPath = fileStorage.storeFile(imageBytes, fileName, "user_photos");
            user.setPhotoAddress(localPath); // Actualizar con la ruta local
            
            log.info("✓ Foto guardada localmente en: {}", localPath);
            
        } catch (Exception e) {
            log.warn("⚠ No se pudo guardar la foto del usuario: {}", e.getMessage());
            // Continuar sin la foto
        }
    }
    
    return user;
}
```

## 🔄 Flujo Completo

```
┌────────────────────────────────────────────────────────────┐
│ Servidor B necesita info de Usuario X (está en Servidor A) │
└────────────────────────────────────────────────────────────┘
                          ↓
┌────────────────────────────────────────────────────────────┐
│ 1. Servidor B → solicitarInfoUsuario(peerA, usuarioX)      │
└────────────────────────────────────────────────────────────┘
                          ↓
┌────────────────────────────────────────────────────────────┐
│ 2. Servidor A → buscarPorUsername(usuarioX)                │
│    - Carga usuario de BD                                   │
│    - Lee foto desde storage/user_photos/abc-123.jpg        │
│    - Codifica foto en Base64                               │
│    - Retorna UserResponseDto con imagenBase64              │
└────────────────────────────────────────────────────────────┘
                          ↓
┌────────────────────────────────────────────────────────────┐
│ 3. Servidor A → Envía respuesta "obtenerInfoUsuario"       │
│    Payload: {                                              │
│      userId: "...",                                        │
│      username: "...",                                      │
│      email: "...",                                         │
│      photoAddress: "user_photos/abc-123.jpg",              │
│      imagenBase64: "iVBORw0KGgoAAAANSUhEU..." ← Base64    │
│    }                                                       │
└────────────────────────────────────────────────────────────┘
                          ↓
┌────────────────────────────────────────────────────────────┐
│ 4. Servidor B → Recibe respuesta                           │
│    - Decodifica imagenBase64                               │
│    - Guarda en storage/user_photos/usuarioX-uuid.jpg       │
│    - Actualiza photoAddress a ruta local                   │
│    - Retorna UserResponseDto con nueva ruta                │
└────────────────────────────────────────────────────────────┘
```

## 📡 Ejemplo de Comunicación P2P

### Request: `obtenerInfoUsuario`
```json
{
  "action": "obtenerInfoUsuario",
  "payload": {
    "usuarioId": "abc-123-def-456"
  }
}
```

### Response: (Servidor A → Servidor B)
```json
{
  "status": "success",
  "message": "Usuario encontrado",
  "data": {
    "userId": "abc-123-def-456",
    "username": "juan.perez",
    "email": "juan@example.com",
    "photoAddress": "user_photos/abc-123-def-456.jpg",
    "imagenBase64": "/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAAEB...", // Imagen completa
    "fechaRegistro": "2024-01-15T10:30:00",
    "estado": "ONLINE"
  }
}
```

## 📊 Comparación: Antes vs Después

### Antes (❌ No funcionaba)

| Servidor | Acción | Resultado |
|----------|--------|-----------|
| Servidor A | Usuario tiene foto en `user_photos/1.jpg` | ✓ OK |
| Servidor B | Solicita info de usuario | ✓ Recibe info |
| Servidor B | Recibe `photoAddress: "user_photos/1.jpg"` | ✓ Recibe ruta |
| Servidor B | Intenta mostrar foto | ❌ **Archivo no existe** |

### Después (✅ Funciona)

| Servidor | Acción | Resultado |
|----------|--------|-----------|
| Servidor A | Usuario tiene foto en `user_photos/1.jpg` | ✓ OK |
| Servidor A | Lee y codifica foto en Base64 | ✓ Codificado |
| Servidor B | Solicita info de usuario | ✓ Recibe info + foto |
| Servidor B | Decodifica y guarda foto localmente | ✓ Guardado |
| Servidor B | Ahora tiene foto en `user_photos/abc-123.jpg` | ✓ **Accesible** |
| Servidor B | Puede mostrar foto | ✅ **Funciona** |

## 🔍 Logs Esperados

### En Servidor A (emisor):
```
→ [P2PNotificationController] Solicitud de info de usuario: abc-123-def-456
  → Incluyendo foto de perfil en Base64
✓ [P2PNotificationController] Usuario encontrado: juan.perez
```

### En Servidor B (receptor):
```
→ [PeerNotificationService] Solicitando info de usuario abc-123-def-456 al peer server-a
→ Guardando foto de perfil de usuario remoto: juan.perez
✓ Foto guardada localmente en: user_photos/abc-123-def-456.jpg
✓ [PeerNotificationService] Info de usuario obtenida
```

## 💡 Ventajas de Base64 vs Chunks

### ¿Por qué Base64 para fotos y Chunks para audio?

**Fotos de perfil (Base64):**
- ✅ Generalmente pequeñas (< 200KB)
- ✅ Se solicitan una vez por usuario
- ✅ Base64 es simple y eficiente para datos pequeños
- ✅ Se incluye en la respuesta JSON directamente

**Archivos de audio (Chunks):**
- ⚠️ Pueden ser grandes (varios MB)
- ⚠️ Se envían frecuentemente
- ✅ Chunks permiten progreso y recuperación
- ✅ Mejor manejo de memoria

## 🚨 Consideraciones

### Tamaño de Fotos

Si las fotos de perfil son muy grandes (> 500KB), considera:

1. **Redimensionar antes de guardar** - Limitar a 200x200px
2. **Usar chunks también para fotos** - Si exceden cierto tamaño
3. **Comprimir con calidad reducida** - JPEG con 80% quality

### Caché de Fotos

Para optimizar, podrías:

1. **Cachear fotos localmente** - No volver a solicitar si ya existe
2. **Agregar timestamp** - Detectar cuándo actualizar
3. **Limpiar fotos antiguas** - De usuarios inactivos

## ✅ Resumen

Con estos cambios, ahora **las fotos de perfil se transfieren automáticamente** entre servidores cuando:

1. ✅ Se solicita información de un usuario remoto
2. ✅ Se invita a un usuario a un canal
3. ✅ Se muestra la lista de miembros de un canal cross-server
4. ✅ Se reciben mensajes de usuarios remotos

La experiencia del usuario es **completamente transparente** - las fotos se ven correctamente sin importar en qué servidor esté el usuario.

## 📚 Archivos Modificados

- `UserServiceImpl.java` - Incluir imagenBase64 al buscar usuario
- `P2PNotificationController.java` - Enviar imagenBase64 en respuesta
- `PeerNotificationServiceImpl.java` - Guardar imagen localmente al recibir

## 🔗 Relación con Audio P2P

Ambos sistemas (fotos y audio) resuelven el mismo problema de **archivos distribuidos**, pero con diferentes estrategias:

| Característica | Fotos (Base64) | Audio (Chunks) |
|----------------|----------------|----------------|
| Tamaño típico | < 200KB | > 1MB |
| Frecuencia | Ocasional | Frecuente |
| Método | Base64 inline | Transferencia por chunks |
| Complejidad | Baja | Media |
| Progreso | No necesario | Sí, importante |

