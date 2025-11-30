package gestorClientes.servicios;

import com.google.gson.Gson;
import conexion.clientes.interfaces.IGestorConexionesCliente;
import conexion.clientes.interfaces.IRouterMensajesCliente;
import dominio.clienteServidor.Mensaje;
import dominio.clienteServidor.Usuario;
import dto.comunicacion.DTOResponse;
import dto.canales.DTOSolicitarHistorialCanal;
import gestorClientes.interfaces.IServicioCliente;
import logger.LoggerCentral;
import repositorio.clienteServidor.CanalMiembroRepositorio;
import repositorio.clienteServidor.MensajeRepositorio;
import repositorio.clienteServidor.UsuarioRepositorio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio del servidor para solicitar historial de mensajes de canal.
 * Valida permisos de acceso y retorna mensajes con datos de autor.
 */
public class ServicioHistorialCanal implements IServicioCliente {

    private static final String TAG = "HistorialCanal";

    // Colores ANSI para logs
    private static final String RESET = "\u001B[0m";
    private static final String VERDE = "\u001B[32m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String ROJO = "\u001B[31m";
    private static final String AZUL = "\u001B[34m";

    private IGestorConexionesCliente gestor;
    private final MensajeRepositorio repoMensaje;
    private final CanalMiembroRepositorio repoCanalMiembro;
    private final UsuarioRepositorio repoUsuario;
    private final Gson gson;

    public ServicioHistorialCanal() {
        this.repoMensaje = new MensajeRepositorio();
        this.repoCanalMiembro = new CanalMiembroRepositorio();
        this.repoUsuario = new UsuarioRepositorio();
        this.gson = new Gson();
        LoggerCentral.info(TAG, AZUL + "Constructor: ServicioHistorialCanal creado" + RESET);
    }

    @Override
    public String getNombre() {
        return "ServicioHistorialCanal";
    }

    @Override
    public void inicializar(IGestorConexionesCliente gestor, IRouterMensajesCliente router) {
        this.gestor = gestor;
        LoggerCentral.info(TAG, AZUL + "Inicializando ServicioHistorialCanal..." + RESET);

        // ==================== RUTA: Solicitar Historial de Canal ====================
        router.registrarAccion("solicitarHistorialCanal", (datos, idSesion) -> {
            try {
                LoggerCentral.info(TAG, CYAN + "📥 Recibida petición de historial de canal" + RESET);

                // Validar autenticación
                String userId = gestor.obtenerUsuarioDeSesion(idSesion);
                if (userId == null) {
                    LoggerCentral.warn(TAG, AMARILLO + "Usuario no autenticado" + RESET);
                    return new DTOResponse("solicitarHistorialCanal", "error", "Usuario no autenticado", null);
                }

                // Parsear datos
                DTOSolicitarHistorialCanal dto = gson.fromJson(datos, DTOSolicitarHistorialCanal.class);

                // Validar datos
                if (dto.getCanalId() == null || dto.getCanalId().isEmpty()) {
                    LoggerCentral.warn(TAG, AMARILLO + "ID de canal requerido" + RESET);
                    return new DTOResponse("solicitarHistorialCanal", "error", "ID de canal requerido", null);
                }

                LoggerCentral.info(TAG, CYAN + "📤 Solicitando historial de canal" + RESET);
                LoggerCentral.info(TAG, "   → Canal: " + dto.getCanalId());
                LoggerCentral.info(TAG, "   → Usuario: " + userId);
                LoggerCentral.info(TAG, "   → Límite: " + dto.getLimite());

                // Validar que el usuario sea miembro del canal
                boolean esMiembro = repoCanalMiembro.esMiembroDelCanal(dto.getCanalId(), userId);
                if (!esMiembro) {
                    LoggerCentral.warn(TAG, ROJO + "Usuario no es miembro del canal: " + dto.getCanalId() + RESET);
                    return new DTOResponse("solicitarHistorialCanal", "error", "No tienes acceso a este canal", null);
                }

                // Obtener historial de mensajes del canal
                List<Mensaje> mensajes = repoMensaje.obtenerMensajesPorCanal(
                    dto.getCanalId(),
                    dto.getLimite() > 0 ? dto.getLimite() : 50,
                    dto.getOffset()
                );

                // Construir lista de DTOs con información completa
                List<Map<String, Object>> mensajesDTO = new ArrayList<>();
                for (Mensaje mensaje : mensajes) {
                    Map<String, Object> mensajeMap = construirMensajeDTO(mensaje);
                    mensajesDTO.add(mensajeMap);
                }

                LoggerCentral.info(TAG, VERDE + "✅ Historial obtenido: " + mensajesDTO.size() + " mensajes" + RESET);

                // Preparar respuesta
                Map<String, Object> respuesta = new HashMap<>();
                respuesta.put("mensajes", mensajesDTO);
                respuesta.put("canalId", dto.getCanalId());
                respuesta.put("totalRecibidos", mensajesDTO.size());
                respuesta.put("hayMasMensajes", mensajes.size() >= dto.getLimite());

                return new DTOResponse("solicitarHistorialCanal", "success", "Historial obtenido", gson.toJsonTree(respuesta));

            } catch (Exception e) {
                LoggerCentral.error(TAG, ROJO + "❌ Error obteniendo historial: " + e.getMessage() + RESET);
                e.printStackTrace();
                return new DTOResponse("solicitarHistorialCanal", "error", "Error interno: " + e.getMessage(), null);
            }
        });

        LoggerCentral.info(TAG, VERDE + "✅ Servicio de historial de canal inicializado" + RESET);
    }

    /**
     * Construye un DTO completo del mensaje con información del autor.
     */
    private Map<String, Object> construirMensajeDTO(Mensaje mensaje) {
        Map<String, Object> dto = new HashMap<>();

        dto.put("messageId", mensaje.getId().toString());
        dto.put("channelId", mensaje.getCanalId() != null ? mensaje.getCanalId().toString() : null);
        dto.put("timestamp", mensaje.getFechaEnvio().toString());

        // Determinar tipo de mensaje
        String messageType = mensaje.getTipo() == Mensaje.Tipo.AUDIO ? "AUDIO" : "TEXT";
        String contenido = mensaje.getContenido();

        // Detectar automáticamente archivos por el contenido
        if (contenido != null) {
            if (contenido.startsWith("audio_files/") || contenido.endsWith(".wav") || contenido.endsWith(".mp3")) {
                messageType = "AUDIO";
                dto.put("fileId", contenido);
            } else if (contenido.startsWith("image_files/") || contenido.endsWith(".jpg") || contenido.endsWith(".png")) {
                messageType = "IMAGE";
                dto.put("fileId", contenido);
            } else if (contenido.startsWith("document_files/") || contenido.endsWith(".pdf")) {
                messageType = "FILE";
                dto.put("fileId", contenido);
            } else {
                // Es texto plano
                dto.put("content", contenido);
            }
        }

        dto.put("messageType", messageType);

        // Información del autor
        Map<String, String> author = new HashMap<>();
        if (mensaje.getRemitenteId() != null) {
            String remitenteIdStr = mensaje.getRemitenteId().toString();
            Usuario remitente = repoUsuario.buscarPorId(remitenteIdStr);
            if (remitente != null) {
                author.put("userId", remitente.getId());
                author.put("username", remitente.getNombre());
                author.put("userPhoto", remitente.getFoto());
            } else {
                author.put("userId", remitenteIdStr);
                author.put("username", "Usuario Desconocido");
            }
        }
        dto.put("author", author);

        return dto;
    }

    @Override
    public void iniciar() {
        LoggerCentral.info(TAG, VERDE + "Servicio de historial de canal iniciado" + RESET);
    }

    @Override
    public void detener() {
        LoggerCentral.info(TAG, AMARILLO + "Servicio de historial de canal detenido" + RESET);
    }
}
