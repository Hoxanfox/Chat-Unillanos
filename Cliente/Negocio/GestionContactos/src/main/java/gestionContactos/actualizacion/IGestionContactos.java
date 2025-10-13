package gestionContactos.actualizacion;

import dto.featureContactos.DTOContacto;
import observador.ISujeto; // Se corrige la importación
import java.util.List;

/**
 * Contrato para el componente de negocio que AHORA gestiona únicamente
 * la lista de contactos.
 */
public interface IGestionContactos extends ISujeto {

    void solicitarActualizacionContactos();
    List<DTOContacto> getContactos();
}
