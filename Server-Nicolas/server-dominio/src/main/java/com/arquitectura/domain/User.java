package com.arquitectura.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

@Entity // 1. Le dice a JPA que esta clase es una tabla de la base de datos.
@Table(name = "users") // 2. Especifica el nombre de la tabla.
public class User {

    @Id // 3. Marca este campo como la clave primaria (primary key).
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 4. Le dice a la BD que genere el ID automáticamente.
    @Column(name = "user_id")
    private int userId;

    @Column(nullable = false, unique = true) // 5. Campo no nulo y único.
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String hashedPassword;

    @Column(name = "photo_address")
    private String photoAddress;

    @Column(nullable = false)
    private String ipAddress;

    // ... el resto de la clase (constructores, getters, setters) no cambia ...

    // Constructor vacío (útil para frameworks)
    public User() {
    }

    // Constructor para crear un nuevo usuario
    public User(String username, String email, String hashedPassword, String ipAddress) {
        this.username = username;
        this.email = email;
        this.hashedPassword = hashedPassword;
        this.ipAddress = ipAddress;
    }

    // --- Getters y Setters ---

    public int getUserId() {
        return userId;
    }

    public void setUserId(int id) {
        this.userId = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public String getPhotoAddress() {
        return photoAddress;
    }

    public void setPhotoAddress(String photoAddress) {
        this.photoAddress = photoAddress;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}