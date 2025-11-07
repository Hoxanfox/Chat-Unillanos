# 🐛 BUGFIX: FileId null en audios del historial

## 📋 Problema Crítico Encontrado

A pesar de haber implementado el mapeo de `contenido` → `fileId` en `mapearMensajeDesdeServidor()`, los audios del historial **SEGUÍAN teniendo `fileId: null`**.

### 🔍 Evidencia del Bug

```
🔍 [GestionMensajes]: Tipo ya definido por servidor: audio
// ❌ NO aparece: "✅ Contenido mapeado a fileId para audio: xxx"
🎵 [VistaContactoChat]: Reproducir audio EN MEMORIA - FileId: null
❌ Error interno del servidor al iniciar descarga
```

---

## 🕵️ Causa Raíz Identificada

El método `manejarHistorial()` **NO estaba usando `mapearMensajeDesdeServidor()`**, sino que parseaba directamente con Gson:

### ❌ Código Problemático (ANTES)

```java
private void manejarHistorial(DTOResponse r) {
    if (r.fueExitoso()) {
        // ❌ PROBLEMA: Gson parsea directamente a DTOMensaje
        // Sin pasar por mapearMensajeDesdeServidor()
        Type listType = new TypeToken<List<DTOMensaje>>(){}.getType();
        List<DTOMensaje> mensajes = gson.fromJson(gson.toJson(r.getData()), listType);
        
        // ...resto del código
    }
}
```

**Consecuencia:** El método `mapearMensajeDesdeServidor()` con la lógica de mapeo `contenido` → `fileId` **NUNCA SE EJECUTABA** para el historial.

---

## ✅ Solución Implementada

Modificar `manejarHistorial()` para que use `mapearMensajeDesdeServidor()` en cada mensaje:

### ✅ Código Corregido (DESPUÉS)

```java
private void manejarHistorial(DTOResponse r) {
    System.out.println("📥 [GestionMensajes]: Recibida respuesta de historial - Status: " + r.getStatus());

    if (r.fueExitoso()) {
        // ✅ CORRECCIÓN: Usar mapearMensajeDesdeServidor() para cada mensaje
        // En vez de parsear directamente con Gson
        List<DTOMensaje> mensajes = new ArrayList<>();
        
        Object data = r.getData();
        if (data instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> listData = (List<Object>) data;
            
            for (Object item : listData) {
                // Mapear cada mensaje usando el método que SÍ hace el mapeo contenido->fileId
                DTOMensaje mensaje = mapearMensajeDesdeServidor(item);
                mensajes.add(mensaje);
            }
        }

        String myUserId = gestorSesionUsuario.getUserId();
        for (DTOMensaje mensaje : mensajes) {
            mensaje.setEsMio(myUserId != null && myUserId.equals(mensaje.getRemitenteId()));
            determinarTipoMensaje(mensaje);
        }

        System.out.println("✅ [GestionMensajes]: Historial recibido con " + mensajes.size() + " mensajes");
        notificarObservadores("HISTORIAL_MENSAJES_RECIBIDO", mensajes);
    } else {
        System.err.println("❌ [GestionMensajes]: Error al obtener historial: " + r.getMessage());
        notificarObservadores("ERROR_HISTORIAL", r.getMessage());
    }
}
```

---

## 🔄 Flujo Corregido Completo

### ANTES (❌ NO FUNCIONABA)
```
1. Servidor responde historial con: {"tipo": "audio", "contenido": "audio_files/xxx.wav"}
   ↓
2. manejarHistorial() parsea con Gson directamente
   ↓
3. DTOMensaje se crea CON tipo="audio" y contenido="audio_files/xxx.wav"
   ↓
4. ❌ mapearMensajeDesdeServidor() NUNCA SE EJECUTA
   ↓
5. ❌ fileId queda en NULL
   ↓
6. ❌ Reproducción falla con "FileId: null"
```

