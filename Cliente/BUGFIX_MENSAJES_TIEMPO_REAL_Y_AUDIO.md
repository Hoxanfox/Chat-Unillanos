# 🔧 BUGFIX: Mensajes en Tiempo Real y Envío de Audio

## 📋 Resumen
Se corrigieron tres problemas críticos:
1. **Mensajes no se actualizaban en tiempo real** - Los mensajes de audio enviados no aparecían inmediatamente en la interfaz
2. **Error "Formato de payload de audio incorrecto"** - El servidor rechazaba los mensajes de audio
3. **Campos null no se serializaban** - Gson omitía `peerRemitenteId` y `peerDestinoId` cuando eran null

---

## 🐛 Problema 1: Mensajes de Audio No se Actualizaban en Tiempo Real

### 🔍 Diagnóstico
Los logs mostraban:
```
[VistaContactoChat]: Notificación recibida - Tipo: MENSAJE_ENVIADO_EXITOSO ✅
[VistaContactoChat]: Mensaje enviado exitosamente → ID: 551ee90f-00c6-41dc-8f85-08aab286b5c5 → Tipo: TEXTO
⚠️ [VistaContactoChat]: Mensaje vacío, no se mostrará
```

**Causa:** La vista solo manejaba la notificación `MENSAJE_ENVIADO_EXITOSO` para mensajes de texto, pero no para `MENSAJE_AUDIO_ENVIADO_EXITOSO`.

### ✅ Solución
**Archivo:** `Presentacion/InterfazEscritorio/src/main/java/interfazEscritorio/dashboard/featureContactos/chatContacto/VistaContactoChat.java`

1. **Agregado manejo de notificación de audio:**
```java
case "MENSAJE_ENVIADO_EXITOSO":
case "MENSAJE_AUDIO_ENVIADO_EXITOSO": // ✅ Agregado para mensajes de audio
    // Confirmación de que nuestro mensaje fue enviado
    if (datos instanceof DTOMensaje) {
        DTOMensaje mensaje = (DTOMensaje) datos;
        // ✅ IMPORTANTE: Verificar que sea para este contacto
        if (mensaje.getDestinatarioId() != null && mensaje.getDestinatarioId().equals(contacto.getId())) {
            Platform.runLater(() -> agregarMensaje(mensaje));
        }
    }
    break;
```

2. **Agregado manejo de errores de audio:**
```java
case "ERROR_ENVIO_MENSAJE":
case "ERROR_ENVIO_MENSAJE_AUDIO": // ✅ Agregado para errores de audio
    String error = datos != null ? datos.toString() : "Error desconocido";
    System.err.println("❌ [VistaContactoChat]: Error al enviar mensaje: " + error);
    break;
```

3. **Validación de destinatario:** Ahora se verifica que el mensaje enviado sea para el contacto actual antes de mostrarlo, evitando que mensajes de otros chats aparezcan incorrectamente.

---

## 🐛 Problema 2: Error "Formato de payload de audio incorrecto"

### 🔍 Diagnóstico Final
El servidor rechazaba el mensaje con dos errores diferentes:

**Error 1:** "El enlace del archivo de audio es requerido, campo: contenido"
- **Causa:** El DTO usaba el campo `audioId` en lugar de `contenido`

**Error 2:** "Formato de payload de audio incorrecto"
- **Causa:** Faltaban los campos `peerRemitenteId` y `peerDestinoId` en el JSON

### ✅ Solución Completa

#### 1. Corregido el nombre del campo de audio
**Archivo:** `Infraestructura/DTO/src/main/java/dto/comunicacion/peticion/mensaje/DTOEnviarMensajeAudioPayload.java`

```java
public class DTOEnviarMensajeAudioPayload {
    private final String peerRemitenteId;
    private final String peerDestinoId;
    private final String remitenteId;
    private final String destinatarioId;
    private final String tipo;
    private final String contenido;  // ✅ Usa 'contenido', no 'audioId'

    public DTOEnviarMensajeAudioPayload(String peerRemitenteId, String peerDestinoId,
                                        String remitenteId, String destinatarioId,
                                        String audioFilePath) {
        this.peerRemitenteId = peerRemitenteId;
        this.peerDestinoId = peerDestinoId;
        this.remitenteId = remitenteId;
        this.destinatarioId = destinatarioId;
        this.tipo = "audio";
        this.contenido = audioFilePath;  // ✅ Asigna a 'contenido'
    }

    // Getters completos
    public String getPeerRemitenteId() { return peerRemitenteId; }
    public String getPeerDestinoId() { return peerDestinoId; }
    public String getRemitenteId() { return remitenteId; }
    public String getDestinatarioId() { return destinatarioId; }
    public String getTipo() { return tipo; }
    public String getContenido() { return contenido; }
}
```

#### 2. Configurado Gson para serializar campos null
**Archivo:** `Persistencia/Comunicacion/src/main/java/comunicacion/EnviadorPeticiones.java`

**Problema:** Gson por defecto **omite campos con valor `null`** al serializar. Por eso `peerRemitenteId` y `peerDestinoId` no aparecían en el JSON.

**Solución:**
```java
public class EnviadorPeticiones implements IEnviadorPeticiones {

    private final GestorConexion gestorConexion;
    private final Gson gson;

    public EnviadorPeticiones() {
        this.gestorConexion = GestorConexion.getInstancia();
        // ✅ CORRECCIÓN: Configurar Gson para serializar campos nulos
        this.gson = new GsonBuilder()
                .serializeNulls()  // ← Esto incluye campos null en el JSON
                .create();
    }
    
    // ...resto del código...
}
```

---

## 📊 Evolución del Formato del Payload

