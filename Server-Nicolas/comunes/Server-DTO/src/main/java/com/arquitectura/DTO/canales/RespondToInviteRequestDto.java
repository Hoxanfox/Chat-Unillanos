package com.arquitectura.DTO.canales;

import java.util.UUID;

public class RespondToInviteRequestDto {
    private UUID channelId;
    private boolean accepted;

    public RespondToInviteRequestDto() {
    }

    public RespondToInviteRequestDto(UUID channelId, boolean accepted) {
        this.channelId = channelId;
        this.accepted = accepted;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public UUID getChannelId() {
        return channelId;
    }

    public void setChannelId(UUID channelId) {
        this.channelId = channelId;
    }
}