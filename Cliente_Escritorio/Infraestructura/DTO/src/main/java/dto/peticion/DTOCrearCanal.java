package dto.peticion;

import java.io.Serializable;

/**
 * DTO (Data Transfer Object) que encapsula los datos necesarios para
 * la petición de creación de un nuevo canal. Este objeto se serializa
 * y se envía como el 'payload' en un DTORequest.
 */
public class DTOCrearCanal implements Serializable {
    private final String nombre;
    private final String tipo;

    /**
     * Constructor para el DTO de creación de canal.
     *
     * @param nombre El nombre deseado para el canal.
     * @param tipo   El tipo de canal (por defecto "GRUPO" si es null).
     */
    public DTOCrearCanal(String nombre, String tipo) {
        this.nombre = nombre;
        this.tipo = tipo != null ? tipo : "GRUPO";
    }

    /**
     * Constructor simplificado que asume tipo "GRUPO".
     *
     * @param nombre El nombre deseado para el canal.
     */
    public DTOCrearCanal(String nombre) {
        this(nombre, "GRUPO");
    }

    // Getters que serán utilizados por la librería de serialización (Gson)

    public String getNombre() {
        return nombre;
    }

    public String getTipo() {
        return tipo;
    }
}
