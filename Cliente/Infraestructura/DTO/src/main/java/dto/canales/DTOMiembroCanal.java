package dto.canales;

/**
 * DTO que representa a un miembro de un canal, usado para listar miembros.
 */
public class DTOMiembroCanal {
    private final String usuarioId;
    private final String nombreUsuario;
    private final String rol;
    private final String fechaUnion;

    public DTOMiembroCanal(String usuarioId, String nombreUsuario, String rol, String fechaUnion) {
        this.usuarioId = usuarioId;
        this.nombreUsuario = nombreUsuario;
        this.rol = rol;
        this.fechaUnion = fechaUnion;
    }

    // Getters y Setters
    public String getUsuarioId() {
        return usuarioId;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getRol() {
        return rol;
    }

    public String getFechaUnion() {
        return fechaUnion;
    }

    @Override
    public String toString() {
        return "DTOMiembroCanal{" +
                "usuarioId='" + usuarioId + '\'' +
                ", nombreUsuario='" + nombreUsuario + '\'' +
                ", rol='" + rol + '\'' +
                '}';
    }
}
