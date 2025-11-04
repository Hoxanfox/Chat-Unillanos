package com.arquitectura.DTO.canales;

import com.arquitectura.DTO.usuarios.UserResponseDto;

import java.util.UUID;

public class ChannelResponseDto {
    private UUID channelId;
    private String channelName;
    private String channelType;
    private UserResponseDto owner; // Usamos el DTO del usuario, no la entidad

    public ChannelResponseDto(UUID channelId, String channelName, String channelType, UserResponseDto owner) {
        this.channelId = channelId;
        this.channelName = channelName;
        this.channelType = channelType;
        this.owner = owner;
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
}