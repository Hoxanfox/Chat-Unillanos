}
```

---

## 🎯 Resultado Esperado

Después de aplicar estos cambios, cuando envíes un mensaje verás:

```
📤 [GestionMensajes]: Enviando mensaje de TEXTO
   → Contenido: s
✅ [GestionMensajes]: Mensaje de texto enviado al servidor

📥 [GestionMensajes]: Recibida RESPUESTA de envío de mensaje - Status: success
🔄 [GestionMensajes]: Completando mensaje con datos de caché
✅ [GestionMensajes]: Mensaje completado - Contenido: s
✅ [GestionMensajes]: Mensaje confirmado por servidor
   → ID: 12412bde-5dcc-4347-b745-2b173f2ac85f
   → Contenido: s  ← ✅ AHORA TIENE CONTENIDO!
   
✅ [VistaContactoChat]: Mensaje enviado exitosamente
   → Contenido: s  ← ✅ SE MUESTRA EN LA UI!
```

---

## 📝 Notas

- Esta solución usa una caché temporal simple que almacena **solo el último mensaje** enviado
- Si se envían múltiples mensajes rápidamente antes de recibir respuesta, solo el último se guardará en caché
- Para una solución más robusta, se podría usar un `Map<String, DTOMensaje>` indexado por algún ID temporal

---

## ✅ Compilar y Probar

```bash
cd /home/deivid/Documents/Chat-Unillanos/Cliente
mvn clean package -DskipTests
java -jar target/Cliente-1.0-SNAPSHOT-jar-with-dependencies.jar
```
# 🐛 BUGFIX: Mensaje Vacío No Se Muestra

## 📋 Problema

Cuando se envía un mensaje de texto, el servidor responde con:
```json
{
  "mensajeId": "12412bde-5dcc-4347-b745-2b173f2ac85f",
  "fechaEnvio": "2025-11-07T05:38:57.250818546"
}
```

Pero **NO devuelve el contenido del mensaje**. Esto causa que el cliente detecte el mensaje como vacío y no lo muestre:

```
⚠️ [VistaContactoChat]: Mensaje vacío, no se mostrará
```

---

## ✅ Solución

Agregar un sistema de caché temporal para almacenar los datos del mensaje enviado y completarlos cuando el servidor responde.

---

## 📝 Cambios a Aplicar

### Archivo: `Negocio/GestionContactos/src/main/java/gestionContactos/mensajes/GestionMensajesImpl.java`

#### 1. Agregar campos de caché (después de la línea `private final Gson gson;`):

```java
private final Gson gson;

