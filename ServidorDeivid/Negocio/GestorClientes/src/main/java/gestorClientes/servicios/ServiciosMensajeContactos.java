package gestorClientes.servicios;

import conexion.clientes.interfaces.IGestorConexionesCliente;
import conexion.clientes.interfaces.IRouterMensajesCliente;
import gestorClientes.interfaces.IServicioCliente;
import gestorP2P.servicios.ServicioSincronizacionDatos;
import logger.LoggerCentral;

/**
 * Coordinador de servicios de mensajería Cliente-Servidor.
 * Agrupa y gestiona los servicios especializados de mensajes:
 * - ServicioMensajesDirectos: Mensajes de texto
 * - ServicioMensajesAudio: Mensajes de audio
 *
 * Los archivos se manejan por separado en ServicioArchivos.
 * Integrado con sincronización P2P.
 */
public class ServiciosMensajeContactos implements IServicioCliente {

    private static final String TAG = "CoordinadorMensajes";

    private final ServicioMensajesDirectos servicioTexto;
    private final ServicioMensajesAudio servicioAudio;

    public ServiciosMensajeContactos() {
        LoggerCentral.info(TAG, "🔧 Inicializando coordinador de mensajería...");

        this.servicioTexto = new ServicioMensajesDirectos();
        this.servicioAudio = new ServicioMensajesAudio();

        LoggerCentral.info(TAG, "✅ Coordinador de mensajería creado");
    }

    /**
     * Configura el servicio de notificaciones CS para todos los servicios de mensajería.
     */
    public void setServicioNotificacion(ServicioNotificacionCliente servicioNotificacion) {
        servicioTexto.setServicioNotificacion(servicioNotificacion);
        servicioAudio.setServicioNotificacion(servicioNotificacion);
        LoggerCentral.info(TAG, "✅ Servicio de notificaciones CS configurado en todos los servicios");
    }

    /**
     * Configura el servicio de sincronización P2P para todos los servicios de mensajería.
     */
    public void setServicioSincronizacionP2P(ServicioSincronizacionDatos servicioSyncP2P) {
        servicioTexto.setServicioSincronizacionP2P(servicioSyncP2P);
        servicioAudio.setServicioSincronizacionP2P(servicioSyncP2P);
        LoggerCentral.info(TAG, "✅ Servicio de sincronización P2P configurado en todos los servicios");
    }

    @Override
    public String getNombre() {
        return "ServiciosMensajeContactos";
    }

    @Override
    public void inicializar(IGestorConexionesCliente gestor, IRouterMensajesCliente router) {
        LoggerCentral.info(TAG, "📡 Inicializando servicios de mensajería...");

        // Inicializar cada servicio especializado
        servicioTexto.inicializar(gestor, router);
        servicioAudio.inicializar(gestor, router);

        LoggerCentral.info(TAG, "✅ Todos los servicios de mensajería inicializados");
    }

    @Override
    public void iniciar() {
        LoggerCentral.info(TAG, "🚀 Iniciando servicios de mensajería...");

        servicioTexto.iniciar();
        servicioAudio.iniciar();

        LoggerCentral.info(TAG, "✅ Servicios de mensajería en ejecución");
    }

    @Override
    public void detener() {
        LoggerCentral.info(TAG, "⏹ Deteniendo servicios de mensajería...");

        servicioTexto.detener();
        servicioAudio.detener();

        LoggerCentral.info(TAG, "✅ Servicios de mensajería detenidos");
    }

    // ==================== GETTERS ====================

    public ServicioMensajesDirectos getServicioTexto() {
        return servicioTexto;
    }

    public ServicioMensajesAudio getServicioAudio() {
        return servicioAudio;
    }
}
