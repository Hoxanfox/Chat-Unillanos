package com.unillanos.server.entity;

import com.unillanos.server.dto.DTOCanal;

import java.time.LocalDateTime;

/**
 * Entidad que representa un canal en la base de datos.
 */
public class CanalEntity {
    
    private String id;              // UUID
    private String nombre;
    private String descripcion;
    private String creadorId;
    private LocalDateTime fechaCreacion;
    private boolean activo;

    // Constructores
    public CanalEntity() {
    }

    public CanalEntity(String id, String nombre, String descripcion, String creadorId,
                       LocalDateTime fechaCreacion, boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.creadorId = creadorId;
        this.fechaCreacion = fechaCreacion;
        this.activo = activo;
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

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    /**
     * Convierte la entidad a DTO.
     *
     * @param cantidadMiembros Cantidad de miembros del canal
     * @return DTOCanal con los datos del canal
     */
    public DTOCanal toDTO(int cantidadMiembros) {
        DTOCanal dto = new DTOCanal();
        dto.setId(this.id);
        dto.setNombre(this.nombre);
        dto.setDescripcion(this.descripcion);
        dto.setCreadorId(this.creadorId);
        dto.setFechaCreacion(this.fechaCreacion != null ? this.fechaCreacion.toString() : null);
        dto.setActivo(this.activo);
        dto.setCantidadMiembros(cantidadMiembros);
        return dto;
    }

    @Override
    public String toString() {
        return "CanalEntity{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", creadorId='" + creadorId + '\'' +
                ", fechaCreacion=" + fechaCreacion +
                ", activo=" + activo +
                '}';
    }
}
