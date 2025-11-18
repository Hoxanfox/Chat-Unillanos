package comunicacion.gestores;

import comunicacion.GestorRespuesta;
import conexion.enums.TipoPool;

/**
 * Gestor de respuestas dedicado a PEERS. Contiene su propio singleton y manejadores.
 */
public class GestorRespuestaPeer extends GestorRespuesta {
    private static GestorRespuestaPeer instancia;

    protected GestorRespuestaPeer() {
        super();
    }

    public static synchronized GestorRespuestaPeer getInstancia() {
        if (instancia == null) instancia = new GestorRespuestaPeer();
        return instancia;
    }

    @Override
    public void iniciarEscucha() {
        iniciarEscucha(TipoPool.PEERS);
    }
}

