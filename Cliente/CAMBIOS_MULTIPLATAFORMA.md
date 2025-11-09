# Cambios para Compatibilidad Multiplataforma (Linux/Windows)

## Problema Identificado
El método `obtenerRutaLocal` y la gestión de archivos tenían problemas en Linux porque:
1. Las carpetas `data/archivos` no se creaban antes de que la interfaz las necesitara
2. Los paths usaban separadores específicos de Windows (`\`)
3. No se validaban permisos de lectura/escritura en Linux

## Solución Implementada

### 1. FachadaArchivosImpl
**Archivo**: `/Negocio/Fachada/src/main/java/fachada/gestionArchivos/FachadaArchivosImpl.java`

**Cambios**:
- ✅ Agregado método `asegurarDirectoriosBase()` que se ejecuta en el constructor
- ✅ Crea todos los directorios necesarios ANTES de que la interfaz los necesite:
  - `data/`
  - `data/archivos/`
  - `data/archivos/user_photos/`
  - `data/archivos/audios/`
  - `data/archivos/audio/`
  - `data/archivos/images/`
  - `data/archivos/documents/`
  - `data/archivos/otros/`
- ✅ Usa `Files.createDirectories()` que es compatible con Linux y Windows
- ✅ No rompe la aplicación si falla, solo registra el error

**Beneficios**:
- La interfaz nunca encontrará carpetas faltantes
- Compatible con permisos de Linux
- Maneja errores gracefully

### 2. ArchivoServiceImpl
**Archivo**: `/Negocio/GestionArchivos/src/main/java/gestionArchivos/ArchivoServiceImpl.java`

**Cambios en `construirRutaArchivo()`**:
- ✅ Normaliza separadores: reemplaza `\` por `/`
- ✅ Previene path traversal: elimina `..` del path
- ✅ Usa `Path.resolve()` para construcción multiplataforma
- ✅ Usa `Path.normalize()` para limpiar el path
- ✅ Crea directorios automáticamente con `Files.createDirectories()`
- ✅ Manejo robusto de errores con fallback seguro

**Ejemplo**:
```java
// Windows
fileId: "user_photos\\deivid1.jpg"
→ Normalizado: "user_photos/deivid1.jpg"
→ Path creado: data/archivos/user_photos/deivid1.jpg

// Linux
fileId: "user_photos/deivid1.jpg"
→ Path creado: data/archivos/user_photos/deivid1.jpg
```

### 3. GestionArchivosImpl
**Archivo**: `/Negocio/GestionArchivos/src/main/java/gestionArchivos/GestionArchivosImpl.java`

**Cambios**:
- ✅ Antes de escribir archivos con `Files.write()`, verifica que el directorio padre exista
- ✅ Usa `Files.createDirectories(parentDir.toPath())` para crear directorios faltantes
- ✅ Compatible con permisos de Linux

**Código agregado**:
```java
File parentDir = archivoFisico.getParentFile();
if (parentDir != null && !parentDir.exists()) {
    Files.createDirectories(parentDir.toPath());
    System.out.println("[GestionArchivos] Directorio creado: " + parentDir.getAbsolutePath());
}
```

## Arquitectura Respetada ✅

La arquitectura se mantiene intacta:
```
Vista → Controlador → Servicio → Fachada → Gestores → Repositorio
```

- **Fachada**: Inicializa directorios al arrancar (punto de entrada)
- **Gestor (GestionArchivos)**: Defensivo al escribir archivos
- **Servicio (ArchivoService)**: Normaliza paths y construye rutas seguras
- **Repositorio**: No cambia, solo maneja BD

## Ventajas

### En Linux:
- ✅ Respeta permisos de archivos
- ✅ Usa `/` como separador nativo
- ✅ Crea directorios con permisos correctos
- ✅ Case-sensitive handling correcto

### En Windows:
- ✅ Normaliza `\` a `/` internamente
- ✅ Sigue funcionando como antes
- ✅ Compatible con paths largos

### General:
- ✅ Previene errores de "directorio no encontrado"
- ✅ Previene ataques de path traversal (`..`)
- ✅ Código más robusto y seguro
- ✅ Logging mejorado para debugging

## Testing Recomendado

1. **En Linux**:
   ```bash
   cd /home/deivid/Documents/Chat-Unillanos/Cliente
   ./iniciar-cliente.sh
   ```
   - Verificar que `data/archivos` y subdirectorios se creen correctamente
   - Probar carga de fotos de perfil
   - Probar descarga de archivos

2. **En Windows**:
   ```cmd
   cd C:\path\to\Cliente
   iniciar-cliente.bat
   ```
   - Verificar compatibilidad retroactiva
   - Probar mismas funcionalidades

## Archivos Modificados

1. `/Negocio/Fachada/src/main/java/fachada/gestionArchivos/FachadaArchivosImpl.java`
2. `/Negocio/GestionArchivos/src/main/java/gestionArchivos/ArchivoServiceImpl.java`
3. `/Negocio/GestionArchivos/src/main/java/gestionArchivos/GestionArchivosImpl.java`

## Compilación

✅ Proyecto compila sin errores:
```bash
mvn clean compile -DskipTests
```

---

**Fecha**: 2025-11-09
**Autor**: GitHub Copilot
**Issue**: Compatibilidad multiplataforma para gestión de archivos

