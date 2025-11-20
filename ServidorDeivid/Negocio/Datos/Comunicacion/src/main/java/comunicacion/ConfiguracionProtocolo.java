package comunicacion;

import comunicacion.clienteServidor.AccionesCS;
import conexion.IGestorConexiones;

/**
 * Clase orquestadora para rutas que NO pertenecen a un servicio específico.
 * Nota: La mayoría de rutas ahora se registran dentro de los servicios (IServicioP2P),
 * por lo que esta clase quedará principalmente para lógica global o legacy.
 */
public class ConfiguracionProtocolo {

    private final AccionesCS accionesCS;

    public ConfiguracionProtocolo(IGestorConexiones gestorConexiones) {
        // AccionesP2P SE ELIMINÓ porque su lógica pasó a ServicioGestionRed
        this.accionesCS = new AccionesCS(gestorConexiones);
    }

    public void configurarRutas(IRouterMensajes router) {
        System.out.println("[Config] Cargando rutas generales...");

        // 1. Registrar acciones Cliente-Servidor (Login, Auth, etc.)
        accionesCS.registrarAcciones(router);

        System.out.println("[Config] Rutas generales cargadas.");
    }
}