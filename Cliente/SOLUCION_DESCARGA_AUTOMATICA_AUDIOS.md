# 🎵 Solución: Descarga Automática de Audios del Historial

## 📋 Problema Identificado

Los audios de los mensajes en el historial **NO se estaban descargando automáticamente**, causando que al intentar reproducirlos se enviara `fileId: null` al servidor, generando errores:

```
🎵 [VistaContactoChat]: Reproducir audio EN MEMORIA - FileId: null
❌ Error interno del servidor al iniciar descarga
```

### Causa Raíz

1. **El campo `audioId` del servidor NO se mapeaba al `fileId` del DTOMensaje**
2. **⚠️ CRÍTICO: El servidor NO envía campo `audioId` en el historial**, solo envía:
   ```json
   {"tipo": "audio", "contenido": "audio_files/xxx.wav"}
   ```
3. **La vista intentaba usar `contenido` en vez de `fileId` para reproducir audios**
4. **No había pre-descarga automática de audios del historial** (similar a las fotos de perfil)

---

## ✅ Solución Implementada

### 1️⃣ **Mapeo del campo `audioId` y contenido en GestionMensajesImpl.java**

#### Archivo: `Negocio/GestionContactos/src/main/java/gestionContactos/mensajes/GestionMensajesImpl.java`

**Cambio realizado en `mapearMensajeDesdeServidor()`:**

```java
// Mapear tipo y contenido primero
String tipo = null;
if (map.containsKey("tipo")) {
    tipo = (String) map.get("tipo");
    mensaje.setTipo(tipo.toUpperCase());
}

String contenido = null;
if (map.containsKey("contenido")) {
    contenido = (String) map.get("contenido");
    mensaje.setContenido(contenido);
}

// ✅ NUEVO: Mapear audioId del servidor al fileId del cliente
if (map.containsKey("audioId")) {
    String audioId = (String) map.get("audioId");
    mensaje.setFileId(audioId);
    System.out.println("✅ [GestionMensajes]: AudioId mapeado a fileId: " + audioId);
}

// ✅ CORRECCIÓN CRÍTICA: Si el tipo es "audio" pero no hay audioId,
// usar el contenido como fileId (el servidor envía el path en contenido)
if (tipo != null && tipo.equalsIgnoreCase("audio") && 
    mensaje.getFileId() == null && contenido != null && !contenido.isEmpty()) {
    
    // Solo si el contenido NO es Base64 (los Base64 son muy largos)
    boolean esBase64 = contenido.startsWith("UklGR") || 
                      contenido.startsWith("data:audio/") || 
                      contenido.length() > 1000;
    
    if (!esBase64) {
        mensaje.setFileId(contenido);
        System.out.println("✅ [GestionMensajes]: Contenido mapeado a fileId para audio: " + contenido);
    }
}
```

**Beneficio:**
- **Maneja ambos casos**: Con `audioId` explícito O con path en `contenido`
- **Detecta Base64**: No confunde Base64 largo con un fileId
- **Funciona con la API actual del servidor**

---

### 2️⃣ **Pre-descarga automática en VistaContactoChat.java**

#### Archivo: `Presentacion/InterfazEscritorio/src/main/java/interfazEscritorio/dashboard/featureContactos/chatContacto/VistaContactoChat.java`

**Cambio realizado en el caso `HISTORIAL_MENSAJES_RECIBIDO`:**

