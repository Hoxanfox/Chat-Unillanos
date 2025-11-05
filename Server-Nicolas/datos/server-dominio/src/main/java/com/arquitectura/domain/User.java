package com.arquitectura.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity // 1. Le dice a JPA que esta clase es una tabla de la base de datos.
@Table(name = "users") // 2. Especifica el nombre de la tabla.
public class User {

    @Id // 3. Marca este campo como la clave primaria (primary key).
    @GeneratedValue(strategy = GenerationType.AUTO) // 4. Le dice a la BD que genere el ID automáticamente.
    @Column(name = "user_id",updatable = false, nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servidor_padre", referencedColumnName = "id")
    private Peer peerId;

    @Column(nullable = false, unique = true) // 5. Campo no nulo y único.
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "hashed_password", nullable = false)
    private String hashedPassword;

    @Column(name = "photo_address")
    private String photoAddress;

    @Column(nullable = false)
    private String ipAddress;

    @Column(nullable = false )
    private Boolean conectado  = false;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    // Constructor vacío (útil para frameworks)
    public User() {
    }

    // Constructor para crear un nuevo usuario

    public User(String username, String email, String hashedPassword, String ipAddress) {
        this.username = username;
        this.email = email;
        this.hashedPassword = hashedPassword;
        this.ipAddress = ipAddress;
        this.conectado = false;
        this.fechaRegistro = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }

    // --- Getters y Setters ---


    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    public Peer getPeerId() {
        return peerId;
    }
    public void setPeerId(Peer peerId) {
        this.peerId = peerId;
    }

    public Boolean getConectado() {
        return conectado;
    }

    public void setConectado(Boolean conectado) {
        this.conectado = conectado;
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

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}