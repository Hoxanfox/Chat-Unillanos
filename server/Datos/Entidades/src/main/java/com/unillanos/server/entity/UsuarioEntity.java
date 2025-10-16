package com.unillanos.server.entity;

import java.time.LocalDateTime;

/**
 * Entidad que representa un usuario en la base de datos.
 */
public class UsuarioEntity {
    private String id;
    private String nombre;
    private String email;
    private String passwordHash;
    private String photoId;
    private String ipAddress;
    private LocalDateTime fechaRegistro;
    private EstadoUsuario estado; // ONLINE, OFFLINE, AWAY

    public UsuarioEntity() {
    }

    public UsuarioEntity(String id, String nombre, String email, String passwordHash,
                        String photoId, String ipAddress, LocalDateTime fechaRegistro, EstadoUsuario estado) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.passwordHash = passwordHash;
        this.photoId = photoId;
        this.ipAddress = ipAddress;
        this.fechaRegistro = fechaRegistro;
        this.estado = estado;
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPhotoId() {
        return photoId;
    }

    public void setPhotoId(String photoId) {
        this.photoId = photoId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public EstadoUsuario getEstado() {
        return estado;
    }

    public void setEstado(EstadoUsuario estado) {
        this.estado = estado;
    }

    /**
     * Convierte la entidad a DTO (sin exponer el password_hash).
     * 
     * @return DTOUsuario con los datos del usuario
     */
    public com.unillanos.server.dto.DTOUsuario toDTO() {
        com.unillanos.server.dto.DTOUsuario dto = new com.unillanos.server.dto.DTOUsuario();
        dto.setId(this.id);
        dto.setNombre(this.nombre);
        dto.setEmail(this.email);
        dto.setPhotoId(this.photoId);
        dto.setEstado(this.estado != null ? this.estado.name() : EstadoUsuario.OFFLINE.name());
        dto.setFechaRegistro(this.fechaRegistro != null ? this.fechaRegistro.toString() : null);
        return dto;
    }

    @Override
    public String toString() {
        return "UsuarioEntity{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                ", email='" + email + '\'' +
                ", photoId='" + photoId + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", fechaRegistro=" + fechaRegistro +
                ", estado=" + estado +
                '}';
    }
}