```java
case "HISTORIAL_MENSAJES_RECIBIDO":
case "HISTORIAL_MENSAJES":
    if (datos instanceof List) {
        List<?> lista = (List<?>) datos;
        Platform.runLater(() -> {
            mensajesBox.getChildren().clear();
            mensajesMostrados.clear();

            for (Object obj : lista) {
                if (obj instanceof DTOMensaje) {
                    DTOMensaje mensaje = (DTOMensaje) obj;
                    agregarMensaje(mensaje);

                    // ✅ CORRECCIÓN: Si es audio, descargar usando el FILEID (no el contenido)
                    if (mensaje.esAudio() && mensaje.getFileId() != null && !mensaje.getFileId().isEmpty()) {
                        String fileId = mensaje.getFileId();
                        System.out.println("📥 [VistaContactoChat]: Descargando audio del historial - FileId: " + fileId);
                        controlador.descargarAudioALocal(fileId)
                                .thenAccept(archivo -> {
                                    if (archivo != null) {
                                        System.out.println("✅ [VistaContactoChat]: Audio del historial descargado: " + archivo.getName());
                                    }
                                });
                    }
                }
            }
        });
    }
    break;
```

---

### 3️⃣ **Uso correcto del fileId en el botón de reproducción**

**Ya estaba correcto en `crearBurbujaAudio()`:**

```java
btnPlay.setOnAction(e -> {
    System.out.println("🎵 [VistaContactoChat]: Reproducir audio EN MEMORIA - FileId: " + mensaje.getFileId());
    
    controlador.reproducirAudioEnMemoria(mensaje.getFileId())
        .thenRun(() -> {
            Platform.runLater(() -> btnPlay.setText("✅"));
        });
});
```

---

## 🔄 Flujo Completo Corregido

```
1. Cliente solicita historial de mensajes
   ↓
2. Servidor responde con lista de mensajes
   → Mensajes de audio: {"tipo": "audio", "contenido": "audio_files/xxx.wav"}
   ↓
3. GestionMensajesImpl recibe el JSON
   ↓
4. mapearMensajeDesdeServidor() detecta:
   ✓ tipo = "audio"
   ✓ contenido = "audio_files/xxx.wav" (path, no Base64)
   ✓ audioId = null
   → Mapea contenido → fileId
   ↓
5. VistaContactoChat recibe el historial con fileId correcto
   ↓
6. Para cada mensaje de audio:
   → Se agrega la burbuja a la vista
   → Se inicia descarga automática: descargarAudioALocal(fileId)
   ↓
7. Audio descargado a: data/archivos/audios/xxx.wav
   ↓
8. Usuario presiona botón ▶️
   → reproducirAudioEnMemoria(fileId) // Ya no es null
   → Audio ya está en caché local
   ↓
9. Reproducción exitosa ✅
```

---

## 📊 Comparación Antes/Después

### ❌ **ANTES:**
```
🔍 [GestionMensajes]: Tipo ya definido por servidor: audio
// ❌ NO SE MAPEABA EL FILEID
📥 [VistaContactoChat]: Historial cargado
🎵 [VistaContactoChat]: Reproducir audio EN MEMORIA - FileId: null
❌ Error interno del servidor al iniciar descarga
```

### ✅ **DESPUÉS:**
```
🔍 [GestionMensajes]: Tipo ya definido por servidor: audio
✅ [GestionMensajes]: Contenido mapeado a fileId para audio: audio_files/xxx.wav
📥 [VistaContactoChat]: Descargando audio del historial - FileId: audio_files/xxx.wav
✅ [VistaContactoChat]: Audio del historial descargado: xxx.wav
🎵 [VistaContactoChat]: Reproducir audio EN MEMORIA - FileId: audio_files/xxx.wav
✅ Audio reproducido exitosamente
```

---

## 🎯 Casos Manejados

La solución maneja **3 escenarios diferentes**:

1. **Servidor envía `audioId` explícito** (ideal):
   ```json
   {"tipo": "audio", "audioId": "audio_files/xxx.wav"}
   ```
   → Se usa `audioId` directamente

2. **Servidor envía path en `contenido`** (caso actual):
   ```json
   {"tipo": "audio", "contenido": "audio_files/xxx.wav"}
   ```
   → Se mapea `contenido` a `fileId`

3. **Servidor envía Base64 en `contenido`** (push en tiempo real):
   ```json
   {"tipo": "audio", "contenido": "UklGR..."}
   ```
   → NO se mapeo
