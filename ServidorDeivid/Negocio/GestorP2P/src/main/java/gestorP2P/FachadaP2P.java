package gestorP2P;

// Implementaciones del módulo Conexion
import conexion.impl.GestorConexionesImpl;
import conexion.impl.RouterMensajesImpl;

// Interfaces del módulo Conexion
import conexion.interfaces.IGestorConexiones;
import conexion.interfaces.IRouterMensajes;

import dto.p2p.DTOPeerDetails;
import gestorP2P.interfaces.IServicioP2P;

// YA NO IMPORTAMOS NADA DE TRANSPORTE AQUÍ
// import transporte... (Eliminado)

import java.util.ArrayList;
import java.util.List;

/**
 * Fachada Principal del sistema P2P.
 * Actúa como contenedor de infraestructura y servicios.
 */
public class FachadaP2P {

    private final IGestorConexiones gestorConexiones;
    private final IRouterMensajes routerMensajes;
    private final List<IServicioP2P> servicios;

    private boolean enEjecucion;

    public FachadaP2P() {
        this.servicios = new ArrayList<>();
        this.enEjecucion = false;

        // 1. Inicializar Infraestructura
        // AHORA: El Gestor crea su propio transporte internamente.
        GestorConexionesImpl gestorImpl = new GestorConexionesImpl();
        this.gestorConexiones = gestorImpl;

        this.routerMensajes = new RouterMensajesImpl(gestorConexiones);

        // Inyección circular (Gestor -> Router)
        // Como gestorImpl es del tipo concreto 'GestorConexionesImpl', podemos acceder al método setter.
        gestorImpl.setRouterMensajes(this.routerMensajes);
    }

    public void iniciar() {
        if (enEjecucion) {
            System.out.println("[FachadaP2P] El nodo ya está en ejecución.");
            return;
        }

        System.out.println("[FachadaP2P] Iniciando servicios registrados...");

        // Delegamos el inicio a cada servicio
        for (IServicioP2P servicio : servicios) {
            try {
                servicio.iniciar();
            } catch (Exception e) {
                System.err.println("[FachadaP2P] Error iniciando servicio " + servicio.getNombre() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        enEjecucion = true;
        System.out.println("[FachadaP2P] Sistema iniciado (Estado: EN EJECUCIÓN).");
    }

    /**
     * Registra un servicio e INYECTA las dependencias automáticamente.
     */
    public void registrarServicio(IServicioP2P servicio) {
        if (enEjecucion) {
            throw new IllegalStateException("No se pueden registrar servicios con el nodo en ejecución.");
        }
        servicios.add(servicio);

        // Usamos 'inicializar' para inyectar tanto el Gestor como el Router en el servicio.
        servicio.inicializar(this.gestorConexiones, this.routerMensajes);

        System.out.println("[FachadaP2P] Servicio registrado e inicializado: " + servicio.getNombre());
    }

    public <T extends IServicioP2P> T obtenerServicio(Class<T> claseServicio) {
        return servicios.stream()
                .filter(s -> claseServicio.isInstance(s))
                .map(claseServicio::cast)
                .findFirst()
                .orElse(null);
    }

    // Métodos delegados útiles

    public void conectarAPeer(String ip, int puerto) {
        gestorConexiones.conectarAPeer(ip, puerto);
    }

    public List<DTOPeerDetails> obtenerPeersConectados() {
        return gestorConexiones.obtenerDetallesPeers();
    }

    public void detener() {
        System.out.println("[FachadaP2P] Deteniendo sistema...");
        for (IServicioP2P servicio : servicios) {
            servicio.detener();
        }
        gestorConexiones.apagar();
        enEjecucion = false;
        System.out.println("[FachadaP2P] Sistema detenido.");
    }

    public IGestorConexiones getGestorConexiones() {
        return gestorConexiones;
    }
}