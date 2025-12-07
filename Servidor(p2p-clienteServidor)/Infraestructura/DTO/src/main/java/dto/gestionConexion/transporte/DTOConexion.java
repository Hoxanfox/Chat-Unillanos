package dto.gestionConexion.transporte;

/**
 * DTO con los parámetros necesarios para establecer una conexión de transporte.
 * Implementación mínima para el proyecto.
 */
public class DTOConexion {
    private final String host;
    private final int puerto;

    public DTOConexion(String host, int puerto) {
        this.host = host;
        this.puerto = puerto;
    }

    public String getHost() {
        return host;
    }

    public int getPuerto() {
        return puerto;
    }
}

