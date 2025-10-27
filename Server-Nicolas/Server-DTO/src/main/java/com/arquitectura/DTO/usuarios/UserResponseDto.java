package com.arquitectura.DTO.usuarios;

public class UserResponseDto {
    private int userId;
    private String username;
    private String email;
    private String photoAddress;

    // Constructor vacío
    public UserResponseDto() {}

    // Constructor con campos
    public UserResponseDto(int userId, String username, String email, String photoAddress) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.photoAddress = photoAddress;
    }

    // Getters y Setters
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhotoAddress() { return photoAddress; }
    public void setPhotoAddress(String photoAddress) { this.photoAddress = photoAddress; }
}