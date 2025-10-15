# Épica 5: Gestión de Archivos Multimedia - Plan Detallado

## Objetivo General

Implementar el sistema completo de gestión de archivos multimedia: subida, almacenamiento, descarga y deduplicación de **imágenes, audios y documentos**. Los usuarios podrán compartir archivos en conversaciones directas y canales grupales, con validación de tipo, tamaño y permisos. El sistema incluirá deduplicación automática mediante hash SHA-256 para optimizar el almacenamiento.

## Contexto Actual

✅ **Ya Implementado (Épicas 1-4):**
- ✅ Sistema completo de mensajería (directa y canal)
- ✅ Campo `fileId` en `MensajeEntity` y `DTOEnviarMensaje` (preparado para Épica 5)
- ✅ ConnectionManager para notificaciones en tiempo real
- ✅ LoggerService para auditoría
- ✅ UsuarioRepository, CanalRepository, MensajeRepository

⚠️ **Pendiente de Implementar:**
- ❌ DTOs específicos para archivos
- ❌ Validadores de archivos (tipo, tamaño, formato)
- ❌ Entidades de archivos (ArchivoEntity, TipoArchivo enum)
- ❌ ArchivoRepository con operaciones JDBC
- ❌ ArchivoService con lógica de negocio
- ❌ Sistema de almacenamiento en disco
- ❌ Acciones en ActionDispatcher: subir archivo, descargar archivo, listar archivos
- ❌ Integración con mensajería (enviar mensaje con archivo adjunto)

---

## Tipos de Archivos Soportados

### 1. Imágenes
- **Formatos**: JPG, JPEG, PNG, GIF, WEBP, BMP
- **Tamaño máximo**: 10 MB por archivo
- **MIME types**: `image/jpeg`, `image/png`, `image/gif`, `image/webp`, `image/bmp`

### 2. Audios
- **Formatos**: MP3, WAV, OGG, M4A, AAC
- **Tamaño máximo**: 20 MB por archivo
- **MIME types**: `audio/mpeg`, `audio/wav`, `audio/ogg`, `audio/mp4`, `audio/aac`

### 3. Documentos
- **Formatos**: PDF, DOCX, TXT, XLSX, PPTX, ZIP
- **Tamaño máximo**: 50 MB por archivo
- **MIME types**: `application/pdf`, `application/vnd.openxmlformats-officedocument.*`, `text/plain`, `application/zip`

---

## Componentes a Implementar

### 1. DTOs para Archivos

**Ubicación:** `Infraestructura/DTOs/src/main/java/com/unillanos/server/dto/`

#### DTOSubirArchivo.java (Request)
```java
public class DTOSubirArchivo {
    private String usuarioId;              // Requerido - Usuario que sube el archivo
    private String nombreArchivo;          // Requerido - Nombre original del archivo
    private String tipoMime;               // Requerido - Tipo MIME del archivo
    private long tamanoBytes;              // Requerido - Tamaño en bytes
    private String base64Data;             // Requerido - Contenido del archivo en Base64
    
    // Getters y Setters
}
```

**Validaciones:**
- `usuarioId` no puede estar vacío
- `nombreArchivo` debe tener extensión válida
- `tipoMime` debe estar en la lista de tipos permitidos
- `tamanoBytes` debe estar dentro del límite según el tipo
- `base64Data` no puede estar vacío

#### DTOArchivo.java (Response)
```java
public class DTOArchivo {
    private String id;                     // UUID del archivo
    private String nombreOriginal;         // Nombre original del archivo
    private String tipoMime;               // Tipo MIME
    private String tipoArchivo;            // "IMAGEN", "AUDIO", "DOCUMENTO"
    private long tamanoBytes;              // Tamaño en bytes
    private String hashSha256;             // Hash SHA-256 del archivo
    private boolean duplicado;             // Si es un archivo duplicado
    private String usuarioId;              // Usuario que subió el archivo
    private String fechaSubida;            // ISO-8601
    private String urlDescarga;            // URL para descargar el archivo
    
    // Getters y Setters
}
```

#### DTODescargarArchivo.java (Request)
```java
public class DTODescargarArchivo {
    private String archivoId;              // Requerido - ID del archivo
    private String usuarioId;              // Requerido - Usuario que descarga
    
    // Getters y Setters
}
```

#### DTOArchivoData.java (Response - Para descarga)
```java
public class DTOArchivoData {
    private String id;
    private String nombreOriginal;
    private String tipoMime;
    private long tamanoBytes;
    private String base64Data;             // Contenido del archivo en Base64
    
    // Getters y Setters
}
```

