## Pruebas Sugeridas

### 1. Prueba de Descarga Básica

```java
@Test
public void testDescargarArchivoEnMemoria() {
    IGestionArchivos gestion = new GestionArchivosImpl();
    
    gestion.descargarArchivoEnMemoria("file123")
        .thenAccept(bytes -> {
            assertNotNull(bytes);
            assertTrue(bytes.length > 0);
        })
        .join();
}
```

### 2. Prueba de Caché

```java
@Test
public void testCacheLocal() {
    IGestionArchivos gestion = new GestionArchivosImpl();
    
    // Primera descarga (desde servidor)
    byte[] bytes1 = gestion.descargarArchivoEnMemoria("file123").join();
    
    // Segunda descarga (desde caché)
    byte[] bytes2 = gestion.descargarArchivoEnMemoria("file123").join();
    
    assertArrayEquals(bytes1, bytes2);
}
```

### 3. Prueba de Reproducción

```java
@Test
public void testReproducirAudio() {
    GestorAudio gestor = new GestorAudio(new GestionArchivosImpl());
    
    gestor.reproducirAudio("audio123")
        .thenRun(() -> {
            assertTrue(gestor.estaReproduciendo());
            assertTrue(gestor.getDuracionTotal() > 0);
        })
        .join();
        
    gestor.dispose();
}
```

---

## Próximos Pasos

1. **Integración con UI:**
   - Agregar botones de reproducción en mensajes de audio
   - Implementar barra de progreso
   - Mostrar duración del audio

2. **Mejoras:**
   - Implementar cola de reproducción
   - Agregar control de volumen
   - Soporte para formatos MP3/OGG

3. **Optimizaciones:**
   - Precarga de audios en conversación activa
   - Limpieza automática de caché antiguo
   - Compresión de audios antes de enviar

---

## Resumen

✅ **Completado:**
- Descarga de archivos en memoria
- Sistema de caché local en H2
- Reproductor de audio básico
- Manejo de errores y recuperación

📋 **Pendiente:**
- Integración con UI de chat
- Soporte para formatos adicionales
- Optimizaciones de rendimiento
# Documentación: Sistema de Descarga y Reproducción de Audio en Memoria

## Descripción General

Este sistema permite descargar y reproducir archivos de audio directamente en memoria, sin necesidad de guardarlos en disco. Utiliza el sistema de chunks existente y agrega capacidad de caché en la base de datos local H2.

## Componentes Principales

### 1. IGestionArchivos - Interfaz

**Ubicación:** `Negocio/GestionArchivos/src/main/java/gestionArchivos/IGestionArchivos.java`

#### Método Principal

```java
CompletableFuture<byte[]> descargarArchivoEnMemoria(String fileId);
```

**Propósito:** Descargar un archivo desde el servidor directamente en memoria como array de bytes.

**Parámetros:**
- `fileId`: Identificador único del archivo en el servidor

**Retorna:** `CompletableFuture<byte[]>` que se completa con los bytes del archivo

---

### 2. GestionArchivosImpl - Implementación

**Ubicación:** `Negocio/GestionArchivos/src/main/java/gestionArchivos/GestionArchivosImpl.java`

#### Flujo de Descarga en Memoria

```
1. Verificar caché local (BD H2)
   ├─ Si existe y está completo → Retornar desde caché
   └─ Si no existe → Descargar desde servidor

2. Descargar desde servidor
   ├─ Solicitar inicio de descarga (startFileDownload)
   ├─ Recibir información (downloadId, chunks totales, etc.)
   ├─ Solicitar chunks uno por uno (requestFileChunk)
   └─ Ensamblar chunks en memoria

3. Guardar en caché local para futuros usos
   ├─ Convertir bytes a Base64
   ├─ Calcular hash SHA-256
   └─ Guardar en BD H2 con estado "completo"

4. Retornar bytes del archivo
```

#### Métodos Clave

##### `descargarArchivoEnMemoria(String fileId)`

Método público que orquesta todo el proceso de descarga en memoria.

```java
@Override
public CompletableFuture<byte[]> descargarArchivoEnMemoria(String fileId) {
    // 1. Verificar caché local
    // 2. Si existe, retornar desde caché
    // 3. Si no existe, descargar desde servidor
    // 4. Guardar en caché
    // 5. Retornar bytes
}
```

