package servicio.clienteServidor;

import gestorClientes.FachadaClientes;
import gestorClientes.servicios.ServicioAutenticacion;
import gestorClientes.servicios.ServicioNotificacionCliente;
import logger.LoggerCentral;

/**
 * Capa de Aplicación para el subsistema de Clientes.
 * Orquesta los servicios que atienden a los usuarios finales (Apps/Web).
 */
public class ServicioCliente implements IServicioClienteControl {

    private static final String TAG = "ServicioCliente";
    private final FachadaClientes fachada;

    // Referencias a servicios clave
    private ServicioNotificacionCliente servicioNotificacion;
    private ServicioAutenticacion servicioAuth;

    private boolean running;

    public ServicioCliente() {
        LoggerCentral.info(TAG, ">> Inicializando Subsistema de Clientes...");
        this.running = false;
        this.fachada = new FachadaClientes();
        configurarServicios();
    }

    private void configurarServicios() {
        // 1. Autenticación (Login/Registro)
        LoggerCentral.info(TAG, "Registrando ServicioAutenticacion...");
        this.servicioAuth = new ServicioAutenticacion();
        fachada.registrarServicio(servicioAuth);

        // 2. Notificaciones Push (Pasarela hacia clientes)
        LoggerCentral.info(TAG, "Registrando ServicioNotificacionCliente...");
        this.servicioNotificacion = new ServicioNotificacionCliente();
        fachada.registrarServicio(servicioNotificacion);

        // Aquí agregarías más servicios CS (ej. ChatCliente)
    }

    // --- IMPLEMENTACIÓN DE LA INTERFAZ DE CONTROL ---

    @Override
    public void iniciarServidor(int puerto) {
        if (running) {
            LoggerCentral.warn(TAG, "Servidor de clientes ya corriendo.");
            return;
        }
        try {
            LoggerCentral.info(TAG, "Levantando servidor de clientes en puerto " + puerto + "...");
            fachada.iniciar(puerto);
            running = true;
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Fallo al iniciar servidor clientes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void detenerServidor() {
        if (!running) return;
        fachada.detener();
        running = false;
        LoggerCentral.info(TAG, "Servidor de clientes detenido.");
    }

    @Override
    public boolean estaCorriendo() {
        return running;
    }

    @Override
    public ServicioNotificacionCliente getServicioNotificacion() {
        return servicioNotificacion;
    }
}