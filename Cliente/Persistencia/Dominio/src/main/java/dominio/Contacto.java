package dominio;

import java.util.UUID;

/**
 * Entidad de Dominio: Contacto
 * Representa un contacto en el sistema.
 */
public class Contacto {
    private UUID idContacto;
    private String nombre;
    private boolean estado;

    public Contacto() {
    }

    public Contacto(UUID idContacto, String nombre, boolean estado) {
        this.idContacto = idContacto;
        this.nombre = nombre;
        this.estado = estado;
    }

    // Getters y Setters
    public UUID getIdContacto() {
        return idContacto;
    }

    public void setIdContacto(UUID idContacto) {
        this.idContacto = idContacto;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return "Contacto{" +
                "idContacto=" + idContacto +
                ", nombre='" + nombre + '\'' +
                ", estado=" + estado +
                '}';
    }
}
