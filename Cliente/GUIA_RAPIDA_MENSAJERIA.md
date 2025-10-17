# 🚀 Guía Rápida - Sistema de Mensajería Actualizado

## 📦 Cambios Implementados

### ✅ DTOs Actualizados

1. **DTOMensaje** - Ahora incluye todos los campos del servidor:
   - mensajeId, remitenteId, destinatarioId
   - remitenteNombre, destinatarioNombre
   - tipo (TEXTO, AUDIO, IMAGEN, ARCHIVO)
   - fileId, fileName
   - fechaEnvio, estado
   - esMio (calculado)

2. **DTOEnviarMensaje** - Tipos correctos según API:
   ```java
   // Texto
   DTOEnviarMensaje.deTexto(userId, contactoId, "Hola");
   
   // Audio
   DTOEnviarMensaje.deAudio(userId, contactoId, fileId, "audio.mp3");
   
   // Imagen (NUEVO)
   DTOEnviarMensaje.deImagen(userId, contactoId, "Mira esto", fileId, "foto.jpg");
   
   // Archivo (NUEVO)
   DTOEnviarMensaje.deArchivo(userId, contactoId, "Documento", fileId, "doc.pdf");
   ```

3. **DTOSolicitarHistorial** (NUEVO) - Para peticiones de historial
4. **DTOHistorialMensajes** (NUEVO) - Para respuestas de historial

### ✅ GestionMensajesImpl - Manejadores Correctos

**Acciones registradas:**
- `enviarMensajePrivado` → Respuesta de confirmación de envío
- `solicitarHistorialPrivado` → Respuesta con historial
- `nuevoMensajeDirecto` → **PUSH** de nuevo mensaje

**Flujos:**

#### Enviar Mensaje
```
Cliente                    Servidor
   |-- enviarMensajePrivado ->|
   |<- confirmación ----------|  (MENSAJE_ENVIADO_EXITOSO)
   
Destinatario
   |<- nuevoMensajeDirecto ---|  (NUEVO_MENSAJE_PRIVADO)
```

#### Solicitar Historial
```
Cliente                    Servidor
   |-- solicitarHistorialPrivado ->|
   |<- historial completo ---------|  (HISTORIAL_MENSAJES)
```

## 🔔 Eventos del Sistema

| Evento | Cuándo | Datos |
|--------|--------|-------|
| `MENSAJE_ENVIADO_EXITOSO` | Confirmación de envío | DTOMensaje |
| `NUEVO_MENSAJE_PRIVADO` | Push de nuevo mensaje | DTOMensaje |
| `HISTORIAL_MENSAJES` | Respuesta de historial | List<DTOMensaje> |
| `ERROR_ENVIO_MENSAJE` | Error al enviar | String |
| `ERROR_HISTORIAL` | Error al obtener historial | String |

## 💻 Ejemplo de Uso en UI

```java
@Override
public void actualizar(String tipo, Object datos) {
    switch (tipo) {
        case "NUEVO_MENSAJE_PRIVADO":
            DTOMensaje mensaje = (DTOMensaje) datos;
            
            // Verificar si el chat está abierto
            if (chatController.esChatActivo(mensaje.getRemitenteId())) {
                // Agregar mensaje a la vista actual
                chatController.agregarMensaje(mensaje);
            } else {
                // Mostrar notificación
                notificationManager.mostrar(
                    mensaje.getRemitenteNombre(),
                    mensaje.getContenido()
                );
                // Incrementar badge
                contactList.incrementarBadge(mensaje.getRemitenteId());
            }
            break;
            
        case "MENSAJE_ENVIADO_EXITOSO":
            DTOMensaje miMensaje = (DTOMensaje) datos;
            chatController.agregarMensaje(miMensaje);
            chatController.limpiarCampoTexto();
            break;
            
        case "HISTORIAL_MENSAJES":
            List<DTOMensaje> mensajes = (List<DTOMensaje>) datos;
            chatController.cargarHistorial(mensajes);
            break;
            
        case "ERROR_ENVIO_MENSAJE":
            String error = (String) datos;
            chatController.mostrarError("Error al enviar: " + error);
            break;
    }
}
```

## 🎨 Renderizado de Mensajes

```java
// En el controlador de chat
private void renderizarMensaje(DTOMensaje msg) {
    if (msg.esTexto()) {
        // Mostrar burbuja de texto
        chatView.agregarBurbuja(msg.getContenido(), msg.esMio(), msg.getAutorConFecha());
        
    } else if (msg.esImagen()) {
        // Mostrar miniatura de imagen
        chatView.agregarImagenBurbuja(
            msg.getContenido(),
            msg.getFileId(),
            msg.getFileName(),
            msg.esMio()
        );
        
    } else if (msg.esAudio()) {
        // Mostrar reproductor de audio
        chatView.agregarAudioBurbuja(
            msg.getFileId(),
            msg.getFileName(),
            msg.esMio()
        );
        
    } else if (msg.esArchivo()) {
        // Mostrar botón de descarga
        chatView.agregarArchivoBurbuja(
            msg.getContenido(),
            msg.getFileId(),
            msg.getFileName(),
            msg.esMio()
        );
    }
}
```

## 📱 Métodos Disponibles en DTOMensaje

```java
// Información básica
mensaje.getMensajeId()           // UUID del mensaje
mensaje.getRemitenteNombre()     // "Juan Pérez"
mensaje.getContenido()           // Texto del mensaje
mensaje.getTipo()                // "TEXTO", "AUDIO", etc.

// Archivos
mensaje.tieneArchivo()           // boolean
mensaje.getFileId()              // UUID del archivo
mensaje.getFileName()            // "foto.jpg"

// UI
mensaje.esMio()                  // Para alinear derecha/izquierda
mensaje.getAutorConFecha()       // "Juan Pérez - 10:35"

// Verificación de tipo
mensaje.esTexto()
mensaje.esAudio()
mensaje.esImagen()
mensaje.esArchivo()
```

## 🔧 Próximos Pasos Sugeridos

1. **Actualizar ControladorChat**
   - Manejar evento `NUEVO_MENSAJE_PRIVADO` para notificaciones
   - Diferenciar entre `MENSAJE_ENVIADO_EXITOSO` y mensajes recibidos
   
2. **Implementar Descarga de Archivos**
   - Usar `mensaje.getFileId()` para descargar
   - Mostrar preview de imágenes
   - Reproductor para audios

3. **Badges y Notificaciones**
   - Contador de mensajes no leídos por contacto
   - Notificaciones de escritorio
   - Sonidos de alerta

4. **Interfaz de Usuario**
   - Burbujas diferentes por tipo de mensaje
   - Indicadores de estado (enviado, entregado, leído)
   - Animaciones de llegada de mensajes

## ⚠️ Notas Importantes

- El campo `esMio` se calcula en el cliente comparando `remitenteId` con el userId de la sesión
- El servidor envía `id` (numérico) o `mensajeId` (string), DTOMensaje maneja ambos
- Las notificaciones push llegan automáticamente si el destinatario está conectado
- El historial puede venir como objeto completo o array directo, hay fallback

## 📚 Documentación Completa

Ver: `ACTUALIZACION_SISTEMA_MENSAJERIA.md` para documentación detallada.

