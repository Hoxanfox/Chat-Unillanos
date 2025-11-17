package comunicacion;

import conexion.TipoPool;
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

    /**
     * Envía una petición usando el pool indicado (CLIENTES o PEERS).
     * Implementaciones existentes pueden delegar en el método por defecto o implementar directamente.
     */
    default void enviar(DTORequest request, TipoPool tipoPool) {
        // Por compatibilidad, la implementación por defecto envía usando el pool de CLIENTES.
        enviar(request);
    }
}
