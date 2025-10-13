package dto.featureContactos;

/**
 * DTO que representa la información de un contacto para ser mostrada en la vista.
 */
public class DTOContacto {
    private final String id; // ID único del usuario
    private final String nombre;
    private final String estado;

    public DTOContacto(String id, String nombre, String estado) {
        this.id = id;
        this.nombre = nombre;
        this.estado = estado;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEstado() {
        return estado;
    }
}

