package dominio;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad de Dominio: Canal
 * Representa un canal de comunicación grupal.
 */
public class Canal {
    private UUID idCanal;
    private String nombre;
    private UUID idAdministrador;

    public Canal() {
    }

    public Canal(UUID idCanal, String nombre, UUID idAdministrador) {
        this.idCanal = idCanal;
        this.nombre = nombre;
        this.idAdministrador = idAdministrador;
    }

    // Getters y Setters
    public UUID getIdCanal() {
        return idCanal;
    }

    public void setIdCanal(UUID idCanal) {
        this.idCanal = idCanal;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public UUID getIdAdministrador() {
        return idAdministrador;
    }

    public void setIdAdministrador(UUID idAdministrador) {
        this.idAdministrador = idAdministrador;
    }

    @Override
    public String toString() {
        return "Canal{" +
                "idCanal=" + idCanal +
                ", nombre='" + nombre + '\'' +
                ", idAdministrador=" + idAdministrador +
                '}';
    }
}