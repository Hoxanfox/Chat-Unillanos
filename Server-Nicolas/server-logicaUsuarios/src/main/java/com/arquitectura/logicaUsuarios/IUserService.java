package com.arquitectura.logicaUsuarios;

import com.arquitectura.DTO.usuarios.LoginRequestDto;
import com.arquitectura.DTO.usuarios.UserRegistrationRequestDto;
import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.domain.User; // Se mantiene para un método interno
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface IUserService {
    /**
     * Registra un nuevo usuario en el sistema.
     * @param requestDto El DTO que contiene la información del usuario a registrar.
     * @param ipAddress La dirección IP desde la cual se realiza el registro.
     * @return Un DTO que contiene la información del usuario registrado.
     * @throws Exception Si ocurre un error durante el registro.
     */
    UserResponseDto registrarUsuario(UserRegistrationRequestDto requestDto, String ipAddress) throws Exception;

    /**
     * Busca un usuario por su nombre de usuario.
     * @param username El nombre de usuario a buscar.
     * @return Un Optional que contiene el DTO del usuario si se encuentra.
     */
    Optional<UserResponseDto> buscarPorUsername(String username);

    /**
     * Obtiene una lista de todos los usuarios registrados.
     * @return Una lista de DTOs de todos los usuarios.
     */
    List<UserResponseDto> obtenerTodosLosUsuarios();
    List<UserResponseDto> getUsersByIds(Set<Integer> userIds);
    /**
     * Busca una entidad de Usuario por su ID.
     * Este método es para uso interno de la capa de negocio (ej. la fachada).
     * @param id El ID del usuario a buscar.
     * @return Un Optional que contiene la entidad User si se encuentra.
     */
    Optional<User> findEntityById(int id);
    /**
     * Autentica a un usuario con su nombre de usuario y contraseña.
     * @param requestDto DTO con las credenciales.
     * @return Un DTO con la información del usuario si la autenticación es exitosa.
     * @throws Exception si las credenciales son incorrectas o el usuario no existe.
     */
    UserResponseDto autenticarUsuario(LoginRequestDto requestDto, String ipAddress) throws Exception;
}
