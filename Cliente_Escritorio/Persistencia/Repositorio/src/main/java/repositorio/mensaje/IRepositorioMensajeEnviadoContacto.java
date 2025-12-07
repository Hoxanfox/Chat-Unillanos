package repositorio.mensaje;

import dominio.MensajeEnviadoContacto;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio para mensajes enviados a contactos.
 */
public interface IRepositorioMensajeEnviadoContacto {

    void guardar(MensajeEnviadoContacto mensaje);

    MensajeEnviadoContacto obtenerPorId(UUID idMensaje);

    List<MensajeEnviadoContacto> obtenerPorRemitente(UUID idRemitente);

    List<MensajeEnviadoContacto> obtenerPorDestinatario(UUID idDestinatario);

    List<MensajeEnviadoContacto> obtenerConversacion(UUID idRemitente, UUID idDestinatario);

    void eliminar(UUID idMensaje);
}

