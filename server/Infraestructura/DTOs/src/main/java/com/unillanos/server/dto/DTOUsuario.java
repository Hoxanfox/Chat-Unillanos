package com.unillanos.server.dto;

/**
 * DTO de respuesta con información del usuario.
 * NUNCA incluye el password_hash por seguridad.
 */
public class DTOUsuario {
    
    private String id;              // UUID
    private String nombre;
    private String email;
    private String photoId;
    private String estado;          // ONLINE, OFFLINE, AWAY
    private String fechaRegistro;   // ISO-8601

    public DTOUsuario() {
    }

    public DTOUsuario(String id, String nombre, String email, String photoId, 
                      String estado, String fechaRegistro) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.photoId = photoId;
        this.estado = estado;
        this.fechaRegistro = fechaRegistro;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhotoId() {
        return photoId;
    }

    public void setPhotoId(String photoId) {
        this.photoId = photoId;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(String fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    @Override
    public String toString() {
        return "DTOUsuario{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                ", email='" + email + '\'' +
                ", photoId='" + photoId + '\'' +
                ", estado='" + estado + '\'' +
                ", fechaRegistro='" + fechaRegistro + '\'' +
                '}';
    }
}

