package com.unillanos.server.dto;

/**
 * DTO para el registro de nuevos usuarios.
 */
public class DTORegistro {
    
    private String nombre;      // Requerido, 3-50 caracteres
    private String email;       // Requerido, formato válido, único
    private String password;    // Requerido, mínimo 8 caracteres
    private String photoId;     // Opcional, referencia a archivo

    public DTORegistro() {
    }

    public DTORegistro(String nombre, String email, String password, String photoId) {
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.photoId = photoId;
    }

    // Getters y Setters
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhotoId() {
        return photoId;
    }

    public void setPhotoId(String photoId) {
        this.photoId = photoId;
    }

    @Override
    public String toString() {
        return "DTORegistro{" +
                "nombre='" + nombre + '\'' +
                ", email='" + email + '\'' +
                ", photoId='" + photoId + '\'' +
                '}'; // NO incluir password en toString por seguridad
    }
}

