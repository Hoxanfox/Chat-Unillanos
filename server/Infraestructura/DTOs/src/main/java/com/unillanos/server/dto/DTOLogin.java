package com.unillanos.server.dto;

/**
 * DTO para autenticación de usuarios (login).
 */
public class DTOLogin {
    
    private String email;       // Requerido
    private String password;    // Requerido

    public DTOLogin() {
    }

    public DTOLogin(String email, String password) {
        this.email = email;
        this.password = password;
    }

    // Getters y Setters
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

    @Override
    public String toString() {
        return "DTOLogin{" +
                "email='" + email + '\'' +
                '}'; // NO incluir password en toString por seguridad
    }
}

