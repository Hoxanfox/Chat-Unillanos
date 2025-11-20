package gestorP2P;

// CORRECCIÓN: Ajuste de paquetes a .impl e .interfaces
import comunicacion.RouterMensajesImpl;
import comunicacion.IRouterMensajes;
import conexion.GestorConexionesImpl;
import conexion.IGestorConexiones;

import dto.p2p.DTOPeerDetails;
import gestorP2P.interfaces.IServicioP2P;
import transporte.p2p.impl.NettyTransporteImpl;
import transporte.p2p.interfaces.ITransporteTcp;

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
        ITransporteTcp transporte = new NettyTransporteImpl(null);

        // Usamos la implementación concreta para poder usar métodos específicos si hiciera falta
        // (aunque aquí lo guardamos en la interfaz, instanciamos la impl correcta)
        GestorConexionesImpl gestorImpl = new GestorConexionesImpl(transporte);
        this.gestorConexiones = gestorImpl;

        this.routerMensajes = new RouterMensajesImpl(gestorConexiones);

        // Inyección circular necesaria para que el gestor pueda pasar mensajes al router
        gestorImpl.setRouterMensajes(this.routerMensajes);
    }

    /**
     * Inicia el sistema delegando la lógica a los servicios registrados.
     * Se asume que un servicio (ej. ServicioGestionRed) se encargará de leer config y arrancar.
     */
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

    public void registrarServicio(IServicioP2P servicio) {
        if (enEjecucion) {
            throw new IllegalStateException("No se pueden registrar servicios con el nodo en ejecución.");
        }
        servicios.add(servicio);
        // IMPORTANTE: Registramos las rutas del servicio en el router central
        servicio.registrarRutas(this.routerMensajes);
        System.out.println("[FachadaP2P] Servicio registrado: " + servicio.getNombre());
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