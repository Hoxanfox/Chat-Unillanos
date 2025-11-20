package gestorP2P.interfaces;

import comunicacion.IRouterMensajes;
import conexion.IGestorConexiones;

public interface IServicioP2P {

    /**
     * Método de inicialización técnica.
     * La Fachada llama a esto automáticamente al registrar el servicio.
     * Aquí el servicio recibe sus dependencias (Gestor y Router) y registra sus rutas.
     */
    void inicializar(IGestorConexiones gestor, IRouterMensajes router);

    /**
     * Lógica de arranque funcional (timers, threads, etc).
     * Se llama cuando se inicia la red.
     */
    void iniciar();

    void detener();

    String getNombre();
}