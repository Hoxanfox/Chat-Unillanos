package com.arquitectura.fachada.usuario;

import com.arquitectura.DTO.usuarios.LoginRequestDto;
import com.arquitectura.DTO.usuarios.UserRegistrationRequestDto;
import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.events.ConnectedUsersRequestEvent;
import com.arquitectura.logicaUsuarios.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Implementación de la fachada de usuarios.
 * Coordina las operaciones relacionadas con usuarios del sistema.
 */
@Component
public class UsuarioFachadaImpl implements IUsuarioFachada {

    private final IUserService userService;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public UsuarioFachadaImpl(IUserService userService, ApplicationEventPublisher eventPublisher) {
        this.userService = userService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void registrarUsuario(UserRegistrationRequestDto requestDto, String ipAddress) throws Exception {
        userService.registrarUsuario(requestDto, ipAddress);
    }

    @Override
    public Optional<UserResponseDto> buscarUsuarioPorUsername(String username) {
        return userService.buscarPorUsername(username);
    }

    @Override
    public List<UserResponseDto> obtenerTodosLosUsuarios() {
        return userService.obtenerTodosLosUsuarios();
    }

    @Override
    public UserResponseDto autenticarUsuario(LoginRequestDto requestDto, String ipAddress) throws Exception {
        return userService.autenticarUsuario(requestDto, ipAddress);
    }

    @Override
    public List<UserResponseDto> getUsersByIds(Set<UUID> userIds) {
        return userService.getUsersByIds(userIds);
    }

    @Override
    public List<UserResponseDto> obtenerUsuariosConectados() {
        ConnectedUsersRequestEvent requestEvent = new ConnectedUsersRequestEvent(this);
        eventPublisher.publishEvent(requestEvent);
        Set<UUID> connectedUserIds = requestEvent.getResponseContainer();
        if (connectedUserIds.isEmpty()) {
            return new ArrayList<>();
        }
        return userService.getUsersByIds(connectedUserIds);
    }

    @Override
    public void cambiarEstadoUsuario(UUID userId, boolean nuevoEstado) throws Exception {
        userService.cambiarEstadoUsuario(userId, nuevoEstado);
    }

    @Override
    public List<UserResponseDto> listarContactos(UUID excludeUserId) {
        return userService.listarContactos(excludeUserId);
    }

    @Override
    public void enviarPedidoLogout(UUID userId, String motivo) throws Exception {
        // Verificar que el usuario existe
        UserResponseDto usuario = userService.getUsersByIds(Set.of(userId)).stream()
                .findFirst()
                .orElseThrow(() -> new Exception("Usuario con ID " + userId + " no encontrado"));

        // Publicar evento para enviar la notificación push de logout
        UUID peerId = usuario.getPeerId();
        eventPublisher.publishEvent(new com.arquitectura.events.ForceLogoutEvent(
                this,
                userId,
                peerId,
                motivo != null ? motivo : "Sesión cerrada por el servidor"
        ));
    }
}

