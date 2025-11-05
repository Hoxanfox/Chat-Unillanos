package com.arquitectura.DTO.canales;

import com.arquitectura.DTO.usuarios.UserResponseDto;

import java.util.UUID;

public class ChannelResponseDto {
    private UUID channelId;
    private String channelName;
    private String channelType;
    private UserResponseDto owner; // Usamos el DTO del usuario, no la entidad
    private UUID peerId; // ID del peer/servidor donde está alojado el canal

    public ChannelResponseDto(UUID channelId, String channelName, String channelType, UserResponseDto owner, UUID peerId) {
        this.channelId = channelId;
        this.channelName = channelName;
        this.channelType = channelType;
        this.owner = owner;
        this.peerId = peerId;
    }

    public ChannelResponseDto() {}

    public UUID getChannelId() {
        return channelId;
    }

    public void setChannelId(UUID channelId) {
        this.channelId = channelId;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getChannelType() {
        return channelType;
    }

    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }

    public UserResponseDto getOwner() {
        return owner;
    }

    public void setOwner(UserResponseDto owner) {
        this.owner = owner;
    }

    public UUID getPeerId() {
        return peerId;
    }

    public void setPeerId(UUID peerId) {
        this.peerId = peerId;
    }
}