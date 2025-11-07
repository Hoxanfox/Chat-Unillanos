package gestionContactos.mensajes;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import comunicacion.EnviadorPeticiones;
import comunicacion.GestorRespuesta;
import comunicacion.IEnviadorPeticiones;
import comunicacion.IGestorRespuesta;
import dto.comunicacion.DTORequest;
import dto.comunicacion.DTOResponse;
import dto.comunicacion.peticion.mensaje.DTOEnviarMensaje;
import dto.comunicacion.peticion.mensaje.DTOEnviarMensajeAudio;
import dto.comunicacion.peticion.mensaje.DTOSolicitarHistorial;
import dto.vistaContactoChat.DTOMensaje;
import gestionContactos.GestorContactoPeers;
import gestionUsuario.sesion.GestorSesionUsuario;
import observador.IObservador;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación del gestor de mensajes alineado con la nueva API del servidor.
 * Maneja tanto respuestas a peticiones como notificaciones push.
 * Ahora incluye soporte para peers WebRTC.
 */
public class GestionMensajesImpl implements IGestionMensajes {

    private final List<IObservador> observadores = new ArrayList<>();
    private final IEnviadorPeticiones enviadorPeticiones;
    private final IGestorRespuesta gestorRespuesta;
    private final GestorSesionUsuario gestorSesionUsuario;
    private final GestorContactoPeers gestorContactoPeers;
    private final Gson gson;

    public GestionMensajesImpl() {
        System.out.println("🔧 [GestionMensajes]: Inicializando gestor de mensajes...");

        this.enviadorPeticiones = new EnviadorPeticiones();
        this.gestorRespuesta = GestorRespuesta.getInstancia();
        this.gestorSesionUsuario = GestorSesionUsuario.getInstancia();
        this.gestorContactoPeers = GestorContactoPeers.getInstancia();
        this.gson = new Gson();

        // Registrar manejadores para respuestas a peticiones
        this.gestorRespuesta.registrarManejador("enviarMensajeDirecto", this::manejarRespuestaEnvioMensaje);
        this.gestorRespuesta.registrarManejador("enviarMensajeDirectoAudio", this::manejarRespuestaEnvioMensajeAudio);
        this.gestorRespuesta.registrarManejador("solicitarHistorialPrivado", this::manejarHistorial);

        // Registrar manejadores para notificaciones PUSH del servidor
        this.gestorRespuesta.registrarManejador("nuevoMensajeDirecto", this::manejarNuevoMensajePush);
        this.gestorRespuesta.registrarManejador("nuevoMensajeDirectoAudio", this::manejarNuevoMensajeAudioPush);

        System.out.println("✅ [GestionMensajes]: Gestor inicializado con manejadores registrados");
        System.out.println("   → Respuestas: enviarMensajeDirecto, enviarMensajeDirectoAudio, solicitarHistorialPrivado");
        System.out.println("   → Push: nuevoMensajeDirecto, nuevoMensajeDirectoAudio");
    }

// java
// Modificaciones en `Negocio/GestionContactos/src/main/java/gestionContactos/mensajes/GestionMensajesImpl.java`

    @Override
    public void solicitarHistorial(String contactoId) {
        String userId = gestorSesionUsuario.getUserId();
        String peerRemitenteId = gestorSesionUsuario.getPeerId();
        String peerDestinatarioId = gestorContactoPeers.getPeerIdDeContacto(contactoId);

        System.out.println("📡 [GestionMensajes]: Solicitando historial de mensajes");
        System.out.println("   → RemitenteId (UserId): " + userId);
        System.out.println("   → PeerRemitenteId: " + peerRemitenteId);
        System.out.println("   → DestinatarioId (ContactoId): " + contactoId);
        System.out.println("   → PeerDestinatarioId: " + peerDestinatarioId);

        if (peerDestinatarioId == null) {
            System.out.println("⚠️ [GestionMensajes]: No se encontró peerId del destinatario — se enviará la petición con peerDestinatarioId = null");
            notificarObservadores("ADVERTENCIA_PEER_NO_ENCONTRADO", "Se solicitará historial sin peerId del destinatario");
            // continuar y enviar con peerDestinatarioId == null
        }

        DTOSolicitarHistorial payload = new DTOSolicitarHistorial(
                userId,
                peerRemitenteId,
                contactoId,
                peerDestinatarioId // puede ser null
        );

        // ✅ CORRECCIÓN: El servidor espera la acción en minúsculas sin camelCase
        DTORequest peticion = new DTORequest("solicitarhistorialprivado", payload);
        enviadorPeticiones.enviar(peticion);
        System.out.println("✅ [GestionMensajes]: Petición de historial enviada al servidor");
    }

