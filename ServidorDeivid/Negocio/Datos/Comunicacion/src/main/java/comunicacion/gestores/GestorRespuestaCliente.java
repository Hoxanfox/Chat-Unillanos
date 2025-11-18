package comunicacion.gestores;

import comunicacion.GestorRespuesta;
import conexion.enums.TipoPool;

/**
 * Gestor de respuestas dedicado a CLIENTES. Contiene su propio singleton y manejadores.
 */
public class GestorRespuestaCliente extends GestorRespuesta {
    private static GestorRespuestaCliente instancia;

    protected GestorRespuestaCliente() {
        super();
    }

    public static synchronized GestorRespuestaCliente getInstancia() {
        if (instancia == null) instancia = new GestorRespuestaCliente();
        return instancia;
    }

    @Override
    public void iniciarEscucha() {
        iniciarEscucha(TipoPool.CLIENTES);
    }
}

