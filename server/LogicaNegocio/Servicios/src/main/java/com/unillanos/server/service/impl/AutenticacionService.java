package com.unillanos.server.service.impl;

import com.unillanos.server.config.ServerConfigProperties;
import com.unillanos.server.dto.*;
import com.unillanos.server.exception.AuthenticationException;
import com.unillanos.server.exception.DuplicateResourceException;
import com.unillanos.server.exception.NotFoundException;
import com.unillanos.server.exception.ValidationException;
import com.unillanos.server.repository.interfaces.IUsuarioRepository;
import com.unillanos.server.repository.models.EstadoUsuario;
import com.unillanos.server.repository.models.UsuarioEntity;
import com.unillanos.server.service.util.PasswordHasher;
import com.unillanos.server.validation.NombreValidator;
import com.unillanos.server.validation.PasswordValidator;
import com.unillanos.server.validation.RegistroValidator;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Servicio de autenticación de usuarios.
 * Maneja registro, login, logout, actualización de perfil y gestión de estados.
 */
@Service
public class AutenticacionService {
    
    private static final Logger logger = LoggerFactory.getLogger(AutenticacionService.class);
    
    private final IUsuarioRepository usuarioRepository;
    private final LoggerService loggerService;
    private final ConnectionManager connectionManager;
    private final ServerConfigProperties config;

    public AutenticacionService(IUsuarioRepository usuarioRepository,
                                LoggerService loggerService,
                                ConnectionManager connectionManager,
                                ServerConfigProperties config) {
        this.usuarioRepository = usuarioRepository;
        this.loggerService = loggerService;
        this.connectionManager = connectionManager;
        this.config = config;
    }

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * @param dto Datos del usuario a registrar
     * @param ipAddress IP del cliente
     * @return DTOResponse con el usuario creado
     */
    public DTOResponse registrarUsuario(DTORegistro dto, String ipAddress) {
        try {
            // 1. Validar datos con RegistroValidator
            RegistroValidator.validate(dto, config);
            
            // 2. Verificar que el email no exista
            if (usuarioRepository.existsByEmail(dto.getEmail())) {
                throw new DuplicateResourceException("Usuario", "email", dto.getEmail());
            }
            
            // 3. Hashear la contraseña con BCrypt
            String passwordHash = PasswordHasher.hash(dto.getPassword(), config);
            
            // 4. Crear UsuarioEntity con ID UUID y estado OFFLINE
            UsuarioEntity usuario = new UsuarioEntity();
            usuario.setId(UUID.randomUUID().toString());
            usuario.setNombre(dto.getNombre().trim());
            usuario.setEmail(dto.getEmail().toLowerCase().trim());
            usuario.setPasswordHash(passwordHash);
            usuario.setPhotoId(dto.getPhotoId());
            usuario.setIpAddress(ipAddress);
            usuario.setFechaRegistro(LocalDateTime.now());
            usuario.setEstado(EstadoUsuario.OFFLINE);
            
            // 5. Guardar en BD
            UsuarioEntity usuarioGuardado = usuarioRepository.save(usuario);
            
            // 6. Registrar en logs
            loggerService.logInfo("registro", 
                String.format("Nuevo usuario registrado: %s (%s)", 
                    usuarioGuardado.getNombre(), usuarioGuardado.getEmail()));
            
            // 7. Retornar DTOResponse.success con DTOUsuario (SIN password_hash)
            return DTOResponse.success("registro", 
                "Usuario registrado exitosamente", 
                usuarioGuardado.toDTO());
                
        } catch (ValidationException | DuplicateResourceException e) {
            throw e; // Re-lanzar para que GlobalExceptionHandler las maneje
        } catch (Exception e) {
            logger.error("Error inesperado en registro de usuario", e);
            throw new RuntimeException("Error al registrar usuario", e);
        }
    }

