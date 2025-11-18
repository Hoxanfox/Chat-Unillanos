package comunicacion;

import conexion.enums.TipoPool;

/**
 * Compatibilidad: clase histórica que ahora reutiliza la implementación central en BaseEnviador.
 * Por compatibilidad de comportamiento previo, su pool por defecto es PEERS.
 */
public class EnviadorPeticiones extends BaseEnviador {

    public EnviadorPeticiones() {
        super();
    }

    @Override
    protected TipoPool getDefaultPool() {
        return TipoPool.PEERS;
    }

    // Mantener el método enviar(DTORequest) heredado de BaseEnviador y otros métodos.
}
