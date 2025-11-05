package com.unillanos.server.dto;

/**
 * DTO para actualizar el perfil de un usuario.
 */
public class DTOActualizarPerfil {
    
    private String userId;          // Requerido (del usuario autenticado)
    private String nombre;          // Opcional
    private String photoId;         // Opcional
    private String passwordActual;  // Requerido si se cambia password
    private String passwordNueva;   // Opcional

    public DTOActualizarPerfil() {
    }

    public DTOActualizarPerfil(String userId, String nombre, String photoId, 
                               String passwordActual, String passwordNueva) {
        this.userId = userId;
        this.nombre = nombre;
        this.photoId = photoId;
        this.passwordActual = passwordActual;
        this.passwordNueva = passwordNueva;
    }

    // Getters y Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPhotoId() {
        return photoId;
    }

    public void setPhotoId(String photoId) {
        this.photoId = photoId;
    }

    public String getPasswordActual() {
        return passwordActual;
    }

    public void setPasswordActual(String passwordActual) {
        this.passwordActual = passwordActual;
    }

    public String getPasswordNueva() {
        return passwordNueva;
    }

    public void setPasswordNueva(String passwordNueva) {
        this.passwordNueva = passwordNueva;
    }

    @Override
    public String toString() {
        return "DTOActualizarPerfil{" +
                "userId='" + userId + '\'' +
                ", nombre='" + nombre + '\'' +
                ", photoId='" + photoId + '\'' +
                '}'; // NO incluir passwords en toString por seguridad
    }
}

