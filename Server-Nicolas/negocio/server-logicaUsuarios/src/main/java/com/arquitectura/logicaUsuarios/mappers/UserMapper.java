package com.arquitectura.logicaUsuarios.mappers;

import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.domain.User;
import com.arquitectura.utils.file.IFileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    private final IFileStorageService fileStorageService;
    @Autowired
    public UserMapper(IFileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }
    public UserResponseDto toDto(User user) {
        if (user == null) return null;

        // Lógica de carga de imagen movida aquí
        String imagenBase64 = null;
        if (user.getPhotoAddress() != null && !user.getPhotoAddress().isEmpty()) {
            try {
                imagenBase64 = fileStorageService.readFileAsBase64(user.getPhotoAddress());
            } catch (Exception e) {
                System.err.println("Error al leer foto de perfil para usuario " + user.getUserId() + ": " + e.getMessage());
                // No fallamos todo el proceso si la imagen falla, simplemente va null
            }
        }

        UserResponseDto dto = new UserResponseDto(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhotoAddress(),
                imagenBase64,
                user.getFechaRegistro(),
                user.getConectado() ? "ONLINE" : "OFFLINE"
        );

        // Mapear PeerID si existe
        if (user.getPeerId() != null) {
            dto.setPeerId(user.getPeerId().getPeerId());
        }

        return dto;
    }

}