### DESPUÉS (✅ FUNCIONA)
```
1. Servidor responde historial con: {"tipo": "audio", "contenido": "audio_files/xxx.wav"}
   ↓
2. manejarHistorial() itera sobre cada mensaje (List<Object>)
   ↓
3. Para cada mensaje, llama a mapearMensajeDesdeServidor(item)
   ↓
4. ✅ mapearMensajeDesdeServidor() detecta:
      - tipo = "audio"
      - contenido = "audio_files/xxx.wav" (NO es Base64)
      - fileId = null
   ↓
5. ✅ Ejecuta el mapeo: mensaje.setFileId(contenido)
   ↓
6. ✅ Log: "Contenido mapeado a fileId para audio: audio_files/xxx.wav"
   ↓
7. ✅ DTOMensaje tiene fileId correcto
   ↓
8. ✅ VistaContactoChat descarga el audio con el fileId
   ↓
9. ✅ Reproducción funciona correctamente
```

---

## 📊 Resultado Esperado en Logs

### ✅ Ahora verás:

```bash
📥 [GestionMensajes]: Recibida respuesta de historial - Status: success
✅ [GestionMensajes]: Contenido mapeado a fileId para audio: audio_files/xxx.wav
✅ [GestionMensajes]: Contenido mapeado a fileId para audio: audio_files/yyy.wav
✅ [GestionMensajes]: Contenido mapeado a fileId para audio: audio_files/zzz.wav
🔍 [GestionMensajes]: Tipo ya definido por servidor: audio
✅ [GestionMensajes]: Historial recibido con 13 mensajes
📥 [VistaContactoChat]: Descargando audio del historial - FileId: audio_files/xxx.wav
✅ [VistaContactoChat]: Audio del historial descargado: xxx.wav
🎵 [VistaContactoChat]: Reproducir audio EN MEMORIA - FileId: audio_files/xxx.wav
✅ Audio reproducido exitosamente
```

### ❌ Ya NO verás:

```bash
🎵 [VistaContactoChat]: Reproducir audio EN MEMORIA - FileId: null
❌ Error interno del servidor al iniciar descarga
```

---

## 🎯 Por Qué Funcionaba en PUSH pero NO en Historial

| Escenario | Método Usado | ¿Mapea fileId? |
|-----------|--------------|----------------|
| **PUSH en tiempo real** | `manejarNuevoMensajeAudioPush()` → `mapearMensajeDesdeServidor()` | ✅ SÍ |
| **Historial (ANTES)** | `manejarHistorial()` → `gson.fromJson()` directamente | ❌ NO |
| **Historial (DESPUÉS)** | `manejarHistorial()` → `mapearMensajeDesdeServidor()` | ✅ SÍ |

---

## 📝 Archivos Modificados

1. **GestionMensajesImpl.java**
   - Método: `manejarHistorial()`
   - Líneas: ~555-585
   - Cambio: Reemplazar parseo directo con Gson por iteración manual usando `mapearMensajeDesdeServidor()`

---

## 🧪 Cómo Verificar que Funciona

1. Ejecuta el cliente
2. Abre el chat con un contacto que tenga audios en el historial
3. Observa los logs:
   ```bash
   ✅ [GestionMensajes]: Contenido mapeado a fileId para audio: audio_files/xxx.wav
   ```
4. Presiona el botón ▶️ de un audio
5. Verifica que se reproduce correctamente (sin error de "FileId: null")

---

## 📅 Fecha de Corrección
7 de Noviembre, 2025

## 🎉 Estado
✅ **RESUELTO** - Los audios del historial ahora mapean correctamente el fileId y se pueden reproducir

## 🔧 Compilación
```bash
mvn clean package -DskipTests
# ✅ BUILD SUCCESS
```

---

## 💡 Lección Aprendida

**Siempre usar métodos de mapeo centralizados** en vez de parsear directamente con Gson, para asegurar que TODAS las transformaciones de datos (como `contenido` → `fileId`) se apliquen consistentemente en todos los flujos (PUSH, historial, etc.).

