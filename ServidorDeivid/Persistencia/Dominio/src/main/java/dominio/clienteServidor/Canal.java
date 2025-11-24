package dominio.clienteServidor;

import dominio.merkletree.IMerkleEntity;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Canal implements Serializable, IMerkleEntity {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private UUID peerPadre;
    private Usuario creador; // Referencia al objeto (en BD es creador_id)

    // Estas listas son útiles en memoria, pero NO se usan para el hash de la entidad Canal
    private List<Usuario> miembros = new ArrayList<>();
    private List<Mensaje> historial = new ArrayList<>();

    public enum Tipo { PRIVADO, PUBLICO }

    private Tipo tipo = Tipo.PUBLICO;
    private Instant fechaCreacion;
    private String nombre;

    public Canal() {
        this.id = UUID.randomUUID();
        this.fechaCreacion = Instant.now();
    }

    public Canal(UUID id, UUID peerPadre, Usuario creador, Tipo tipo, String nombre, Instant fechaCreacion) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.peerPadre = peerPadre;
        this.creador = creador;
        this.tipo = tipo == null ? Tipo.PUBLICO : tipo;
        this.nombre = nombre;
        this.fechaCreacion = fechaCreacion == null ? Instant.now() : fechaCreacion;
    }

    // --- IMPLEMENTACIÓN MERKLE ---
    @Override
    public String getId() {
        return id.toString();
    }

    @Override
    public String getDatosParaHash() {
        // Solo hasheamos los datos planos de la tabla 'canales'
        return id.toString() + "|" +
                (peerPadre != null ? peerPadre.toString() : "") + "|" +
                (creador != null ? creador.getId().toString() : "") + "|" + // Usamos el ID del creador
                (nombre != null ? nombre : "") + "|" +
                (tipo != null ? tipo.name() : "") + "|" +
                (fechaCreacion != null ? fechaCreacion.toString() : "");
    }

    // Getters y setters
    public void setId(UUID id) { this.id = id; }
    public UUID getPeerPadre() { return peerPadre; }
    public void setPeerPadre(UUID peerPadre) { this.peerPadre = peerPadre; }
    public Usuario getCreador() { return creador; }
    public void setCreador(Usuario creador) { this.creador = creador; }
    public List<Usuario> getMiembros() { return miembros; }
    public void setMiembros(List<Usuario> miembros) { this.miembros = miembros == null ? new ArrayList<>() : miembros; }
    public List<Mensaje> getHistorial() { return historial; }
    public void setHistorial(List<Mensaje> historial) { this.historial = historial == null ? new ArrayList<>() : historial; }
    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Instant fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public void addMiembro(Usuario u) { if (u != null && !miembros.contains(u)) miembros.add(u); }
    public void removeMiembro(Usuario u) { miembros.remove(u); }
    public void addMensaje(Mensaje m) { if (m != null) historial.add(m); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Canal canal = (Canal) o;
        return Objects.equals(id, canal.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "Canal{" + "id=" + id + ", nombre='" + nombre + '\'' + '}';
    }
}