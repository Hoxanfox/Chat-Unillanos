package gestorClientes.servicios;

import com.google.gson.Gson;
import conexion.clientes.interfaces.IGestorConexionesCliente;
import conexion.clientes.interfaces.IRouterMensajesCliente;
import dominio.clienteServidor.Canal;
import dominio.clienteServidor.Usuario;
import dto.comunicacion.DTOResponse;
import gestorClientes.interfaces.IServicioCliente;
import logger.LoggerCentral;
import repositorio.clienteServidor.CanalRepositorio;
import repositorio.clienteServidor.UsuarioRepositorio;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio del servidor para notificar invitaciones a canales.
 * Envía notificaciones push a los usuarios invitados con información detallada del canal.
 *
 * Este servicio NO modifica el estado de la base de datos, solo envía notificaciones.
 * Por lo tanto, NO requiere sincronización P2P.
 */
public class ServicioNotificarInvitacionCanal implements IServicioCliente {

    private static final String TAG = "NotificarInvitacionCanal";

    // Colores ANSI para logs
    private static final String RESET = "\u001B[0m";
    private static final String VERDE = "\u001B[32m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String AZUL = "\u001B[34m";
    private static final String MAGENTA = "\u001B[35m";

    private IGestorConexionesCliente gestor;
    private final CanalRepositorio canalRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final Gson gson;

    public ServicioNotificarInvitacionCanal() {
        this.canalRepositorio = new CanalRepositorio();
        this.usuarioRepositorio = new UsuarioRepositorio();
        this.gson = new Gson();
        LoggerCentral.info(TAG, AZUL + "Constructor: ServicioNotificarInvitacionCanal creado" + RESET);
    }

    @Override
    public String getNombre() {
        return "ServicioNotificarInvitacionCanal";
    }

    @Override
    public void inicializar(IGestorConexionesCliente gestor, IRouterMensajesCliente router) {
        this.gestor = gestor;
        LoggerCentral.info(TAG, VERDE + "✅ Servicio de notificaciones de invitación a canal inicializado" + RESET);
    }

    /**
     * Envía una notificación push al usuario invitado sobre una invitación a un canal.
     * El cliente espera recibir un mensaje tipo "notificacionInvitacionCanal" con:
     * - channelId
     * - channelName
     * - owner (información del invitador)
     *
     * @param canalId El ID del canal al que fue invitado
     * @param usuarioInvitadoId El ID del usuario que recibe la invitación
     * @param usuarioInvitadorId El ID del usuario que envió la invitación
     */
    public void notificarInvitacion(String canalId, String usuarioInvitadoId, String usuarioInvitadorId) {
        if (canalId == null || usuarioInvitadoId == null || usuarioInvitadorId == null) {
            LoggerCentral.warn(TAG, AMARILLO + "Parámetros nulos, no se puede notificar" + RESET);
            return;
        }

        LoggerCentral.info(TAG, CYAN + "📬 Notificando invitación a canal" + RESET);
        LoggerCentral.info(TAG, "   → Canal: " + canalId);
        LoggerCentral.info(TAG, "   → Invitado: " + usuarioInvitadoId);
        LoggerCentral.info(TAG, "   → Invitador: " + usuarioInvitadorId);

        try {
            // Obtener información del canal
            Canal canal = canalRepositorio.obtenerPorId(UUID.fromString(canalId));
            if (canal == null) {
                LoggerCentral.warn(TAG, AMARILLO + "Canal no encontrado: " + canalId + RESET);
                return;
            }

            // Obtener información del invitador
            Usuario invitador = usuarioRepositorio.buscarPorId(usuarioInvitadorId);
            if (invitador == null) {
                LoggerCentral.warn(TAG, AMARILLO + "Usuario invitador no encontrado: " + usuarioInvitadorId + RESET);
                return;
            }

            // Construir el DTO de la invitación según lo que espera el cliente
            Map<String, Object> invitacionData = construirInvitacionDTO(canal, invitador);

            // Crear DTOResponse con tipo "notificacionInvitacionCanal"
            DTOResponse notificacion = new DTOResponse(
                "notificacionInvitacionCanal",
                "success",
                "Nueva invitación a canal",
                gson.toJsonTree(invitacionData)
            );

            // Serializar a JSON
            String mensajeJson = gson.toJson(notificacion);

            // Enviar notificación push al usuario invitado
            gestor.enviarMensajeAUsuario(usuarioInvitadoId, mensajeJson);

            LoggerCentral.info(TAG, VERDE + "✅ Notificación de invitación enviada exitosamente" + RESET);
            LoggerCentral.info(TAG, MAGENTA + "   → Usuario invitado notificado sobre el canal '" + canal.getNombre() + "'" + RESET);

        } catch (Exception e) {
            LoggerCentral.error(TAG, "❌ Error enviando notificación de invitación: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Construye el DTO de la invitación con toda la información necesaria para el cliente.
     * Estructura esperada por el cliente:
     * {
     *   "channelId": "...",
     *   "channelName": "...",
     *   "owner": {
     *     "userId": "...",
     *     "username": "...",
     *     "userPhoto": "..."
     *   }
     * }
     */
    private Map<String, Object> construirInvitacionDTO(Canal canal, Usuario invitador) {
        Map<String, Object> dto = new HashMap<>();

        // Información del canal
        dto.put("channelId", canal.getId());
        dto.put("channelName", canal.getNombre());

        // Información del invitador (owner)
        Map<String, String> owner = new HashMap<>();
        owner.put("userId", invitador.getId());
        owner.put("username", invitador.getNombre());
        owner.put("userPhoto", invitador.getFoto() != null ? invitador.getFoto() : "");

        dto.put("owner", owner);

        return dto;
    }

    @Override
    public void iniciar() {
        LoggerCentral.info(TAG, VERDE + "Servicio de notificaciones de invitación a canal iniciado" + RESET);
    }

    @Override
    public void detener() {
        LoggerCentral.info(TAG, AMARILLO + "Servicio de notificaciones de invitación a canal detenido" + RESET);
    }
}