    @Override
    public CompletableFuture<Void> enviarMensajeTexto(String destinatarioId, String contenido) {
        String remitenteId = gestorSesionUsuario.getUserId();
        String peerRemitenteId = gestorSesionUsuario.getPeerId();
        String peerDestinoId = gestorContactoPeers.getPeerIdDeContacto(destinatarioId);

        System.out.println("📤 [GestionMensajes]: Enviando mensaje de TEXTO");
        System.out.println("   → Remitente: " + remitenteId + " (Peer: " + peerRemitenteId + ")");
        System.out.println("   → Destinatario: " + destinatarioId + " (Peer: " + peerDestinoId + ")");
        System.out.println("   → Contenido: " + contenido);

        if (peerDestinoId == null) {
            System.out.println("⚠️ [GestionMensajes]: No se encontró peerId del destinatario — se enviará el mensaje con peerDestinoId = null");
            notificarObservadores("ADVERTENCIA_PEER_NO_ENCONTRADO", "Se enviará mensaje de texto sin peerId del destinatario");
        }

        DTOEnviarMensaje payload = DTOEnviarMensaje.deTexto(
                peerRemitenteId,
                peerDestinoId, // puede ser null
                remitenteId,
                destinatarioId,
                contenido
        );
        // ✅ CORRECCIÓN: El servidor espera la acción en minúsculas sin camelCase
        DTORequest peticion = new DTORequest("enviarmensajedirecto", payload);
        enviadorPeticiones.enviar(peticion);

        System.out.println("✅ [GestionMensajes]: Mensaje de texto enviado al servidor");
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> enviarMensajeAudio(String destinatarioId, String audioFileId) {
        String remitenteId = gestorSesionUsuario.getUserId();
        String peerRemitenteId = gestorSesionUsuario.getPeerId();
        String peerDestinoId = gestorContactoPeers.getPeerIdDeContacto(destinatarioId);

        System.out.println("📤 [GestionMensajes]: Enviando mensaje de AUDIO (ruta de archivo)");
        System.out.println("   → Remitente: " + remitenteId + " (Peer: " + peerRemitenteId + ")");
        System.out.println("   → Destinatario: " + destinatarioId + " (Peer: " + peerDestinoId + ")");
        System.out.println("   → AudioFilePath: " + audioFileId);

        if (peerDestinoId == null) {
            System.out.println("⚠️ [GestionMensajes]: No se encontró peerId del destinatario — se enviará el audio con peerDestinoId = null");
            notificarObservadores("ADVERTENCIA_PEER_NO_ENCONTRADO", "Se enviará mensaje de audio sin peerId del destinatario");
        }

        // ✅ CORRECCIÓN: Usar DTOEnviarMensaje.deAudio con la ruta del archivo en 'contenido'
        DTOEnviarMensaje payload = DTOEnviarMensaje.deAudio(
                peerRemitenteId,
                peerDestinoId, // puede ser null
                remitenteId,
                destinatarioId,
                audioFileId // Esta es la ruta/URL del archivo, NO Base64
        );

        // ✅ CORRECCIÓN: El servidor espera la acción en minúsculas sin camelCase
        DTORequest peticion = new DTORequest("enviarmensajedirectoaudio", payload);
        enviadorPeticiones.enviar(peticion);

        System.out.println("✅ [GestionMensajes]: Mensaje de audio enviado al servidor");
        return CompletableFuture.completedFuture(null);
    }


    /**
     * Helper: normaliza tipo según fileId/contenido
     * - Si fileId presente y contenido vacío -> AUDIO
     * - Si contenido presente y fileId vacío -> TEXTO
     * - Si ambos presentes -> FILE (o conservar tipo si ya viene)
     * - Si ninguno presente -> TEXTO por defecto
     */
    private void determinarTipoMensaje(DTOMensaje mensaje) {
        if (mensaje == null) return;

        boolean hasFile = mensaje.getFileId() != null && !mensaje.getFileId().isEmpty();
        boolean hasText = mensaje.getContenido() != null && !mensaje.getContenido().isEmpty();

        if (hasFile && !hasText) {
            mensaje.setTipo("AUDIO");
        } else if (hasText && !hasFile) {
            mensaje.setTipo("TEXTO");
        } else if (hasFile && hasText) {
            if (mensaje.getTipo() == null || mensaje.getTipo().isEmpty()) {
                mensaje.setTipo("FILE");
            }
        } else {
            if (mensaje.getTipo() == null || mensaje.getTipo().isEmpty()) {
                mensaje.setTipo("TEXTO");
            }
        }
    }

    /**
     * Mapea la respuesta del servidor (con estructura del push)
     * al formato esperado por DTOMensaje (remitenteId/contenido/tipo).
     *
     * Estructura esperada del servidor para PUSH:
     * - mensajeId, remitenteId, remitenteNombre, peerRemitenteId, peerDestinoId
     * - tipo, contenido, fechaEnvio, destinatarioId
     *
     * Nota: Los mensajes de audio en PUSH vienen con contenido en Base64
     */
    private DTOMensaje mapearMensajeDesdeServidor(Object data) {
        DTOMensaje mensaje = new DTOMensaje();

        try {
            // Convertir a Map para acceder a los campos
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) data;

            // Mapear mensajeId directamente (el push usa este nombre)
            if (map.containsKey("mensajeId")) {
                mensaje.setMensajeId((String) map.get("mensajeId"));
            }

            // Mapear fechaEnvio directamente (el push usa este nombre)
            if (map.containsKey("fechaEnvio")) {
                mensaje.setFechaEnvio((String) map.get("fechaEnvio"));
            }

            // Mapear remitenteId directamente (el push usa este nombre)
            if (map.containsKey("remitenteId")) {
                mensaje.setRemitenteId((String) map.get("remitenteId"));
            }

            // Mapear remitenteNombre directamente (el push usa este nombre)
            if (map.containsKey("remitenteNombre")) {
                mensaje.setRemitenteNombre((String) map.get("remitenteNombre"));
            }

            // Mapear peerRemitenteId directamente (el push usa este nombre)
            if (map.containsKey("peerRemitenteId")) {
                mensaje.setPeerRemitenteId((String) map.get("peerRemitenteId"));
            }

            // Mapear peerDestinoId directamente (el push usa este nombre)
            if (map.containsKey("peerDestinoId")) {
                mensaje.setPeerDestinoId((String) map.get("peerDestinoId"));
            }

            // Mapear destinatarioId directamente (el push usa este nombre)
            if (map.containsKey("destinatarioId")) {
                mensaje.setDestinatarioId((String) map.get("destinatarioId"));
            }

            // Mapear tipo directamente (el push usa "texto" o "audio" en minúsculas)
            if (map.containsKey("tipo")) {
                String tipo = (String) map.get("tipo");
                // Convertir a formato esperado por el cliente (TEXTO/AUDIO en mayúsculas)
                mensaje.setTipo(tipo.toUpperCase());
            }

            // Mapear contenido directamente (el push usa este nombre)
            // Para mensajes de texto: contenido normal
            // Para mensajes de audio: datos Base64 (data:audio/webm;base64,...)
            if (map.containsKey("contenido")) {
                mensaje.setContenido((String) map.get("contenido"));
            }

        } catch (Exception e) {
            System.err.println("❌ [GestionMensajes]: Error al mapear mensaje desde servidor: " + e.getMessage());
            e.printStackTrace();
        }

        return mensaje;
    }

