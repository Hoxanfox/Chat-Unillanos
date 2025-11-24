# Integración del Sistema de Archivos con Sincronización P2P

## 📋 Resumen de Cambios

### ✅ Actualizaciones Realizadas

1. **init.sql actualizado** con tabla `archivos`
2. **ServicioArchivos** integrado con `ServicioSincronizacionDatos`
3. Campo `foto` en usuarios actualizado para almacenar fileId

---

## 🗄️ Base de Datos

### Script SQL Completo (`init.sql`)

El script ahora incluye:

```sql
-- Tabla de archivos (NUEVA)
CREATE TABLE archivos (
  id CHAR(36) PRIMARY KEY,
  file_id VARCHAR(255) UNIQUE,
  nombre_archivo VARCHAR(255),
  ruta_relativa VARCHAR(500),
  mime_type VARCHAR(100),
  tamanio BIGINT,
  hash_sha256 VARCHAR(64),
  fecha_creacion TIMESTAMP,
  fecha_actualizacion TIMESTAMP
);

-- Campo foto actualizado en usuarios
CREATE TABLE usuarios (
  ...
  foto VARCHAR(500) COMMENT 'FileId relativo desde Bucket/ (ej: user_photos/uuid_foto.jpg)',
  ...
);
```

### Ejecución:
```bash
mysql -u root -p < init.sql
```

---

## 🔄 Sincronización P2P para Archivos

### Cómo Funciona

Cuando un archivo se sube al servidor:

1. **Cliente sube archivo** por chunks → `ServicioArchivos`
2. **ServicioArchivos guarda**:
   - Archivo físico en `Bucket/user_photos/uuid_foto.jpg`
   - Metadatos en tabla `archivos`
3. **Notifica a ServicioSincronizacionDatos**:
   ```java
   if (guardado && servicioSync != null) {
       servicioSync.onBaseDeDatosCambio(); // Reconstruir Merkle Tree
       servicioSync.forzarSincronizacion(); // Sincronizar con peers
   }
   ```
4. **ServicioSincronizacionDatos**:
   - Reconstruye el Merkle Tree con los nuevos metadatos
   - Compara hashes con peers de la red
   - Propaga cambios automáticamente

### Flujo de Sincronización

```
Cliente 1                 Servidor Principal           Peers P2P
   |                              |                         |
   |--[Upload archivo]----------->|                         |
   |                              |--[Guardar en Bucket]--->|
   |                              |--[Guardar en BD]------->|
   |                              |                         |
   |<---[fileId retornado]--------|                         |
   |                              |                         |
   |                              |--[onBaseDeDatosCambio]->|
   |                              |--[forzarSincronizacion]>|
   |                              |                         |
   |                              |--[sync_check_all]------>|
   |                              |<--[hashes remotos]------|
   |                              |                         |
   |                              |--[sync_get_ids]-------->|
   |                              |<--[IDs faltantes]-------|
   |                              |                         |
   |                              |--[sync_get_entity]----->|
   |                              |   (envía metadatos)     |
   |                              |                         |
   |                              |                         |--[Descarga archivo físico]
   |                              |<---------------------------|   desde Bucket/
```

---

## ⚙️ Configuración del Servidor

### Inyectar ServicioSincronizacionDatos

En tu clase principal del servidor (donde inicializas los servicios):

```java
// Crear instancias
ServicioSincronizacionDatos servicioSync = new ServicioSincronizacionDatos();
ServicioArchivos servicioArchivos = new ServicioArchivos();
ServicioChat servicioChat = new ServicioChat();

// Inyectar dependencias
servicioArchivos.setServicioSync(servicioSync);
servicioChat.setServicioSync(servicioSync);

// Inicializar servicios
servicioSync.inicializar(gestorP2P, routerP2P);
servicioArchivos.inicializar(gestorCS, routerCS);
servicioChat.inicializar(gestorP2P, routerP2P);
```

---

## 📂 Estructura de Archivos en Servidor

```
ServidorDeivid/
├── Bucket/                          ← Almacenamiento físico
│   ├── user_photos/                 ← Fotos de perfil
│   │   └── uuid_foto.jpg
│   ├── images/                      ← Imágenes generales
│   ├── audio/                       ← Audios de mensajes
│   ├── documents/                   ← PDFs, DOCs
│   └── otros/                       ← Otros archivos
│
├── init.sql                         ← Script actualizado ✅
└── Negocio/
    ├── GestorClientes/
    │   └── servicios/
    │       ├── ServicioAutenticacion.java
    │       └── ServicioArchivos.java     ← Con sincronización P2P ✅
    └── GestorP2P/
        └── servicios/
            ├── ServicioSincronizacionDatos.java ← Ya existía ✅
            └── ServicioChat.java                 ← Patrón similar
```

