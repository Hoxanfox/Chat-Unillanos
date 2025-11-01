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
        this.gestorRespuesta.registrarManejador("solicitarHistorialPrivado", this::manejarHistorial);

        // Registrar manejadores para notificaciones PUSH del servidor
        this.gestorRespuesta.registrarManejador("nuevoMensajeDirecto", this::manejarNuevoMensajePush);

        System.out.println("✅ [GestionMensajes]: Gestor inicializado con manejadores registrados");
        System.out.println("   → Respuestas: enviarMensajeDirecto, solicitarHistorialPrivado");
        System.out.println("   → Push: nuevoMensajeDirecto");
    }

    @Override
    public void solicitarHistorial(String contactoId) {
        String userId = gestorSesionUsuario.getUserId();
        System.out.println("📡 [GestionMensajes]: Solicitando historial de mensajes");
        System.out.println("   → UserId: " + userId);
        System.out.println("   → ContactoId: " + contactoId);

        // Crear el DTO correcto con ambos IDs
        DTOSolicitarHistorial payload = new DTOSolicitarHistorial(userId, contactoId);
        DTORequest peticion = new DTORequest("solicitarHistorialPrivado", payload);
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
            System.err.println("❌ [GestionMensajes]: No se pudo obtener el peerId del destinatario");
            notificarObservadores("ERROR_PEER_NO_ENCONTRADO", "El contacto no está disponible");
            return CompletableFuture.completedFuture(null);
        }

        DTOEnviarMensaje payload = DTOEnviarMensaje.deTexto(peerRemitenteId, peerDestinoId, remitenteId, destinatarioId, contenido);
        DTORequest peticion = new DTORequest("enviarMensajeDirecto", payload);
        enviadorPeticiones.enviar(peticion);

        System.out.println("✅ [GestionMensajes]: Mensaje de texto enviado al servidor");
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> enviarMensajeAudio(String destinatarioId, String audioFileId) {
        String remitenteId = gestorSesionUsuario.getUserId();
        String peerRemitenteId = gestorSesionUsuario.getPeerId();
        String peerDestinoId = gestorContactoPeers.getPeerIdDeContacto(destinatarioId);

        System.out.println("📤 [GestionMensajes]: Enviando mensaje de AUDIO");
        System.out.println("   → Remitente: " + remitenteId + " (Peer: " + peerRemitenteId + ")");
        System.out.println("   → Destinatario: " + destinatarioId + " (Peer: " + peerDestinoId + ")");
        System.out.println("   → AudioFileId: " + audioFileId);

        if (peerDestinoId == null) {
            System.err.println("❌ [GestionMensajes]: No se pudo obtener el peerId del destinatario");
            notificarObservadores("ERROR_PEER_NO_ENCONTRADO", "El contacto no está disponible");
            return CompletableFuture.completedFuture(null);
        }

        DTOEnviarMensaje payload = DTOEnviarMensaje.deAudio(peerRemitenteId, peerDestinoId, remitenteId, destinatarioId, audioFileId, audioFileId);
        DTORequest peticion = new DTORequest("enviarMensajeDirecto", payload);
        enviadorPeticiones.enviar(peticion);

        System.out.println("✅ [GestionMensajes]: Mensaje de audio enviado al servidor");
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Envía un mensaje con imagen adjunta.
     */
    public CompletableFuture<Void> enviarMensajeImagen(String destinatarioId, String contenido,
                                                        String imageFileId, String fileName) {
        String remitenteId = gestorSesionUsuario.getUserId();
        String peerRemitenteId = gestorSesionUsuario.getPeerId();
        String peerDestinoId = gestorContactoPeers.getPeerIdDeContacto(destinatarioId);

        System.out.println("📤 [GestionMensajes]: Enviando mensaje de IMAGEN");
        System.out.println("   → Remitente: " + remitenteId + " (Peer: " + peerRemitenteId + ")");
        System.out.println("   → Destinatario: " + destinatarioId + " (Peer: " + peerDestinoId + ")");
        System.out.println("   → ImageFileId: " + imageFileId);
        System.out.println("   → FileName: " + fileName);

        if (peerDestinoId == null) {
            System.err.println("❌ [GestionMensajes]: No se pudo obtener el peerId del destinatario");
            notificarObservadores("ERROR_PEER_NO_ENCONTRADO", "El contacto no está disponible");
            return CompletableFuture.completedFuture(null);
        }

        DTOEnviarMensaje payload = DTOEnviarMensaje.deImagen(peerRemitenteId, peerDestinoId, remitenteId, destinatarioId, contenido, imageFileId, fileName);
        DTORequest peticion = new DTORequest("enviarMensajeDirecto", payload);
        enviadorPeticiones.enviar(peticion);

        System.out.println("✅ [GestionMensajes]: Mensaje de imagen enviado al servidor");
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Envía un mensaje con archivo adjunto.
     */
    public CompletableFuture<Void> enviarMensajeArchivo(String destinatarioId, String contenido,
                                                         String fileId, String fileName) {
        String remitenteId = gestorSesionUsuario.getUserId();
        String peerRemitenteId = gestorSesionUsuario.getPeerId();
        String peerDestinoId = gestorContactoPeers.getPeerIdDeContacto(destinatarioId);

        System.out.println("📤 [GestionMensajes]: Enviando mensaje de ARCHIVO");
        System.out.println("   → Remitente: " + remitenteId + " (Peer: " + peerRemitenteId + ")");
        System.out.println("   → Destinatario: " + destinatarioId + " (Peer: " + peerDestinoId + ")");
        System.out.println("   → FileId: " + fileId);
        System.out.println("   → FileName: " + fileName);

        if (peerDestinoId == null) {
            System.err.println("❌ [GestionMensajes]: No se pudo obtener el peerId del destinatario");
            notificarObservadores("ERROR_PEER_NO_ENCONTRADO", "El contacto no está disponible");
            return CompletableFuture.completedFuture(null);
        }

        DTOEnviarMensaje payload = DTOEnviarMensaje.deArchivo(peerRemitenteId, peerDestinoId, remitenteId, destinatarioId, contenido, fileId, fileName);
        DTORequest peticion = new DTORequest("enviarMensajeDirecto", payload);
        enviadorPeticiones.enviar(peticion);

        System.out.println("✅ [GestionMensajes]: Mensaje de archivo enviado al servidor");
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Maneja la RESPUESTA del servidor después de enviar un mensaje.
     * Esta es la confirmación de que el mensaje fue enviado exitosamente.
     */
    private void manejarRespuestaEnvioMensaje(DTOResponse r) {
        System.out.println("📥 [GestionMensajes]: Recibida RESPUESTA de envío de mensaje - Status: " + r.getStatus());

        if(r.fueExitoso()) {
            DTOMensaje mensaje = gson.fromJson(gson.toJson(r.getData()), DTOMensaje.class);
            System.out.println("✅ [GestionMensajes]: Mensaje confirmado por servidor");
            System.out.println("   → ID: " + mensaje.getMensajeId());
            System.out.println("   → Fecha: " + mensaje.getFechaEnvio());

            // Marcar como "es mío" ya que es el mensaje que nosotros enviamos
            mensaje.setEsMio(true);

            // Notificar a los observadores que el mensaje fue enviado exitosamente
            notificarObservadores("MENSAJE_ENVIADO_EXITOSO", mensaje);
        } else {
            // Manejo granular de errores según la especificación
            String errorMsg = r.getMessage();
            System.err.println("❌ [GestionMensajes]: Error en respuesta de envío: " + errorMsg);

            if (errorMsg.contains("Destinatario no encontrado") || errorMsg.contains("desconectado")) {
                notificarObservadores("ERROR_DESTINATARIO_NO_DISPONIBLE", errorMsg);
            } else if (errorMsg.contains("inválidos") || errorMsg.contains("Datos de mensaje inválidos")) {
                // Intentar extraer detalles del error de validación
                notificarObservadores("ERROR_VALIDACION", r.getData() != null ? r.getData() : errorMsg);
            } else {
                notificarObservadores("ERROR_ENVIO_MENSAJE", errorMsg);
            }
        }
    }

    /**
     * Maneja las NOTIFICACIONES PUSH de nuevos mensajes directos.
     * Esto se ejecuta cuando otro usuario nos envía un mensaje.
     * Ahora incluye información de peers WebRTC y filtrado de duplicados.
     */
    private void manejarNuevoMensajePush(DTOResponse r) {
        System.out.println("🔔 [GestionMensajes]: Recibida NOTIFICACIÓN PUSH de nuevo mensaje - Status: " + r.getStatus());

        if(r.fueExitoso()) {
            DTOMensaje mensaje = gson.fromJson(gson.toJson(r.getData()), DTOMensaje.class);

            String myUserId = gestorSesionUsuario.getUserId();
            String myPeerId = gestorSesionUsuario.getPeerId();

            System.out.println("✅ [GestionMensajes]: Nuevo mensaje recibido");
            System.out.println("   → De: " + mensaje.getRemitenteNombre() + " (" + mensaje.getRemitenteId() + ")");
            System.out.println("   → Peer Remitente: " + mensaje.getPeerRemitenteId());
            System.out.println("   → Peer Destino: " + mensaje.getPeerDestinoId());
            System.out.println("   → Tipo: " + mensaje.getTipo());
            System.out.println("   → Contenido: " + (mensaje.getContenido() != null ? mensaje.getContenido() : "[archivo]"));
            System.out.println("   → Fecha: " + mensaje.getFechaEnvio());

            // ✅ FILTRO 1: Ignorar pushes de mis propios mensajes (ya procesados en respuesta)
            boolean esMio = mensaje.getRemitenteId().equals(myUserId);
            if (esMio) {
                System.out.println("⚠️ [GestionMensajes]: Ignorando push de mi propio mensaje (ya procesado)");
                return;
            }

            // ✅ FILTRO 2: Validar que el mensaje es para mi peer actual
            if (myPeerId != null && mensaje.getPeerDestinoId() != null &&
                !mensaje.getPeerDestinoId().equals(myPeerId)) {
                System.out.println("⚠️ [GestionMensajes]: Mensaje no es para mi peer actual");
                System.out.println("   → Peer destino del mensaje: " + mensaje.getPeerDestinoId());
                System.out.println("   → Mi peer actual: " + myPeerId);
                return;
            }

            // Marcar como mensaje del otro usuario
            mensaje.setEsMio(false);

            // Notificar a los observadores que llegó un nuevo mensaje
            System.out.println("📢 [GestionMensajes]: Notificando nuevo mensaje de: " + mensaje.getRemitenteNombre());
            notificarObservadores("NUEVO_MENSAJE_PRIVADO", mensaje);

        } else {
            String errorMsg = r.getMessage();
            System.err.println("❌ [GestionMensajes]: Error en notificación push: " + errorMsg);
            notificarObservadores("ERROR_NOTIFICACION_MENSAJE", errorMsg);
        }
    }

    /**
     * Maneja la respuesta del servidor con el historial de mensajes.
     */
    private void manejarHistorial(DTOResponse r) {
        System.out.println("📥 [GestionMensajes]: Recibida respuesta de historial - Status: " + r.getStatus());

        if(r.fueExitoso()) {
            Type listType = new TypeToken<List<DTOMensaje>>(){}.getType();
            List<DTOMensaje> mensajes = gson.fromJson(gson.toJson(r.getData()), listType);

            String myUserId = gestorSesionUsuario.getUserId();

            // Marcar cada mensaje como "mío" o "del otro"
            for (DTOMensaje mensaje : mensajes) {
                mensaje.setEsMio(mensaje.getRemitenteId().equals(myUserId));
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
