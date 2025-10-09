package dto.featureContactos;

/**
 * DTO que representa la información de un contacto para ser mostrada en la vista.
 */
public class DTOContacto {
    private final String nombre;
    private final String estado; // "Online", "Away", "Offline"

    public DTOContacto(String nombre, String estado) {
        this.nombre = nombre;
        this.estado = estado;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEstado() {
        return estado;
    }
}
