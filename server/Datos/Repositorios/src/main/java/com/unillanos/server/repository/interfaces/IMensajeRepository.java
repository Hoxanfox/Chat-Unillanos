package com.unillanos.server.repository.interfaces;

import com.unillanos.server.entity.EstadoMensaje;
import com.unillanos.server.entity.MensajeEntity;
import com.unillanos.server.entity.TipoMensaje;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz del repositorio de mensajes.
 * Define operaciones CRUD para la entidad Mensaje.
 */
public interface IMensajeRepository {
    
    // --- MÉTODOS DE CONSULTA ---
    
    /**
     * Busca un mensaje por su ID.
     *
     * @param id ID del mensaje
     * @return Optional con el mensaje si existe
     */
    Optional<MensajeEntity> findById(Long id);
    
    /**
     * Obtiene el historial de mensajes directos entre dos usuarios.
     *
     * @param usuarioId1 ID del primer usuario
     * @param usuarioId2 ID del segundo usuario
     * @param limit Número máximo de resultados
     * @param offset Desplazamiento inicial
     * @return Lista de mensajes ordenados por fecha descendente
     */
    List<MensajeEntity> findMensajesDirectos(String usuarioId1, String usuarioId2, int limit, int offset);
    
    /**
     * Obtiene el historial de mensajes de un canal.
     *
     * @param canalId ID del canal
     * @param limit Número máximo de resultados
     * @param offset Desplazamiento inicial
     * @return Lista de mensajes ordenados por fecha descendente
     */
    List<MensajeEntity> findMensajesCanal(String canalId, int limit, int offset);
    
    /**
     * Obtiene el último mensaje entre dos usuarios (para previsualización).
     *
     * @param usuarioId1 ID del primer usuario
     * @param usuarioId2 ID del segundo usuario
     * @return Optional con el último mensaje
     */
    Optional<MensajeEntity> findUltimoMensajeDirecto(String usuarioId1, String usuarioId2);
    
    /**
     * Obtiene el último mensaje de un canal (para previsualización).
     *
     * @param canalId ID del canal
     * @return Optional con el último mensaje
     */
    Optional<MensajeEntity> findUltimoMensajeCanal(String canalId);
    
    /**
     * Cuenta el total de mensajes entre dos usuarios.
     *
     * @param usuarioId1 ID del primer usuario
     * @param usuarioId2 ID del segundo usuario
     * @return Cantidad de mensajes
     */
    int countMensajesDirectos(String usuarioId1, String usuarioId2);
    
    /**
     * Cuenta el total de mensajes de un canal.
     *
     * @param canalId ID del canal
     * @return Cantidad de mensajes
     */
    int countMensajesCanal(String canalId);
    
    // --- MÉTODOS DE ESCRITURA ---
    
    /**
     * Guarda un nuevo mensaje.
     *
     * @param mensaje Entidad del mensaje
     * @return Mensaje guardado con ID asignado
     */
    MensajeEntity save(MensajeEntity mensaje);
    
    /**
     * Elimina un mensaje por su ID.
     * (Opcional - puede ser una feature futura)
     *
     * @param id ID del mensaje
     */
    void deleteById(Long id);
    
    // --- MÉTODOS DE ESTADO DE MENSAJES ---
    
    /**
     * Actualiza el estado de un mensaje.
     *
     * @param mensajeId ID del mensaje
     * @param nuevoEstado Nuevo estado del mensaje
     */
    void actualizarEstado(Long mensajeId, EstadoMensaje nuevoEstado);
    
    /**
     * Marca un mensaje como entregado (cuando se envía la notificación).
     *
     * @param mensajeId ID del mensaje
     */
    void marcarComoEntregado(Long mensajeId);
    
    /**
     * Marca un mensaje como leído por el destinatario.
     *
     * @param mensajeId ID del mensaje
     * @param usuarioId ID del usuario que lee el mensaje
     */
    void marcarComoLeido(Long mensajeId, String usuarioId);
    
    /**
     * Obtiene mensajes no leídos para un usuario específico.
     *
     * @param usuarioId ID del usuario
     * @return Lista de IDs de mensajes no leídos
     */
    List<Long> obtenerMensajesNoLeidos(String usuarioId);
    
    /**
     * Obtiene el conteo de mensajes no leídos para un usuario.
     *
     * @param usuarioId ID del usuario
     * @return Cantidad de mensajes no leídos
     */
    int contarMensajesNoLeidos(String usuarioId);

    /**
     * Cuenta mensajes por tipo del día actual.
     *
     * @param tipo Tipo de mensaje a contar
     * @return Cantidad de mensajes del tipo especificado enviados hoy
     */
    int countByTipoHoy(TipoMensaje tipo);
}

