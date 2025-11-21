package com.arquitectura.logicaUsuarios.ProfileService;

import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface IUserProfileService {
    List<UserResponseDto> obtenerTodosLosUsuarios();
    List<UserResponseDto> getUsersByIds(Set<UUID> userIds);
    Optional<UserResponseDto> buscarPorUsername(String username);
    Optional<User> findEntityById(UUID id);
    void cambiarEstadoUsuario(UUID userId, boolean conectado);
    List<UserResponseDto> listarContactos(UUID excludeUserId);
}