##### `recibirChunksEnMemoria(DTODownloadInfo, String)`

Método privado que recibe y ensambla los chunks en memoria.

```java
private CompletableFuture<byte[]> recibirChunksEnMemoria(
    DTODownloadInfo downloadInfo, 
    String fileId
) {
    // 1. Solicitar cada chunk
    // 2. Almacenar en lista temporal
    // 3. Calcular progreso
    // 4. Ensamblar todos los chunks en un solo array
    // 5. Guardar en caché local
    // 6. Retornar bytes completos
}
```

---

### 3. GestorAudio - Reproductor de Audio

**Ubicación:** `Negocio/GestionArchivos/src/main/java/gestionArchivos/GestorAudio.java`

#### Propósito

Gestionar la reproducción de archivos de audio descargados en memoria utilizando la API `javax.sound.sampled`.

#### Características

- ✅ Reproducción de audio desde bytes en memoria
- ✅ Control de reproducción (pausar, reanudar, detener)
- ✅ Navegación por posición
- ✅ Información de duración y progreso
- ✅ Liberación automática de recursos

#### Métodos Públicos

```java
// Descargar y reproducir audio
CompletableFuture<Void> reproducirAudio(String fileId)

// Controles de reproducción
void pausar()
void reanudar()
void detener()

// Información de reproducción
boolean estaReproduciendo()
long getPosicionActual()    // en microsegundos
long getDuracionTotal()     // en microsegundos

// Navegación
void setPosicion(long microsegundos)

// Liberar recursos
void dispose()
```

#### Ejemplo de Uso

```java
// 1. Crear instancia
IGestionArchivos gestionArchivos = new GestionArchivosImpl();
GestorAudio gestorAudio = new GestorAudio(gestionArchivos);

// 2. Reproducir audio
gestorAudio.reproducirAudio("file123")
    .thenRun(() -> {
        System.out.println("Audio reproduciéndose...");
    })
    .exceptionally(ex -> {
        System.err.println("Error: " + ex.getMessage());
        return null;
    });

// 3. Control de reproducción
gestorAudio.pausar();
gestorAudio.reanudar();
gestorAudio.detener();

// 4. Obtener información
boolean reproduciendo = gestorAudio.estaReproduciendo();
long posicion = gestorAudio.getPosicionActual();
long duracion = gestorAudio.getDuracionTotal();

// 5. Navegar
gestorAudio.setPosicion(5000000); // 5 segundos

// 6. Limpiar al finalizar
gestorAudio.dispose();
```

---

## Sistema de Caché Local

### Base de Datos H2

Los archivos descargados se almacenan en la tabla `archivos` con la siguiente estructura:

```sql
CREATE TABLE archivos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_id_servidor VARCHAR(255) UNIQUE NOT NULL,
    nombre_archivo VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100),
    tamano BIGINT,
    hash_sha256 VARCHAR(64),
    contenido_base64 CLOB,
    estado VARCHAR(20),  -- 'descargando', 'completo', 'error'
    fecha_descarga TIMESTAMP,
    fecha_actualizacion TIMESTAMP
);
```

### Ventajas del Caché

1. **Rendimiento:** Archivos descargados una vez se reutilizan
2. **Offline:** Acceso a archivos sin conexión
3. **Ancho de Banda:** Reduce el tráfico de red
4. **Verificación:** Hash SHA-256 garantiza integridad

---

## Protocolo de Comunicación

### 1. Inicio de Descarga

**Cliente → Servidor:**
```json
{
    "action": "startFileDownload",
    "data": {
        "fileId": "file123"
    }
}
```

**Servidor → Cliente:**
```json
{
    "success": true,
    "data": {
        "downloadId": "download456",
        "fileName": "audio.wav",
        "mimeType": "audio/wav",
        "fileSize": 1048576,
        "totalChunks": 4
    }
}
```

### 2. Solicitud de Chunk

**Cliente → Servidor:**
```json
{
    "action": "requestFileChunk",
    "data": {
        "downloadId": "download456",
        "chunkNumber": 1
    }
}
```

**Servidor → Cliente:**
```json
{
    "success": true,
    "data": {
        "downloadId": "download456",
        "chunkNumber": 1,
        "chunkData": "base64EncodedData..."
    }
}
```

