package comunicacion;

import dto.comunicacion.DTOResponse;
import java.util.function.Consumer;

/**
 * Contrato para el componente que escucha y gestiona las respuestas del servidor.
 */
public interface IGestorRespuesta {
    /**
     * Inicia el proceso de escucha en un hilo separado.
     */
    void iniciarEscucha();

    /**
     * Detiene el proceso de escucha.
     */
    void detenerEscucha();

    /**
     * Registra un "manejador" para un tipo de operación específico.
     * Cuando llegue una respuesta de ese tipo, se llamará a este manejador.
     * @param tipoOperacion El tipo de operación a manejar (ej. "RESPUESTA_AUTENTICACION").
     * @param manejador El código a ejecutar cuando se reciba la respuesta.
     */
    void registrarManejador(String tipoOperacion, Consumer<DTOResponse> manejador);
}
