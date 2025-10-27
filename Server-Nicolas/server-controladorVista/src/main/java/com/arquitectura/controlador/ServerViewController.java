package com.arquitectura.controlador;

import com.arquitectura.DTO.Mensajes.TranscriptionResponseDto;
import com.arquitectura.DTO.canales.ChannelResponseDto;
import com.arquitectura.DTO.usuarios.UserRegistrationRequestDto;
import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.events.ForceDisconnectEvent;
import com.arquitectura.fachada.IChatFachada;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
public class ServerViewController {

    private final IChatFachada chatFachada;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public ServerViewController(IChatFachada chatFachada, ApplicationEventPublisher eventPublisher) {
        this.chatFachada = chatFachada;
        this.eventPublisher = eventPublisher;
    }

    // metodos registro y mensaje

    public List<UserResponseDto> obtenerUsuariosRegistrados() {
        return chatFachada.obtenerTodosLosUsuarios();
    }

    public void enviarMensajeBroadcast(String contenido) {
        final int ADMIN_ID = 1; // Asumimos que el admin tiene el userId = 1
        try {
            chatFachada.enviarMensajeBroadcast(contenido, ADMIN_ID);
        } catch (Exception e) {
            e.printStackTrace(); // En una app real, usaríamos un logger
        }
    }

    public void registrarNuevoUsuario(UserRegistrationRequestDto requestDto) throws Exception {
        // La IP para un registro desde el servidor puede ser "localhost"
        chatFachada.registrarUsuario(requestDto, "127.0.0.1");
    }
    // MÉTODOS PARA LOS INFORMES

    public Map<ChannelResponseDto, List<UserResponseDto>> obtenerCanalesConMiembros() {
        return chatFachada.obtenerCanalesConMiembros();
    }
    public String getLogContents() {
        try {
            return chatFachada.getLogContents();
        } catch (IOException e) {
            return "Error al acceder a los logs: " + e.getMessage();
        }
    }
    public List<TranscriptionResponseDto> obtenerTranscripciones() {
        return chatFachada.obtenerTranscripciones();
    }

    public void disconnectUser(int userId) {
        eventPublisher.publishEvent(new ForceDisconnectEvent(this, userId));
    }
    public List<UserResponseDto> obtenerUsuariosConectados() {
        return chatFachada.obtenerUsuariosConectados();
    }



}