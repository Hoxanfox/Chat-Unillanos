package gestorClientes;

import conexion.clientes.impl.GestorConexionesClienteImpl;
import conexion.clientes.impl.RouterMensajesClienteImpl;
import conexion.clientes.interfaces.IGestorConexionesCliente;
import conexion.clientes.interfaces.IRouterMensajesCliente;
import gestorClientes.interfaces.IServicioCliente;
import logger.LoggerCentral;

import java.util.ArrayList;
import java.util.List;

/**
 * Fachada Principal del sistema Cliente-Servidor.
 * Gestiona la conexión con los usuarios finales (Apps móviles/Web).
 */
public class FachadaClientes {

    private static final String TAG = "FachadaClientes";
    private final IGestorConexionesCliente gestorClientes;
    private final IRouterMensajesCliente routerClientes;
    private final List<IServicioCliente> servicios;

    private boolean enEjecucion;

    public FachadaClientes() {
        this.servicios = new ArrayList<>();
        this.enEjecucion = false;

        // 1. Inicializar Infraestructura de Clientes
        // Usamos la implementación concreta para configuración interna
        GestorConexionesClienteImpl gestorImpl = new GestorConexionesClienteImpl();
        this.gestorClientes = gestorImpl;

        this.routerClientes = new RouterMensajesClienteImpl(gestorClientes);

        // Inyección circular (El gestor necesita el router para procesar mensajes entrantes)
        gestorImpl.setRouter(this.routerClientes);
    }

    public void iniciar(int puertoEscucha) {
        if (enEjecucion) {
            LoggerCentral.warn(TAG, "El servidor de clientes ya está corriendo.");
            return;
        }

        LoggerCentral.info(TAG, "Iniciando servicios de atención a clientes...");

        // Iniciar lógica de negocio de cada servicio
        for (IServicioCliente servicio : servicios) {
            try {
                servicio.iniciar();
            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error iniciando servicio " + servicio.getNombre() + ": " + e.getMessage());
            }
        }

        // Levantar el socket servidor (ej. puerto 8000)
        new Thread(() -> gestorClientes.iniciarServidor(puertoEscucha)).start();

        enEjecucion = true;
        LoggerCentral.info(TAG, "Servidor de Clientes ONLINE en puerto " + puertoEscucha);
    }

    public void registrarServicio(IServicioCliente servicio) {
        if (enEjecucion) {
            throw new IllegalStateException("No se pueden registrar servicios con el servidor en ejecución.");
        }
        servicios.add(servicio);

        // Inyección de dependencias
        servicio.inicializar(this.gestorClientes, this.routerClientes);

        LoggerCentral.info(TAG, "Servicio registrado: " + servicio.getNombre());
    }

    public <T extends IServicioCliente> T obtenerServicio(Class<T> claseServicio) {
        return servicios.stream()
                .filter(s -> claseServicio.isInstance(s))
                .map(claseServicio::cast)
                .findFirst()
                .orElse(null);
    }

    public void detener() {
        LoggerCentral.info(TAG, "Deteniendo servidor de clientes...");
        for (IServicioCliente servicio : servicios) {
            servicio.detener();
        }
        gestorClientes.apagar();
        enEjecucion = false;
    }

    public IGestorConexionesCliente getGestorClientes() {
        return gestorClientes;
    }
}