package com.arquitectura.DTO.canales;

public class InviteMemberRequestDto {
    private int channelId;
    private int userIdToInvite;

    public InviteMemberRequestDto() {
    }

    public InviteMemberRequestDto(int channelId, int userIdToInvite) {
        this.channelId = channelId;
        this.userIdToInvite = userIdToInvite;
    }

    public int getUserIdToInvite() {
        return userIdToInvite;
    }

    public void setUserIdToInvite(int userIdToInvite) {
        this.userIdToInvite = userIdToInvite;
    }

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }
}