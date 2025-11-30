package dto.canales;

/**
 * DTO para representar un miembro de un canal.
 * Contiene información del usuario que pertenece al canal.
 */
public class DTOMiembroCanal {
    private String usuarioId;
    private String nombre;
    private String email;
    private String foto;
    private String estado; // ONLINE, OFFLINE

    public DTOMiembroCanal() {}

    public DTOMiembroCanal(String usuarioId, String nombre, String email, String foto, String estado) {
        this.usuarioId = usuarioId;
        this.nombre = nombre;
        this.email = email;
        this.foto = foto;
        this.estado = estado;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return "DTOMiembroCanal{" +
                "usuarioId='" + usuarioId + '\'' +
                ", nombre='" + nombre + '\'' +
                ", email='" + email + '\'' +
                ", foto='" + foto + '\'' +
                ", estado='" + estado + '\'' +
                '}';
    }
}