### 3. Proceso Completo

```
Cliente                                    Servidor
   |                                          |
   |------ startFileDownload ---------------→|
   |←----- downloadInfo ----------------------|
   |                                          |
   |------ requestFileChunk (1) -------------→|
   |←----- chunk 1 ---------------------------|
   |                                          |
   |------ requestFileChunk (2) -------------→|
   |←----- chunk 2 ---------------------------|
   |                                          |
   |------ requestFileChunk (N) -------------→|
   |←----- chunk N ---------------------------|
   |                                          |
   [Ensamblar chunks en memoria]
   [Guardar en caché local]
   [Retornar bytes]
```

---

## Integración con la UI

### Ejemplo: Controlador de Chat

```java
public class ControladorChat {
    private final IGestionArchivos gestionArchivos;
    private final GestorAudio gestorAudio;
    
    public ControladorChat() {
        this.gestionArchivos = new GestionArchivosImpl();
        this.gestorAudio = new GestorAudio(gestionArchivos);
    }
    
    public void reproducirMensajeAudio(String fileId) {
        // Mostrar indicador de carga
        mostrarIndicadorCarga("Descargando audio...");
        
        gestorAudio.reproducirAudio(fileId)
            .thenRun(() -> {
                // Ocultar indicador
                ocultarIndicadorCarga();
                
                // Actualizar UI
                actualizarBotonReproduccion("pausar");
            })
            .exceptionally(ex -> {
                ocultarIndicadorCarga();
                mostrarError("Error al reproducir audio: " + ex.getMessage());
                return null;
            });
    }
    
    public void alternarReproduccion() {
        if (gestorAudio.estaReproduciendo()) {
            gestorAudio.pausar();
            actualizarBotonReproduccion("play");
        } else {
            gestorAudio.reanudar();
            actualizarBotonReproduccion("pausar");
        }
    }
    
    public void detenerAudio() {
        gestorAudio.detener();
        actualizarBotonReproduccion("play");
    }
}
```

---

## Manejo de Errores

### Errores Comunes

1. **Archivo no encontrado en servidor**
   ```
   ERROR: File not found with id: file123
   ```

2. **Error durante descarga de chunk**
   ```
   ERROR en chunk 3: Connection timeout
   ```

3. **Formato de audio no soportado**
   ```
   ERROR: Unsupported audio format: audio/mp4
   ```

4. **Error al reproducir**
   ```
   ERROR: Line unavailable - No audio device found
   ```

### Estrategias de Recuperación

```java
gestorAudio.reproducirAudio(fileId)
    .exceptionally(ex -> {
        if (ex.getMessage().contains("File not found")) {
            // Archivo no existe
            mostrarError("El archivo no está disponible");
        } else if (ex.getMessage().contains("Unsupported")) {
            // Formato no soportado
            mostrarError("Formato de audio no compatible");
        } else {
            // Error genérico
            mostrarError("Error al reproducir audio");
            // Reintentar o limpiar caché
            limpiarCacheArchivo(fileId);
        }
        return null;
    });
```

---

## Formatos de Audio Soportados

La API `javax.sound.sampled` soporta los siguientes formatos de forma nativa:

- ✅ **WAV** (audio/wav) - Recomendado
- ✅ **AIFF** (audio/aiff)
- ✅ **AU** (audio/basic)

Para otros formatos (MP3, OGG, etc.), se requieren bibliotecas adicionales como:
- JLayer (MP3)
- Vorbis SPI (OGG)
- Tritonus (varios formatos)

---

## Consideraciones de Rendimiento

### Tamaño de Chunks

- **Actual:** 256 bytes (definido en `CHUNK_SIZE`)
- **Ventajas:** Progreso granular, menor uso de memoria
- **Desventajas:** Mayor overhead de red

### Caché Local

- **Ventaja:** Acceso instantáneo en descargas subsecuentes
- **Desventaja:** Uso de espacio en disco
- **Recomendación:** Implementar limpieza periódica de caché

### Memoria

```java
// Archivo de 5 MB:
// - En memoria: 5 MB de bytes + 6.67 MB en Base64 = ~12 MB
// - En BD H2: 6.67 MB (solo Base64)
```

---


