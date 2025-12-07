package gestorClientes.servicios;

import com.google.gson.Gson;
import conexion.clientes.interfaces.IGestorConexionesCliente;
import conexion.clientes.interfaces.IRouterMensajesCliente;
import dominio.clienteServidor.Mensaje;
import dominio.clienteServidor.Usuario;
import dto.comunicacion.DTOResponse;
import gestorClientes.interfaces.IServicioCliente;
import logger.LoggerCentral;
import repositorio.clienteServidor.CanalMiembroRepositorio;
import repositorio.clienteServidor.UsuarioRepositorio;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio del servidor para notificar nuevos mensajes de canal a los miembros.
 * Envía notificaciones push a todos los miembros conectados del canal.
 */
public class ServicioNotificarMensajeCanal implements IServicioCliente {

    private static final String TAG = "NotificarMensajeCanal";

    // Colores ANSI para logs
    private static final String RESET = "\u001B[0m";
    private static final String VERDE = "\u001B[32m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String AZUL = "\u001B[34m";

    private IGestorConexionesCliente gestor;
    private final CanalMiembroRepositorio repoCanalMiembro;
    private final UsuarioRepositorio repoUsuario;
    private final Gson gson;

    public ServicioNotificarMensajeCanal() {
        this.repoCanalMiembro = new CanalMiembroRepositorio();
        this.repoUsuario = new UsuarioRepositorio();
        this.gson = new Gson();
        LoggerCentral.info(TAG, AZUL + "Constructor: ServicioNotificarMensajeCanal creado" + RESET);
    }

    @Override
    public String getNombre() {
        return "ServicioNotificarMensajeCanal";
    }

    @Override
    public void inicializar(IGestorConexionesCliente gestor, IRouterMensajesCliente router) {
        this.gestor = gestor;
        LoggerCentral.info(TAG, VERDE + "✅ Servicio de notificaciones de canal inicializado" + RESET);
    }

    /**
     * Notifica a todos los miembros de un canal sobre un nuevo mensaje.
     * Este método es llamado por ServicioEnviarMensajeCanal después de guardar el mensaje.
     *
     * @param canalId El ID del canal donde se envió el mensaje
     * @param mensajeId El ID del mensaje que fue enviado
     */
    public void enviarNotificacionCanal(String canalId, String mensajeId) {
        if (canalId == null || mensajeId == null) {
            LoggerCentral.warn(TAG, AMARILLO + "Canal ID o mensaje ID nulo, no se puede notificar" + RESET);
            return;
        }

        LoggerCentral.info(TAG, CYAN + "📢 Notificando nuevo mensaje de canal" + RESET);
        LoggerCentral.info(TAG, "   → Canal: " + canalId);
        LoggerCentral.info(TAG, "   → Mensaje: " + mensajeId);

        // Obtener todos los miembros del canal
        List<String> miembrosIds = repoCanalMiembro.obtenerMiembrosDelCanal(canalId);

        if (miembrosIds.isEmpty()) {
            LoggerCentral.warn(TAG, AMARILLO + "No hay miembros en el canal: " + canalId + RESET);
            return;
        }

        // Construir notificación simple
        Map<String, Object> notificacionData = new HashMap<>();
        notificacionData.put("channelId", canalId);
        notificacionData.put("messageId", mensajeId);

        // Crear DTOResponse
        DTOResponse notificacion = new DTOResponse(
            "nuevoMensajeCanal",
            "success",
            "Nuevo mensaje en canal",
            gson.toJsonTree(notificacionData)
        );

        // Serializar a JSON
        String mensajeJson = gson.toJson(notificacion);

        // Contador de notificaciones enviadas
        int notificacionesEnviadas = 0;

        // Enviar notificación a cada miembro
        for (String miembroId : miembrosIds) {
            try {
                // Enviar mensaje al usuario (el gestor busca su sesión activa)
                gestor.enviarMensajeAUsuario(miembroId, mensajeJson);
                notificacionesEnviadas++;
            } catch (Exception e) {
                LoggerCentral.warn(TAG, "Error enviando notificación a usuario " + miembroId + ": " + e.getMessage());
            }
        }

        LoggerCentral.info(TAG, VERDE + "✅ Notificaciones enviadas: " + notificacionesEnviadas +
                         " de " + miembrosIds.size() + " miembros" + RESET);
    }

    /**
     * Notifica a todos los miembros de un canal sobre un nuevo mensaje (versión extendida).
     * Este método es llamado por ServicioEnviarMensajeCanal después de guardar el mensaje.
     *
     * @param mensaje El mensaje que fue enviado al canal
     * @param tipoMensaje El tipo de mensaje (TEXTO, AUDIO, ARCHIVO, etc.)
     */
    public void notificarNuevoMensaje(Mensaje mensaje, String tipoMensaje) {
        if (mensaje == null || mensaje.getCanalId() == null) {
            LoggerCentral.warn(TAG, AMARILLO + "Mensaje o canal ID nulo, no se puede notificar" + RESET);
            return;
        }

        String canalId = mensaje.getCanalId().toString();
        String remitenteId = mensaje.getRemitenteId().toString();

        LoggerCentral.info(TAG, CYAN + "📢 Notificando nuevo mensaje de canal (versión extendida)" + RESET);
        LoggerCentral.info(TAG, "   → Canal: " + canalId);
        LoggerCentral.info(TAG, "   → Remitente: " + remitenteId);

        // Obtener todos los miembros del canal
        List<String> miembrosIds = repoCanalMiembro.obtenerMiembrosDelCanal(canalId);

        if (miembrosIds.isEmpty()) {
            LoggerCentral.warn(TAG, AMARILLO + "No hay miembros en el canal: " + canalId + RESET);
            return;
        }

        // Obtener información del remitente
        Usuario remitente = repoUsuario.buscarPorId(remitenteId);
        if (remitente == null) {
            LoggerCentral.warn(TAG, AMARILLO + "Remitente no encontrado: " + remitenteId + RESET);
            return;
        }

        // Construir el DTO del mensaje para enviar
        Map<String, Object> mensajeDTO = construirMensajeDTO(mensaje, remitente, tipoMensaje);

        // Crear DTOResponse
        DTOResponse notificacion = new DTOResponse(
            "nuevoMensajeCanal",
            "success",
            "Nuevo mensaje en canal",
            gson.toJsonTree(mensajeDTO)
        );

        // Serializar a JSON
        String mensajeJson = gson.toJson(notificacion);

        // Contador de notificaciones enviadas
        int notificacionesEnviadas = 0;

        // Enviar notificación a cada miembro (excepto al remitente)
        for (String miembroId : miembrosIds) {
            // No notificar al remitente del mensaje
            if (miembroId.equals(remitenteId)) {
                continue;
            }

            try {
                // Enviar mensaje al usuario (el gestor busca su sesión activa)
                gestor.enviarMensajeAUsuario(miembroId, mensajeJson);
                notificacionesEnviadas++;
            } catch (Exception e) {
                LoggerCentral.warn(TAG, "Error enviando notificación a usuario " + miembroId + ": " + e.getMessage());
            }
        }

        LoggerCentral.info(TAG, VERDE + "✅ Notificaciones enviadas: " + notificacionesEnviadas +
                         " de " + (miembrosIds.size() - 1) + " miembros" + RESET);
    }

    /**
     * Construye el DTO del mensaje con toda la información necesaria para el cliente.
     */
    private Map<String, Object> construirMensajeDTO(Mensaje mensaje, Usuario remitente, String tipoMensaje) {
        Map<String, Object> dto = new HashMap<>();

        dto.put("messageId", mensaje.getId().toString());
        dto.put("channelId", mensaje.getCanalId().toString());
        dto.put("messageType", tipoMensaje);
        dto.put("timestamp", mensaje.getFechaEnvio().toString());

        // Contenido según el tipo
        if ("TEXTO".equalsIgnoreCase(tipoMensaje)) {
            dto.put("content", mensaje.getContenido());
        } else {
            dto.put("fileId", mensaje.getContenido());
        }

        // Información del autor
        Map<String, String> author = new HashMap<>();
        author.put("userId", remitente.getId());
        author.put("username", remitente.getNombre());
        author.put("userPhoto", remitente.getFoto());
        dto.put("author", author);

        return dto;
    }

    @Override
    public void iniciar() {
        LoggerCentral.info(TAG, VERDE + "Servicio de notificaciones de canal iniciado" + RESET);
    }

    @Override
    public void detener() {
        LoggerCentral.info(TAG, AMARILLO + "Servicio de notificaciones de canal detenido" + RESET);
    }
}
