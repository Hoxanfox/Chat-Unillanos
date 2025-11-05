# ArchivoService - Servicio de Gestión de Archivos

## 📋 Descripción

`ArchivoService` es un componente especializado para obtener archivos del servidor usando solo su identificador (`fileId`). Maneja automáticamente la verificación local, descarga desde el servidor y gestión de caché.

## 🎯 Características

- ✅ **Verificación local automática**: Antes de descargar, verifica si el archivo ya existe localmente
- ✅ **Descarga inteligente**: Solo descarga si es necesario
- ✅ **Gestión de directorios**: Crea automáticamente directorios si no existen
- ✅ **Caché en BD**: Guarda metadatos (NO binarios completos) en la base de datos local
- ✅ **Regeneración desde BD**: Si el archivo se eliminó del disco pero está en BD, lo regenera
- ✅ **Manejo de errores robusto**: Gestiona permisos, errores de red y timeouts
- ✅ **Asíncrono**: Usa `CompletableFuture` para operaciones no bloqueantes

## 🏗️ Arquitectura

```
ArchivoService (Coordinador)
    ↓
    ├── IRepositorioArchivo (Metadatos/Caché)
    │   └── Guarda solo: fileId, nombre, estado, tamaño, mime, hash
    │
    └── IGestionArchivos (Descarga real)
        └── Maneja chunks, protocolo servidor, ensamblado
```

**Importante**: NO almacena binarios completos en BD, solo metadatos y enlaces.

## 📦 Componentes Creados

### 1. `IArchivoService.java`
Interfaz del servicio con los siguientes métodos:

```java
// Obtener archivo con directorio específico
CompletableFuture<File> obtenerPorFileId(String fileId, File directorioDestino);

// Obtener archivo con directorio automático
CompletableFuture<File> obtenerPorFileId(String fileId);

// Verificar si existe localmente
CompletableFuture<Boolean> existeLocalmente(String fileId);

// Obtener ruta local sin descargar
CompletableFuture<File> obtenerRutaLocal(String fileId);
```

### 2. `ArchivoServiceImpl.java`
Implementación completa con:
- Validación de parámetros
- Creación automática de directorios
- Lógica de verificación y descarga
- Regeneración desde BD si es necesario
- Detección automática de tipo de archivo

### 3. `EjemploUsoArchivoService.java`
Ejemplos de uso para diferentes casos

## 🚀 Uso Rápido

### Inicialización

```java
// Crear dependencias
IRepositorioArchivo repositorioArchivo = new RepositorioArchivoImpl();
IGestionArchivos gestionArchivos = new GestionArchivosImpl();

// Crear servicio
IArchivoService archivoService = new ArchivoServiceImpl(
    repositorioArchivo,
    gestionArchivos,
    new File("data/archivos") // Directorio raíz
);
```

### Caso de Uso: Descargar Foto de Perfil después de Autenticación

```java
// En AutenticarUsuario, después de recibir la respuesta del servidor:
String fileId = "user_photos/deivid1.jpg"; // Del JSON de respuesta

archivoService.obtenerPorFileId(fileId)
    .thenAccept(fotoPerfil -> {
        System.out.println("✅ Foto lista: " + fotoPerfil.getAbsolutePath());
        // Usar en la UI, guardar en Usuario, etc.
    })
    .exceptionally(ex -> {
        System.err.println("❌ Error: " + ex.getMessage());
        // Usar foto por defecto
        return null;
    });
```

## 🔄 Flujo de Operación

```
┌─────────────────────────────────────────┐
│ obtenerPorFileId("user_photos/user.jpg")│
└───────────────┬─────────────────────────┘
                │
                ↓
    ┌───────────────────────┐
    │ ¿Existe en repositorio?│
    └───────┬───────────────┘
            │
      ┌─────┴─────┐
      │           │
     SÍ          NO
      │           │
      ↓           ↓
┌─────────────┐  ┌──────────────────┐
│¿En disco?   │  │Descargar servidor│
└─┬───────────┘  └────────┬─────────┘
  │                       │
  ├─SÍ→ Retornar          ↓
  │                   ┌─────────┐
  ↓                   │Guardar  │
┌──────────────┐      │metadata │
│¿En BD Base64?│      │en BD    │
└─┬────────────┘      └────┬────┘
  │                        │
  ├─SÍ→ Regenerar          ↓
  │                   Retornar
  ↓
Descargar servidor
```