// ✅ Cachés temporales para completar mensajes cuando el servidor responde
private DTOMensaje ultimoMensajeTextoEnviado;
private DTOMensaje ultimoMensajeAudioEnviado;
```

#### 2. Modificar el método `enviarMensajeTexto` (al final del método, antes del return):

```java
@Override
public CompletableFuture<Void> enviarMensajeTexto(String destinatarioId, String contenido) {
    String remitenteId = gestorSesionUsuario.getUserId();
    String peerRemitenteId = gestorSesionUsuario.getPeerId();
    String peerDestinoId = gestorContactoPeers.getPeerIdDeContacto(destinatarioId);

    System.out.println("📤 [GestionMensajes]: Enviando mensaje de TEXTO");
    System.out.println("   → Remitente: " + remitenteId);
    System.out.println("   → Destinatario: " + destinatarioId);
    System.out.println("   → Contenido: " + contenido);

    if (peerDestinoId == null) {
        System.out.println("   ℹ️  Destinatario offline, mensaje se guardará para entrega posterior");
    }

    DTOEnviarMensaje payload = DTOEnviarMensaje.deTexto(
            peerRemitenteId,
            peerDestinoId,
            remitenteId,
            destinatarioId,
            contenido
    );
    DTORequest peticion = new DTORequest("enviarmensajedirecto", payload);
    enviadorPeticiones.enviar(peticion);

    // ✅ NUEVO: Almacenar en caché el mensaje enviado
    ultimoMensajeTextoEnviado = new DTOMensaje();
    ultimoMensajeTextoEnviado.setContenido(contenido);
    ultimoMensajeTextoEnviado.setTipo("TEXTO");
    ultimoMensajeTextoEnviado.setRemitenteId(remitenteId);
    ultimoMensajeTextoEnviado.setDestinatarioId(destinatarioId);
    ultimoMensajeTextoEnviado.setPeerRemitenteId(peerRemitenteId);
    ultimoMensajeTextoEnviado.setPeerDestinoId(peerDestinoId);

    System.out.println("✅ [GestionMensajes]: Mensaje de texto enviado al servidor");
    return CompletableFuture.completedFuture(null);
}
```

#### 3. Modificar el método `enviarMensajeAudio` (al final del método, antes del return):

```java
@Override
public CompletableFuture<Void> enviarMensajeAudio(String destinatarioId, String audioFileId) {
    String remitenteId = gestorSesionUsuario.getUserId();
    String peerRemitenteId = gestorSesionUsuario.getPeerId();
    String peerDestinoId = gestorContactoPeers.getPeerIdDeContacto(destinatarioId);

    System.out.println("📤 [GestionMensajes]: Enviando mensaje de AUDIO");
    System.out.println("   → Remitente: " + remitenteId);
    System.out.println("   → Destinatario: " + destinatarioId);
    System.out.println("   → AudioFileId: " + audioFileId);

    if (peerDestinoId == null) {
        System.out.println("   ℹ️  Destinatario offline, mensaje se guardará para entrega posterior");
    }

    DTOEnviarMensaje payload = DTOEnviarMensaje.deAudio(
            peerRemitenteId,
            peerDestinoId,
            remitenteId,
            destinatarioId,
            audioFileId
    );

    DTORequest peticion = new DTORequest("enviarmensajedirectoaudio", payload);
    enviadorPeticiones.enviar(peticion);

    // ✅ NUEVO: Almacenar en caché el mensaje de audio enviado
    ultimoMensajeAudioEnviado = new DTOMensaje();
    ultimoMensajeAudioEnviado.setContenido(audioFileId);
    ultimoMensajeAudioEnviado.setTipo("AUDIO");
    ultimoMensajeAudioEnviado.setRemitenteId(remitenteId);
    ultimoMensajeAudioEnviado.setDestinatarioId(destinatarioId);
    ultimoMensajeAudioEnviado.setPeerRemitenteId(peerRemitenteId);
    ultimoMensajeAudioEnviado.setPeerDestinoId(peerDestinoId);

    System.out.println("✅ [GestionMensajes]: Mensaje de audio enviado al servidor");
    return CompletableFuture.completedFuture(null);
}
```

#### 4. Reemplazar el método `manejarRespuestaEnvioMensaje` completo:

```java
private void manejarRespuestaEnvioMensaje(DTOResponse r) {
    System.out.println("📥 [GestionMensajes]: Recibida RESPUESTA de envío de mensaje - Status: " + r.getStatus());

    if (r.fueExitoso()) {
        DTOMensaje mensaje = gson.fromJson(gson.toJson(r.getData()), DTOMensaje.class);
        
        // ✅ NUEVO: Completar con datos de caché
        if (ultimoMensajeTextoEnviado != null) {
            System.out.println("🔄 [GestionMensajes]: Completando mensaje con datos de caché");
            
            String idServidor = mensaje.getMensajeId();
            String fechaServidor = mensaje.getFechaEnvio();
            
            mensaje.setContenido(ultimoMensajeTextoEnviado.getContenido());
            mensaje.setTipo(ultimoMensajeTextoEnviado.getTipo());
            mensaje.setRemitenteId(ultimoMensajeTextoEnviado.getRemitenteId());
            mensaje.setDestinatarioId(ultimoMensajeTextoEnviado.getDestinatarioId());
            mensaje.setPeerRemitenteId(ultimoMensajeTextoEnviado.getPeerRemitenteId());
            mensaje.setPeerDestinoId(ultimoMensajeTextoEnviado.getPeerDestinoId());
            
            mensaje.setMensajeId(idServidor);
            mensaje.setFechaEnvio(fechaServidor);
            
            ultimoMensajeTextoEnviado = null;
            
            System.out.println("✅ [GestionMensajes]: Mensaje completado - Contenido: " + mensaje.getContenido());
        }
        
        determinarTipoMensaje(mensaje);

        System.out.println("✅ [GestionMensajes]: Mensaje confirmado por servidor");
        System.out.println("   → ID: " + mensaje.getMensajeId());
        System.out.println("   → Fecha: " + mensaje.getFechaEnvio());
        System.out.println("   → Tipo: " + mensaje.getTipo());
        System.out.println("   → Contenido: " + mensaje.getContenido());

        mensaje.setEsMio(true);
        notificarObservadores("MENSAJE_ENVIADO_EXITOSO", mensaje);
    } else {
        ultimoMensajeTextoEnviado = null; // Limpiar caché en caso de error
        
        String errorMsg = r.getMessage();
        System.err.println("❌ [GestionMensajes]: Error en respuesta de envío: " + errorMsg);

        if (errorMsg.contains("query did not return a unique result")) {
            System.err.println("⚠️ [GestionMensajes]: ERROR DEL SERVIDOR - Base de datos tiene registros duplicados");
            notificarObservadores("ERROR_BD_SERVIDOR_DUPLICADOS",
                "El servidor tiene registros duplicados. Por favor, contacta al administrador del servidor.");
        } else if (errorMsg.contains("Destinatario no encontrado") || errorMsg.contains("desconectado")) {
            notificarObservadores("ERROR_DESTINATARIO_NO_DISPONIBLE", errorMsg);
        } else if (errorMsg.contains("inválidos") || errorMsg.contains("Datos de mensaje inválidos")) {
            notificarObservadores("ERROR_VALIDACION", r.getData() != null ? r.getData() : errorMsg);
        } else {
            notificarObservadores("ERROR_ENVIO_MENSAJE", errorMsg);
        }
    }
}
```

#### 5. Reemplazar el método `manejarRespuestaEnvioMensajeAudio` completo:

```java
private void manejarRespuestaEnvioMensajeAudio(DTOResponse r) {
    System.out.println("📥 [GestionMensajes]: Recibida RESPUESTA de envío de mensaje de audio - Status: " + r.getStatus());

    if (r.fueExitoso()) {
        DTOMensaje mensaje = gson.fromJson(gson.toJson(r.getData()), DTOMensaje.class);
        
        // ✅ NUEVO: Completar con datos de caché
        if (ultimoMensajeAudioEnviado != null) {
            System.out.println("🔄 [GestionMensajes]: Completando mensaje de audio con datos de caché");
            
            String idServidor = mensaje.getMensajeId();
            String fechaServidor = mensaje.getFechaEnvio();
            
            mensaje.setContenido(ultimoMensajeAudioEnviado.getContenido());
            mensaje.setTipo(ultimoMensajeAudioEnviado.getTipo());
            mensaje.setRemitenteId(ultimoMensajeAudioEnviado.getRemitenteId());
            mensaje.setDestinatarioId(ultimoMensajeAudioEnviado.getDestinatarioId());
            mensaje.setPeerRemitenteId(ultimoMensajeAudioEnviado.getPeerRemitenteId());
            mensaje.setPeerDestinoId(ultimoMensajeAudioEnviado.getPeerDestinoId());
            
            mensaje.setMensajeId(idServidor);
            mensaje.setFechaEnvio(fechaServidor);
            
            ultimoMensajeAudioEnviado = null;
            
            System.out.println("✅ [GestionMensajes]: Mensaje de audio completado - FileId: " + mensaje.getContenido());
        }
        
        determinarTipoMensaje(mensaje);

        System.out.println("✅ [GestionMensajes]: Mensaje de audio confirmado por servidor");
        System.out.println("   → ID: " + mensaje.getMensajeId());
        System.out.println("   → Fecha: " + mensaje.getFechaEnvio());
        System.out.println("   → FileId: " + mensaje.getContenido());

        mensaje.setEsMio(true);
        notificarObservadores("MENSAJE_AUDIO_ENVIADO_EXITOSO", mensaje);
    } else {
        ultimoMensajeAudioEnviado = null; // Limpiar caché en caso de error
        
        String errorMsg = r.getMessage();
        System.err.println("❌ [GestionMensajes]: Error en respuesta de envío de mensaje de audio: " + errorMsg);

        if (errorMsg.contains("query did not return a unique result")) {
            System.err.println("⚠️ [GestionMensajes]: ERROR DEL SERVIDOR - Base de datos tiene registros duplicados");
            notificarObservadores("ERROR_BD_SERVIDOR_DUPLICADOS",
                "El servidor tiene registros duplicados. Por favor, contacta al administrador del servidor.");
        } else if (errorMsg.contains("Destinatario no encontrado") || errorMsg.contains("desconectado")) {
            notificarObservadores("ERROR_DESTINATARIO_NO_DISPONIBLE", errorMsg);
        } else if (errorMsg.contains("inválidos") || errorMsg.contains("Datos de mensaje inválidos")) {
            notificarObservadores("ERROR_VALIDACION", r.getData() != null ? r.getData() : errorMsg);
        } else {
            notificarObservadores("ERROR_ENVIO_MENSAJE_AUDIO", errorMsg);
        }
    }

