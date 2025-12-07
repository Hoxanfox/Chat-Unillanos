package gestorClientes.servicios;

import com.google.gson.Gson;
import conexion.clientes.interfaces.IGestorConexionesCliente;
import conexion.clientes.interfaces.IRouterMensajesCliente;
import dominio.clienteServidor.Canal;
import dominio.clienteServidor.Usuario;
import dominio.clienteServidor.relaciones.CanalInvitacion;
import dto.comunicacion.DTOResponse;
import gestorClientes.interfaces.IServicioCliente;
import logger.LoggerCentral;
import repositorio.clienteServidor.CanalRepositorio;
import repositorio.clienteServidor.CanalInvitacionRepositorio;
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
    private static final String ROJO = "\u001B[31m";

    private IGestorConexionesCliente gestor;
    private final CanalRepositorio canalRepositorio;
    private final CanalInvitacionRepositorio invitacionRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final Gson gson;

    public ServicioNotificarInvitacionCanal() {
        this.canalRepositorio = new CanalRepositorio();
        this.invitacionRepositorio = new CanalInvitacionRepositorio();
        this.usuarioRepositorio = new UsuarioRepositorio();
        this.gson = new Gson();
        LoggerCentral.info(TAG, ROJO + "Constructor: ServicioNotificarInvitacionCanal creado" + RESET);
    }

    @Override
    public String getNombre() {
        return "ServicioNotificarInvitacionCanal";
    }

    @Override
    public void inicializar(IGestorConexionesCliente gestor, IRouterMensajesCliente router) {
        this.gestor = gestor;
        LoggerCentral.info(TAG, ROJO + "✅ Servicio de notificaciones de invitación a canal inicializado" + RESET);
    }

    /**
     * Envía una notificación push al usuario invitado sobre una invitación a un canal.
     * El cliente espera recibir un mensaje tipo "notificacionInvitacionCanal" con la misma
     * estructura que obtenerInvitaciones para consistencia.
     *
     * @param canalId El ID del canal al que fue invitado
     * @param usuarioInvitadoId El ID del usuario que recibe la invitación
     * @param usuarioInvitadorId El ID del usuario que envió la invitación
     */
    public void notificarInvitacion(String canalId, String usuarioInvitadoId, String usuarioInvitadorId) {
        if (canalId == null || usuarioInvitadoId == null || usuarioInvitadorId == null) {
            LoggerCentral.warn(TAG, ROJO + "Parámetros nulos, no se puede notificar" + RESET);
            return;
        }

        LoggerCentral.info(TAG, ROJO + "📬 Notificando invitación a canal" + RESET);
        LoggerCentral.info(TAG, ROJO + "   → Canal: " + canalId + RESET);
        LoggerCentral.info(TAG, ROJO + "   → Invitado: " + usuarioInvitadoId + RESET);
        LoggerCentral.info(TAG, ROJO + "   → Invitador: " + usuarioInvitadorId + RESET);

        try {
            // Obtener información del canal
            Canal canal = canalRepositorio.obtenerPorId(UUID.fromString(canalId));
            if (canal == null) {
                LoggerCentral.warn(TAG, ROJO + "Canal no encontrado: " + canalId + RESET);
                return;
            }

            // Obtener información del invitador
            Usuario invitador = usuarioRepositorio.buscarPorId(usuarioInvitadorId);
            if (invitador == null) {
                LoggerCentral.warn(TAG, ROJO + "Usuario invitador no encontrado: " + usuarioInvitadorId + RESET);
                return;
            }

            // 🆕 Buscar la invitación en la BD para obtener el invitacionId, estado y fecha
            CanalInvitacion invitacion = invitacionRepositorio.buscarInvitacionPendiente(
                UUID.fromString(canalId),
                UUID.fromString(usuarioInvitadoId)
            );

            if (invitacion == null) {
                LoggerCentral.warn(TAG, ROJO + "Invitación no encontrada en BD" + RESET);
                return;
            }

            LoggerCentral.info(TAG, ROJO + "✅ Invitación encontrada - ID: " + invitacion.getId() + RESET);

            // Construir el DTO de la invitación según lo que espera el cliente (igual que obtenerInvitaciones)
            Map<String, Object> invitacionData = construirInvitacionDTO(canal, invitador, invitacion);

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

            LoggerCentral.info(TAG, ROJO + "✅ Notificación de invitación enviada exitosamente" + RESET);
            LoggerCentral.info(TAG, ROJO + "   → Usuario invitado notificado sobre el canal '" + canal.getNombre() + "'" + RESET);

        } catch (Exception e) {
            LoggerCentral.error(TAG, ROJO + "❌ Error enviando notificación de invitación: " + e.getMessage() + RESET);
            e.printStackTrace();
        }
    }

    /**
     * Construye el DTO de la invitación con TODA la información necesaria para el cliente.
     * 🆕 Estructura UNIFICADA con obtenerInvitaciones:
     * {
     *   "invitacionId": "...",
     *   "channelId": "...",
     *   "channelName": "...",
     *   "estado": "PENDIENTE",
     *   "fechaCreacion": "...",
     *   "invitador": {
     *     "userId": "...",
     *     "username": "...",
     *     "userPhoto": "..."
     *   }
     * }
     */
    private Map<String, Object> construirInvitacionDTO(Canal canal, Usuario invitador, CanalInvitacion invitacion) {
        Map<String, Object> dto = new HashMap<>();

        // 🆕 ID de la invitación (importante para responder aceptar/rechazar)
        dto.put("invitacionId", invitacion.getId().toString());

        // Información del canal
        dto.put("channelId", canal.getId().toString());
        dto.put("channelName", canal.getNombre());

        // 🆕 Estado y fecha de la invitación
        dto.put("estado", invitacion.getEstado());
        dto.put("fechaCreacion", invitacion.getFechaCreacion().toString());

        // Información del invitador (ahora usa "invitador" en lugar de "owner" para consistencia)
        Map<String, String> invitadorDTO = new HashMap<>();
        invitadorDTO.put("userId", invitador.getId());
        invitadorDTO.put("username", invitador.getNombre());
        invitadorDTO.put("userPhoto", invitador.getFoto() != null ? invitador.getFoto() : "");

        dto.put("invitador", invitadorDTO);

        LoggerCentral.info(TAG, ROJO + "📦 DTO construido - InvitacionId: " + invitacion.getId() + RESET);

        return dto;
    }

    @Override
    public void iniciar() {
        LoggerCentral.info(TAG, ROJO + "Servicio de notificación de invitaciones iniciado" + RESET);
    }

    @Override
    public void detener() {
        LoggerCentral.info(TAG, ROJO + "Servicio de notificación de invitaciones detenido" + RESET);
    }
}
