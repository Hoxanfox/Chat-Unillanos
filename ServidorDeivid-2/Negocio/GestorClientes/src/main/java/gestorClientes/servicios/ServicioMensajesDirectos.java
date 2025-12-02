package gestorClientes.servicios;

import com.google.gson.Gson;
import conexion.clientes.interfaces.IGestorConexionesCliente;
import conexion.clientes.interfaces.IRouterMensajesCliente;
import dominio.clienteServidor.Mensaje;
import dto.comunicacion.DTOResponse;
import dto.mensajeria.DTOEnviarMensaje;
import dto.mensajeria.DTOSolicitarHistorial;
import gestorClientes.interfaces.IServicioCliente;
import gestorP2P.servicios.ServicioSincronizacionDatos;
import logger.LoggerCentral;
import repositorio.clienteServidor.MensajeRepositorio;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio del servidor para gestionar mensajes de TEXTO directos entre contactos.
 * Maneja envío, almacenamiento, historial y señales de actualización.
 * Integrado con sincronización P2P.
 */
public class ServicioMensajesDirectos implements IServicioCliente {

    private static final String TAG = "MensajesDirectos";

    // Colores ANSI para logs
    private static final String RESET = "\u001B[0m";
    private static final String VERDE = "\u001B[32m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String ROJO = "\u001B[31m";
    private static final String AZUL = "\u001B[34m";

    private IGestorConexionesCliente gestor;
    private final MensajeRepositorio repoMensaje;
    private final Gson gson;

    // Referencias a servicios
    private ServicioNotificacionCliente servicioNotificacion;
    private ServicioSincronizacionDatos servicioSyncP2P;

    public ServicioMensajesDirectos() {
        this.repoMensaje = new MensajeRepositorio();
        this.gson = new Gson();
        LoggerCentral.info(TAG, AZUL + "Constructor: ServicioMensajesDirectos creado" + RESET);
    }

    /**
     * Inyecta el servicio de notificaciones CS.
     */
    public void setServicioNotificacion(ServicioNotificacionCliente servicioNotificacion) {
        this.servicioNotificacion = servicioNotificacion;
        LoggerCentral.info(TAG, VERDE + "Servicio de notificaciones CS configurado" + RESET);
    }

    /**
     * ✅ NUEVO: Inyecta el servicio de sincronización P2P.
     */
    public void setServicioSincronizacionP2P(ServicioSincronizacionDatos servicioSyncP2P) {
        this.servicioSyncP2P = servicioSyncP2P;
        LoggerCentral.info(TAG, VERDE + "Servicio de sincronización P2P configurado" + RESET);
    }

    @Override
    public String getNombre() {
        return "ServicioMensajesDirectos";
    }

    @Override
    public void inicializar(IGestorConexionesCliente gestor, IRouterMensajesCliente router) {
        this.gestor = gestor;
        LoggerCentral.info(TAG, AZUL + "Inicializando ServicioMensajesDirectos..." + RESET);

        // ==================== RUTA: Enviar Mensaje de Texto ====================
        router.registrarAccion("enviarmensajedirecto", (datos, idSesion) -> {
            try {
                LoggerCentral.info(TAG, CYAN + "📥 Recibida petición de envío de mensaje de texto" + RESET);

                // Validar autenticación
                String userId = gestor.obtenerUsuarioDeSesion(idSesion);
                if (userId == null) {
                    LoggerCentral.warn(TAG, AMARILLO + "Usuario no autenticado" + RESET);
                    return new DTOResponse("enviarmensajedirecto", "error", "Usuario no autenticado", null);
                }

                // Parsear datos
                DTOEnviarMensaje dto = gson.fromJson(datos, DTOEnviarMensaje.class);

                // Validar datos
                if (dto.getRemitenteId() == null || dto.getDestinatarioId() == null || dto.getContenido() == null) {
                    LoggerCentral.warn(TAG, AMARILLO + "Datos de mensaje inválidos" + RESET);
                    return new DTOResponse("enviarmensajedirecto", "error", "Datos de mensaje inválidos", null);
                }

                // Validar que el remitente sea el usuario autenticado
                if (!userId.equals(dto.getRemitenteId())) {
                    LoggerCentral.warn(TAG, ROJO + "Usuario no autorizado para enviar desde: " + dto.getRemitenteId() + RESET);
                    return new DTOResponse("enviarmensajedirecto", "error", "No autorizado", null);
                }

                LoggerCentral.info(TAG, CYAN + "📤 Enviando mensaje de TEXTO" + RESET);
                LoggerCentral.info(TAG, "   → De: " + dto.getRemitenteId() + " (Peer: " + dto.getPeerRemitenteId() + ")");
                LoggerCentral.info(TAG, "   → Para: " + dto.getDestinatarioId() + " (Peer: " + dto.getPeerDestinoId() + ")");
                LoggerCentral.info(TAG, "   → Contenido: " + dto.getContenido());

                // Crear mensaje en BD
                Mensaje mensaje = new Mensaje();
                mensaje.setId(UUID.randomUUID());
                mensaje.setRemitenteId(UUID.fromString(dto.getRemitenteId()));
                mensaje.setDestinatarioUsuarioId(UUID.fromString(dto.getDestinatarioId()));
                mensaje.setPeerRemitenteId(dto.getPeerRemitenteId());
                mensaje.setPeerDestinoId(dto.getPeerDestinoId());
                mensaje.setTipo(Mensaje.Tipo.TEXTO);
                mensaje.setContenido(dto.getContenido());
                mensaje.setFechaEnvio(Instant.now());

                // Guardar en BD
                boolean guardado = repoMensaje.guardar(mensaje);

                if (!guardado) {
                    LoggerCentral.error(TAG, ROJO + "❌ Error al guardar mensaje en BD" + RESET);
                    return new DTOResponse("enviarmensajedirecto", "error", "Error al guardar mensaje", null);
                }

                LoggerCentral.info(TAG, VERDE + "✅ Mensaje guardado en BD - ID: " + mensaje.getId() + RESET);

                // ✅ 1. SIGNAL_UPDATE a clientes CS
                if (servicioNotificacion != null) {
                    servicioNotificacion.actualizar("NUEVO_MENSAJE", null);
                    LoggerCentral.info(TAG, VERDE + "✅ SIGNAL_UPDATE enviado a clientes CS" + RESET);
                }

                // ✅ 2. Activar sincronización P2P
                if (servicioSyncP2P != null) {
                    LoggerCentral.info(TAG, CYAN + "🔄 Activando sincronización P2P..." + RESET);
                    servicioSyncP2P.onBaseDeDatosCambio();
                    servicioSyncP2P.forzarSincronizacion();
                    LoggerCentral.info(TAG, VERDE + "✅ Sincronización P2P activada" + RESET);
                }

                // Preparar respuesta para el remitente
                Map<String, Object> respuestaRemitente = construirRespuestaMensaje(mensaje);


                return new DTOResponse("enviarmensajedirecto", "success", "Mensaje enviado", gson.toJsonTree(respuestaRemitente));

            } catch (Exception e) {
                LoggerCentral.error(TAG, ROJO + "❌ Error en enviarmensajedirecto: " + e.getMessage() + RESET);
                return new DTOResponse("enviarmensajedirecto", "error", "Error interno del servidor", null);
            }
        });

        // ==================== RUTA: Solicitar Historial ====================
        router.registrarAccion("solicitarhistorialprivado", (datos, idSesion) -> {
            try {
                LoggerCentral.info(TAG, CYAN + "📥 Recibida petición de historial privado" + RESET);

                // Validar autenticación
                String userId = gestor.obtenerUsuarioDeSesion(idSesion);
                if (userId == null) {
                    LoggerCentral.warn(TAG, AMARILLO + "Usuario no autenticado" + RESET);
                    return new DTOResponse("solicitarhistorialprivado", "error", "Usuario no autenticado", null);
                }

                // Parsear datos
                DTOSolicitarHistorial dto = gson.fromJson(datos, DTOSolicitarHistorial.class);

                // Validar que el usuario sea participante de la conversación
                if (!userId.equals(dto.getRemitenteId()) && !userId.equals(dto.getDestinatarioId())) {
                    LoggerCentral.warn(TAG, ROJO + "Usuario no autorizado para ver este historial" + RESET);
                    return new DTOResponse("solicitarhistorialprivado", "error", "No autorizado", null);
                }

                LoggerCentral.info(TAG, CYAN + "📋 Consultando historial" + RESET);
                LoggerCentral.info(TAG, "   → Usuario1: " + dto.getRemitenteId());
                LoggerCentral.info(TAG, "   → Usuario2: " + dto.getDestinatarioId());

                // Obtener historial de BD
                List<Mensaje> mensajes = repoMensaje.obtenerHistorialEntre(
                    dto.getRemitenteId(),
                    dto.getDestinatarioId()
                );

                LoggerCentral.info(TAG, VERDE + "✅ Historial obtenido: " + mensajes.size() + " mensajes" + RESET);

                // Convertir a formato de respuesta
                List<Map<String, Object>> historial = mensajes.stream()
                    .map(this::construirRespuestaMensaje)
                    .toList();

                return new DTOResponse("solicitarhistorialprivado", "success", "Historial obtenido", gson.toJsonTree(historial));

            } catch (Exception e) {
                LoggerCentral.error(TAG, ROJO + "❌ Error en solicitarhistorialprivado: " + e.getMessage() + RESET);
                return new DTOResponse("solicitarhistorialprivado", "error", "Error interno del servidor", null);
            }
        });

        LoggerCentral.info(TAG, VERDE + "✅ ServicioMensajesDirectos inicializado" + RESET);
        LoggerCentral.info(TAG, "   → Rutas registradas: enviarmensajedirecto, solicitarhistorialprivado");
    }

    @Override
    public void iniciar() {
        LoggerCentral.info(TAG, VERDE + "Servicio de mensajes directos iniciado" + RESET);
    }

    @Override
    public void detener() {
        LoggerCentral.info(TAG, AMARILLO + "Servicio de mensajes directos detenido" + RESET);
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Construye la respuesta de un mensaje para enviar al cliente.
     */
    private Map<String, Object> construirRespuestaMensaje(Mensaje mensaje) {
        Map<String, Object> response = new HashMap<>();
        response.put("mensajeId", mensaje.getId());
        response.put("remitenteId", mensaje.getRemitenteId() != null ? mensaje.getRemitenteId().toString() : null);
        response.put("destinatarioId", mensaje.getDestinatarioUsuarioId() != null ? mensaje.getDestinatarioUsuarioId().toString() : null);
        response.put("peerRemitenteId", mensaje.getPeerRemitenteId());
        response.put("peerDestinoId", mensaje.getPeerDestinoId());
        response.put("tipo", mensaje.getTipo() != null ? mensaje.getTipo().name().toLowerCase() : "texto");
        response.put("contenido", mensaje.getContenido());
        response.put("fechaEnvio", mensaje.getFechaEnvio() != null ? mensaje.getFechaEnvio().toString() : null);
        return response;
    }
}
