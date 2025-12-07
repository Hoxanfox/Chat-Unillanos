package fachada.gestionConexion;

import gestionConexion.GestionConexionImpl;
import gestionConexion.IGestionConexion;

import java.util.concurrent.CompletableFuture;

/**
 * Implementación de la Fachada de conexión.
 * Delega la tarea al componente de gestión correspondiente.
 */
public class FachadaConexionImpl implements IFachadaConexion {

    // La fachada depende del componente de gestión.
    private final IGestionConexion gestionConexion;

    public FachadaConexionImpl() {
        // En una aplicación real, esta dependencia se inyectaría.
        this.gestionConexion = new GestionConexionImpl();
    }

    @Override
    public CompletableFuture<Boolean> conectar() {
        // Simplemente delega la llamada.
        return gestionConexion.conectar();
    }
}

