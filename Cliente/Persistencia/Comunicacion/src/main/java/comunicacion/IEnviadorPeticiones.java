package comunicacion;

import dto.comunicacion.DTORequest;

/**
 * Contrato para el componente encargado de enviar peticiones al servidor.
 */
public interface IEnviadorPeticiones {
    /**
     * Serializa y envía una petición al servidor a través de la conexión activa.
     * @param request El DTO de la petición a enviar.
     */
    void enviar(DTORequest request);
}
