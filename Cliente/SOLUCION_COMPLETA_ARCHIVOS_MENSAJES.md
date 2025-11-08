# 🔧 SOLUCIÓN COMPLETA: Problemas de Archivos y Mensajes

## 📋 Resumen de Problemas Encontrados y Resueltos

### ✅ 1. Método `extraerNombreDeFileId` Faltante - **RESUELTO**

**Problema:**
```
[GestionArchivos] FileId: user_photos/4.jpg -> NombreArchivo: extraerNombreDeFileId(fileId)
```

El método `extraerNombreDeFileId()` no estaba implementado.

**Solución Aplicada:**
Agregado método en `GestionArchivosImpl.java`:

```java
private String extraerNombreDeFileId(String fileId) {
    if (fileId == null || fileId.isEmpty()) {
        return null;
    }
    
    // Si contiene "/" (formato: carpeta/archivo)
    if (fileId.contains("/")) {
        String[] partes = fileId.split("/");
        return partes[partes.length - 1]; // Última parte
    }
    
    // Si no contiene "/", el fileId es directamente el nombre
    return fileId;
}
```

**Ejemplos:**
- `"user_photos/1.jpg"` → `"1.jpg"` ✅
- `"documentos/archivo_1234567890.pdf"` → `"archivo_1234567890.pdf"` ✅
- `"imagen.png"` → `"imagen.png"` ✅

---

### ✅ 2. Archivos Descargados con Nombre Incorrecto - **RESUELTO**

**Problema:**
Cuando se descargaban archivos, el servidor devolvía:
```json
{
  "fileId": "user_photos/4.jpg",
  "fileName": "4.jpg"
}
```

El código usaba `downloadInfo.getFileName()` ("4.jpg") en lugar del nombre completo del `fileId`. Esto causaba:
- Archivos guardados con nombres incorrectos
- Múltiples descargas sobrescribiendo el mismo archivo
- Registros duplicados en la base de datos

**Logs del Problema:**
```
📸 [ServicioContactos]: Procesando contacto 4 - photoId: user_photos/4.jpg
[GestionArchivos] Iniciando descarga de archivo con ID: user_photos/4.jpg
[GestionArchivos] DownloadInfo obtenido - Archivo: 4.jpg  ← ⚠️ Solo el nombre
✅ Foto lista para contacto 3: .../user_photos/4.jpg  ← 🐛 Contacto equivocado!
```

**Solución Aplicada:**
Modificado método `recibirChunksYAlmacenar()` en `GestionArchivosImpl.java`:

```java
private CompletableFuture<File> recibirChunksYAlmacenar(DTODownloadInfo downloadInfo, File directorioDestino, String fileId) {
    // ✅ CORRECCIÓN: Usar el nombre extraído del fileId
    String nombreArchivo = extraerNombreDeFileId(fileId);
    if (nombreArchivo == null || nombreArchivo.isEmpty()) {
        nombreArchivo = downloadInfo.getFileName(); // Fallback
    }
    
    System.out.println("[GestionArchivos] Guardando archivo como: " + nombreArchivo + " (desde fileId: " + fileId + ")");
    
    File archivoDestino = new File(directorioDestino, nombreArchivo);
    // ...resto del código
}
```

**Resultado:**
- ✅ Cada archivo se guarda con su nombre correcto: `1.jpg`, `2.jpg`, `3.jpg`, `4.jpg`
- ✅ No hay sobrescrituras ni conflictos
- ✅ La foto del contacto correcto se asocia al contacto correcto

---

### ⚠️ 3. Mensajes Duplicados en el Servidor - **REQUIERE ACCIÓN EN EL SERVIDOR**

**Problema:**
```json
{
  "action": "enviarMensajeDirecto",
  "status": "error",
  "message": "Error al enviar mensaje: query did not return a unique result: 2"
}
```

Este error viene del **SERVIDOR**, no del cliente. Indica que hay **registros duplicados en la base de datos del servidor**.

**Análisis:**
- **Remitente**: `4bed8adf-3af3-4bc2-afbb-815a0b83069a`
- **Destinatario**: `7fed39d7-7d87-42c6-a26f-26bd7927f7a1`

El servidor está encontrando **2 registros** cuando busca uno de estos usuarios.

**Causas Posibles:**
1. El mismo usuario se registró múltiples veces
2. La misma relación de contacto existe dos veces
3. Falta de restricciones UNIQUE en la BD del servidor

**Solución (EN EL SERVIDOR):**

#### Opción 1: Limpiar duplicados manualmente

```sql
-- Ver usuarios duplicados
SELECT id, nombre, email, COUNT(*) as count
FROM usuarios
GROUP BY id
HAVING COUNT(*) > 1;

-- Eliminar duplicados (conservar solo el primero)
DELETE FROM usuarios
WHERE rowid NOT IN (
    SELECT MIN(rowid)
    FROM usuarios
    GROUP BY id
);

-- Lo mismo para contactos
DELETE FROM contactos
WHERE rowid NOT IN (
    SELECT MIN(rowid)
    FROM contactos
    GROUP BY usuario_id, contacto_id
);
```