#### DTOListarArchivos.java (Request)
```java
public class DTOListarArchivos {
    private String usuarioId;              // Requerido - Usuario que solicita
    private String tipoArchivo;            // Opcional - Filtrar por tipo (IMAGEN, AUDIO, DOCUMENTO)
    private int limit;                     // Opcional - Por defecto 50
    private int offset;                    // Opcional - Por defecto 0
    
    // Getters y Setters
}
```

---

### 2. Validadores de Archivos

**Ubicación:** `LogicaNegocio/Validadores/src/main/java/com/unillanos/server/validation/`

#### TipoArchivoValidator.java (NUEVO)
```java
public class TipoArchivoValidator {
    
    // Extensiones permitidas
    private static final Set<String> EXTENSIONES_IMAGEN = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");
    private static final Set<String> EXTENSIONES_AUDIO = Set.of("mp3", "wav", "ogg", "m4a", "aac");
    private static final Set<String> EXTENSIONES_DOCUMENTO = Set.of("pdf", "docx", "txt", "xlsx", "pptx", "zip");
    
    // MIME types permitidos
    private static final Set<String> MIME_IMAGEN = Set.of(
        "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"
    );
    private static final Set<String> MIME_AUDIO = Set.of(
        "audio/mpeg", "audio/wav", "audio/ogg", "audio/mp4", "audio/aac"
    );
    private static final Set<String> MIME_DOCUMENTO = Set.of(
        "application/pdf", 
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "text/plain",
        "application/zip"
    );
    
    /**
     * Valida la extensión del archivo.
     */
    public static void validateExtension(String nombreArchivo) throws ValidationException {
        if (nombreArchivo == null || !nombreArchivo.contains(".")) {
            throw new ValidationException("El archivo debe tener una extensión válida", "nombreArchivo");
        }
        
        String extension = nombreArchivo.substring(nombreArchivo.lastIndexOf(".") + 1).toLowerCase();
        
        if (!EXTENSIONES_IMAGEN.contains(extension) && 
            !EXTENSIONES_AUDIO.contains(extension) && 
            !EXTENSIONES_DOCUMENTO.contains(extension)) {
            throw new ValidationException(
                "Extensión de archivo no permitida: " + extension, 
                "nombreArchivo"
            );
        }
    }
    
    /**
     * Valida el tipo MIME del archivo.
     */
    public static void validateMimeType(String tipoMime) throws ValidationException {
        if (tipoMime == null || tipoMime.trim().isEmpty()) {
            throw new ValidationException("El tipo MIME es requerido", "tipoMime");
        }
        
        if (!MIME_IMAGEN.contains(tipoMime) && 
            !MIME_AUDIO.contains(tipoMime) && 
            !MIME_DOCUMENTO.contains(tipoMime)) {
            throw new ValidationException(
                "Tipo MIME no permitido: " + tipoMime, 
                "tipoMime"
            );
        }
    }
    
    /**
     * Determina el tipo de archivo según el MIME type.
     */
    public static TipoArchivo detectarTipo(String tipoMime) {
        if (MIME_IMAGEN.contains(tipoMime)) {
            return TipoArchivo.IMAGEN;
        } else if (MIME_AUDIO.contains(tipoMime)) {
            return TipoArchivo.AUDIO;
        } else if (MIME_DOCUMENTO.contains(tipoMime)) {
            return TipoArchivo.DOCUMENTO;
        }
        return null;
    }
}
```

#### TamanoArchivoValidator.java (NUEVO)
```java
public class TamanoArchivoValidator {
    
    private static final long MAX_SIZE_IMAGEN = 10 * 1024 * 1024;     // 10 MB
    private static final long MAX_SIZE_AUDIO = 20 * 1024 * 1024;      // 20 MB
    private static final long MAX_SIZE_DOCUMENTO = 50 * 1024 * 1024;  // 50 MB
    
    /**
     * Valida el tamaño del archivo según su tipo.
     */
    public static void validate(long tamanoBytes, TipoArchivo tipo) throws ValidationException {
        if (tamanoBytes <= 0) {
            throw new ValidationException("El tamaño del archivo debe ser mayor a 0", "tamanoBytes");
        }
        
        long maxSize = switch (tipo) {
            case IMAGEN -> MAX_SIZE_IMAGEN;
            case AUDIO -> MAX_SIZE_AUDIO;
            case DOCUMENTO -> MAX_SIZE_DOCUMENTO;
        };
        
        if (tamanoBytes > maxSize) {
            throw new ValidationException(
                String.format("El archivo excede el tamaño máximo permitido (%d MB)", maxSize / (1024 * 1024)), 
                "tamanoBytes"
            );
        }
    }
}
```

