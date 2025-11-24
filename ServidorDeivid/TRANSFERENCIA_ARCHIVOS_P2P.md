# 📦 Transferencia de Archivos Físicos P2P - Paso 2

## 🎯 Objetivo
Implementar la descarga automática de archivos físicos del `Bucket/` después de sincronizar los metadatos entre peers.

## 🏗️ Arquitectura

### Flujo de Sincronización Completa

```
┌─────────────────────────────────────────────────────────────────┐
│  PASO 1: Sincronización de Metadatos (Ya existente)            │
│  - Sincroniza tablas: USUARIO, CANAL, MIEMBRO, MENSAJE, ARCHIVO│
│  - Usa Merkle Trees para detectar diferencias                   │
│  - Guarda metadatos en BD                                       │
└──────────────────┬──────────────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────────────┐
│  PASO 2: Transferencia de Archivos Físicos (✅ NUEVO)          │
│  - Detecta archivos faltantes en Bucket/                       │
│  - Descarga archivos por chunks de peers                        │
│  - Verifica integridad con hash SHA-256                         │
│  - Guarda en Bucket/ local                                      │
└─────────────────────────────────────────────────────────────────┘
```

## 📋 Componentes Implementados

### 1. **ServicioTransferenciaArchivos.java**
- **Ubicación**: `Negocio/GestorP2P/src/main/java/gestorP2P/servicios/`
- **Responsabilidades**:
  - Detectar archivos faltantes comparando metadatos en BD con archivos físicos en `Bucket/`
  - Gestionar sesiones de descarga por chunks
  - Verificar integridad de archivos con hash SHA-256
  - Guardar archivos en la estructura correcta de `Bucket/`

#### Rutas P2P Registradas

| Ruta | Tipo | Descripción |
|------|------|-------------|
| `p2p_file_metadata_request` | Acción | Solicita metadatos de un archivo |
| `p2p_file_chunk_request` | Acción | Solicita un chunk específico de un archivo |
| `p2p_file_metadata_request` | Respuesta | Recibe metadatos y activa descarga |
| `p2p_file_chunk_request` | Respuesta | Recibe chunk y ensambla archivo |

### 2. **Integración con ServicioSincronizacionDatos**
- Se agregó método `setServicioTransferenciaArchivos()` para inyección de dependencias
- Se agregó lógica en `procesarDiferenciasEnOrden()` para activar descarga automática después de sincronizar metadatos
- El servicio se activa solo cuando todos los metadatos están sincronizados

### 3. **Integración con ServicioP2P**
- Se registra `ServicioTransferenciaArchivos` en el sistema P2P
- Se inyecta en `ServicioSincronizacionDatos` para conexión automática
- Se inicializa con `IGestorConexiones` y `IRouterMensajes`

## 🔄 Flujo de Ejecución

### Cuando se sincroniza un peer:

```
1. ServicioSincronizacionDatos detecta peer conectado
   ↓
2. Sincroniza metadatos usando Merkle Trees
   ↓
3. Guarda metadatos de archivos en BD (tabla ARCHIVO)
   ↓
4. Al finalizar sincronización, verifica archivos faltantes
   ↓
5. ServicioTransferenciaArchivos.verificarYDescargarArchivosFaltantes()
   ↓
6. Compara metadatos en BD con archivos en Bucket/
   ↓
7. Para cada archivo faltante:
   a. Solicita metadatos al peer (p2p_file_metadata_request)
   b. Recibe metadatos (nombre, tamaño, hash, totalChunks)
   c. Solicita chunks secuencialmente (p2p_file_chunk_request)
   d. Ensambla archivo completo
   e. Verifica hash SHA-256
   f. Guarda en Bucket/ con estructura correcta
```

## 📁 Estructura de Archivos

```
Bucket/
├── user_photos/       # Fotos de perfil
├── images/            # Imágenes generales
├── audio/             # Archivos de audio
├── documents/         # Documentos (PDF, Word, etc.)
└── otros/             # Otros tipos de archivos
```

## 🔒 Seguridad y Validación

1. **Verificación de Hash**: Cada archivo descargado se verifica con hash SHA-256
2. **Chunks**: Los archivos se transfieren en bloques de 512 KB para eficiencia
3. **Sesiones de Descarga**: Sistema de cache para evitar descargas duplicadas
4. **Timeout**: Descargas se limpian automáticamente si no se completan

## 💾 Persistencia

