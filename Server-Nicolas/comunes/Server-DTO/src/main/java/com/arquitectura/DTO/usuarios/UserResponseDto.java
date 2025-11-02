package com.arquitectura.DTO.usuarios;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserResponseDto {
    private UUID userId;
    private String username;
    private String email;
    private String photoAddress;
    private LocalDateTime fechaRegistro;
    private String estado;

    // Constructor vacío
    public UserResponseDto(UUID userId, String username, String email, String photoAddress, LocalDateTime fechaRegistro) {}

    // Constructor con campos
    public UserResponseDto(UUID userId, String username, String email, String photoAddress, LocalDateTime fechaRegistro, String estado) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.photoAddress = photoAddress;
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
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public String getEstado() {
        return estado;
    }
    public void setEstado(String estado) {
        this.estado = estado;
    }
}