package gestorClientes.servicios;

import com.google.gson.Gson;
import conexion.clientes.interfaces.IGestorConexionesCliente;
import conexion.clientes.interfaces.IRouterMensajesCliente;
import dominio.clienteServidor.Archivo;
import dominio.clienteServidor.Mensaje;
import dto.comunicacion.DTOResponse;
import dto.mensajeria.DTOEnviarMensajeAudio;
import gestorClientes.interfaces.IServicioCliente;
import gestorP2P.servicios.ServicioSincronizacionDatos;
import gestorTranscripcion.FachadaTranscripcion;
import logger.LoggerCentral;
import repositorio.clienteServidor.ArchivoRepositorio;
import repositorio.clienteServidor.MensajeRepositorio;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio del servidor para gestionar mensajes de AUDIO directos entre contactos.
 * Maneja envío de mensajes de audio con referencia a archivos en Bucket/.
 * Integrado con sincronización P2P.
 */
public class ServicioMensajesAudio implements IServicioCliente {

    private static final String TAG = "MensajesAudio";

    // Colores ANSI para logs
    private static final String RESET = "\u001B[0m";
    private static final String VERDE = "\u001B[32m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String ROJO = "\u001B[31m";
    private static final String AZUL = "\u001B[34m";

    private IGestorConexionesCliente gestor;
    private final MensajeRepositorio repoMensaje;
    private final ArchivoRepositorio repoArchivo;
    private final Gson gson;

    // Referencias a servicios
    private ServicioNotificacionCliente servicioNotificacion;
    private ServicioSincronizacionDatos servicioSyncP2P;
    private FachadaTranscripcion fachadaTranscripcion;

    public ServicioMensajesAudio() {
        this.repoMensaje = new MensajeRepositorio();
        this.repoArchivo = new ArchivoRepositorio();
        this.gson = new Gson();
        this.fachadaTranscripcion = FachadaTranscripcion.getInstance();
        LoggerCentral.info(TAG, AZUL + "Constructor: ServicioMensajesAudio creado" + RESET);
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
        return "ServicioMensajesAudio";
    }

    @Override
    public void inicializar(IGestorConexionesCliente gestor, IRouterMensajesCliente router) {
        this.gestor = gestor;
        LoggerCentral.info(TAG, AZUL + "Inicializando ServicioMensajesAudio..." + RESET);

        // ==================== RUTA: Enviar Mensaje de Audio ====================
        router.registrarAccion("enviarmensajedirectoaudio", (datos, idSesion) -> {
            try {
                LoggerCentral.info(TAG, CYAN + "📥 Recibida petición de envío de mensaje de audio" + RESET);

                // Validar autenticación
                String userId = gestor.obtenerUsuarioDeSesion(idSesion);
                if (userId == null) {
                    LoggerCentral.warn(TAG, AMARILLO + "Usuario no autenticado" + RESET);
                    return new DTOResponse("enviarmensajedirectoaudio", "error", "Usuario no autenticado", null);
                }

                // Parsear datos
                DTOEnviarMensajeAudio dto = gson.fromJson(datos, DTOEnviarMensajeAudio.class);

                // Validar datos
                if (dto.getRemitenteId() == null || dto.getDestinatarioId() == null || dto.getAudioId() == null) {
                    LoggerCentral.warn(TAG, AMARILLO + "Datos de mensaje de audio inválidos" + RESET);
                    return new DTOResponse("enviarmensajedirectoaudio", "error", "Datos de mensaje inválidos", null);
                }

                // Validar que el remitente sea el usuario autenticado
                if (!userId.equals(dto.getRemitenteId())) {
                    LoggerCentral.warn(TAG, ROJO + "Usuario no autorizado para enviar desde: " + dto.getRemitenteId() + RESET);
                    return new DTOResponse("enviarmensajedirectoaudio", "error", "No autorizado", null);
                }

                // Validar que el archivo de audio exista en BD
                Archivo archivoAudio = repoArchivo.buscarPorFileId(dto.getAudioId());
                if (archivoAudio == null) {
                    LoggerCentral.warn(TAG, AMARILLO + "Archivo de audio no encontrado: " + dto.getAudioId() + RESET);
                    return new DTOResponse("enviarmensajedirectoaudio", "error", "Archivo de audio no encontrado", null);
                }

                LoggerCentral.info(TAG, CYAN + "📤 Enviando mensaje de AUDIO" + RESET);
                LoggerCentral.info(TAG, "   → De: " + dto.getRemitenteId() + " (Peer: " + dto.getPeerRemitenteId() + ")");
                LoggerCentral.info(TAG, "   → Para: " + dto.getDestinatarioId() + " (Peer: " + dto.getPeerDestinoId() + ")");
                LoggerCentral.info(TAG, "   → AudioId: " + dto.getAudioId());
                LoggerCentral.info(TAG, "   → Nombre archivo: " + archivoAudio.getNombreArchivo());

                // Crear mensaje en BD
                Mensaje mensaje = new Mensaje();
                mensaje.setId(UUID.randomUUID());
                mensaje.setRemitenteId(UUID.fromString(dto.getRemitenteId()));
                mensaje.setDestinatarioUsuarioId(UUID.fromString(dto.getDestinatarioId()));
                mensaje.setPeerRemitenteId(dto.getPeerRemitenteId());
                mensaje.setPeerDestinoId(dto.getPeerDestinoId());
                mensaje.setTipo(Mensaje.Tipo.AUDIO);
                mensaje.setContenido(dto.getAudioId());
                mensaje.setFechaEnvio(Instant.now());

                // Guardar en BD
                boolean guardado = repoMensaje.guardar(mensaje);

                if (!guardado) {
                    LoggerCentral.error(TAG, ROJO + "❌ Error al guardar mensaje de audio en BD" + RESET);
                    return new DTOResponse("enviarmensajedirectoaudio", "error", "Error al guardar mensaje", null);
                }

                LoggerCentral.info(TAG, VERDE + "✅ Mensaje de audio guardado en BD - ID: " + mensaje.getId() + RESET);

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

                // ✅ 3. Notificar a sistema de transcripción
                if (fachadaTranscripcion != null) {
                    LoggerCentral.info(TAG, CYAN + "📝 Notificando a sistema de transcripción..." + RESET);
                    fachadaTranscripcion.notificarNuevoAudio(dto.getAudioId());
                    LoggerCentral.info(TAG, VERDE + "✅ Sistema de transcripción notificado" + RESET);
                }

                // Preparar respuesta para el remitente
                Map<String, Object> respuestaRemitente = construirRespuestaMensajeAudio(mensaje, archivoAudio);

                return new DTOResponse("enviarmensajedirectoaudio", "success", "Mensaje de audio enviado", gson.toJsonTree(respuestaRemitente));

            } catch (Exception e) {
                LoggerCentral.error(TAG, ROJO + "❌ Error en enviarmensajedirectoaudio: " + e.getMessage() + RESET);
                return new DTOResponse("enviarmensajedirectoaudio", "error", "Error interno del servidor", null);
            }
        });

        LoggerCentral.info(TAG, VERDE + "✅ ServicioMensajesAudio inicializado" + RESET);
        LoggerCentral.info(TAG, "   → Rutas registradas: enviarmensajedirectoaudio");
    }

    @Override
    public void iniciar() {
        LoggerCentral.info(TAG, VERDE + "Servicio de mensajes de audio iniciado" + RESET);
    }

    @Override
    public void detener() {
        LoggerCentral.info(TAG, AMARILLO + "Servicio de mensajes de audio detenido" + RESET);
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Construye la respuesta de un mensaje de audio para enviar al cliente.
     */
    private Map<String, Object> construirRespuestaMensajeAudio(Mensaje mensaje, Archivo archivo) {
        Map<String, Object> response = new HashMap<>();
        response.put("mensajeId", mensaje.getId());
        response.put("remitenteId", mensaje.getRemitenteId() != null ? mensaje.getRemitenteId().toString() : null);
        response.put("destinatarioId", mensaje.getDestinatarioUsuarioId() != null ? mensaje.getDestinatarioUsuarioId().toString() : null);
        response.put("peerRemitenteId", mensaje.getPeerRemitenteId());
        response.put("peerDestinoId", mensaje.getPeerDestinoId());
        response.put("tipo", mensaje.getTipo() != null ? mensaje.getTipo().name().toLowerCase() : "audio");
        response.put("contenido", mensaje.getContenido());
        response.put("audioId", mensaje.getContenido());
        response.put("fechaEnvio", mensaje.getFechaEnvio() != null ? mensaje.getFechaEnvio().toString() : null);

        // Información adicional del archivo de audio
        if (archivo != null) {
            response.put("nombreArchivo", archivo.getNombreArchivo());
            response.put("tamanioArchivo", archivo.getTamanio());
            response.put("mimeType", archivo.getMimeType());
        }

        return response;
    }
}
