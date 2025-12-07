package controlador.conexion;

import observador.ISujeto;

import java.util.concurrent.CompletableFuture;

/**
 * Contrato para el controlador que gestiona la conexión automática con el servidor.
 */
public interface IControladorConexion extends ISujeto {

    /**
     * Inicia el proceso de conexión automática.
     * @return Una promesa que se resolverá con 'true' si la conexión es exitosa.
     */
    CompletableFuture<Boolean> conectar();

    /**
     * Solicita una actualización del estado de la conexión.
     */
    void solicitarActualizacionEstado();
}
