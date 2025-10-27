package com.arquitectura.logicaUsuarios;

import com.arquitectura.DTO.usuarios.LoginRequestDto;
import com.arquitectura.DTO.usuarios.UserRegistrationRequestDto;
import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.domain.User;
import com.arquitectura.utils.file.FileStorageService;
import com.arquitectura.utils.mail.EmailService;
import com.arquitectura.persistence.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final FileStorageService fileStorageService;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, EmailService emailService, FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.emailService = emailService;
    }

    // Este método ahora recibe y devuelve DTOs
    @Override
    @Transactional
    public UserResponseDto registrarUsuario(UserRegistrationRequestDto requestDto, String ipAddress) throws Exception {
        if (userRepository.findByUsername(requestDto.getUsername()).isPresent()) {
            throw new Exception("El nombre de usuario ya está en uso.");
        }

        String photoPath = null;
        if (requestDto.getPhotoFilePath() != null && !requestDto.getPhotoFilePath().isEmpty()) {
            File photoFile = new File(requestDto.getPhotoFilePath());
            if (photoFile.exists()) {
                // El nuevo nombre del archivo será el username.
                photoPath = fileStorageService.storeFile(photoFile, requestDto.getUsername(), "user_photos");
            }
        }

        String hashedPassword = passwordEncoder.encode(requestDto.getPassword());

        User newUserEntity = new User(
                requestDto.getUsername(),
                requestDto.getEmail(),
                hashedPassword,
                ipAddress
        );

        // Asignamos la ruta de la foto guardada
        newUserEntity.setPhotoAddress(photoPath);

        User savedUser = userRepository.save(newUserEntity);

        // ... (el resto del método no cambia)
        emailService.enviarCredenciales(savedUser.getEmail(), savedUser.getUsername(), requestDto.getPassword());

        return new UserResponseDto(
                savedUser.getUserId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getPhotoAddress()
        );
    }
    @Override
    @Transactional
    public UserResponseDto autenticarUsuario(LoginRequestDto requestDto, String ipAddress) throws Exception {
        // 1. Buscar al usuario por su nombre de usuario
        User user = userRepository.findByUsername(requestDto.getUsername())
                .orElseThrow(() -> new Exception("Credenciales incorrectas.")); // Mensaje genérico por seguridad

        // 2. Verificar que la contraseña coincida
        if (!passwordEncoder.matches(requestDto.getPassword(), user.getHashedPassword())) {
            throw new Exception("Credenciales incorrectas.");
        }
        // Actualizamos la IP del usuario con la de la conexión actual
        user.setIpAddress(ipAddress);
        userRepository.save(user);

        // 3. Si todo está bien, mapear la entidad a un DTO de respuesta y devolverlo
        return new UserResponseDto(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhotoAddress()
        );
    }
    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getUsersByIds(Set<Integer> userIds) {
        return userRepository.findAllById(userIds).stream()
                .map(user -> new UserResponseDto(
                        user.getUserId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getPhotoAddress()))
                .collect(Collectors.toList());
    }

    // Devuelve una lista de DTOs
    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> obtenerTodosLosUsuarios() {
        return userRepository.findAll().stream()
                .map(user -> new UserResponseDto(
                        user.getUserId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getPhotoAddress()))
                .collect(Collectors.toList());
    }

    // Devuelve un DTO opcional
    @Override
    @Transactional(readOnly = true)
    public Optional<UserResponseDto> buscarPorUsername(String username) {
        return userRepository.findByUsername(username)
                .map(user -> new UserResponseDto(
                        user.getUserId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getPhotoAddress()));
    }

    // Devuelve la entidad para uso interno
    @Override
    @Transactional(readOnly = true)
    public Optional<User> findEntityById(int id) {
        return userRepository.findById(id);
    }
}