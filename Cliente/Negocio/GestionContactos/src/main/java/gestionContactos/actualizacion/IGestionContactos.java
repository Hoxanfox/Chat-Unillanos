package gestionContactos.actualizacion;
import observador.ISujeto;

/**
 * Contrato para el componente de negocio que gestiona la lógica de los contactos.
 * Es utilizado por la Fachada y se comunica con la capa de Persistencia.
 */
public interface IGestionContactos extends ISujeto {

    /**
     * Inicia una petición al servidor para obtener la lista de contactos.
     */
    void solicitarActualizacionContactos();
}