### Base de Datos (Metadatos)
```java
Archivo {
  UUID id;
  String fileId;           // ID único (ej: "images/abc123_foto.jpg")
  String nombreArchivo;    // Nombre original
  String rutaRelativa;     // Ruta desde Bucket/
  String mimeType;
  long tamanio;
  String hashSHA256;       // ✅ Para verificación de integridad
  Instant fechaCreacion;
  Instant fechaUltimaActualizacion;
}
```

### Sistema de Archivos (Archivos físicos)
- Ubicación: `./Bucket/{categoria}/{uuid}_{nombre}`
- Ejemplo: `./Bucket/images/abc-123-def_foto.jpg`

## 🚀 Ventajas del Sistema

1. **Sincronización Automática**: No requiere intervención manual
2. **Eficiencia**: Solo descarga archivos faltantes
3. **Integridad**: Verificación de hash garantiza archivos íntegros
4. **Escalabilidad**: Descargas en paralelo (máximo 3 simultáneas)
5. **Tolerancia a Fallos**: Reintentos automáticos si un peer no responde

## 🔧 Configuración

### Constantes Importantes
```java
CHUNK_SIZE = 524288;           // 512 KB por chunk
BUCKET_PATH = "./Bucket/";     // Ruta del bucket
MAX_DESCARGAS_PARALELAS = 3;   // Descargas simultáneas
```

## 📝 Logs y Monitoreo

El sistema genera logs detallados con colores ANSI:

- 🔵 **AZUL**: Información general
- 🟢 **VERDE**: Operaciones exitosas
- 🟡 **AMARILLO**: Advertencias
- 🔴 **ROJO**: Errores
- 🔷 **CYAN**: Detalles de descarga

### Ejemplo de Logs
```
[TransferenciaArchivos] 🔍 Verificando archivos faltantes en Bucket/...
[TransferenciaArchivos] ⚠ Archivo faltante: images/abc123_foto.jpg
[TransferenciaArchivos] 📥 Descargando foto.jpg (5 chunks)
[TransferenciaArchivos] ✓ Chunk 1/5 recibido
[TransferenciaArchivos] ✓ Chunk 2/5 recibido
...
[TransferenciaArchivos] ✅ Archivo descargado y guardado: foto.jpg
```

## 🧪 Pruebas

### Escenario 1: Peer nuevo se conecta
1. Peer A tiene archivos físicos en Bucket/
2. Peer B se conecta sin archivos
3. Resultado: Peer B descarga automáticamente todos los archivos faltantes

### Escenario 2: Archivo nuevo subido
1. Usuario sube archivo en Peer A
2. ServicioArchivos guarda archivo y metadatos
3. ServicioArchivos activa sincronización P2P
4. Peer B recibe metadatos
5. ServicioTransferenciaArchivos descarga archivo físico
6. Resultado: Archivo disponible en ambos peers

### Escenario 3: Verificación de integridad
1. Archivo se descarga con un chunk corrupto
2. Hash SHA-256 no coincide
3. Descarga se marca como fallida
4. Sistema puede reintentar (si se implementa lógica de reintentos)

## 🔮 Mejoras Futuras

1. **Reintentos automáticos**: Si descarga falla, reintentar con otro peer
2. **Priorización**: Descargar primero archivos más pequeños
3. **Compresión**: Comprimir chunks antes de transferir
4. **Progress tracking**: Reportar progreso de descarga a clientes
5. **Eliminación de archivos**: Sincronizar eliminación de archivos entre peers
6. **Deduplicación**: Evitar almacenar archivos duplicados usando hash

## ✅ Estado de Implementación

- ✅ Detección de archivos faltantes
- ✅ Descarga por chunks P2P
- ✅ Verificación de integridad (SHA-256)
- ✅ Guardado en estructura correcta
- ✅ Integración con sincronización de metadatos
- ✅ Logs detallados
- ⏳ Reintentos automáticos (pendiente)
- ⏳ Progress tracking (pendiente)
- ⏳ Compresión de chunks (pendiente)

## 📚 Referencias

- **ServicioArchivos.java**: Gestión de archivos Cliente-Servidor
- **ServicioSincronizacionDatos.java**: Sincronización de metadatos P2P
- **ArchivoRepositorio.java**: Acceso a datos de archivos
- **INTEGRACION_ARCHIVOS_P2P.md**: Documentación de la integración

---

**Implementado por**: Sistema de sincronización P2P
**Fecha**: 2025-01-24
**Versión**: 1.0

