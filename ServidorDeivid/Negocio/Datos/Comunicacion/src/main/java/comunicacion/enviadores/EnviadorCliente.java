package comunicacion.enviadores;

import comunicacion.BaseEnviador;
import conexion.enums.TipoPool;

public class EnviadorCliente extends BaseEnviador {
    @Override
    protected TipoPool getDefaultPool() {
        return TipoPool.CLIENTES;
    }
}