    /**
     * Autentica un usuario (login).
     *
     * @param dto Credenciales de login
     * @param ctx Contexto de Netty para registrar conexión
     * @param ipAddress IP del cliente
     * @return DTOResponse con el usuario autenticado
     */
    public DTOResponse autenticarUsuario(DTOLogin dto, ChannelHandlerContext ctx, String ipAddress) {
        try {
            // 1. Validar que email y password no estén vacíos
            if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
                throw new ValidationException("El email es requerido", "email");
            }
            if (dto.getPassword() == null || dto.getPassword().isEmpty()) {
                throw new ValidationException("La contraseña es requerida", "password");
            }
            
            // 2. Buscar usuario por email
            UsuarioEntity usuario = usuarioRepository.findByEmail(dto.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new AuthenticationException("Credenciales inválidas", "EMAIL_NOT_FOUND"));
            
            // 3. Verificar contraseña con BCrypt
            if (!PasswordHasher.verify(dto.getPassword(), usuario.getPasswordHash())) {
                throw new AuthenticationException("Credenciales inválidas", "INVALID_PASSWORD");
            }
            
            // 4. Actualizar estado a ONLINE
            usuarioRepository.updateEstado(usuario.getId(), EstadoUsuario.ONLINE);
            usuario.setEstado(EstadoUsuario.ONLINE);
            
            // 5. Actualizar IP del usuario
            usuarioRepository.updateIpAddress(usuario.getId(), ipAddress);
            
            // 6. Registrar conexión en ConnectionManager
            connectionManager.registerConnection(usuario.getId(), ctx);
            
            // 7. Registrar login en logs
            loggerService.logLogin(usuario.getId(), ipAddress);
            
            // 8. Retornar DTOResponse.success con DTOUsuario
            logger.info("Usuario autenticado: {} ({})", usuario.getNombre(), usuario.getEmail());
            return DTOResponse.success("login", 
                "Autenticación exitosa", 
                usuario.toDTO());
                
        } catch (ValidationException | AuthenticationException e) {
            // Registrar intento fallido
            loggerService.logError("login", 
                String.format("Intento fallido de login: %s desde IP: %s - Razón: %s", 
                    dto.getEmail(), ipAddress, e.getMessage()));
            throw e;
        } catch (Exception e) {
            logger.error("Error inesperado en autenticación", e);
            throw new RuntimeException("Error al autenticar usuario", e);
        }
    }

    /**
     * Cierra la sesión de un usuario (logout).
     *
     * @param userId ID del usuario
     * @param ipAddress IP del cliente
     * @return DTOResponse confirmando el logout
     */
    public DTOResponse logout(String userId, String ipAddress) {
        try {
            // 1. Verificar que el usuario existe
            UsuarioEntity usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuario", userId));
            
            // 2. Actualizar estado a OFFLINE
            usuarioRepository.updateEstado(userId, EstadoUsuario.OFFLINE);
            
            // 3. Eliminar del ConnectionManager
            connectionManager.removeConnection(userId);
            
            // 4. Registrar logout en logs
            loggerService.logLogout(userId, ipAddress);
            
            // 5. Retornar DTOResponse.success
            logger.info("Usuario desconectado: {} ({})", usuario.getNombre(), usuario.getEmail());
            return DTOResponse.success("logout", 
                "Sesión cerrada exitosamente", 
                null);
                
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error inesperado en logout", e);
            throw new RuntimeException("Error al cerrar sesión", e);
        }
    }

    /**
     * Actualiza el perfil de un usuario.
     *
     * @param dto Datos a actualizar
     * @return DTOResponse con el usuario actualizado
     */
    public DTOResponse actualizarPerfil(DTOActualizarPerfil dto) {
        try {
            // 1. Validar que userId no esté vacío
            if (dto.getUserId() == null || dto.getUserId().trim().isEmpty()) {
                throw new ValidationException("El ID del usuario es requerido", "userId");
            }
            
            // 2. Buscar usuario por ID
            UsuarioEntity usuario = usuarioRepository.findById(dto.getUserId())
                .orElseThrow(() -> new NotFoundException("Usuario", dto.getUserId()));
            
            boolean actualizado = false;
            
            // 3. Si se cambia el nombre, validar y actualizar
            if (dto.getNombre() != null && !dto.getNombre().trim().isEmpty()) {
                NombreValidator.validate(dto.getNombre());
                usuario.setNombre(dto.getNombre().trim());
                actualizado = true;
            }
            
            // Si se cambia la foto de perfil
            if (dto.getPhotoId() != null) {
                usuario.setPhotoId(dto.getPhotoId());
                actualizado = true;
            }
            
            // 4. Si se cambia la contraseña
            if (dto.getPasswordNueva() != null && !dto.getPasswordNueva().isEmpty()) {
                // Validar passwordActual
                if (dto.getPasswordActual() == null || dto.getPasswordActual().isEmpty()) {
                    throw new ValidationException("Se requiere la contraseña actual para cambiarla", "passwordActual");
                }
                
                if (!PasswordHasher.verify(dto.getPasswordActual(), usuario.getPasswordHash())) {
                    throw new AuthenticationException("La contraseña actual es incorrecta", "INVALID_CURRENT_PASSWORD");
                }
                
                // Validar passwordNueva
                PasswordValidator.validate(dto.getPasswordNueva(), config);
                
                // Hashear nueva contraseña
                usuario.setPasswordHash(PasswordHasher.hash(dto.getPasswordNueva(), config));
                actualizado = true;
            }
            
            if (!actualizado) {
                throw new ValidationException("No hay datos para actualizar", "dto");
            }
            
            // 5. Guardar en BD
            usuarioRepository.update(usuario);
            
            // 6. Registrar en logs
            loggerService.logInfo("actualizarPerfil", 
                String.format("Perfil actualizado: %s (%s)", usuario.getNombre(), usuario.getEmail()));
            
            // 7. Retornar DTOResponse.success con DTOUsuario actualizado
            logger.info("Perfil actualizado: {}", usuario.getEmail());
            return DTOResponse.success("actualizarPerfil", 
                "Perfil actualizado exitosamente", 
                usuario.toDTO());
                
        } catch (ValidationException | NotFoundException | AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error inesperado en actualización de perfil", e);
            throw new RuntimeException("Error al actualizar perfil", e);
        }
    }

    /**
     * Cambia el estado de un usuario (ONLINE, OFFLINE, AWAY).
     *
     * @param dto Datos del cambio de estado
     * @return DTOResponse confirmando el cambio
     */
    public DTOResponse cambiarEstado(DTOCambiarEstado dto) {
        try {
            // 1. Validar que el userId no esté vacío
            if (dto.getUserId() == null || dto.getUserId().trim().isEmpty()) {
                throw new ValidationException("El ID del usuario es requerido", "userId");
            }
            
            // Validar que el estado sea válido
            EstadoUsuario nuevoEstado;
            try {
                nuevoEstado = EstadoUsuario.valueOf(dto.getNuevoEstado().toUpperCase());
            } catch (Exception e) {
                throw new ValidationException("Estado inválido. Debe ser ONLINE, OFFLINE o AWAY", "nuevoEstado");
            }
            
            // 2. Verificar que el usuario existe
            UsuarioEntity usuario = usuarioRepository.findById(dto.getUserId())
                .orElseThrow(() -> new NotFoundException("Usuario", dto.getUserId()));
            
            // 3. Actualizar estado en BD
            usuarioRepository.updateEstado(dto.getUserId(), nuevoEstado);
            
            // 4. Si cambia a OFFLINE, eliminar del ConnectionManager
            if (nuevoEstado == EstadoUsuario.OFFLINE) {
                connectionManager.removeConnection(dto.getUserId());
            }
            
            // 5. Registrar en logs
            loggerService.logInfo("cambiarEstado", 
                String.format("Estado cambiado: %s -> %s", usuario.getEmail(), nuevoEstado));
            
            // 6. Retornar DTOResponse.success
            logger.info("Estado cambiado: {} -> {}", usuario.getEmail(), nuevoEstado);
            return DTOResponse.success("cambiarEstado", 
                "Estado actualizado exitosamente", 
                null);
                
        } catch (ValidationException | NotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error inesperado en cambio de estado", e);
            throw new RuntimeException("Error al cambiar estado", e);
        }
    }
}

