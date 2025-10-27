package com.arquitectura.DTO.canales;

public class CreateChannelRequestDto {
    private String channelName;
    private String channelType; // "DIRECTO", "GRUPO", "BROADCAST"

    // Constructor vacío
    public CreateChannelRequestDto() {}

    // Constructor con campos
    public CreateChannelRequestDto(String channelName, String channelType) {
        this.channelName = channelName;
        this.channelType = channelType;
    }

    // Getters y Setters
    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }
    public String getChannelType() { return channelType; }
    public void setChannelType(String channelType) { this.channelType = channelType; }
}