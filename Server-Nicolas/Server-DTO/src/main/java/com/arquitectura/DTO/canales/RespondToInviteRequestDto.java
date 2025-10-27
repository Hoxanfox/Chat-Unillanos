package com.arquitectura.DTO.canales;

public class RespondToInviteRequestDto {
    private int channelId;
    private boolean accepted;

    public RespondToInviteRequestDto() {
    }

    public RespondToInviteRequestDto(int channelId, boolean accepted) {
        this.channelId = channelId;
        this.accepted = accepted;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }
}