#### SubirArchivoValidator.java (NUEVO - Validador compuesto)
```java
public class SubirArchivoValidator {
    
    public static void validate(DTOSubirArchivo dto) throws ValidationException {
        if (dto == null) {
            throw new ValidationException("Los datos del archivo son requeridos", "dto");
        }
        
        // Validar usuarioId
        if (dto.getUsuarioId() == null || dto.getUsuarioId().trim().isEmpty()) {
            throw new ValidationException("El ID del usuario es requerido", "usuarioId");
        }
        
        // Validar nombreArchivo
        if (dto.getNombreArchivo() == null || dto.getNombreArchivo().trim().isEmpty()) {
            throw new ValidationException("El nombre del archivo es requerido", "nombreArchivo");
        }
        TipoArchivoValidator.validateExtension(dto.getNombreArchivo());
        
        // Validar tipoMime
        TipoArchivoValidator.validateMimeType(dto.getTipoMime());
        
        // Validar tamaño
        TipoArchivo tipo = TipoArchivoValidator.detectarTipo(dto.getTipoMime());
        TamanoArchivoValidator.validate(dto.getTamanoBytes(), tipo);
        
        // Validar base64Data
        if (dto.getBase64Data() == null || dto.getBase64Data().trim().isEmpty()) {
            throw new ValidationException("El contenido del archivo es requerido", "base64Data");
        }
    }
}
```

---

### 3. Entidades y Mappers de Archivos

**Ubicación:** `Datos/Repositorios/src/main/java/com/unillanos/server/repository/models/`

#### TipoArchivo.java (NUEVO - Enum)
```java
public enum TipoArchivo {
    IMAGEN,      // Imágenes (JPG, PNG, GIF, WEBP, BMP)
    AUDIO,       // Audios (MP3, WAV, OGG, M4A, AAC)
    DOCUMENTO;   // Documentos (PDF, DOCX, TXT, XLSX, PPTX, ZIP)
    
    public static TipoArchivo fromString(String tipo) {
        if (tipo == null || tipo.trim().isEmpty()) {
            return DOCUMENTO; // Por defecto
        }
        
        try {
            return TipoArchivo.valueOf(tipo.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return DOCUMENTO;
        }
    }
}
```

#### ArchivoEntity.java (NUEVO)
```java
public class ArchivoEntity {
    private String id;                      // UUID
    private String nombreOriginal;          // Nombre original del archivo
    private String nombreAlmacenado;        // Nombre UUID en disco
    private String tipoMime;                // Tipo MIME
    private TipoArchivo tipoArchivo;        // IMAGEN, AUDIO, DOCUMENTO
    private String hashSha256;              // Hash SHA-256 para deduplicación
    private long tamanoBytes;               // Tamaño en bytes
    private String rutaAlmacenamiento;      // Ruta relativa en el sistema de archivos
    private String usuarioId;               // Usuario que subió el archivo
    private LocalDateTime fechaSubida;
    
    // Constructores
    public ArchivoEntity() {}
    
    public ArchivoEntity(String id, String nombreOriginal, String nombreAlmacenado,
                         String tipoMime, TipoArchivo tipoArchivo, String hashSha256,
                         long tamanoBytes, String rutaAlmacenamiento, String usuarioId,
                         LocalDateTime fechaSubida) {
        this.id = id;
        this.nombreOriginal = nombreOriginal;
        this.nombreAlmacenado = nombreAlmacenado;
        this.tipoMime = tipoMime;
        this.tipoArchivo = tipoArchivo;
        this.hashSha256 = hashSha256;
        this.tamanoBytes = tamanoBytes;
        this.rutaAlmacenamiento = rutaAlmacenamiento;
        this.usuarioId = usuarioId;
        this.fechaSubida = fechaSubida;
    }
    
    // Getters y Setters
    // ...
    
    /**
     * Convierte la entidad a DTO.
     */
    public DTOArchivo toDTO(boolean esDuplicado) {
        DTOArchivo dto = new DTOArchivo();
        dto.setId(this.id);
        dto.setNombreOriginal(this.nombreOriginal);
        dto.setTipoMime(this.tipoMime);
        dto.setTipoArchivo(this.tipoArchivo != null ? this.tipoArchivo.name() : null);
        dto.setTamanoBytes(this.tamanoBytes);
        dto.setHashSha256(this.hashSha256);
        dto.setDuplicado(esDuplicado);
        dto.setUsuarioId(this.usuarioId);
        dto.setFechaSubida(this.fechaSubida != null ? this.fechaSubida.toString() : null);
        dto.setUrlDescarga("/api/archivos/descargar/" + this.id);
        return dto;
    }
}
```

