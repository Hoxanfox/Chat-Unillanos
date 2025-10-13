package controlador.conexion;

import servicio.conexion.IServicioConexion;
import servicio.conexion.ServicioConexion;
import servicio.negocio.IServicioNegocio;
import servicio.negocio.ServicioNegocioImpl;

import java.util.concurrent.CompletableFuture;

/**
 * Implementación del controlador que AHORA orquesta la inicialización
 * a través de un servicio dedicado.
 */
public class ControladorConexion implements IControladorConexion {

    private final IServicioConexion servicioConexion;
    private final IServicioNegocio servicioNegocio; // Nueva dependencia

    public ControladorConexion() {
        this.servicioConexion = new ServicioConexion();
        this.servicioNegocio = new ServicioNegocioImpl(); // Se instancia el nuevo servicio
    }

    @Override
    public CompletableFuture<Boolean> conectar() {
        // 1. Se delega la conexión al servicio.
        return servicioConexion.conectar()
                .thenApply(conectado -> {
                    // 2. 'thenApply' se ejecuta cuando la promesa de conexión se completa.
                    if (conectado) {
                        // 3. Si la conexión fue exitosa, el Controlador llama al nuevo
                        //    servicio para inicializar la capa de negocio.
                        System.out.println("ControladorConexion: Conexión exitosa. Orquestando inicialización del negocio...");
                        servicioNegocio.inicializar();
                    }
                    // 4. Se devuelve el resultado booleano original a la Vista.
                    return conectado;
                });
    }
}



