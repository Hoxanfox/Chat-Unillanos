package com.arquitectura.DTO.canales;

import java.util.UUID;

public class InviteMemberRequestDto {
    private UUID channelId;
    private UUID userIdToInvite;

    public InviteMemberRequestDto() {
    }

    public InviteMemberRequestDto(UUID channelId, UUID userIdToInvite) {
        this.channelId = channelId;
        this.userIdToInvite = userIdToInvite;
    }

    public UUID getUserIdToInvite() {
        return userIdToInvite;
    }

    public void setUserIdToInvite(UUID userIdToInvite) {
        this.userIdToInvite = userIdToInvite;
    }

    public UUID getChannelId() {
        return channelId;
    }

    public void setChannelId(UUID channelId) {
        this.channelId = channelId;
    }
}