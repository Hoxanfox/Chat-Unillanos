package com.unillanos.server.netty.handler;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.unillanos.server.dto.DTORequest;
import com.unillanos.server.dto.DTOResponse;
import com.unillanos.server.service.impl.ConnectionManager;
import com.unillanos.server.service.interfaces.IActionDispatcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler de Netty que procesa las peticiones de los clientes.
 * Deserializa el JSON, delega al ActionDispatcher y serializa la respuesta.
 * Gestiona el ciclo de vida de las conexiones con ConnectionManager.
 */
public class ClientRequestHandler extends ChannelInboundHandlerAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(ClientRequestHandler.class);
    private final Gson gson = new Gson();
    private final IActionDispatcher actionDispatcher;
    private final ConnectionManager connectionManager;

    public ClientRequestHandler(IActionDispatcher actionDispatcher, ConnectionManager connectionManager) {
        this.actionDispatcher = actionDispatcher;
        this.connectionManager = connectionManager;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        logger.info("Cliente conectado: {}", ctx.channel().remoteAddress());
        // Nota: El usuario no se registra aquí, se registrará cuando se autentique
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            String json = (String) msg;
            logger.debug("Mensaje recibido: {}", json);
            
            // Deserializar DTORequest
            DTORequest request = gson.fromJson(json, DTORequest.class);
            
            // Despachar al servicio correspondiente
            DTOResponse response = actionDispatcher.dispatch(request, ctx);
            
            // Serializar y enviar respuesta
            String responseJson = gson.toJson(response);
            ctx.writeAndFlush(responseJson + "\n");
            
        } catch (JsonSyntaxException e) {
            logger.error("Error al parsear JSON", e);
            DTOResponse errorResponse = DTOResponse.error("unknown", "Formato JSON inválido");
            ctx.writeAndFlush(gson.toJson(errorResponse) + "\n");
        } catch (Exception e) {
            logger.error("Error al procesar petición", e);
            DTOResponse errorResponse = DTOResponse.error("unknown", "Error interno del servidor");
            ctx.writeAndFlush(gson.toJson(errorResponse) + "\n");
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.info("Cliente desconectado: {}", ctx.channel().remoteAddress());
        
        // Intentar eliminar la conexión del ConnectionManager
        String userId = connectionManager.removeConnectionByContext(ctx);
        if (userId != null) {
            logger.info("Usuario desregistrado del ConnectionManager: {}", userId);
            // En fases posteriores, aquí se actualizará el estado del usuario en BD
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Excepción en el canal: {}", ctx.channel().remoteAddress(), cause);
        
        // Limpiar la conexión del ConnectionManager antes de cerrar
        connectionManager.removeConnectionByContext(ctx);
        
        // Cerrar el canal
        ctx.close();
    }
}

