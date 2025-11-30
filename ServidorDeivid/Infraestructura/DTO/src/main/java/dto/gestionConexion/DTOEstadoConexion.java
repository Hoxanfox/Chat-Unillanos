package dto.gestionConexion;

/**
 * DTO con información de estado de la conexión (simple).
 */
public class DTOEstadoConexion {
    private final boolean conectado;
    private final String servidor;
    private final int ping;
    private final String estado;

    public DTOEstadoConexion(boolean conectado, String servidor, int ping, String estado) {
        this.conectado = conectado;
        this.servidor = servidor;
        this.ping = ping;
        this.estado = estado;
    }

    public boolean isConectado() {
        return conectado;
    }

    public String getServidor() {
        return servidor;
    }

    public int getPing() {
        return ping;
    }

    public String getEstado() {
        return estado;
    }
}