#### ArchivoMapper.java (NUEVO)
```java
public class ArchivoMapper {
    
    public static ArchivoEntity mapRow(ResultSet rs) throws SQLException {
        ArchivoEntity archivo = new ArchivoEntity();
        
        archivo.setId(rs.getString("id"));
        archivo.setNombreOriginal(rs.getString("nombre_original"));
        archivo.setNombreAlmacenado(rs.getString("nombre_almacenado"));
        archivo.setTipoMime(rs.getString("tipo_mime"));
        
        // Convertir String a TipoArchivo enum
        String tipoStr = rs.getString("tipo_archivo");
        archivo.setTipoArchivo(TipoArchivo.fromString(tipoStr));
        
        archivo.setHashSha256(rs.getString("hash_sha256"));
        archivo.setTamanoBytes(rs.getLong("tamano_bytes"));
        archivo.setRutaAlmacenamiento(rs.getString("ruta_almacenamiento"));
        archivo.setUsuarioId(rs.getString("usuario_id"));
        
        // Convertir Timestamp a LocalDateTime
        Timestamp fechaSubida = rs.getTimestamp("fecha_subida");
        if (fechaSubida != null) {
            archivo.setFechaSubida(fechaSubida.toLocalDateTime());
        }
        
        return archivo;
    }
}
```

---

### 4. Interfaz de Repositorio

**Ubicación:** `Datos/Repositorios/src/main/java/com/unillanos/server/repository/interfaces/`

#### IArchivoRepository.java (NUEVO)
```java
public interface IArchivoRepository {
    
    // --- MÉTODOS DE CONSULTA ---
    
    /**
     * Busca un archivo por su ID.
     */
    Optional<ArchivoEntity> findById(String id);
    
    /**
     * Busca un archivo por su hash SHA-256.
     * Útil para detectar duplicados.
     */
    Optional<ArchivoEntity> findByHash(String hashSha256);
    
    /**
     * Obtiene todos los archivos de un usuario con paginación.
     */
    List<ArchivoEntity> findByUsuario(String usuarioId, int limit, int offset);
    
    /**
     * Obtiene archivos de un usuario filtrados por tipo.
     */
    List<ArchivoEntity> findByUsuarioYTipo(String usuarioId, TipoArchivo tipo, int limit, int offset);
    
    /**
     * Cuenta el total de archivos de un usuario.
     */
    int countByUsuario(String usuarioId);
    
    /**
     * Verifica si existe un archivo con el hash dado.
     */
    boolean existsByHash(String hashSha256);
    
    // --- MÉTODOS DE ESCRITURA ---
    
    /**
     * Guarda un nuevo archivo.
     */
    ArchivoEntity save(ArchivoEntity archivo);
    
    /**
     * Elimina un archivo por su ID.
     */
    void deleteById(String id);
}
```

---

### 5. Implementación del Repositorio

**Ubicación:** `Datos/Repositorios/src/main/java/com/unillanos/server/repository/impl/`

#### ArchivoRepositoryImpl.java (NUEVO)

**Métodos a Implementar:**

```java
@Repository
public class ArchivoRepositoryImpl implements IArchivoRepository {
    
    private final DataSource dataSource;
    
    @Override
    public Optional<ArchivoEntity> findById(String id) {
        // SELECT * FROM archivos WHERE id = ?
    }
    
    @Override
    public Optional<ArchivoEntity> findByHash(String hashSha256) {
        // SELECT * FROM archivos WHERE hash_sha256 = ?
    }
    
    @Override
    public List<ArchivoEntity> findByUsuario(String usuarioId, int limit, int offset) {
        // SELECT * FROM archivos WHERE usuario_id = ? ORDER BY fecha_subida DESC LIMIT ? OFFSET ?
    }
    
    @Override
    public List<ArchivoEntity> findByUsuarioYTipo(String usuarioId, TipoArchivo tipo, int limit, int offset) {
        // SELECT * FROM archivos WHERE usuario_id = ? AND tipo_archivo = ? ORDER BY fecha_subida DESC LIMIT ? OFFSET ?
    }
    
    @Override
    public int countByUsuario(String usuarioId) {
        // SELECT COUNT(*) FROM archivos WHERE usuario_id = ?
    }
    
    @Override
    public boolean existsByHash(String hashSha256) {
        // SELECT EXISTS(SELECT 1 FROM archivos WHERE hash_sha256 = ?)
    }
    
    @Override
    public ArchivoEntity save(ArchivoEntity archivo) {
        // INSERT INTO archivos (id, nombre_original, nombre_almacenado, tipo_mime, tipo_archivo, hash_sha256, tamano_bytes, ruta_almacenamiento, usuario_id, fecha_subida)
        // VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        // Si archivo.id es null, generar UUID
    }
    
    @Override
    public void deleteById(String id) {
        // DELETE FROM archivos WHERE id = ?
    }
}
```

