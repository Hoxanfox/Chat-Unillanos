package com.arquitectura.logicaUsuarios;

import com.arquitectura.DTO.usuarios.LoginRequestDto;
import com.arquitectura.DTO.usuarios.UserRegistrationRequestDto;
import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.domain.Peer;
import com.arquitectura.domain.User;
import com.arquitectura.persistence.repository.PeerRepository;
import com.arquitectura.utils.file.FileStorageService;
import com.arquitectura.utils.mail.EmailService;
import com.arquitectura.utils.network.NetworkUtils;
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
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;
    private final PeerRepository peerRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final FileStorageService fileStorageService;
    private final NetworkUtils networkUtils;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, EmailService emailService,
                          FileStorageService fileStorageService, PeerRepository peerRepository,
                          NetworkUtils networkUtils) {
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.emailService = emailService;
        this.peerRepository = peerRepository;
        this.networkUtils = networkUtils;
    }

    @Override
    @Transactional
    public void registrarUsuario(UserRegistrationRequestDto requestDto, String ipAddress) throws Exception {
        // Validar que el username no exista
        if (userRepository.findByUsername(requestDto.getUsername()).isPresent()) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso");
        }
        
        // Validar que el email no exista
        if (userRepository.findByEmail(requestDto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("El email ya está registrado");
        }
        //guardar la foto si existe
        String photoPath = null;
        if (requestDto.getPhotoFilePath() != null && !requestDto.getPhotoFilePath().isEmpty()) {
            File photoFile = new File(requestDto.getPhotoFilePath());
            if (photoFile.exists()) {
                photoPath = fileStorageService.storeFile(photoFile, requestDto.getUsername(), "user_photos");
            }
        }

        String hashedPassword = passwordEncoder.encode(requestDto.getPassword());

        // Obtener automáticamente la IP del servidor donde se ejecuta la aplicación
        String serverPeerAddress = networkUtils.getServerIPAddress();

        Peer currentPeer = peerRepository.findByIp(serverPeerAddress)
                .orElseGet(() -> peerRepository.save(new Peer(serverPeerAddress, 9000, "ONLINE")));

        User newUserEntity = new User(
                requestDto.getUsername(),
                requestDto.getEmail(),
                hashedPassword,
                ipAddress
        );

        // Asignamos la ruta de la foto guardada
        newUserEntity.setPhotoAddress(photoPath);
        newUserEntity.setPeerId(currentPeer);

        User savedUser = userRepository.save(newUserEntity);

        emailService.enviarCredenciales(savedUser.getEmail(), savedUser.getUsername(), requestDto.getPassword());
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
        user.setConectado(true);
        userRepository.save(user);

        // 3. Convertir la foto a Base64 si existe (LÓGICA DE NEGOCIO)
        String imagenBase64 = getImagenBase64(user);

        // 4. Retornar el DTO con la imagen codificada
        return new UserResponseDto(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhotoAddress(),
                imagenBase64, // Imagen codificada en Base64
                user.getFechaRegistro(),
                "ONLINE"
        );
    }

    private String getImagenBase64(User user) {
        String imagenBase64 = null;
        if (user.getPhotoAddress() != null && !user.getPhotoAddress().isEmpty()) {
            try {
                imagenBase64 = fileStorageService.readFileAsBase64(user.getPhotoAddress());
            } catch (Exception e) {
                System.err.println("Error al leer la foto del usuario: " + e.getMessage());
                // Si hay error, imagenBase64 queda null
            }
        }
        return imagenBase64;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getUsersByIds(Set<UUID> userIds) {
        return userRepository.findAllById(userIds).stream()
                .map(user -> new UserResponseDto(
                        user.getUserId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getPhotoAddress(),
                        user.getFechaRegistro(),
                        user.getConectado() ? "ONLINE" : "OFFLINE"))
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
                        user.getPhotoAddress(),
                        user.getFechaRegistro(),
                        user.getConectado() ? "ONLINE" : "OFFLINE"))
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
                        user.getPhotoAddress(),
                        user.getFechaRegistro(),
                        user.getConectado() ? "ONLINE" : "OFFLINE"));
    }

    // Devuelve la entidad para uso interno
    @Override
    @Transactional(readOnly = true)
    public Optional<User> findEntityById(UUID id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional
    public void cambiarEstadoUsuario(UUID userId, boolean estado) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setConectado(estado);
            userRepository.save(user);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> listarContactos(UUID excludeUserId) {
        return userRepository.findAll().stream()
                .filter(user -> !user.getUserId().equals(excludeUserId)) // Excluir al usuario actual
                .map(user -> {
                    String imagenBase64 = getImagenBase64(user);
                    return new UserResponseDto(
                            user.getUserId(),
                            user.getUsername(),
                            user.getEmail(),
                            user.getPhotoAddress(),
                            imagenBase64, // Incluir imagen en Base64
                            user.getFechaRegistro(),
                            user.getConectado() ? "ONLINE" : "OFFLINE"
                    );
                })
                .collect(Collectors.toList());
    }

}
