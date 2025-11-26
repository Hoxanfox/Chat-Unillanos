package gestorClientes.servicios;

import com.google.gson.Gson;
import conexion.clientes.interfaces.IGestorConexionesCliente;
import conexion.clientes.interfaces.IRouterMensajesCliente;
import dominio.clienteServidor.Mensaje;
import dto.comunicacion.DTOResponse;
import dto.canales.DTOEnviarMensajeCanal;
import gestorClientes.interfaces.IServicioCliente;
import gestorP2P.servicios.ServicioSincronizacionDatos;
import logger.LoggerCentral;
import repositorio.clienteServidor.MensajeRepositorio;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio del servidor para enviar mensajes a canales.
 * Maneja envío, almacenamiento y activación de sincronización P2P.
 */
public class ServicioEnviarMensajeCanal implements IServicioCliente {

    private static final String TAG = "EnviarMensajeCanal";

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
    private ServicioNotificarMensajeCanal servicioNotificarCanal;

    public ServicioEnviarMensajeCanal() {
        this.repoMensaje = new MensajeRepositorio();
        this.gson = new Gson();
        LoggerCentral.info(TAG, AZUL + "Constructor: ServicioEnviarMensajeCanal creado" + RESET);
    }

    /**
     * Inyecta el servicio de notificaciones CS.
     */
    public void setServicioNotificacion(ServicioNotificacionCliente servicioNotificacion) {
        this.servicioNotificacion = servicioNotificacion;
        LoggerCentral.info(TAG, VERDE + "Servicio de notificaciones CS configurado" + RESET);
    }

    /**
     * Inyecta el servicio de sincronización P2P.
     */
    public void setServicioSincronizacionP2P(ServicioSincronizacionDatos servicioSyncP2P) {
        this.servicioSyncP2P = servicioSyncP2P;
        LoggerCentral.info(TAG, VERDE + "Servicio de sincronización P2P configurado" + RESET);
    }

    /**
     * Inyecta el servicio de notificaciones de canal.
     */
    public void setServicioNotificarCanal(ServicioNotificarMensajeCanal servicioNotificarCanal) {
        this.servicioNotificarCanal = servicioNotificarCanal;
        LoggerCentral.info(TAG, VERDE + "Servicio de notificaciones de canal configurado" + RESET);
    }

    @Override
    public String getNombre() {
        return "ServicioEnviarMensajeCanal";
    }