**Consideraciones:**
- Usar `PreparedStatement` en TODAS las operaciones
- Usar `ArchivoMapper.mapRow()` para convertir filas a entidades
- Cerrar recursos en bloques `finally`
- Lanzar `RepositoryException` en caso de error SQL
- El ID del archivo es UUID generado en Java

---

### 6. ArchivoService

**Ubicación:** `LogicaNegocio/Servicios/src/main/java/com/unillanos/server/service/impl/ArchivoService.java`

**Responsabilidades:**
- Subir archivos (validar, calcular hash, guardar en disco, persistir en BD)
- Descargar archivos (validar permisos, leer desde disco)
- Listar archivos del usuario
- Deduplicación automática por hash SHA-256
- Gestión de almacenamiento en disco

**Configuración del Directorio de Almacenamiento:**

Agregar a `application.properties`:
```properties
# Configuración de archivos
archivos.directorio.base=./uploads
archivos.directorio.imagenes=imagenes
archivos.directorio.audios=audios
archivos.directorio.documentos=documentos
```

**Métodos Principales:**

```java
@Service
public class ArchivoService {
    
    private final IArchivoRepository archivoRepository;
    private final IUsuarioRepository usuarioRepository;
    private final LoggerService loggerService;
    private final String directorioBase;
    
    public ArchivoService(IArchivoRepository archivoRepository,
                         IUsuarioRepository usuarioRepository,
                         LoggerService loggerService,
                         @Value("${archivos.directorio.base}") String directorioBase) {
        this.archivoRepository = archivoRepository;
        this.usuarioRepository = usuarioRepository;
        this.loggerService = loggerService;
        this.directorioBase = directorioBase;
        
        // Crear directorios si no existen
        inicializarDirectorios();
    }
    
    /**
     * Sube un archivo al sistema.
     * 
     * @param dto Datos del archivo a subir
     * @return DTOResponse con información del archivo guardado
     */
    public DTOResponse subirArchivo(DTOSubirArchivo dto) {
        // 1. Validar datos con SubirArchivoValidator
        // 2. Verificar que el usuario existe
        // 3. Decodificar Base64 a bytes
        // 4. Calcular hash SHA-256 del contenido
        // 5. Verificar si ya existe un archivo con ese hash (deduplicación)
        // 6. Si existe, retornar el archivo existente marcado como duplicado
        // 7. Si no existe:
        //    a. Generar UUID para el archivo
        //    b. Determinar subdirectorio según tipo (imagenes/, audios/, documentos/)
        //    c. Guardar archivo en disco (uploads/imagenes/uuid.ext)
        //    d. Crear ArchivoEntity
        //    e. Guardar en BD
        // 8. Registrar en logs
        // 9. Retornar DTOResponse.success con DTOArchivo
    }
    
    /**
     * Descarga un archivo del sistema.
     * 
     * @param dto Datos de descarga
     * @return DTOResponse con el archivo en Base64
     */
    public DTOResponse descargarArchivo(DTODescargarArchivo dto) {
        // 1. Validar que archivoId y usuarioId estén presentes
        // 2. Verificar que el archivo existe
        // 3. Validar permisos:
        //    - El usuario debe ser el dueño del archivo
        //    - O el archivo debe estar en un mensaje visible para el usuario
        // 4. Leer archivo desde disco
        // 5. Codificar a Base64
        // 6. Registrar en logs
        // 7. Retornar DTOResponse.success con DTOArchivoData
    }
    
    /**
     * Lista los archivos de un usuario.
     * 
     * @param dto Parámetros de consulta
     * @return DTOResponse con lista de archivos
     */
    public DTOResponse listarArchivos(DTOListarArchivos dto) {
        // 1. Validar que usuarioId esté presente
        // 2. Validar límites de paginación
        // 3. Si hay filtro por tipo, obtener archivos filtrados
        // 4. Si no hay filtro, obtener todos los archivos del usuario
        // 5. Convertir cada ArchivoEntity a DTOArchivo
        // 6. Retornar DTOResponse.success con lista de DTOArchivo
    }
    
    /**
     * Calcula el hash SHA-256 de un array de bytes.
     */
    private String calcularHash(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        return bytesToHex(hash);
    }
    
    /**
     * Convierte bytes a hexadecimal.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * Inicializa los directorios de almacenamiento.
     */
    private void inicializarDirectorios() {
        try {
            Path baseDir = Paths.get(directorioBase);
            Files.createDirectories(baseDir.resolve("imagenes"));
            Files.createDirectories(baseDir.resolve("audios"));
            Files.createDirectories(baseDir.resolve("documentos"));
        } catch (IOException e) {
            logger.error("Error al crear directorios de almacenamiento", e);
            throw new RuntimeException("Error al inicializar directorios", e);
        }
    }
    
    /**
     * Guarda un archivo en disco.
     */
    private void guardarEnDisco(byte[] data, String rutaRelativa) throws IOException {
        Path rutaCompleta = Paths.get(directorioBase, rutaRelativa);
        Files.write(rutaCompleta, data);
    }
    
    /**
     * Lee un archivo desde disco.
     */
    private byte[] leerDesdeDisco(String rutaRelativa) throws IOException {
        Path rutaCompleta = Paths.get(directorioBase, rutaRelativa);
        return Files.readAllBytes(rutaCompleta);
    }
}
```

