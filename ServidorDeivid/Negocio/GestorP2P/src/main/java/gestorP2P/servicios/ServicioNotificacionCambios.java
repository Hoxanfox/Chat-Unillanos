package gestorP2P.servicios;

import com.google.gson.Gson;
import conexion.p2p.interfaces.IRouterMensajes;
import conexion.p2p.interfaces.IGestorConexiones;
import gestorP2P.interfaces.IServicioP2P;
import gestorP2P.utils.GsonUtil;
import logger.LoggerCentral;
import observador.IObservador;
import observador.ISujeto;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio central que escucha cambios en la Base de Datos y notifica a:
 * 1. Clientes Locales (UI, WebSockets) -> Para refrescar la vista.
 * 2. Sistema P2P (Merkle Tree) -> Para mantener la integridad.
 */
public class ServicioNotificacionCambios implements IServicioP2P, ISujeto {

    private static final String TAG = "NotificadorCambios";
    private final List<IObservador> observadores;
    private final Gson gson;

    public enum TipoEvento {
        NUEVO_MENSAJE,
        NUEVO_USUARIO,
        NUEVO_CANAL,
        ACTUALIZACION_ESTADO,
        PROGRESO_DESCARGA,      // ✅ NUEVO: Para notificar progreso de descargas P2P
        DESCARGA_COMPLETADA,    // ✅ NUEVO: Para notificar cuando una descarga termina
        DESCARGA_FALLIDA,       // ✅ NUEVO: Para notificar cuando una descarga falla
        SINCRONIZACION_COMPLETADA  // ✅ NUEVO: Para notificar cuando termina la sincronización P2P
    }

    public ServicioNotificacionCambios() {
        this.observadores = new ArrayList<>();
        this.gson = GsonUtil.crearGson();
    }

    @Override
    public String getNombre() { return "ServicioNotificacionCambios"; }

    @Override
    public void inicializar(IGestorConexiones gestor, IRouterMensajes router) {
        // Este servicio es interno, no necesita registrar rutas de red propias
        LoggerCentral.info(TAG, "Sistema de notificación de cambios activo.");
    }

    /**
     * Método público para reportar que algo cambió en la BD.
     * @param tipo El tipo de cambio (INSERT, UPDATE, DELETE).
     * @param entidad La entidad afectada ("Mensaje", "Usuario").
     * @param datos El objeto que cambió.
     */
    public void notificarCambio(TipoEvento tipo, Object datos) {
        LoggerCentral.debug(TAG, "Cambio detectado: " + tipo);

        // Notificar a todos los observadores internos (Sync, UI, etc.)
        notificarObservadores(tipo.name(), datos);

        // Aquí podrías agregar lógica específica para enviar a WebSockets de clientes
        enviarAClientesConectados(tipo, datos);
    }

    /**
     * Simulación del envío a clientes conectados (Frontend).
     */
    private void enviarAClientesConectados(TipoEvento tipo, Object datos) {
        // En un sistema real, aquí iterarías sobre tus sesiones de WebSocket
        // y enviarías el JSON.
        String jsonPush = gson.toJson(datos);
        // LoggerCentral.debug(TAG, "PUSH a Clientes >> " + tipo + ": " + jsonPush);
    }

    @Override
    public void registrarObservador(IObservador observador) {
        observadores.add(observador);
    }

    @Override
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
    }

    @Override
    public void notificarObservadores(String tipo, Object datos) {
        for (IObservador obs : observadores) {
            obs.actualizar(tipo, datos);
        }
    }

    @Override
    public void iniciar() {}

    @Override
    public void detener() {}
}