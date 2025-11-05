package com.arquitectura.controlador;

import com.arquitectura.DTO.usuarios.UserResponseDto;

public interface IClientHandler {
    void sendMessage(String message);
    String getClientIpAddress();
    void setAuthenticatedUser(UserResponseDto user);
    UserResponseDto getAuthenticatedUser();
    boolean isAuthenticated();
    void clearAuthenticatedUser();
    void forceDisconnect();
}