package com.arquitectura.events;

import com.arquitectura.DTO.canales.ChannelResponseDto;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

public class UserInvitedEvent extends ApplicationEvent {

    private final UUID invitedUserId;
    private final ChannelResponseDto channelDto;

    public UserInvitedEvent(Object source, UUID invitedUserId, ChannelResponseDto channelDto) {
        super(source);
        this.invitedUserId = invitedUserId;
        this.channelDto = channelDto;
    }

    public UUID getInvitedUserId() {
        return invitedUserId;
    }

    public ChannelResponseDto getChannelDto() {
        return channelDto;
    }
}