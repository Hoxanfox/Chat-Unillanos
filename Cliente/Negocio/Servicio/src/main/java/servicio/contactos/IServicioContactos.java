package servicio.contactos;

import observador.ISujeto;
import java.util.List;
import dto.featureContactos.DTOContacto;

/**
 * Contrato para el servicio que gestiona la lógica de negocio de los contactos.
 * También actúa como un "Sujeto" para notificar a las vistas de los cambios.
 */
public interface IServicioContactos extends ISujeto {

    /**
     * Obtiene la lista actual de contactos.
     * @return Una lista de DTOs de contacto.
     */
    List<DTOContacto> getContactos();

    /**
     * Inicia una petición para actualizar la lista de contactos desde el servidor.
     */
    void solicitarActualizacionContactos();
}