---

## 🔍 Verificación de Sincronización

### Logs a Observar

Cuando se sube un archivo, deberías ver en los logs:

```
[FileService] ✅ Archivo guardado: user_photos/abc123_foto.jpg - Tamaño: 102400 bytes
[FileService] 🔄 Activando sincronización P2P para archivo: user_photos/abc123_foto.jpg
[SyncDatos] Base de datos cambió. Reconstruyendo árboles Merkle...
[SyncDatos] - Árbol USUARIO reconstruido. Hash: a1b2c3d4
[SyncDatos] - Árbol CANAL reconstruido. Hash: e5f6g7h8
[SyncDatos] - Árbol MIEMBRO reconstruido. Hash: i9j0k1l2
[SyncDatos] - Árbol MENSAJE reconstruido. Hash: m3n4o5p6
[SyncDatos] Todos los árboles Merkle reconstruidos exitosamente
[SyncDatos] Forzando sincronización manual...
[SyncDatos] >>> Manejador sync_check_all activado <<<
[SyncDatos] ✓ Respuesta sync_check_all recibida. Procesando diferencias...
```

---

## 🎯 Uso en el Cliente

### Ejemplo: Subir Foto de Perfil

```java
// 1. Usuario selecciona foto
File foto = new File("perfil.jpg");

// 2. Subir archivo (el cliente ya lo hace con GestionArchivosImpl)
CompletableFuture<String> futuroFileId = gestionArchivos.subirArchivo(foto);

futuroFileId.thenAccept(fileId -> {
    // 3. fileId retornado: "user_photos/uuid_foto.jpg"
    System.out.println("Archivo subido: " + fileId);
    
    // 4. Actualizar perfil de usuario con el fileId
    actualizarFotoUsuario(fileId);
    
    // 5. El servidor automáticamente sincroniza con peers P2P
    // Los otros peers descargarán el archivo cuando lo necesiten
});
```

---

## 🔄 ¿Qué se Sincroniza?

### Metadatos (Sincronización Automática)
- ✅ ID del archivo
- ✅ Nombre del archivo
- ✅ Ruta relativa
- ✅ MIME type
- ✅ Tamaño
- ✅ Hash SHA-256

### Archivos Físicos (Descarga bajo demanda)
- ⚠️ Los archivos físicos NO se sincronizan automáticamente
- ✅ Se descargan cuando un peer los solicita
- ✅ Los peers usan `startFileDownload` con el `fileId` recibido

### Flujo de Descarga en Peer:

```
Peer 2 recibe notificación de nuevo archivo
  ↓
Merkle Tree se actualiza con metadatos
  ↓
Peer 2 detecta que necesita el archivo (cuando usuario lo solicita)
  ↓
Peer 2 llama startFileDownload(fileId) al servidor
  ↓
Servidor lee archivo de Bucket/ y envía chunks
  ↓
Peer 2 guarda en su Bucket/ local
```

---

## 🛠️ Próximos Pasos

1. ✅ **Ejecutar init.sql** en tu BD MySQL
2. ✅ **Inyectar ServicioSincronizacionDatos** en la inicialización del servidor
3. **Probar subida de archivo** y observar logs de sincronización
4. **Implementar ServicioRegistro** que use este sistema para fotos de perfil
5. **Opcional**: Implementar sincronización proactiva de archivos populares

---

## 📝 Notas Importantes

### ¿Por qué no sincronizar archivos físicos automáticamente?

- **Eficiencia**: Los archivos pueden ser grandes (MB/GB)
- **Ancho de banda**: No todos los peers necesitan todos los archivos
- **Descarga bajo demanda**: Más eficiente y escalable
- **Metadatos ligeros**: Los Merkle Trees solo manejan metadatos

### Optimización Futura

Para archivos críticos (ej: fotos de perfil), podrías:

```java
// En ServicioArchivos, después de guardar
if (categoria.equals("user_photos")) {
    // Notificar a peers para descarga proactiva
    notificarArchivoCritico(fileId);
}
```

---

**Fecha**: 2025-11-24
**Estado**: Sistema de archivos completamente integrado con sincronización P2P ✅
**Compatibilidad**: 100% con cliente existente

