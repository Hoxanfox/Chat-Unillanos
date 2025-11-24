package gestorP2P.servicios;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import conexion.p2p.interfaces.IRouterMensajes;
import conexion.p2p.interfaces.IGestorConexiones;
import dominio.clienteServidor.Mensaje;
import dto.comunicacion.DTORequest;
import dto.comunicacion.DTOResponse;
import dto.p2p.DTOPeerDetails;
import gestorP2P.interfaces.IServicioP2P;
import logger.LoggerCentral;
import repositorio.clienteServidor.MensajeRepositorio;

import java.time.Instant;
import java.util.UUID;

public class ServicioChat implements IServicioP2P {

    private static final String TAG = "ServicioChat";
    private static final String CYAN = "\u001B[36m";
    private static final String RESET = "\u001B[0m";

    private IGestorConexiones gestorConexiones;
    private final Gson gson;
    private final MensajeRepositorio repositorio;

    // Referencia al bus de eventos (Reemplaza a servicioSync directo)
    private ServicioNotificacionCambios notificador;

    public ServicioChat() {
        this.gson = new Gson();
        this.repositorio = new MensajeRepositorio();
    }

    /**
     * Inyecta el Notificador para avisar de cambios en la BD.
     * Esto soluciona el error "Cannot resolve method setNotificador".
     */
    public void setNotificador(ServicioNotificacionCambios notificador) {
        this.notificador = notificador;
    }

    // Mantenemos este por compatibilidad si aún lo usas en alguna parte,
    // pero idealmente ya no se debería usar.
    public void setServicioSync(ServicioSincronizacionDatos sync) {
        // Deprecado en favor de setNotificador
    }

    @Override
    public String getNombre() { return "ServicioChat"; }

    @Override
    public void inicializar(IGestorConexiones gestor, IRouterMensajes router) {
        this.gestorConexiones = gestor;

        // =================================================================
        // RECEPCIÓN DE MENSAJES
        // =================================================================
        router.registrarAccion("mensajeChat", (payload, origenId) -> {
            if (payload == null) return null;

            try {
                JsonObject obj = payload.getAsJsonObject();
                String texto = obj.has("texto") ? obj.get("texto").getAsString() : "";
                String usuario = obj.has("usuario") ? obj.get("usuario").getAsString() : "Anónimo";

                Mensaje m = new Mensaje();
                if (obj.has("id")) {
                    m.setId(UUID.fromString(obj.get("id").getAsString()));
                } else {
                    m.setId(UUID.randomUUID());
                }
                m.setContenido(texto);
                m.setFechaEnvio(Instant.now());

                // Guardar
                boolean guardado = repositorio.guardar(m);

                // Notificar al sistema (Sync y UI)
                if (guardado && notificador != null) {
                    notificador.notificarCambio(
                            ServicioNotificacionCambios.TipoEvento.NUEVO_MENSAJE,
                            m
                    );
                }

                String logMsg = String.format("%sMensaje de %s:%s %s", CYAN, usuario, RESET, texto);
                LoggerCentral.info(TAG, logMsg);

                return new DTOResponse("mensajeChatAck", "success", "Recibido", null);

            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error procesando mensaje: " + e.getMessage());
                return new DTOResponse("mensajeChat", "error", "Malformado", null);
            }
        });
    }

    // =================================================================
    // ENVÍO DE MENSAJES
    // =================================================================

    public void enviarMensajePublico(String miNombreUsuario, String texto) {
        if (gestorConexiones == null) {
            LoggerCentral.error(TAG, "No hay conexión de red.");
            return;
        }

        Mensaje m = new Mensaje();
        m.setContenido(texto);
        m.setFechaEnvio(Instant.now());

        boolean guardado = repositorio.guardar(m);

        // Notificar cambio local
        if (guardado && notificador != null) {
            notificador.notificarCambio(
                    ServicioNotificacionCambios.TipoEvento.NUEVO_MENSAJE,
                    m
            );
        }

        JsonObject msg = new JsonObject();
        msg.addProperty("id", m.getId().toString());
        msg.addProperty("usuario", miNombreUsuario);
        msg.addProperty("texto", texto);

        DTORequest req = new DTORequest("mensajeChat", msg);
        gestorConexiones.broadcast(gson.toJson(req));
    }

    public void enviarMensajePrivado(String peerId, String miNombreUsuario, String texto) {
        if (gestorConexiones == null) return;

        Mensaje m = new Mensaje();
        m.setContenido(texto);
        m.setFechaEnvio(Instant.now());

        boolean guardado = repositorio.guardar(m);

        if (guardado && notificador != null) {
            notificador.notificarCambio(ServicioNotificacionCambios.TipoEvento.NUEVO_MENSAJE, m);
        }

        JsonObject msg = new JsonObject();
        msg.addProperty("id", m.getId().toString());
        msg.addProperty("usuario", miNombreUsuario);
        msg.addProperty("texto", texto);

        DTORequest req = new DTORequest("mensajeChat", msg);

        DTOPeerDetails destino = gestorConexiones.obtenerDetallesPeers().stream()
                .filter(p -> p.getId().equals(peerId)).findFirst().orElse(null);

        if (destino != null) {
            gestorConexiones.enviarMensaje(destino, gson.toJson(req));
            LoggerCentral.info(TAG, "Enviado privado a " + peerId);
        } else {
            LoggerCentral.error(TAG, "Peer destino no encontrado: " + peerId);
        }
    }

    @Override
    public void iniciar() {
        LoggerCentral.info(TAG, "Servicio de Chat listo.");
    }

    @Override
    public void detener() {}
}