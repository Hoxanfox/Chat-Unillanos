package dto.cliente;

public class DTOSesionCliente {

    // ID técnico de la conexión (ej. ip:puerto_efimero)
    private String idSesion;

    // ID de negocio (UUID del Usuario logueado). Null si no se ha autenticado.
    private String idUsuario;

    private String ip;
    private int puerto; // Puerto desde donde se conecta el cliente
    private String estado; // "CONECTADO", "AUTENTICADO"
    private String fechaConexion;

    public DTOSesionCliente(String idSesion, String ip, int puerto, String estado, String fechaConexion) {
        this.idSesion = idSesion;
        this.ip = ip;
        this.puerto = puerto;
        this.estado = estado;
        this.fechaConexion = fechaConexion;
    }

    // Getters y Setters
    public String getIdSesion() { return idSesion; }
    public String getIdUsuario() { return idUsuario; }

    // Vital para cuando el usuario hace Login
    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
        this.estado = "AUTENTICADO";
    }

    public String getIp() { return ip; }
    public int getPuerto() { return puerto; }
    public String getEstado() { return estado; }
    public String getFechaConexion() { return fechaConexion; }

    public boolean estaAutenticado() {
        return idUsuario != null;
    }
}