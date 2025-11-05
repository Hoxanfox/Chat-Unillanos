package dto.featureContactos;

/**
 * DTO que representa la información de un contacto para ser mostrada en la vista.
 */
public class DTOContacto {
    private final String id;      // ID único del usuario
    private final String peerId;  // ID del peer WebRTC del contacto
    private final String nombre;
    private final String email;   // Email del contacto
    private final String estado;

    // Constructor sin peerId para compatibilidad
    public DTOContacto(String id, String nombre, String estado) {
        this.id = id;
        this.peerId = null;
        this.nombre = nombre;
        this.email = null;
        this.estado = estado;
    }

    // Constructor completo con todos los campos
    public DTOContacto(String id, String peerId, String nombre, String email, String estado) {
        this.id = id;
        this.peerId = peerId;
        this.nombre = nombre;
        this.email = email;
        this.estado = estado;
    }

    public String getId() {
        return id;
    }

    public String getPeerId() {
        return peerId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }

    public String getEstado() {
        return estado;
    }
}
