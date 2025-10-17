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
import dto.comunicacion.respuesta.DTOHistorialMensajes;
import dto.vistaContactoChat.DTOMensaje;
import gestionUsuario.sesion.GestorSesionUsuario;
import observador.IObservador;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación del gestor de mensajes alineado con la API del servidor.
 * Maneja tanto respuestas a peticiones como notificaciones push.
 */
public class GestionMensajesImpl implements IGestionMensajes {

    private final List<IObservador> observadores = new ArrayList<>();
    private final IEnviadorPeticiones enviadorPeticiones;
    private final IGestorRespuesta gestorRespuesta;
    private final GestorSesionUsuario gestorSesionUsuario;
    private final Gson gson;

    public GestionMensajesImpl() {
        System.out.println("🔧 [GestionMensajes]: Inicializando gestor de mensajes...");

        this.enviadorPeticiones = new EnviadorPeticiones();
        this.gestorRespuesta = GestorRespuesta.getInstancia();
        this.gestorSesionUsuario = GestorSesionUsuario.getInstancia();
        this.gson = new Gson();

        // Registrar manejadores para respuestas a peticiones
        this.gestorRespuesta.registrarManejador("enviarMensajePrivado", this::manejarRespuestaEnvioMensaje);
        this.gestorRespuesta.registrarManejador("enviarMensajeDirecto", this::manejarRespuestaEnvioMensaje); // ← NUEVO: el servidor responde con este action
        this.gestorRespuesta.registrarManejador("solicitarHistorialPrivado", this::manejarHistorial);

        // Registrar manejadores para notificaciones PUSH del servidor
        this.gestorRespuesta.registrarManejador("nuevoMensajeDirecto", this::manejarNuevoMensajePush);

        System.out.println("✅ [GestionMensajes]: Gestor inicializado con manejadores registrados");
        System.out.println("   → Respuestas: enviarMensajePrivado, enviarMensajeDirecto, solicitarHistorialPrivado");
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
        System.out.println("📤 [GestionMensajes]: Enviando mensaje de TEXTO");
        System.out.println("   → Remitente: " + remitenteId);
        System.out.println("   → Destinatario: " + destinatarioId);
        System.out.println("   → Contenido: " + contenido);

        DTOEnviarMensaje payload = DTOEnviarMensaje.deTexto(remitenteId, destinatarioId, contenido);
        DTORequest peticion = new DTORequest("enviarMensajePrivado", payload);
        enviadorPeticiones.enviar(peticion);

        System.out.println("✅ [GestionMensajes]: Mensaje de texto enviado al servidor");
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> enviarMensajeAudio(String destinatarioId, String audioFileId) {
        String remitenteId = gestorSesionUsuario.getUserId();
        System.out.println("📤 [GestionMensajes]: Enviando mensaje de AUDIO");
        System.out.println("   → Remitente: " + remitenteId);
        System.out.println("   → Destinatario: " + destinatarioId);
        System.out.println("   → AudioFileId: " + audioFileId);

        // El audioFileId ya viene del servidor después de subir el archivo
        DTOEnviarMensaje payload = DTOEnviarMensaje.deAudio(remitenteId, destinatarioId, audioFileId, audioFileId);
        DTORequest peticion = new DTORequest("enviarMensajePrivado", payload);
        enviadorPeticiones.enviar(peticion);

        System.out.println("✅ [GestionMensajes]: Mensaje de audio enviado al servidor");
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Envía un mensaje con imagen adjunta.
     * @param destinatarioId UUID del destinatario
     * @param contenido Texto que acompaña la imagen
     * @param imageFileId UUID de la imagen ya subida al servidor
     * @param fileName Nombre del archivo de imagen
     */
    public CompletableFuture<Void> enviarMensajeImagen(String destinatarioId, String contenido,
                                                        String imageFileId, String fileName) {
        String remitenteId = gestorSesionUsuario.getUserId();
        System.out.println("📤 [GestionMensajes]: Enviando mensaje de IMAGEN");
        System.out.println("   → Remitente: " + remitenteId);
        System.out.println("   → Destinatario: " + destinatarioId);
        System.out.println("   → ImageFileId: " + imageFileId);
        System.out.println("   → FileName: " + fileName);

        DTOEnviarMensaje payload = DTOEnviarMensaje.deImagen(remitenteId, destinatarioId, contenido, imageFileId, fileName);
        DTORequest peticion = new DTORequest("enviarMensajePrivado", payload);
        enviadorPeticiones.enviar(peticion);

        System.out.println("✅ [GestionMensajes]: Mensaje de imagen enviado al servidor");
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Envía un mensaje con archivo adjunto.
     * @param destinatarioId UUID del destinatario
     * @param contenido Texto que acompaña el archivo
     * @param fileId UUID del archivo ya subido al servidor
     * @param fileName Nombre del archivo
     */
    public CompletableFuture<Void> enviarMensajeArchivo(String destinatarioId, String contenido,
                                                         String fileId, String fileName) {
        String remitenteId = gestorSesionUsuario.getUserId();
        System.out.println("📤 [GestionMensajes]: Enviando mensaje de ARCHIVO");
        System.out.println("   → Remitente: " + remitenteId);
        System.out.println("   → Destinatario: " + destinatarioId);
        System.out.println("   → FileId: " + fileId);
        System.out.println("   → FileName: " + fileName);

        DTOEnviarMensaje payload = DTOEnviarMensaje.deArchivo(remitenteId, destinatarioId, contenido, fileId, fileName);
        DTORequest peticion = new DTORequest("enviarMensajePrivado", payload);
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
            System.out.println("   → Tipo: " + mensaje.getTipo());
            System.out.println("   → Contenido: " + mensaje.getContenido());

            // Marcar como "es mío" ya que es el mensaje que nosotros enviamos
            mensaje.setEsMio(true);

            // Notificar a los observadores que el mensaje fue enviado exitosamente
            notificarObservadores("MENSAJE_ENVIADO_EXITOSO", mensaje);
        } else {
            System.err.println("❌ [GestionMensajes]: Error en respuesta de envío: " + r.getMessage());
            notificarObservadores("ERROR_ENVIO_MENSAJE", r.getMessage());
        }
    }

    /**
     * Maneja las NOTIFICACIONES PUSH de nuevos mensajes directos.
     * Esto se ejecuta cuando otro usuario nos envía un mensaje.
     */
    private void manejarNuevoMensajePush(DTOResponse r) {
        System.out.println("🔔 [GestionMensajes]: Recibida NOTIFICACIÓN PUSH de nuevo mensaje - Status: " + r.getStatus());
        if(r.fueExitoso()) {
            DTOMensaje mensaje = gson.fromJson(gson.toJson(r.getData()), DTOMensaje.class);
            System.out.println("✅ [GestionMensajes]: Nuevo mensaje recibido");
            System.out.println("   → De: " + mensaje.getRemitenteNombre() + " (" + mensaje.getRemitenteId() + ")");
            System.out.println("   → Tipo: " + mensaje.getTipo());
            System.out.println("   → Contenido: " + mensaje.getContenido());

            // Verificar si el mensaje es para nosotros
            String myUserId = gestorSesionUsuario.getUserId();
            boolean esMio = mensaje.getRemitenteId().equals(myUserId);
            mensaje.setEsMio(esMio);

            // Notificar a los observadores que llegó un nuevo mensaje
            notificarObservadores("NUEVO_MENSAJE_PRIVADO", mensaje);
        } else {
            System.err.println("❌ [GestionMensajes]: Error en notificación push: " + r.getMessage());
        }
    }

    /**
     * Maneja la respuesta del servidor con el historial de mensajes.
     */
    private void manejarHistorial(DTOResponse r) {
        System.out.println("📥 [GestionMensajes]: Recibida respuesta de historial - Status: " + r.getStatus());
        if(r.fueExitoso()) {
            try {
                // El servidor puede enviar la estructura completa o solo el array de mensajes
                DTOHistorialMensajes historialCompleto = gson.fromJson(gson.toJson(r.getData()), DTOHistorialMensajes.class);
                List<DTOMensaje> mensajes = historialCompleto.getMensajes();

                if (mensajes != null) {
                    System.out.println("✅ [GestionMensajes]: Historial parseado correctamente");
                    System.out.println("   → Total mensajes: " + mensajes.size());
                    System.out.println("   → Tiene más: " + historialCompleto.isTieneMas());
                    System.out.println("   → Contacto: " + historialCompleto.getContactoNombre());

                    // Marcar cada mensaje como "mío" o "del otro"
                    String myUserId = gestorSesionUsuario.getUserId();
                    for (DTOMensaje msg : mensajes) {
                        msg.setEsMio(msg.getRemitenteId().equals(myUserId));
                    }

                    notificarObservadores("HISTORIAL_MENSAJES", mensajes);
                } else {
                    System.out.println("⚠️ [GestionMensajes]: El historial no contiene mensajes");
                    notificarObservadores("HISTORIAL_MENSAJES", new ArrayList<>());
                }
            } catch (Exception e) {
                System.err.println("❌ [GestionMensajes]: Error al parsear historial: " + e.getMessage());
                e.printStackTrace();

                // Intentar parsear como array directo
                try {
                    Type t = new TypeToken<ArrayList<DTOMensaje>>(){}.getType();
                    List<DTOMensaje> mensajes = gson.fromJson(gson.toJson(r.getData()), t);

                    String myUserId = gestorSesionUsuario.getUserId();
                    for (DTOMensaje msg : mensajes) {
                        msg.setEsMio(msg.getRemitenteId().equals(myUserId));
                    }

                    System.out.println("✅ [GestionMensajes]: Historial parseado como array - Total: " + mensajes.size());
                    notificarObservadores("HISTORIAL_MENSAJES", mensajes);
                } catch (Exception e2) {
                    System.err.println("❌ [GestionMensajes]: Error al parsear historial como array: " + e2.getMessage());
                    notificarObservadores("ERROR_HISTORIAL", e2.getMessage());
                }
            }
        } else {
            System.err.println("❌ [GestionMensajes]: Error en respuesta de historial: " + r.getMessage());
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
