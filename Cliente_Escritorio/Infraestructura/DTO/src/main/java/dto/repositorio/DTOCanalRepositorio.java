package dto.repositorio;

import java.io.Serializable;

/**
 * DTO para transferir la información de un canal a la capa de Repositorio.
 * Permite que la capa de negocio no dependa de las entidades de dominio.
 */
public class DTOCanalRepositorio implements Serializable {
    private final String id;
    private final String nombre;
    private final String idAdministrador;

    public DTOCanalRepositorio(String id, String nombre, String idAdministrador) {
        this.id = id;
        this.nombre = nombre;
        this.idAdministrador = idAdministrador;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getIdAdministrador() {
        return idAdministrador;
    }
}
