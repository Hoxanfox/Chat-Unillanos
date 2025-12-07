package repositorio.mensaje;

import dominio.MensajeRecibidoContacto;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio para mensajes recibidos de contactos.
 */
public interface IRepositorioMensajeRecibidoContacto {

    void guardar(MensajeRecibidoContacto mensaje);
    
    MensajeRecibidoContacto obtenerPorId(UUID idMensaje);
    
    List<MensajeRecibidoContacto> obtenerPorDestinatario(UUID idDestinatario);
    
    List<MensajeRecibidoContacto> obtenerPorRemitente(UUID idRemitente);
    
    List<MensajeRecibidoContacto> obtenerConversacion(UUID idRemitente, UUID idDestinatario);
    
    void eliminar(UUID idMensaje);
}

