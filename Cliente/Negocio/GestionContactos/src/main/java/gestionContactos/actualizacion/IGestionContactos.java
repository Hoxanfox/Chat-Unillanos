package gestionContactos.actualizacion;

import dto.featureContactos.DTOContacto;
import observador.ISujeto; // Se corrige la importación
import java.util.List;

/**
 * Contrato para el componente de negocio que gestiona la lista de contactos.
 * - listarContactos: Petición REQUEST del cliente al servidor
 * - solicitarListaContactos: Notificación PUSH del servidor al cliente
 */
public interface IGestionContactos extends ISujeto {

    /**
     * Solicita al servidor la lista de contactos del usuario actual
     * Envía petición "listarContactos" al servidor
     */
    void solicitarActualizacionContactos();

    /**
     * Obtiene la lista de contactos en caché
     * @return Lista de contactos
     */
    List<DTOContacto> getContactos();

    /**
     * Establece el ID del usuario actual para las peticiones
     * @param usuarioId ID del usuario
     */
    void setUsuarioId(String usuarioId);
}