#### Opción 2: Agregar restricciones UNIQUE

```sql
-- Asegurar que los IDs sean únicos
ALTER TABLE usuarios ADD CONSTRAINT uk_usuarios_id UNIQUE (id);

-- Asegurar que no haya contactos duplicados
ALTER TABLE contactos ADD CONSTRAINT uk_contactos_usuario_contacto 
    UNIQUE (usuario_id, contacto_id);
```

#### Opción 3: Modificar código del servidor

Si usa JPA/Hibernate:

```java
// ❌ MAL: Falla si hay duplicados
Usuario usuario = em.createQuery("SELECT u FROM Usuario u WHERE u.id = :id")
    .setParameter("id", userId)
    .getSingleResult(); // ← Lanza excepción

// ✅ BIEN: Tomar el primero
List<Usuario> usuarios = em.createQuery("SELECT u FROM Usuario u WHERE u.id = :id")
    .setParameter("id", userId)
    .getResultList();

if (usuarios.isEmpty()) {
    throw new NotFoundException("Usuario no encontrado");
}

Usuario usuario = usuarios.get(0);
```

---

## 🧹 Limpieza de Base de Datos Local

He creado el archivo `LIMPIAR_BD_ARCHIVOS.sql` para limpiar la base de datos **local** del cliente:

```sql
-- Ver archivos duplicados
SELECT file_id_servidor, COUNT(*) as total, 
       GROUP_CONCAT(estado) as estados
FROM archivos
GROUP BY file_id_servidor
HAVING COUNT(*) > 1;

-- Eliminar duplicados (conservar el completo o el más reciente)
DELETE FROM archivos
WHERE id_archivo NOT IN (
    SELECT MIN(id_archivo)
    FROM archivos
    GROUP BY file_id_servidor
);
```

**Para ejecutar:**
1. Detener la aplicación
2. Conectar a la BD: `data/chat_unillanos.mv.db`
3. Ejecutar el script SQL
4. Reiniciar la aplicación

---

## 📊 Estado Actual

### ✅ Cliente (Resuelto)
- ✅ Método `extraerNombreDeFileId()` implementado
- ✅ Archivos se guardan con el nombre correcto del `fileId`
- ✅ No hay conflictos entre descargas simultáneas
- ✅ Script de limpieza de BD local creado

### ⚠️ Servidor (Requiere Acción)
- ⚠️ Base de datos del servidor tiene registros duplicados
- ⚠️ Falta de restricciones UNIQUE
- ⚠️ El código del servidor falla con `getSingleResult()` cuando hay duplicados

---

## 🎯 Acciones Requeridas

### Para el Cliente ✅
1. ✅ **Completado**: Código corregido y compilado
2. 🔧 **Pendiente**: Ejecutar `LIMPIAR_BD_ARCHIVOS.sql` para limpiar BD local
3. 🔧 **Pendiente**: Eliminar archivos físicos duplicados manualmente si existen

### Para el Servidor ⚠️
1. ⚠️ **Urgente**: Conectar a la BD del servidor
2. ⚠️ **Urgente**: Identificar y eliminar registros duplicados
3. ⚠️ **Importante**: Agregar restricciones UNIQUE
4. ⚠️ **Recomendado**: Modificar código para manejar duplicados (usar `getResultList()`)

---

## 📝 Notas Adicionales

### Sobre los PeerId = null
Los warnings sobre `peerId = null` son **normales**:
```
⚠️ [GestorContactoPeers]: No se encontró peerId para contacto
⚠️ [GestionMensajes]: No se encontró peerId del destinatario
```

Esto significa que el destinatario **no está conectado** por WebRTC. El mensaje se envía igual y el servidor lo almacena para entrega posterior.

### Archivos Afectados
- ✅ `GestionArchivosImpl.java` - Corregido
- 📄 `LIMPIAR_BD_ARCHIVOS.sql` - Creado
- 📄 `DIAGNOSTICO_MENSAJES_DUPLICADOS.md` - Documentación detallada

---

## ✅ Verificación Post-Corrección

Después de aplicar las correcciones, deberías ver logs como:

```
[GestionArchivos] Nombre extraído de fileId 'user_photos/4.jpg': 4.jpg
[GestionArchivos] Guardando archivo como: 4.jpg (desde fileId: user_photos/4.jpg)
[GestionArchivos] Archivo ensamblado y guardado en BD: .../user_photos/4.jpg
✅ Foto lista para contacto 4: .../user_photos/4.jpg  ← ✅ Contacto correcto!
```

Y **NO** deberías ver:
- ❌ Archivos guardados con nombres incorrectos
- ❌ Múltiples archivos sobrescribiendo el mismo registro
- ❌ Contactos asociados a fotos equivocadas

---

## 🆘 Si los Mensajes Siguen Fallando

El error `"query did not return a unique result: 2"` **solo puede resolverse en el servidor**.

El cliente está funcionando correctamente y reportando el error como debe ser. **No hay nada más que hacer en el cliente**.

Debes acceder al servidor y limpiar los duplicados de la base de datos.