    /**
     * Convierte el tipo de mensaje del servidor (TEXT, IMAGE, AUDIO, FILE)
     * al formato esperado por el cliente (TEXTO, IMAGEN, AUDIO, ARCHIVO).
     */
    private String convertirTipoMensaje(String messageType) {
        if (messageType == null) return "TEXTO";

        switch (messageType.toUpperCase()) {
            case "TEXT":
                return "TEXTO";
            case "IMAGE":
                return "IMAGEN";
            case "AUDIO":
                return "AUDIO";
            case "FILE":
                return "ARCHIVO";
            default:
                return messageType;
        }
    }

    private void manejarRespuestaEnvioMensaje(DTOResponse r) {
        System.out.println("📥 [GestionMensajes]: Recibida RESPUESTA de envío de mensaje - Status: " + r.getStatus());

        if (r.fueExitoso()) {
            DTOMensaje mensaje = gson.fromJson(gson.toJson(r.getData()), DTOMensaje.class);
            determinarTipoMensaje(mensaje);

            System.out.println("✅ [GestionMensajes]: Mensaje confirmado por servidor");
            System.out.println("   → ID: " + mensaje.getMensajeId());
            System.out.println("   → Fecha: " + mensaje.getFechaEnvio());
            System.out.println("   → Tipo: " + mensaje.getTipo());

            mensaje.setEsMio(true);
            notificarObservadores("MENSAJE_ENVIADO_EXITOSO", mensaje);
        } else {
            String errorMsg = r.getMessage();
            System.err.println("❌ [GestionMensajes]: Error en respuesta de envío: " + errorMsg);

            if (errorMsg.contains("Destinatario no encontrado") || errorMsg.contains("desconectado")) {
                notificarObservadores("ERROR_DESTINATARIO_NO_DISPONIBLE", errorMsg);
            } else if (errorMsg.contains("inválidos") || errorMsg.contains("Datos de mensaje inválidos")) {
                notificarObservadores("ERROR_VALIDACION", r.getData() != null ? r.getData() : errorMsg);
            } else {
                notificarObservadores("ERROR_ENVIO_MENSAJE", errorMsg);
            }
        }
    }

