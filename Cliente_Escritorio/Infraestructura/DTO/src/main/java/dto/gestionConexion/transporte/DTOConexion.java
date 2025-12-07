package dto.gestionConexion.transporte;


/**
 * DTO (Data Transfer Object) para encapsular los datos de conexión al servidor.
 * Es inmutable para garantizar la integridad de los datos.
 */
public final class DTOConexion {

    private final String host;
    private final int puerto;

    /**
     * Constructor para inicializar los datos de conexión.
     * @param host La dirección IP o nombre de host del servidor.
     * @param puerto El puerto del servidor.
     */
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