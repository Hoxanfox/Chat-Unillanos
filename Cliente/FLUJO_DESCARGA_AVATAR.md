# Flujo de Descarga de Avatar - Arquitectura en Capas

## 📋 Resumen
Implementación completa del flujo de descarga de avatar del usuario respetando la arquitectura en capas.

## 🏗️ Arquitectura Correcta

```
Vista (FeatureHeader)
    ↓ llama a
Controlador (IControladorUsuario)
    ↓ llama a
Servicio (IServicioUsuario)
    ↓ llama a
Fachada (IFachadaUsuarios)
    ↓ llama a
Gestor (IGestionArchivos)
```

## 🔄 Flujo Completo

### 1. **Autenticación del Usuario**
- `AutenticarUsuario` recibe del servidor el `photoId` (NO el Base64 completo)
- Se guarda en `Usuario.photoIdServidor` en la BD local
- Se notifica a los observadores con `AUTENTICACION_EXITOSA`

### 2. **Carga del Usuario en Header**
```java
FeatureHeader.cargarInformacionUsuario()
    → controladorUsuario.cargarInformacionUsuarioLogueado()
    → devuelve DTOUsuario con photoId en avatarUrl
```

### 3. **Descarga del Avatar (Async)**
```java
FeatureHeader.cargarAvatarDesdePhotoId(photoId)
    → controladorUsuario.descargarAvatar(photoId, directorioCache)
        → servicioUsuario.descargarAvatar(photoId, directorioCache)
            → fachadaUsuarios.descargarAvatar(photoId, directorioCache)
                → gestionArchivos.descargarArchivo(photoId, directorioCache)
                    → Verifica caché en BD local
                    → Si no existe, descarga del servidor por chunks
                    → Guarda en caché y devuelve File
```

### 4. **Actualización de UI**
- `FeatureHeader` recibe el `File` y lo carga en el `ImageView`
- Si falla, muestra emoji por defecto 👤
- Todo en el hilo de JavaFX con `Platform.runLater()`

## 📦 Archivos Modificados

### 1. **Autenticación**
- `Negocio/GestionUsuario/src/main/java/gestionUsuario/autenticacion/AutenticarUsuario.java`
  - Cambio: `String photoId = firstString(datosUsuario, "photoId", "fileId", "imagenId");`
  - Antes recibía `imagenBase64` completo

### 2. **Controlador**
- `Presentacion/Controlador/src/main/java/controlador/usuario/IControladorUsuario.java`
  - Agregado: `CompletableFuture<File> descargarAvatar(String photoId, File directorioDestino)`
- `Presentacion/Controlador/src/main/java/controlador/usuario/ControladorUsuario.java`
  - Implementación que delega al servicio

### 3. **Servicio**
- `Negocio/Servicio/src/main/java/servicio/usuario/IServicioUsuario.java`
  - Agregado: `CompletableFuture<File> descargarAvatar(String photoId, File directorioDestino)`
- `Negocio/Servicio/src/main/java/servicio/usuario/ServicioUsuarioImpl.java`
  - Implementación que delega a la fachada

### 4. **Fachada**
- `Negocio/Fachada/src/main/java/fachada/gestionUsuarios/insercionDB/IFachadaUsuarios.java`
  - Agregado: `CompletableFuture<File> descargarAvatar(String photoId, File directorioDestino)`
- `Negocio/Fachada/src/main/java/fachada/gestionUsuarios/insercionDB/FachadaUsuariosImpl.java`
  - Agrega instancia de `IGestionArchivos`
  - Implementación que delega a `gestionArchivos.descargarArchivo()`

### 5. **Vista**
- `Presentacion/InterfazEscritorio/src/main/java/interfazEscritorio/dashboard/featureHeader/FeatureHeader.java`
  - **NO instancia** `GestionArchivosImpl` directamente
  - Solo se comunica con `IControladorUsuario`
  - Maneja el avatar con `ImageView` circular
  - Caché en `~/.chatUnillanos/cache/imagenes/`

## ✅ Ventajas de Esta Arquitectura

1. **Separación de Responsabilidades**
   - Vista: Solo UI y eventos del usuario
   - Controlador: Coordina entre vista y lógica de negocio
   - Servicio: Orquesta múltiples fachadas si es necesario
   - Fachada: Coordina múltiples gestores
   - Gestor: Lógica específica (archivos, usuarios, etc.)

2. **Caché Inteligente**
   - `GestionArchivos` verifica primero en BD local
   - Solo descarga si no está en caché
   - Las siguientes cargas son instantáneas

3. **Eficiencia**
   - Login rápido (solo ID, no imagen completa)
   - Descarga asíncrona en segundo plano
   - No bloquea la UI

4. **Testeable**
   - Cada capa puede ser probada independientemente
   - Fácil mockear las dependencias

## 🚀 Próximos Pasos

1. El servidor debe devolver `photoId` en lugar de `imagenBase64` en la respuesta de autenticación
2. Probar el flujo completo con un usuario real
3. Agregar imagen por defecto en recursos si se desea (opcional, actualmente usa emoji 👤)

## 📝 Ejemplo de Respuesta del Servidor

```json
{
  "success": true,
  "data": {
    "userId": "uuid-del-usuario",
    "nombre": "Juan Pérez",
    "email": "juan@unillanos.edu.co",
    "photoId": "file-id-12345"
  }
}
```

## 🎯 Directorio de Caché

- Ubicación: `~/.chatUnillanos/cache/imagenes/`
- Los archivos se guardan con el nombre del `photoId`
- Persiste entre sesiones

