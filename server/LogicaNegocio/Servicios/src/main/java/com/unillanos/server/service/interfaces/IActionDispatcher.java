package com.unillanos.server.service.interfaces;

import com.unillanos.server.dto.DTORequest;
import com.unillanos.server.dto.DTOResponse;
import io.netty.channel.ChannelHandlerContext;

/**
 * Interfaz para el despachador de acciones.
 * Enruta las peticiones del cliente a los servicios correspondientes.
 */
public interface IActionDispatcher {
    /**
     * Despacha una petición al servicio apropiado según la acción.
     *
     * @param request DTORequest con la acción y payload
     * @param ctx Contexto de Netty para la conexión del cliente
     * @return DTOResponse con el resultado de la operación
     */
    DTOResponse dispatch(DTORequest request, ChannelHandlerContext ctx);
}

