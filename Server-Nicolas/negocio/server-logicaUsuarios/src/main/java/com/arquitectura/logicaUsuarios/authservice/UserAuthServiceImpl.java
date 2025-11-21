package com.arquitectura.logicaUsuarios.authservice;

import com.arquitectura.DTO.usuarios.LoginRequestDto;
import com.arquitectura.DTO.usuarios.UserRegistrationRequestDto;
import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.domain.Peer;
import com.arquitectura.domain.User;
import com.arquitectura.events.UserRegisteredEvent;
import com.arquitectura.logicaPeers.IPeerService;
import com.arquitectura.logicaUsuarios.mappers.UserMapper;
import com.arquitectura.persistence.repository.UserRepository;
import com.arquitectura.utils.file.IFileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;

@Service
public class UserAuthServiceImpl implements IUserAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final IFileStorageService fileStorageService;
    private final IPeerService peerService;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public UserAuthServiceImpl(UserRepository userRepository,
                               IFileStorageService fileStorageService,
                               IPeerService peerService,
                               UserMapper userMapper,
                               ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.peerService = peerService;
        this.userMapper = userMapper;
        this.eventPublisher = eventPublisher;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    @Transactional
    public void registrarUsuario(UserRegistrationRequestDto requestDto, String ipAddress) throws Exception {
        if (userRepository.findByUsername(requestDto.getUsername()).isPresent()) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso");
        }
        if (userRepository.findByEmail(requestDto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        // Guardar foto (lógica de negocio de registro)
        String photoPath = null;
        if (requestDto.getPhotoFilePath() != null && !requestDto.getPhotoFilePath().isEmpty()) {
            File photoFile = new File(requestDto.getPhotoFilePath());
            if (photoFile.exists()) {
                photoPath = fileStorageService.storeFile(photoFile, requestDto.getUsername(), "user_photos");
            }
        }

        String hashedPassword = passwordEncoder.encode(requestDto.getPassword());
        Peer currentPeer = peerService.obtenerPeerActual();

        User newUser = new User(requestDto.getUsername(), requestDto.getEmail(), hashedPassword, ipAddress);
        newUser.setPhotoAddress(photoPath);
        newUser.setPeerId(currentPeer);

        User savedUser = userRepository.save(newUser);

        // Publicar evento para enviar email
        eventPublisher.publishEvent(new UserRegisteredEvent(this, savedUser.getEmail(), savedUser.getUsername(), requestDto.getPassword()));
    }

    @Override
    @Transactional
    public UserResponseDto autenticarUsuario(LoginRequestDto requestDto, String ipAddress) throws Exception {
        User user = userRepository.findByUsername(requestDto.getUsername())
                .orElseThrow(() -> new Exception("Credenciales incorrectas."));

        if (!passwordEncoder.matches(requestDto.getPassword(), user.getHashedPassword())) {
            throw new Exception("Credenciales incorrectas.");
        }

        user.setIpAddress(ipAddress);
        user.setConectado(true);
        userRepository.save(user);

        return userMapper.toDto(user);
    }
}