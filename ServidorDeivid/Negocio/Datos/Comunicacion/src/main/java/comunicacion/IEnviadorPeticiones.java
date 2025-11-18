package comunicacion;

import conexion.enums.TipoPool;
import dto.comunicacion.DTORequest;
import dto.comunicacion.DTOResponse;

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

    /**
     * Enviar una petición específicamente a la sesión que coincida con la IP y puerto indicados (si existe en el pool).
     * Devuelve true si se envió correctamente, false en caso de timeout o error.
     */
    @SuppressWarnings("unused")
    boolean enviarA(String ip, int port, DTORequest request, TipoPool tipoPool);

    /**
     * Envia un DTOResponse serializado directamente a la sesión correspondiente a ip:port.
     * Devuelve true si se envió correctamente.
     */
    @SuppressWarnings("unused")
    boolean enviarResponseA(String ip, int port, DTOResponse response, TipoPool tipoPool);
}
