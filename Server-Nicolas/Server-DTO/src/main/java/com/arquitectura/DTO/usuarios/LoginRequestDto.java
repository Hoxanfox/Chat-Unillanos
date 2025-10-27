package com.arquitectura.DTO.usuarios;

public class LoginRequestDto {
    private String username;
    private String password;

    public LoginRequestDto(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getters
    public String getUsername() { return username; }
    public String getPassword() { return password; }
}