package gestionConexion;

import java.util.concurrent.CompletableFuture;

/**
 * Contrato para el componente de negocio que gestiona el establecimiento de la conexión.
 */
public interface IGestionConexion {
    CompletableFuture<Boolean> conectar();
}
