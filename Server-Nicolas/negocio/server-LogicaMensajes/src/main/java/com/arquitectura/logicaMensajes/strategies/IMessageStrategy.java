package com.arquitectura.logicaMensajes.strategies;

import com.arquitectura.DTO.Mensajes.SendMessageRequestDto;
import com.arquitectura.domain.Channel;
import com.arquitectura.domain.Message;
import com.arquitectura.domain.User;

public interface IMessageStrategy {
    /**
     * Procesa el contenido y guarda el mensaje en la base de datos.
     */
    Message procesarYGuardar(SendMessageRequestDto requestDto, User autor, Channel canal) throws Exception;

    /**
     * Indica si esta estrategia soporta el tipo de mensaje dado.
     */
    boolean soportaTipo(String messageType);
}