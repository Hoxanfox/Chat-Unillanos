package controlador.contactos;

import observador.IObservador;
import dto.featureContactos.DTOContacto;
import java.util.List;

/**
 * Contrato para el controlador que gestiona las interacciones
 * entre la vista de contactos y la capa de negocio.
 */
public interface IControladorContactos {

    /**
     * Permite que una vista se registre como observador de los cambios en los contactos.
     * @param observador La vista que desea ser notificada.
     */
    void registrarObservador(IObservador observador);

    /**
     * Obtiene la lista inicial de contactos desde la caché del servicio.
     * @return Una lista con la información de los contactos.
     */
    List<DTOContacto> getContactos();

    /**
     * Inicia una petición para solicitar la lista actualizada de contactos al servidor.
     */
    void solicitarActualizacionContactos();
}

