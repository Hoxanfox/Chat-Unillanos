package com.unillanos.server.repository.interfaces;

import com.unillanos.server.repository.models.MensajeEntity;

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
}

