package repositorio.notificacion;

import dto.featureNotificaciones.DTONotificacion;

import java.util.List;

/**
 * Contrato para el Repositorio de Notificaciones.
 * El repositorio SOLO maneja almacenamiento local en caché.
 * NO debe tener comunicación con el servidor (eso es responsabilidad de los gestores).
 */
public interface IRepositorioNotificacion {

    /**
     * Guarda una notificación en la caché local.
     *
     * @param notificacion La notificación a guardar
     */
    void guardar(DTONotificacion notificacion);

    /**
     * Guarda una lista de notificaciones en la caché local.
     *
     * @param notificaciones Lista de notificaciones
     */
    void guardarTodas(List<DTONotificacion> notificaciones);

    /**
     * Obtiene todas las notificaciones desde la caché local.
     *
     * @return Lista de notificaciones en caché
     */
    List<DTONotificacion> obtenerTodas();

    /**
     * Remueve una notificación de la caché por su ID.
     *
     * @param notificacionId ID de la notificación
     */
    void remover(String notificacionId);

    /**
     * Limpia todas las notificaciones de la caché.
     */
    void limpiarCache();

    /**
     * Busca una notificación por su ID.
     *
     * @param notificacionId ID de la notificación
     * @return La notificación si existe, null en caso contrario
     */
    DTONotificacion buscarPorId(String notificacionId);
}
