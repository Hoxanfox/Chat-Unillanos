package comunicacion.fabrica;

import comunicacion.ModoComunicacion;
import conexion.enums.TipoPool;
import comunicacion.IEnviadorPeticiones;
import comunicacion.IGestorRespuesta;

/**
 * Contrato para la fábrica que crea componentes de comunicación (enviador y gestor de respuestas).
 */
public interface FabricaComunicacion {
    // Métodos primarios: implementaciones deben proporcionar la variante por TipoPool
    IEnviadorPeticiones crearEnviador(TipoPool pool);
    IGestorRespuesta crearGestorRespuesta(TipoPool pool);

    // Versiones que aceptan ModoComunicacion delegan por defecto a las anteriores
    default IEnviadorPeticiones crearEnviador(ModoComunicacion modo, TipoPool pool) {
        return crearEnviador(pool);
    }

    default IGestorRespuesta crearGestorRespuesta(ModoComunicacion modo, TipoPool pool) {
        return crearGestorRespuesta(pool);
    }
}
