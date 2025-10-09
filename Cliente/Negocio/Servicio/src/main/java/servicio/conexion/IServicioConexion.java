package servicio.conexion;

import java.util.concurrent.CompletableFuture;

/**
 * Contrato para el servicio que gestiona la conexión automática.
 */
public interface IServicioConexion {

    /**
     * Inicia el proceso de conexión automática con el servidor,
     * utilizando la configuración definida en la capa de negocio.
     * @return Una promesa que se resolverá con 'true' si la conexión es exitosa.
     */
    CompletableFuture<Boolean> conectar();
}

