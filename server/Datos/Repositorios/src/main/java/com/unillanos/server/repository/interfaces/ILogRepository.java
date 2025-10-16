package com.unillanos.server.repository.interfaces;

import com.unillanos.server.entity.LogEntity;

import java.util.List;

/**
 * Interfaz del repositorio de logs del sistema.
 * Define operaciones para persistir y consultar logs.
 */
public interface ILogRepository {
    
    /**
     * Guarda un nuevo log en la base de datos.
     *
     * @param log Entidad del log a guardar
     */
    void save(LogEntity log);
    
    /**
     * Busca logs por tipo.
     *
     * @param tipo Tipo de log (LOGIN, LOGOUT, ERROR, INFO, SYSTEM)
     * @param limit Número máximo de logs a retornar
     * @return Lista de logs del tipo especificado
     */
    List<LogEntity> findByTipo(String tipo, int limit);
    
    /**
     * Busca logs por usuario.
     *
     * @param usuarioId ID del usuario
     * @param limit Número máximo de logs a retornar
     * @return Lista de logs del usuario
     */
    List<LogEntity> findByUsuarioId(String usuarioId, int limit);
    
    /**
     * Obtiene los logs más recientes.
     *
     * @param limit Número máximo de logs a retornar
     * @return Lista de logs recientes
     */
    List<LogEntity> findRecent(int limit);
}