---

### 7. Actualizar ActionDispatcher

**Ubicación:** `LogicaNegocio/Servicios/src/main/java/com/unillanos/server/service/impl/ActionDispatcherImpl.java`

**Nuevas Acciones a Enrutar:**

```java
// Inyectar ArchivoService en el constructor
private final ArchivoService archivoService;

// En el switch de dispatch():

// --- ACCIONES DE ARCHIVOS ---
case "subirArchivo" -> handleSubirArchivo(request);
case "descargarArchivo" -> handleDescargarArchivo(request);
case "listarArchivos" -> handleListarArchivos(request);

// Métodos privados:

private DTOResponse handleSubirArchivo(DTORequest request) {
    DTOSubirArchivo dto = gson.fromJson(gson.toJson(request.getPayload()), DTOSubirArchivo.class);
    return archivoService.subirArchivo(dto);
}

private DTOResponse handleDescargarArchivo(DTORequest request) {
    DTODescargarArchivo dto = gson.fromJson(gson.toJson(request.getPayload()), DTODescargarArchivo.class);
    return archivoService.descargarArchivo(dto);
}

private DTOResponse handleListarArchivos(DTORequest request) {
    DTOListarArchivos dto = gson.fromJson(gson.toJson(request.getPayload()), DTOListarArchivos.class);
    return archivoService.listarArchivos(dto);
}
```

---

### 8. Integración con Mensajería

#### Actualizar MensajeriaService

Ya está preparado en Épica 4, solo necesitamos:

1. Cuando se envía un mensaje con `fileId`:
   - Verificar que el archivo existe
   - Verificar que el usuario es el dueño del archivo
   - Asociar el archivo al mensaje

2. Al obtener historial:
   - Si el mensaje tiene `fileId`, obtener información del archivo
   - Incluir `fileName` y `fileId` en el DTOMensaje

**Modificación en `enviarMensajeDirecto` y `enviarMensajeCanal`:**

```java
// Si dto tiene fileId, validar que existe y pertenece al usuario
if (dto.getFileId() != null && !dto.getFileId().trim().isEmpty()) {
    Optional<ArchivoEntity> archivoOpt = archivoRepository.findById(dto.getFileId());
    if (archivoOpt.isEmpty()) {
        throw new NotFoundException("Archivo no encontrado", "FILE_NOT_FOUND");
    }
    ArchivoEntity archivo = archivoOpt.get();
    if (!archivo.getUsuarioId().equals(dto.getRemitenteId())) {
        throw new AuthenticationException(
            "No tienes permisos para adjuntar este archivo",
            "FILE_NOT_OWNED"
        );
    }
    // Usar el nombreOriginal del archivo para el DTOMensaje
    fileName = archivo.getNombreOriginal();
}
```

---

## Orden de Implementación

### Fase 1: DTOs y Validadores (Base)
1. ✅ Crear 5 DTOs de archivos
2. ✅ Crear 3 validadores (TipoArchivoValidator, TamanoArchivoValidator, SubirArchivoValidator)

### Fase 2: Entidades y Repositorio
3. ✅ Crear enum TipoArchivo
4. ✅ Crear ArchivoEntity con método toDTO()
5. ✅ Crear ArchivoMapper
6. ✅ Crear IArchivoRepository
7. ✅ Implementar ArchivoRepositoryImpl con JDBC (8 métodos)

### Fase 3: Lógica de Negocio
8. ✅ Agregar configuración de directorios en application.properties
9. ✅ Implementar ArchivoService completo (3 métodos principales + helpers)
10. ✅ Actualizar ActionDispatcher con las 3 nuevas acciones
11. ✅ Actualizar MensajeriaService para validar archivos adjuntos