### ❌ Intento 1 (Rechazado - Campo incorrecto)
```json
{
  "action": "enviarmensajedirectoaudio",
  "payload": {
    "remitenteId": "...",
    "destinatarioId": "...",
    "tipo": "audio",
    "audioId": "audio_files/..."  // ❌ Campo incorrecto
  }
}
```
**Error:** "El enlace del archivo de audio es requerido, campo: contenido"

### ❌ Intento 2 (Rechazado - Campos faltantes)
```json
{
  "action": "enviarmensajedirectoaudio",
  "payload": {
    "remitenteId": "...",
    "destinatarioId": "...",
    "tipo": "audio",
    "contenido": "audio_files/..."
    // ❌ FALTAN: peerRemitenteId y peerDestinoId
  }
}
```
**Error:** "Formato de payload de audio incorrecto"

### ✅ Resultado Final (Correcto)
```json
{
  "action": "enviarmensajedirectoaudio",
  "payload": {
    "peerRemitenteId": null,      // ✅ Incluido gracias a .serializeNulls()
    "peerDestinoId": null,         // ✅ Incluido gracias a .serializeNulls()
    "remitenteId": "a406e00f-95bc-42fd-928c-e07395ca7624",
    "destinatarioId": "8d1ce81e-620c-4687-b293-d80261c369a1",
    "tipo": "audio",
    "contenido": "audio_files/a406e00f-95bc-42fd-928c-e07395ca7624_1762495552685.wav"
  }
}
```

---

## 🎯 Cambios Realizados

### Archivos Modificados:
1. ✅ `Presentacion/InterfazEscritorio/src/main/java/interfazEscritorio/dashboard/featureContactos/chatContacto/VistaContactoChat.java`
   - Agregado manejo de `MENSAJE_AUDIO_ENVIADO_EXITOSO`
   - Agregado manejo de `ERROR_ENVIO_MENSAJE_AUDIO`
   - Agregada validación de destinatario para mensajes enviados

2. ✅ `Negocio/GestionContactos/src/main/java/gestionContactos/mensajes/GestionMensajesImpl.java`
   - Actualizado método `enviarMensajeAudio()` para usar el nuevo DTO
   - Corrección del formato del payload con campo `contenido`

3. ✅ `Persistencia/Comunicacion/src/main/java/comunicacion/EnviadorPeticiones.java`
   - **CRÍTICO:** Configurado Gson con `.serializeNulls()` para incluir campos null
   - Sin esto, los campos `peerRemitenteId` y `peerDestinoId` se omitían del JSON

### Archivos Creados:
4. ✅ `Infraestructura/DTO/src/main/java/dto/comunicacion/peticion/mensaje/DTOEnviarMensajeAudioPayload.java`
   - Nuevo DTO específico para mensajes de audio con el formato correcto
   - Usa `contenido` en lugar de `audioId`
   - Incluye todos los campos requeridos por el servidor

---

## 🧪 Pruebas Esperadas

### Escenario 1: Enviar mensaje de audio
1. ✅ El audio se graba correctamente
2. ✅ El audio se sube al servidor mediante chunks
3. ✅ Se obtiene el `fileId` del servidor (ej: `audio_files/user_123.wav`)
4. ✅ El payload se envía con el formato correcto (incluye todos los campos)
5. ✅ El servidor acepta el mensaje sin errores
6. ✅ El mensaje aparece inmediatamente en la interfaz del usuario que envió
7. ✅ El destinatario recibe una notificación PUSH con el audio en Base64

### Escenario 2: Recibir mensaje de audio
1. ✅ El PUSH del servidor llega con el audio en Base64
2. ✅ El mensaje se muestra en la interfaz con botón de reproducción
3. ✅ El audio se puede reproducir en memoria

---

## 📝 Notas Importantes

### ¿Por qué se necesita .serializeNulls()?

Gson por defecto omite campos `null` para reducir el tamaño del JSON. Sin embargo, algunos servidores requieren que **todos los campos** estén presentes en el JSON, aunque sean `null`, para validar el esquema correctamente.

**Sin `.serializeNulls()`:**
```json
{
  "remitenteId": "...",
  "tipo": "audio"
  // peerRemitenteId y peerDestinoId se omiten
}
```

**Con `.serializeNulls()`:**
```json
{
  "peerRemitenteId": null,  // ← Incluido
  "peerDestinoId": null,    // ← Incluido
  "remitenteId": "...",
  "tipo": "audio"
}
```

### Diferencia entre Petición y PUSH de Audio

| Aspecto | Petición (enviarMensajeDirectoAudio) | Push (nuevoMensajeDirectoAudio) |
|---------|--------------------------------------|----------------------------------|
| **Campo de audio** | `contenido` (ruta/fileId) | `contenido` (Base64) |
| **Ejemplo** | `"audio_files/user_123.wav"` | `"data:audio/webm;base64,..."` |
| **¿Listo para reproducir?** | No, requiere descarga | Sí, reproducir directamente |
| **Tamaño del JSON** | Pequeño (~50 bytes) | Grande (>10KB) |

### Sobre el Almacenamiento de Audio
- Los archivos de audio se suben primero al servidor mediante `GestionArchivos`
- El servidor devuelve un `fileId` (ruta del archivo en el servidor)
- Este `fileId` se usa en el campo `contenido` del payload del mensaje
- El cliente **NO** almacena el audio en su base de datos local antes de enviarlo
- El historial de mensajes se obtiene del servidor con las rutas de los archivos

---

## ✅ Estado: COMPLETADO

Los tres problemas han sido resueltos:
1. ✅ Los mensajes de audio ahora se actualizan en tiempo real
2. ✅ El formato del payload de audio es correcto (usa `contenido`)
3. ✅ Todos los campos se serializan correctamente, incluyendo los null

**Fecha:** 7 de noviembre de 2025
**Última actualización:** Configurado Gson para serializar campos null
