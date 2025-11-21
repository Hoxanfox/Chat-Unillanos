package com.arquitectura.logicaUsuarios.ProfileService;

import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.domain.User;
import com.arquitectura.logicaUsuarios.mappers.UserMapper;
import com.arquitectura.persistence.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserProfileServiceImpl implements IUserProfileService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Autowired
    public UserProfileServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> obtenerTodosLosUsuarios() {
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getUsersByIds(Set<UUID> userIds) {
        return userRepository.findAllById(userIds).stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserResponseDto> buscarPorUsername(String username) {
        return userRepository.findByUsername(username)
                .map(userMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findEntityById(UUID id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional
    public void cambiarEstadoUsuario(UUID userId, boolean conectado) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setConectado(conectado);
            userRepository.save(user);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> listarContactos(UUID excludeUserId) {
        return userRepository.findAll().stream()
                .filter(user -> !user.getUserId().equals(excludeUserId))
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }
}