### Fase 4: Integración y Pruebas
12. ✅ Compilar y verificar que no hay errores
13. ✅ Probar flujo de subida de imagen
14. ✅ Probar flujo de subida de audio
15. ✅ Probar flujo de subida de documento
16. ✅ Probar deduplicación (subir mismo archivo dos veces)
17. ✅ Probar descarga de archivos
18. ✅ Probar envío de mensaje con archivo adjunto
19. ✅ Verificar logs en base de datos
20. ✅ Verificar archivos en sistema de archivos

---

## Criterios de Aceptación

✅ **Épica 5 estará completa cuando:**

1. Un usuario puede subir una imagen (JPG, PNG, GIF, etc.)
2. Un usuario puede subir un audio (MP3, WAV, OGG, etc.)
3. Un usuario puede subir un documento (PDF, DOCX, TXT, etc.)
4. El sistema valida el tipo MIME y extensión del archivo
5. El sistema valida el tamaño según el tipo de archivo
6. El sistema calcula el hash SHA-256 del archivo
7. Si un archivo con el mismo hash ya existe, se reutiliza (deduplicación)
8. Los archivos se guardan en subdirectorios según su tipo
9. Los archivos se nombran con UUID para evitar colisiones
10. Un usuario puede descargar sus propios archivos
11. Un usuario puede descargar archivos de mensajes que puede ver
12. Un usuario puede listar sus archivos con paginación
13. Un usuario puede filtrar archivos por tipo (IMAGEN, AUDIO, DOCUMENTO)
14. Un usuario puede enviar un mensaje con archivo adjunto
15. El archivo adjunto aparece en el historial de mensajes
16. Todas las operaciones se registran en `logs_sistema`
17. No hay errores de compilación (`mvn clean install`)
18. Los archivos existen físicamente en el sistema de archivos
19. Los datos en la BD son correctos (archivos, tipos, hashes, rutas)

---

## Verificación Final

### Prueba 1: Subir Imagen
```bash
telnet localhost 8080

{"action":"subirArchivo","payload":{"usuarioId":"uuid-usuario","nombreArchivo":"foto.jpg","tipoMime":"image/jpeg","tamanoBytes":524288,"base64Data":"iVBORw0KGgoAAAANS..."}}

# Esperado:
{
  "action":"subirArchivo",
  "status":"success",
  "message":"Archivo subido exitosamente",
  "data":{
    "id":"uuid-archivo",
    "nombreOriginal":"foto.jpg",
    "tipoMime":"image/jpeg",
    "tipoArchivo":"IMAGEN",
    "tamanoBytes":524288,
    "hashSha256":"a3b2c1...",
    "duplicado":false,
    "usuarioId":"uuid-usuario",
    "fechaSubida":"2025-10-14T...",
    "urlDescarga":"/api/archivos/descargar/uuid-archivo"
  }
}
```

### Prueba 2: Subir Audio
```bash
{"action":"subirArchivo","payload":{"usuarioId":"uuid-usuario","nombreArchivo":"audio.mp3","tipoMime":"audio/mpeg","tamanoBytes":2097152,"base64Data":"SUQzAwAAAA..."}}

# Esperado: Similar a Prueba 1 con tipoArchivo:"AUDIO"
```

### Prueba 3: Subir Documento
```bash
{"action":"subirArchivo","payload":{"usuarioId":"uuid-usuario","nombreArchivo":"documento.pdf","tipoMime":"application/pdf","tamanoBytes":1048576,"base64Data":"JVBERi0xLj..."}}

# Esperado: Similar a Prueba 1 con tipoArchivo:"DOCUMENTO"
```

### Prueba 4: Deduplicación (subir mismo archivo)
```bash
# Subir el mismo archivo otra vez
{"action":"subirArchivo","payload":{"usuarioId":"uuid-usuario","nombreArchivo":"foto.jpg","tipoMime":"image/jpeg","tamanoBytes":524288,"base64Data":"iVBORw0KGgoAAAANS..."}}

# Esperado:
{
  "action":"subirArchivo",
  "status":"success",
  "message":"Archivo ya existe, se reutilizó la copia existente",
  "data":{
    "id":"uuid-archivo",  // Mismo ID que antes
    "duplicado":true      // Marcado como duplicado
  }
}
```

### Prueba 5: Descargar Archivo
```bash
{"action":"descargarArchivo","payload":{"archivoId":"uuid-archivo","usuarioId":"uuid-usuario"}}

# Esperado:
{
  "action":"descargarArchivo",
  "status":"success",
  "message":"Archivo descargado exitosamente",
  "data":{
    "id":"uuid-archivo",
    "nombreOriginal":"foto.jpg",
    "tipoMime":"image/jpeg",
    "tamanoBytes":524288,
    "base64Data":"iVBORw0KGgoAAAANS..."
  }
}
```

