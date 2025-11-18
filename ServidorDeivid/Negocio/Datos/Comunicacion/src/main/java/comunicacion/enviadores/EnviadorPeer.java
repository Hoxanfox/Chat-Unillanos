package comunicacion.enviadores;

import comunicacion.BaseEnviador;
import conexion.enums.TipoPool;

public class EnviadorPeer extends BaseEnviador {
    @Override
    protected TipoPool getDefaultPool() {
        return TipoPool.PEERS;
    }
}

