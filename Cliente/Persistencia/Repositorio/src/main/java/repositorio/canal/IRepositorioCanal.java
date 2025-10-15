package repositorio.canal;

import dominio.Canal;

import java.util.List;
import java.util.UUID;

/**
 * Contrato para el Repositorio de Canales.
 * Define operaciones CRUD básicas.
 */
public interface IRepositorioCanal {

    /**
     * Guarda un nuevo canal en la base de datos.
     * @param canal Entidad de dominio Canal
     */
    void guardar(Canal canal);

    /**
     * Obtiene un canal por su ID.
     * @param idCanal UUID del canal
     * @return Canal encontrado o null
     */
    Canal obtenerPorId(UUID idCanal);

    /**
     * Obtiene un canal por su nombre.
     * @param nombre Nombre del canal
     * @return Canal encontrado o null
     */
    Canal obtenerPorNombre(String nombre);

    /**
     * Actualiza un canal existente.
     * @param canal Canal con datos actualizados
     */
    void actualizar(Canal canal);

    /**
     * Elimina un canal por su ID.
     * @param idCanal UUID del canal
     */
    void eliminar(UUID idCanal);

    /**
     * Obtiene todos los canales.
     * @return Lista de canales
     */
    List<Canal> obtenerTodos();

    /**
     * Obtiene los canales administrados por un usuario.
     * @param idAdministrador UUID del administrador
     * @return Lista de canales
     */
    List<Canal> obtenerPorAdministrador(UUID idAdministrador);
}

