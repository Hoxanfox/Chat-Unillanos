package servidor.dto;

/**
 * DTO que representa un contacto/usuario en línea.
 */
public class DTOContacto {
    private String nombre;
    private String estado;

    public DTOContacto() {}

    public DTOContacto(String nombre, String estado) {
        this.nombre = nombre;
        this.estado = estado;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}

