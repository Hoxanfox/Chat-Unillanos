package comunicacion.fabrica;

import comunicacion.ModoComunicacion;
import conexion.enums.TipoPool;
import comunicacion.IEnviadorPeticiones;
import comunicacion.IGestorRespuesta;
import comunicacion.EnviadorPeticiones;
import comunicacion.GestorRespuesta;
import comunicacion.gestores.GestorRespuestaPeer;
import comunicacion.gestores.GestorRespuestaCliente;
import comunicacion.enviadores.EnviadorPeer;
import comunicacion.enviadores.EnviadorCliente;
import logger.LoggerCentral;

/**
 * Implementación por defecto de la fábrica de comunicación.
 * Devuelve implementaciones por defecto y actúa como punto único para sustituir
 * por variantes servidor/peer más específicas en el futuro.
 */
public class FabricaComunicacionImpl implements FabricaComunicacion {
    private static FabricaComunicacionImpl instancia;

    private FabricaComunicacionImpl() {}

    public static synchronized FabricaComunicacionImpl getInstancia() {
        if (instancia == null) instancia = new FabricaComunicacionImpl();
        return instancia;
    }

    // Implementaciones primarias (TipoPool) requeridas por la interfaz
    @Override
    public IEnviadorPeticiones crearEnviador(TipoPool pool) {
        // Delegar a la implementación que acepta modo (permitirá centralizar la lógica)
        return crearEnviador((ModoComunicacion) null, pool);
    }

    @Override
    public IGestorRespuesta crearGestorRespuesta(TipoPool pool) {
        return crearGestorRespuesta((ModoComunicacion) null, pool);
    }

    // Versiones con ModoComunicacion para ampliar comportamiento si es necesario
    @Override
    public IEnviadorPeticiones crearEnviador(ModoComunicacion modo, TipoPool pool) {
        LoggerCentral.debug("FabricaComunicacionImpl.crearEnviador modo=" + modo + " pool=" + pool);

        // Elegir la implementación adecuada según pool
        if (pool == TipoPool.PEERS) return new EnviadorPeer();
        if (pool == TipoPool.CLIENTES) return new EnviadorCliente();
        // Por defecto mantener compatibilidad devolviendo la implementación histórica
        return new EnviadorPeticiones();
    }

    @Override
    public IGestorRespuesta crearGestorRespuesta(ModoComunicacion modo, TipoPool pool) {
        LoggerCentral.debug("FabricaComunicacionImpl.crearGestorRespuesta modo=" + modo + " pool=" + pool);

        // Devolver gestores específicos por pool
        if (pool == TipoPool.PEERS) return GestorRespuestaPeer.getInstancia();
        if (pool == TipoPool.CLIENTES) return GestorRespuestaCliente.getInstancia();
        // Por defecto devolvemos el singleton GestorRespuesta por compatibilidad.
        return GestorRespuesta.getInstancia();
    }
}
