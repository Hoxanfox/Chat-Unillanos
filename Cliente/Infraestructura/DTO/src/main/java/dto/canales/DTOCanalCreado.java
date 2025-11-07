package dto.canales;

import dto.featureContactos.DTOContacto;
import java.io.Serializable;

/**
 * DTO (Data Transfer Object) que representa la información esencial de un canal
 * recién creado. Se utiliza para devolver el resultado desde la capa de negocio
 * hacia la capa de presentación sin exponer objetos de dominio.
 */
public class DTOCanalCreado implements Serializable {
    private final String id;
    private final String nombre;
    private String tipo;
    private DTOContacto owner;

    /**
     * Constructor para el DTO del canal creado.
     *
     * @param id     El UUID del canal como String.
     * @param nombre El nombre del canal.
     */
    public DTOCanalCreado(String id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public DTOContacto getOwner() {
        return owner;
    }

    public void setOwner(DTOContacto owner) {
        this.owner = owner;
    }

    @Override
    public String toString() {
        return "DTOCanalCreado{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                ", tipo='" + tipo + '\'' +
                '}';
    }
}
