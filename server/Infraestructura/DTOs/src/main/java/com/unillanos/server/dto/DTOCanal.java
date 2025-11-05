package com.unillanos.server.dto;

/**
 * DTO de respuesta con información de un canal.
 */
public class DTOCanal {
    
    private String id;              // UUID del canal
    private String nombre;
    private String descripcion;
    private String creadorId;
    private String fechaCreacion;   // ISO-8601
    private boolean activo;
    private int cantidadMiembros;   // Cantidad de miembros en el canal

    public DTOCanal() {
    }

    public DTOCanal(String id, String nombre, String descripcion, String creadorId,
                    String fechaCreacion, boolean activo, int cantidadMiembros) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.creadorId = creadorId;
        this.fechaCreacion = fechaCreacion;
        this.activo = activo;
        this.cantidadMiembros = cantidadMiembros;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getCreadorId() {
        return creadorId;
    }

    public void setCreadorId(String creadorId) {
        this.creadorId = creadorId;
    }

    public String getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(String fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public int getCantidadMiembros() {
        return cantidadMiembros;
    }

    public void setCantidadMiembros(int cantidadMiembros) {
        this.cantidadMiembros = cantidadMiembros;
    }

    @Override
    public String toString() {
        return "DTOCanal{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", creadorId='" + creadorId + '\'' +
                ", fechaCreacion='" + fechaCreacion + '\'' +
                ", activo=" + activo +
                ", cantidadMiembros=" + cantidadMiembros +
                '}';
    }
}

