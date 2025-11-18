package comunicacion;

import conexion.enums.TipoPool;
import dto.comunicacion.DTOResponse;
import java.util.function.Consumer;

/**
 * Contrato para el componente que escucha y gestiona las respuestas del servidor.
 */
public interface IGestorRespuesta {
    /**
     * Inicia el proceso de escucha en un hilo separado (por defecto usa pool CLIENTES).
     */
    void iniciarEscucha();

    /**
     * Inicia la escucha usando el pool indicado (CLIENTES o PEERS). Implementación por defecto delega a iniciarEscucha().
     */
    default void iniciarEscucha(TipoPool tipoPool) {
        // Compatibilidad: las implementaciones que no soporten pool elegirán CLIENTES por defecto
        iniciarEscucha();
    }

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

    /**
     * Elimina un manejador previamente registrado para el tipo de operación.
     * @param tipoOperacion La clave del manejador a eliminar.
     */
    void removerManejador(String tipoOperacion);
}
