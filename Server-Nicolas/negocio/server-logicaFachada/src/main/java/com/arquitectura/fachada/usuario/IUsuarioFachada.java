package com.arquitectura.fachada.usuario;

import com.arquitectura.DTO.usuarios.LoginRequestDto;
import com.arquitectura.DTO.usuarios.UserRegistrationRequestDto;
import com.arquitectura.DTO.usuarios.UserResponseDto;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Fachada especializada para operaciones relacionadas con usuarios.
 */
public interface IUsuarioFachada {

    /**
     * Registra un nuevo usuario en el sistema.
     * @param requestDto Datos del usuario a registrar
     * @param ipAddress Dirección IP desde donde se registra
     * @throws Exception si hay error en el registro
     */
    void registrarUsuario(UserRegistrationRequestDto requestDto, String ipAddress) throws Exception;

    /**
     * Busca un usuario por su nombre de usuario.
     * @param username Nombre de usuario a buscar
     * @return Optional con el usuario si existe
     */
    Optional<UserResponseDto> buscarUsuarioPorUsername(String username);

    /**
     * Obtiene todos los usuarios registrados en el sistema.
     * @return Lista de todos los usuarios
     */
    List<UserResponseDto> obtenerTodosLosUsuarios();

    /**
     * Autentica un usuario en el sistema.
     * @param requestDto Credenciales del usuario
     * @param ipAddress Dirección IP desde donde se autentica
     * @return Datos del usuario autenticado
     * @throws Exception si las credenciales son inválidas
     */
    UserResponseDto autenticarUsuario(LoginRequestDto requestDto, String ipAddress) throws Exception;

    /**
     * Obtiene usuarios por sus IDs.
     * @param userIds Conjunto de IDs de usuarios
     * @return Lista de usuarios encontrados
     */
    List<UserResponseDto> getUsersByIds(Set<UUID> userIds);

    /**
     * Obtiene la lista de usuarios conectados actualmente.
     * @return Lista de usuarios conectados
     */
    List<UserResponseDto> obtenerUsuariosConectados();

    /**
     * Cambia el estado de conexión de un usuario.
     * @param userId ID del usuario
     * @param nuevoEstado Nuevo estado (true = conectado, false = desconectado)
     * @throws Exception si el usuario no existe
     */
    void cambiarEstadoUsuario(UUID userId, boolean nuevoEstado) throws Exception;

    /**
     * Lista los contactos disponibles, excluyendo un usuario específico.
     * @param excludeUserId ID del usuario a excluir de la lista
     * @return Lista de contactos
     */
    List<UserResponseDto> listarContactos(UUID excludeUserId);

    /**
     * Envía una notificación push al cliente para forzar su logout.
     * @param userId ID del usuario al que se le pedirá cerrar sesión
     * @param motivo Motivo del logout forzado (opcional)
     * @throws Exception si el usuario no existe
     */
    void enviarPedidoLogout(UUID userId, String motivo) throws Exception;
}