    private void manejarRespuestaEnvioMensajeAudio(DTOResponse r) {
        System.out.println("📥 [GestionMensajes]: Recibida RESPUESTA de envío de mensaje de audio - Status: " + r.getStatus());

        if (r.fueExitoso()) {
            DTOMensaje mensaje = gson.fromJson(gson.toJson(r.getData()), DTOMensaje.class);
            determinarTipoMensaje(mensaje);

            System.out.println("✅ [GestionMensajes]: Mensaje de audio confirmado por servidor");
            System.out.println("   → ID: " + mensaje.getMensajeId());
            System.out.println("   → Fecha: " + mensaje.getFechaEnvio());
            System.out.println("   → FileId: " + mensaje.getFileId());

            mensaje.setEsMio(true);
            notificarObservadores("MENSAJE_AUDIO_ENVIADO_EXITOSO", mensaje);
        } else {
            String errorMsg = r.getMessage();
            System.err.println("❌ [GestionMensajes]: Error en respuesta de envío de mensaje de audio: " + errorMsg);

            if (errorMsg.contains("Destinatario no encontrado") || errorMsg.contains("desconectado")) {
                notificarObservadores("ERROR_DESTINATARIO_NO_DISPONIBLE", errorMsg);
            } else if (errorMsg.contains("inválidos") || errorMsg.contains("Datos de mensaje inválidos")) {
                notificarObservadores("ERROR_VALIDACION", r.getData() != null ? r.getData() : errorMsg);
            } else {
                notificarObservadores("ERROR_ENVIO_MENSAJE_AUDIO", errorMsg);
            }
        }
    }

    private void manejarNuevoMensajePush(DTOResponse r) {
        System.out.println("📥 [GestionMensajes]: Recibido PUSH de nuevo mensaje directo");

        if (!r.fueExitoso()) {
            System.err.println("❌ [GestionMensajes]: Push de mensaje con error: " + r.getMessage());
            notificarObservadores("ERROR_NOTIFICACION_MENSAJE", r.getMessage());
            return;
        }

        // Usar el método de mapeo para convertir la estructura del servidor al DTO esperado
        DTOMensaje mensaje = mapearMensajeDesdeServidor(r.getData());
        determinarTipoMensaje(mensaje);

        String myUserId = gestorSesionUsuario.getUserId();
        String myPeerId = gestorSesionUsuario.getPeerId();

        // Marcar si el mensaje es mío
        boolean esMio = myUserId != null && myUserId.equals(mensaje.getRemitenteId());
        mensaje.setEsMio(esMio);

        // Null-safe peer destination filter - solo filtrar si NO es mío
        if (!esMio && myPeerId != null && mensaje.getPeerDestinoId() != null &&
                !myPeerId.equals(mensaje.getPeerDestinoId())) {
            System.out.println("⏩ [GestionMensajes]: Ignorando mensaje dirigido a otro peer");
            return;
        }

        System.out.println("✅ [GestionMensajes]: Nuevo mensaje privado recibido");
        System.out.println("   → De: " + mensaje.getRemitenteId() + (esMio ? " (YO)" : ""));
        System.out.println("   → Tipo: " + mensaje.getTipo());
        System.out.println("   → Contenido: " + mensaje.getContenido());

        notificarObservadores("NUEVO_MENSAJE_PRIVADO", mensaje);
    }

