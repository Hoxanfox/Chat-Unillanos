package fachada.gestionConexion;
import java.util.concurrent.CompletableFuture;

/**
 * Contrato para la Fachada que gestiona la conexión con el servidor.
 * Es el punto de entrada desde el Servicio a esta lógica de negocio.
 */
public interface IFachadaConexion {

    /**
     * Inicia el proceso de conexión automática.
     * @return Una promesa que se resolverá con 'true' si la conexión es exitosa.
     */
    CompletableFuture<Boolean> conectar();
}