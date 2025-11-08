package com.arquitectura.DTO.usuarios;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserResponseDto {
    private UUID userId;
    private String username;
    private String email;
    private String photoAddress; // Ruta en el servidor (para uso interno)
    private String imagenBase64; // Imagen codificada (para enviar al cliente)
    private LocalDateTime fechaRegistro;
    private String estado;
    private String rol; // "ADMIN" o "MIEMBRO"
    private UUID peerId; // ID del servidor padre (peer)

    // Constructor con 5 parámetros
    public UserResponseDto(UUID userId, String username, String email, String photoAddress, LocalDateTime fechaRegistro) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.photoAddress = photoAddress;
        this.fechaRegistro = fechaRegistro;
    }

    // Constructor con campos
    public UserResponseDto(UUID userId, String username, String email, String photoAddress, LocalDateTime fechaRegistro, String estado) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.photoAddress = photoAddress;
        this.fechaRegistro = fechaRegistro;
        this.estado = estado;
    }

    // Constructor completo con imagenBase64
    public UserResponseDto(UUID userId, String username, String email, String photoAddress, String imagenBase64, LocalDateTime fechaRegistro, String estado) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.photoAddress = photoAddress;
        this.imagenBase64 = imagenBase64;
        this.fechaRegistro = fechaRegistro;
        this.estado = estado;
    }

    // Getters y Setters
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhotoAddress() { return photoAddress; }
    public void setPhotoAddress(String photoAddress) { this.photoAddress = photoAddress; }
    public String getImagenBase64() { return imagenBase64; }
    public void setImagenBase64(String imagenBase64) { this.imagenBase64 = imagenBase64; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public String getEstado() {
        return estado;
    }
    public void setEstado(String estado) {
        this.estado = estado;
    }
    public String getRol() {
        return rol;
    }
    public void setRol(String rol) {
        this.rol = rol;
    }
    public UUID getPeerId() {
        return peerId;
    }
    public void setPeerId(UUID peerId) {
        this.peerId = peerId;
    }
}