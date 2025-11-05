package com.unillanos.server.dto;

/**
 * DTO para crear un nuevo canal.
 */
public class DTOCrearCanal {
    
    private String creadorId;       // Requerido - Usuario que crea el canal
    private String nombre;          // Requerido, único, 3-50 caracteres
    private String descripcion;     // Opcional, máximo 200 caracteres

    public DTOCrearCanal() {
    }

    public DTOCrearCanal(String creadorId, String nombre, String descripcion) {
        this.creadorId = creadorId;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    // Getters y Setters
    public String getCreadorId() {
        return creadorId;
    }

    public void setCreadorId(String creadorId) {
        this.creadorId = creadorId;
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

    @Override
    public String toString() {
        return "DTOCrearCanal{" +
                "creadorId='" + creadorId + '\'' +
                ", nombre='" + nombre + '\'' +
                ", descripcion='" + descripcion + '\'' +
                '}';
    }
}

