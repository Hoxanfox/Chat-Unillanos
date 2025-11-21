package com.arquitectura.logicaUsuarios.replicateuser;

import com.arquitectura.DTO.usuarios.UserRegistrationRequestDto;
import com.arquitectura.domain.User;
import com.arquitectura.persistence.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserReplicationServiceImpl implements IUserReplicationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserReplicationServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    @Transactional
    public void guardarUsuarioReplicado(UserRegistrationRequestDto dto) throws Exception {
        // 1. Idempotencia: Si el usuario ya existe, no hacemos nada (evita errores si llega doble mensaje)
        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            System.out.println("ℹ [Replicación] El usuario " + dto.getUsername() + " ya existe localmente. Omitiendo.");
            return;
        }

        // 2. Crear la entidad User
        // NOTA: En un entorno P2P real, deberíamos recibir el HASH de la contraseña para que sea idéntica.
        // Si tu DTO tiene la contraseña plana, la hasheamos aquí.
        String finalPassword = passwordEncoder.encode(dto.getPassword());

        User replicaUser = new User(
            dto.getUsername(),
            dto.getEmail(),
            finalPassword,
            "0.0.0.0" // IP genérica para indicar que es una réplica
        );

        // Forzamos estado desconectado al crearlo (solo el servidor origen sabe si está online)
        replicaUser.setConectado(false);
        
        // Si tienes lógica de fotos, aquí podrías manejar la URL o dejarla null por ahora
        replicaUser.setPhotoAddress(dto.getPhotoFilePath()); 

        // 3. Guardar en Base de Datos
        userRepository.save(replicaUser);
        
        System.out.println("✓ [Replicación] Usuario " + dto.getUsername() + " replicado y guardado en BD local.");
    }
}