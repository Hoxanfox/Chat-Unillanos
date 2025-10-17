package com.unillanos.server.repository.interfaces;

import com.unillanos.server.entity.ArchivoEntity;
import com.unillanos.server.entity.TipoArchivo;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz del repositorio de archivos multimedia.
 * Define operaciones CRUD para la entidad Archivo.
 */
public interface IArchivoRepository {
    
    // --- MÉTODOS DE CONSULTA ---
    
    /**
     * Busca un archivo por su ID.
     *
     * @param id ID del archivo
     * @return Optional con el archivo si existe
     */
    Optional<ArchivoEntity> findById(String id);
    
    /**
     * Busca un archivo por su hash SHA-256.
     * Útil para detectar duplicados.
     *
     * @param hashSha256 Hash SHA-256 del archivo
     * @return Optional con el archivo si existe
     */
    Optional<ArchivoEntity> findByHash(String hashSha256);
    
    /**
     * Obtiene todos los archivos de un usuario con paginación.
     *
     * @param usuarioId ID del usuario
     * @param limit Número máximo de resultados
     * @param offset Desplazamiento inicial
     * @return Lista de archivos ordenados por fecha descendente
     */
    List<ArchivoEntity> findByUsuario(String usuarioId, int limit, int offset);
    
    /**
     * Obtiene archivos de un usuario filtrados por tipo.
     *
     * @param usuarioId ID del usuario
     * @param tipo Tipo de archivo (IMAGE, AUDIO, DOCUMENT)
     * @param limit Número máximo de resultados
     * @param offset Desplazamiento inicial
     * @return Lista de archivos filtrados ordenados por fecha descendente
     */
    List<ArchivoEntity> findByUsuarioYTipo(String usuarioId, TipoArchivo tipo, int limit, int offset);
    
    /**
     * Cuenta el total de archivos de un usuario.
     *
     * @param usuarioId ID del usuario
     * @return Cantidad de archivos
     */
    int countByUsuario(String usuarioId);
    
    /**
     * Verifica si existe un archivo con el hash dado.
     *
     * @param hashSha256 Hash SHA-256 del archivo
     * @return true si existe un archivo con ese hash
     */
    boolean existsByHash(String hashSha256);
    
    // --- MÉTODOS DE ESCRITURA ---
    
    /**
     * Guarda un nuevo archivo.
     *
     * @param archivo Entidad del archivo
     * @return Archivo guardado con ID asignado
     */
    ArchivoEntity save(ArchivoEntity archivo);
    
    /**
     * Elimina un archivo por su ID.
     *
     * @param id ID del archivo
     */
    void deleteById(String id);
}