### Prueba 6: Listar Archivos
```bash
{"action":"listarArchivos","payload":{"usuarioId":"uuid-usuario","limit":10,"offset":0}}

# Esperado: Lista de archivos del usuario, ordenados por fecha descendente
```

### Prueba 7: Listar Archivos Filtrados por Tipo
```bash
{"action":"listarArchivos","payload":{"usuarioId":"uuid-usuario","tipoArchivo":"IMAGEN","limit":10,"offset":0}}

# Esperado: Solo imágenes del usuario
```

### Prueba 8: Enviar Mensaje con Archivo Adjunto
```bash
{"action":"enviarMensajeDirecto","payload":{"remitenteId":"uuid-usuario-1","destinatarioId":"uuid-usuario-2","contenido":"Te envío esta imagen","fileId":"uuid-archivo"}}

# Esperado:
{
  "action":"enviarMensajeDirecto",
  "status":"success",
  "message":"Mensaje enviado exitosamente",
  "data":{
    "id":1,
    "contenido":"Te envío esta imagen",
    "fileId":"uuid-archivo",
    "fileName":"foto.jpg"
  }
}
```

### Prueba 9: Verificar Sistema de Archivos
```bash
# Windows (PowerShell)
dir uploads\imagenes
dir uploads\audios
dir uploads\documentos

# Linux/Mac
ls uploads/imagenes
ls uploads/audios
ls uploads/documentos

# Debe mostrar archivos con nombres UUID
```

### Prueba 10: Verificar BD
```sql
-- Verificar que los archivos fueron creados
SELECT id, nombre_original, tipo_archivo, tipo_mime, tamano_bytes, hash_sha256
FROM archivos
ORDER BY fecha_subida DESC
LIMIT 10;

-- Verificar logs de archivos
SELECT * FROM logs_sistema
WHERE accion IN ('subirArchivo', 'descargarArchivo', 'listarArchivos')
ORDER BY timestamp DESC;

-- Verificar mensajes con archivos adjuntos
SELECT m.id, m.contenido, m.file_id, a.nombre_original
FROM mensajes m
LEFT JOIN archivos a ON m.file_id = a.id
WHERE m.file_id IS NOT NULL
ORDER BY m.fecha_envio DESC;
```

---

## Dependencias Nuevas Requeridas

No se requieren dependencias adicionales. Todas las funcionalidades se implementan con:
- Java NIO (Files, Paths) - Incluido en JDK
- java.security.MessageDigest - Incluido en JDK
- java.util.Base64 - Incluido en JDK
- Spring Boot, JDBC, HikariCP, Gson (ya declaradas)

---

## Estimación de Tiempo

- **Fase 1 (DTOs y Validadores):** ~30-40 minutos
- **Fase 2 (Entidades y Repositorio):** ~40-50 minutos
- **Fase 3 (Lógica de Negocio):** ~60-75 minutos
- **Fase 4 (Integración y Pruebas):** ~30-40 minutos

**Total Estimado:** 160-205 minutos (2.7 - 3.4 horas)

---

## Notas Importantes

🔐 **Seguridad:**
- Validar que el usuario sea dueño del archivo antes de permitir descarga
- Validar permisos de acceso a archivos en mensajes compartidos
- Sanitizar nombres de archivos para evitar path traversal
- Validar tipos MIME para prevenir ejecución de archivos maliciosos

⚡ **Performance:**
- Deduplicación por hash ahorra espacio en disco
- Usar índices en `archivos` (hash_sha256, usuario_id)
- Limitar resultados con paginación
- Considerar cache de archivos frecuentemente accedidos (futuro)

📊 **Almacenamiento:**
- Archivos organizados por tipo en subdirectorios
- Nombres UUID evitan colisiones
- Hash SHA-256 para deduplicación
- Tamaños máximos: IMAGEN=10MB, AUDIO=20MB, DOCUMENTO=50MB

🎯 **Reglas de Negocio:**
- Un archivo puede ser usado en múltiples mensajes
- Los archivos duplicados reutilizan el físico existente
- Solo el dueño puede eliminar archivos (futuro)
- Los archivos en mensajes compartidos son accesibles para receptores

📝 **Funcionalidades Opcionales (para futuro):**
- Miniaturas de imágenes (thumbnails)
- Conversión automática de formatos de audio
- Previsualización de documentos PDF
- Límite de espacio por usuario
- Papelera de reciclaje para archivos eliminados
- Compresión automática de archivos grandes
- Streaming de audio/video
- OCR para documentos escaneados

---

¿Estás listo para comenzar la implementación de la Épica 5?

