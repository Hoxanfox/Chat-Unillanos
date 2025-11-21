package com.arquitectura.logicaCanales;

import com.arquitectura.DTO.canales.ChannelResponseDto;
import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.domain.Channel;
import com.arquitectura.domain.User;
import org.springframework.stereotype.Component;

@Component
public class ChannelMapper {
    public ChannelResponseDto toChannelResponseDto(Channel channel) {
        if (channel == null) return null;

        UserResponseDto ownerDto = toUserResponseDto(channel.getOwner());
        // Obtener el peerId si existe
        java.util.UUID peerId = channel.getPeerId() != null ? channel.getPeerId().getPeerId() : null;

        return new ChannelResponseDto(
                channel.getChannelId(),
                channel.getName(),
                channel.getTipo().toString(),
                ownerDto,
                peerId
        );
    }
    public UserResponseDto toUserResponseDto(User user) {
        if (user == null) return null;

        UserResponseDto dto = new UserResponseDto(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhotoAddress(),
                user.getFechaRegistro()
        );

        // Mapear estado y rol si es necesario en otros contextos,
        // pero para el owner básico esto es suficiente.
        return dto;
    }
}
