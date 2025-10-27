package com.arquitectura.events;

import com.arquitectura.DTO.canales.ChannelResponseDto;
import org.springframework.context.ApplicationEvent;

public class UserInvitedEvent extends ApplicationEvent {

    private final int invitedUserId;
    private final ChannelResponseDto channelDto;

    public UserInvitedEvent(Object source, int invitedUserId, ChannelResponseDto channelDto) {
        super(source);
        this.invitedUserId = invitedUserId;
        this.channelDto = channelDto;
    }

    public int getInvitedUserId() {
        return invitedUserId;
    }

    public ChannelResponseDto getChannelDto() {
        return channelDto;
    }
}