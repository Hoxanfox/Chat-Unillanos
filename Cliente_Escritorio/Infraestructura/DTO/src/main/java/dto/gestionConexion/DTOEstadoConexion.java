package dto.gestionConexion;

/**
 * DTO que representa el estado actual de la conexión con el servidor.
 */
public class DTOEstadoConexion {
    
    private final boolean conectado;
    private final String servidor;
    private final int ping; // en milisegundos
    private final String mensaje;
    
    public DTOEstadoConexion(boolean conectado, String servidor, int ping, String mensaje) {
        this.conectado = conectado;
        this.servidor = servidor;
        this.ping = ping;
        this.mensaje = mensaje;
    }
    
    public boolean isConectado() { return conectado; }
    public String getServidor() { return servidor; }
    public int getPing() { return ping; }
    public String getMensaje() { return mensaje; }
    
    public String getEstadoTexto() {
        return conectado ? "Connected" : "Disconnected";
    }
    
    public String getPingTexto() {
        return "Ping: " + ping + "ms";
    }
}