## 📝 Integración con tu Código Existente

### En `AutenticarUsuario.java`

Después de recibir la respuesta de autenticación, agrega:

```java
// Inicializar servicio (hacer una vez, puede ser en constructor)
private final IArchivoService archivoService;

public AutenticarUsuario(...) {
    // ... código existente ...
    this.archivoService = new ArchivoServiceImpl(
        new RepositorioArchivoImpl(),
        new GestionArchivosImpl()
    );
}

// En el método que procesa la respuesta de autenticación:
private void procesarRespuestaAutenticacion(DTOResponse respuesta) {
    // ... código existente para crear usuario ...
    
    String fileId = datosUsuario.get("fileId").getAsString();
    
    // Descargar foto de perfil automáticamente
    archivoService.obtenerPorFileId(fileId)
        .thenAccept(fotoPerfil -> {
            // Guardar ruta en el usuario
            usuario.setRutaFotoPerfil(fotoPerfil.getAbsolutePath());
            
            // Notificar a observadores con foto incluida
            notificarObservadores("AUTENTICACION_EXITOSA", usuario);
        })
        .exceptionally(ex -> {
            System.err.println("⚠️ No se pudo descargar foto, usando por defecto");
            // Continuar sin foto
            notificarObservadores("AUTENTICACION_EXITOSA", usuario);
            return null;
        });
}
```

## 📂 Estructura de Directorios Generados

```
data/
└── archivos/
    ├── user_photos/     # Fotos de perfil
    ├── audio/           # Archivos de audio
    ├── images/          # Otras imágenes
    ├── documents/       # Documentos
    └── otros/           # Otros tipos
```

La estructura se crea automáticamente según el tipo de archivo detectado en el `fileId`.

## ⚠️ Consideraciones Importantes

1. **No almacena binarios completos en BD**: Solo metadatos para evitar saturar H2
2. **Thread-safe**: Usa operaciones asíncronas con `CompletableFuture`
3. **Reutiliza código**: Delega la descarga real a `GestionArchivosImpl` existente
4. **Manejo de errores**: Siempre usa `.exceptionally()` en tus llamadas
5. **Permisos**: Asegura que el proceso tenga permisos de escritura en `data/archivos/`

## 🧪 Testing

Para probar el servicio:

```bash
# Compilar el proyecto
cd /home/deivid/Documents/Chat-Unillanos/Cliente
mvn clean compile

# Ejecutar ejemplo
mvn exec:java -pl Negocio/GestionArchivos \
  -Dexec.mainClass="gestionArchivos.EjemploUsoArchivoService"
```

## 📊 Logs

El servicio genera logs detallados:

```
[ArchivoService] Servicio inicializado con directorio raíz: data/archivos
[ArchivoService] Solicitando archivo con fileId: user_photos/deivid1.jpg
[ArchivoService] El archivo existe en el repositorio, verificando en disco...
[ArchivoService] ✅ Archivo encontrado en disco: data/archivos/user_photos/deivid1.jpg
[ArchivoService] ✅ Archivo obtenido exitosamente: data/archivos/user_photos/deivid1.jpg
```

## 🔧 Troubleshooting

| Problema | Causa | Solución |
|----------|-------|----------|
| "No se pudo crear directorio" | Permisos insuficientes | Ejecutar con permisos o cambiar ruta |
| "Archivo en BD pero no en disco" | Archivo eliminado manualmente | Se regenera automáticamente desde BD |
| "Error al descargar" | Servidor no responde | El error se propaga, usar foto por defecto |
| UUID inválido en respuesta | Servidor envía ID numérico | Ya está resuelto en commits anteriores |

## 🎓 Ejemplos Adicionales

Ver `EjemploUsoArchivoService.java` para casos de uso completos incluyendo:
- Descarga con directorio específico
- Descarga con directorio automático
- Verificación de existencia
- Integración con autenticación
- Verificación de múltiples archivos en paralelo

---

**Creado**: 5 de noviembre de 2025  
**Módulo**: `Negocio/GestionArchivos`  
**Dependencias**: `Persistencia/Repositorio`, `Persistencia/Dominio`, `Infraestructura/DTO`

