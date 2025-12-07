package dominio;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

/**
 * Entidad de Dominio: Canal
 * Representa un canal de comunicación grupal.
 */
public class Canal {
    private UUID idCanal;
    private String nombre;
    private UUID idAdministrador;
    // Nueva lista de miembros (IDs de usuario)
    private List<UUID> miembros = new ArrayList<>();

    public Canal() {
    }

    public Canal(UUID idCanal, String nombre, UUID idAdministrador) {
        this.idCanal = idCanal;
        this.nombre = nombre;
        this.idAdministrador = idAdministrador;
    }

    /**
     * Constructor extendido que permite inicializar miembros.
     */
    public Canal(UUID idCanal, String nombre, UUID idAdministrador, List<UUID> miembros) {
        this.idCanal = idCanal;
        this.nombre = nombre;
        this.idAdministrador = idAdministrador;
        this.miembros = miembros != null ? new ArrayList<>(miembros) : new ArrayList<>();
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

    public List<UUID> getMiembros() {
        return new ArrayList<>(miembros);
    }

    public void setMiembros(List<UUID> miembros) {
        this.miembros = miembros != null ? new ArrayList<>(miembros) : new ArrayList<>();
    }

    public boolean agregarMiembro(UUID usuarioId) {
        if (usuarioId == null) return false;
        if (!miembros.contains(usuarioId)) {
            miembros.add(usuarioId);
            return true;
        }
        return false;
    }

    public boolean removerMiembro(UUID usuarioId) {
        if (usuarioId == null) return false;
        return miembros.remove(usuarioId);
    }

    @Override
    public String toString() {
        return "Canal{" +
                "idCanal=" + idCanal +
                ", nombre='" + nombre + '\'' +
                ", idAdministrador=" + idAdministrador +
                ", miembros=" + miembros +
                '}';
    }
}