package com.arquitectura.controlador;

import com.arquitectura.DTO.usuarios.UserResponseDto;

/**
 * Handler temporal para capturar la respuesta de una petición retransmitida.
 * Este handler NO se conecta a ningún socket real, solo captura la respuesta
 * generada por el RequestDispatcher para poder retransmitirla al peer de origen.
 */
public class RetransmitClientHandler implements IClientHandler {
    
    private String capturedResponse;
    private boolean authenticated = false;
    private UserResponseDto authenticatedUser;

    @Override
    public void sendMessage(String message) {
        // En lugar de enviar a un socket, simplemente capturamos la respuesta
        this.capturedResponse = message;
    }

    @Override
    public void forceDisconnect() {
        // No hay conexión real que desconectar forzosamente
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticatedUser(UserResponseDto user) {
        this.authenticatedUser = user;
        this.authenticated = true;
    }

    @Override
    public UserResponseDto getAuthenticatedUser() {
        return authenticatedUser;
    }

    @Override
    public void clearAuthenticatedUser() {
        this.authenticatedUser = null;
        this.authenticated = false;
    }

    @Override
    public String getClientIpAddress() {
        // Para peticiones retransmitidas, la IP real del cliente no está disponible
        return "0.0.0.0";
    }

    /**
     * Obtiene la respuesta capturada durante el procesamiento
     * @return La respuesta JSON generada por el RequestDispatcher
     */
    public String getResponse() {
        return capturedResponse;
    }
}
