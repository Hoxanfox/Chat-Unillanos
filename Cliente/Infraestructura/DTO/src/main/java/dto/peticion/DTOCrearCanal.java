package dto.peticion;

import java.io.Serializable;

/**
 * DTO (Data Transfer Object) que encapsula los datos necesarios para
 * la petición de creación de un nuevo canal. Este objeto se serializa
 * y se envía como el 'payload' en un DTORequest.
 */
public class DTOCrearCanal implements Serializable {
    private final String creadorId;
    private final String nombre;
    private final String descripcion;

    /**
     * Constructor para el DTO de creación de canal.
     *
     * @param creadorId   El ID del usuario que está creando el canal.
     * @param nombre      El nombre deseado para el canal.
     * @param descripcion Una descripción opcional para el canal.
     */
    public DTOCrearCanal(String creadorId, String nombre, String descripcion) {
        this.creadorId = creadorId;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    // Getters que serán utilizados por la librería de serialización (Gson)

    public String getCreadorId() {
        return creadorId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
