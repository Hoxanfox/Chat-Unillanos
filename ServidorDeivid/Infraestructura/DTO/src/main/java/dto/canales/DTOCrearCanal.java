package dto.canales;

/**
 * DTO para crear un nuevo canal.
 * Usado en la ruta: crearcanal
 */
public class DTOCrearCanal {
    private String nombre;
    private String tipo; // "PRIVADO" o "PUBLICO"
    private String creadorId; // ID del usuario que crea el canal

    public DTOCrearCanal() {}

    public DTOCrearCanal(String nombre, String tipo) {
        this.nombre = nombre;
        this.tipo = tipo;
    }

    public DTOCrearCanal(String nombre, String tipo, String creadorId) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.creadorId = creadorId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getCreadorId() {
        return creadorId;
    }

    public void setCreadorId(String creadorId) {
        this.creadorId = creadorId;
    }

    @Override
    public String toString() {
        return "DTOCrearCanal{" +
                "nombre='" + nombre + '\'' +
                ", tipo='" + tipo + '\'' +
                ", creadorId='" + creadorId + '\'' +
                '}';
    }
}