    private void manejarNuevoMensajeAudioPush(DTOResponse r) {
        System.out.println("📥 [GestionMensajes]: Recibido PUSH de nuevo mensaje de audio");

        if (!r.fueExitoso()) {
            System.err.println("❌ [GestionMensajes]: Push de audio con error: " + r.getMessage());
            notificarObservadores("ERROR_NOTIFICACION_MENSAJE_AUDIO", r.getMessage());
            return;
        }

        // Usar el método de mapeo para convertir la estructura del servidor al DTO esperado
        DTOMensaje mensaje = mapearMensajeDesdeServidor(r.getData());
        determinarTipoMensaje(mensaje);

        String myUserId = gestorSesionUsuario.getUserId();
        String myPeerId = gestorSesionUsuario.getPeerId();

        // Marcar si el mensaje es mío
        boolean esMio = myUserId != null && myUserId.equals(mensaje.getRemitenteId());
        mensaje.setEsMio(esMio);

        // Null-safe peer destination filter - solo filtrar si NO es mío
        if (!esMio && myPeerId != null && mensaje.getPeerDestinoId() != null &&
                !myPeerId.equals(mensaje.getPeerDestinoId())) {
            System.out.println("⏩ [GestionMensajes]: Ignorando mensaje de audio dirigido a otro peer");
            return;
        }

        System.out.println("✅ [GestionMensajes]: Nuevo mensaje de audio recibido");
        System.out.println("   → De: " + mensaje.getRemitenteId() + (esMio ? " (YO)" : ""));
        System.out.println("   → Tipo: " + mensaje.getTipo());
        // El contenido de audio en PUSH viene en Base64 (data:audio/webm;base64,...)
        if (mensaje.getContenido() != null && mensaje.getContenido().startsWith("data:audio")) {
            System.out.println("   → Audio Base64: Sí (listo para reproducir)");
        } else {
            System.out.println("   → Contenido: " + mensaje.getContenido());
        }

        notificarObservadores("NUEVO_MENSAJE_AUDIO_PRIVADO", mensaje);
    }

    private void manejarHistorial(DTOResponse r) {
        System.out.println("📥 [GestionMensajes]: Recibida respuesta de historial - Status: " + r.getStatus());

        if (r.fueExitoso()) {
            Type listType = new TypeToken<List<DTOMensaje>>(){}.getType();
            List<DTOMensaje> mensajes = gson.fromJson(gson.toJson(r.getData()), listType);

            if (mensajes == null) {
                mensajes = new ArrayList<>();
            }

            String myUserId = gestorSesionUsuario.getUserId();
            for (DTOMensaje mensaje : mensajes) {
                // Null-safe esMio assignment
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

    @Override
    public void registrarObservador(IObservador o) {
        if (!observadores.contains(o)) {
            observadores.add(o);
            System.out.println("🔔 [GestionMensajes]: Observador registrado - Total: " + observadores.size());
        }
    }

    @Override
    public void removerObservador(IObservador o) {
        observadores.remove(o);
        System.out.println("🔕 [GestionMensajes]: Observador removido - Total: " + observadores.size());
    }

    @Override
    public void notificarObservadores(String t, Object d) {
        System.out.println("📢 [GestionMensajes]: Notificando a " + observadores.size() + " observadores - Tipo: " + t);
        for (IObservador o : observadores) {
            o.actualizar(t, d);
        }
    }
}