    @Override
    public void inicializar(IGestorConexionesCliente gestor, IRouterMensajesCliente router) {
        this.gestor = gestor;
        LoggerCentral.info(TAG, AZUL + "Inicializando ServicioEnviarMensajeCanal..." + RESET);

        // ==================== RUTA: Enviar Mensaje a Canal ====================
        router.registrarAccion("enviarMensajeCanal", (datos, idSesion) -> {
            try {
                LoggerCentral.info(TAG, CYAN + "📥 Recibida petición de envío de mensaje a canal" + RESET);

                // Validar autenticación
                String userId = gestor.obtenerUsuarioDeSesion(idSesion);
                if (userId == null) {
                    LoggerCentral.warn(TAG, AMARILLO + "Usuario no autenticado" + RESET);
                    return new DTOResponse("enviarMensajeCanal", "error", "Usuario no autenticado", null);
                }

                // Parsear datos
                DTOEnviarMensajeCanal dto = gson.fromJson(datos, DTOEnviarMensajeCanal.class);

                // Validar datos
                if (dto.getRemitenteId() == null || dto.getCanalId() == null) {
                    LoggerCentral.warn(TAG, AMARILLO + "Datos de mensaje inválidos" + RESET);
                    return new DTOResponse("enviarMensajeCanal", "error", "Datos de mensaje inválidos", null);
                }

                // Validar que el remitente sea el usuario autenticado
                if (!userId.equals(dto.getRemitenteId())) {
                    LoggerCentral.warn(TAG, ROJO + "Usuario no autorizado: " + dto.getRemitenteId() + RESET);
                    return new DTOResponse("enviarMensajeCanal", "error", "No autorizado", null);
                }

                // Determinar tipo de mensaje
                String tipoMensaje = determinarTipoMensaje(dto);

                LoggerCentral.info(TAG, CYAN + "📤 Enviando mensaje a canal" + RESET);
                LoggerCentral.info(TAG, "   → De: " + dto.getRemitenteId());
                LoggerCentral.info(TAG, "   → Canal: " + dto.getCanalId());
                LoggerCentral.info(TAG, "   → Tipo: " + tipoMensaje);

                // Crear mensaje en BD
                Mensaje mensaje = new Mensaje();
                mensaje.setId(UUID.randomUUID());
                mensaje.setRemitenteId(UUID.fromString(dto.getRemitenteId()));
                mensaje.setCanalId(UUID.fromString(dto.getCanalId()));

                // Configurar tipo y contenido según el tipo de mensaje
                if ("TEXTO".equalsIgnoreCase(tipoMensaje)) {
                    mensaje.setTipo(Mensaje.Tipo.TEXTO);
                    mensaje.setContenido(dto.getContenido());
                } else if ("AUDIO".equalsIgnoreCase(tipoMensaje)) {
                    mensaje.setTipo(Mensaje.Tipo.AUDIO);
                    mensaje.setContenido(dto.getFileId()); // Para audio, el contenido es el fileId
                } else {
                    // Para otros tipos de archivos, usar TEXTO pero con fileId
                    mensaje.setTipo(Mensaje.Tipo.TEXTO);
                    mensaje.setContenido(dto.getFileId());
                }

                mensaje.setFechaEnvio(Instant.now());

                // Guardar en BD
                boolean guardado = repoMensaje.guardar(mensaje);

                if (!guardado) {
                    LoggerCentral.error(TAG, ROJO + "❌ Error al guardar mensaje en BD" + RESET);
                    return new DTOResponse("enviarMensajeCanal", "error", "Error al guardar mensaje", null);
                }

                LoggerCentral.info(TAG, VERDE + "✅ Mensaje guardado en BD - ID: " + mensaje.getId() + RESET);

                // ✅ 1. SIGNAL_UPDATE a clientes CS
                if (servicioNotificacion != null) {
                    Map<String, Object> notificacionData = new HashMap<>();
                    notificacionData.put("tipo", "NUEVO_MENSAJE_CANAL");
                    notificacionData.put("canalId", dto.getCanalId());
                    notificacionData.put("mensajeId", mensaje.getId().toString());

                    servicioNotificacion.actualizar("NUEVO_MENSAJE_CANAL", notificacionData);
                    LoggerCentral.info(TAG, VERDE + "✅ SIGNAL_UPDATE enviado a clientes CS" + RESET);
                }

                // ✅ 2. Activar sincronización P2P
                if (servicioSyncP2P != null) {
                    LoggerCentral.info(TAG, CYAN + "🔄 Activando sincronización P2P..." + RESET);
                    servicioSyncP2P.onBaseDeDatosCambio();
                    servicioSyncP2P.forzarSincronizacion();
                    LoggerCentral.info(TAG, VERDE + "✅ Sincronización P2P activada" + RESET);
                }

                // ✅ 3. Enviar notificación push a miembros del canal
                if (servicioNotificarCanal != null) {
                    LoggerCentral.info(TAG, CYAN + "📲 Enviando notificación push a miembros del canal..." + RESET);
                    servicioNotificarCanal.enviarNotificacionCanal(dto.getCanalId(), mensaje.getId().toString());
                    LoggerCentral.info(TAG, VERDE + "✅ Notificación push enviada a miembros del canal" + RESET);
                }

                // Preparar respuesta
                Map<String, Object> respuesta = construirRespuestaMensaje(mensaje, tipoMensaje);

                return new DTOResponse("enviarMensajeCanal", "success", "Mensaje enviado al canal", gson.toJsonTree(respuesta));

            } catch (Exception e) {
                LoggerCentral.error(TAG, ROJO + "❌ Error procesando mensaje de canal: " + e.getMessage() + RESET);
                e.printStackTrace();
                return new DTOResponse("enviarMensajeCanal", "error", "Error interno: " + e.getMessage(), null);
            }
        });

        LoggerCentral.info(TAG, VERDE + "✅ Servicio de envío de mensajes a canal inicializado" + RESET);
    }

    /**
     * Determina el tipo de mensaje basándose en el DTO.
     */
    private String determinarTipoMensaje(DTOEnviarMensajeCanal dto) {
        if (dto.getContenido() != null && !dto.getContenido().isEmpty()) {
            return "TEXTO";
        } else if (dto.getFileId() != null && !dto.getFileId().isEmpty()) {
            // Intentar determinar si es audio por la extensión
            String fileId = dto.getFileId().toLowerCase();
            if (fileId.contains("audio") || fileId.endsWith(".wav") || fileId.endsWith(".mp3") || fileId.endsWith(".ogg")) {
                return "AUDIO";
            }
            return "ARCHIVO";
        }
        return "TEXTO";
    }

    /**
     * Construye la respuesta con los datos del mensaje para el cliente.
     */
    private Map<String, Object> construirRespuestaMensaje(Mensaje mensaje, String tipo) {
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("messageId", mensaje.getId().toString());
        respuesta.put("channelId", mensaje.getCanalId().toString());
        respuesta.put("messageType", tipo);

        if ("TEXTO".equalsIgnoreCase(tipo)) {
            respuesta.put("content", mensaje.getContenido());
        } else {
            respuesta.put("fileId", mensaje.getContenido());
        }

        respuesta.put("timestamp", mensaje.getFechaEnvio().toString());

        // Información del autor
        Map<String, String> author = new HashMap<>();
        author.put("userId", mensaje.getRemitenteId().toString());
        respuesta.put("author", author);

        return respuesta;
    }

    @Override
    public void iniciar() {
        LoggerCentral.info(TAG, VERDE + "Servicio de envío de mensajes a canal iniciado" + RESET);
    }

    @Override
    public void detener() {
        LoggerCentral.info(TAG, AMARILLO + "Servicio de envío de mensajes a canal detenido" + RESET);
    }
}
