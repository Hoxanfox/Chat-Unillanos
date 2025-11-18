package comunicacion;

import comunicacion.fabrica.FabricaComunicacionImpl;
import conexion.enums.TipoPool;
import comunicacion.enviadores.EnviadorPeer;
import comunicacion.enviadores.EnviadorCliente;
import comunicacion.gestores.GestorRespuestaPeer;
import comunicacion.gestores.GestorRespuestaCliente;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FabricaComunicacionTest {

    @Test
    public void crearComponentes_noNull() {
        FabricaComunicacionImpl fabrica = FabricaComunicacionImpl.getInstancia();
        IEnviadorPeticiones enviador = fabrica.crearEnviador(TipoPool.PEERS);
        IGestorRespuesta gestor = fabrica.crearGestorRespuesta(TipoPool.PEERS);

        assertNotNull(enviador, "La fábrica debe devolver un IEnviadorPeticiones no nulo");
        assertNotNull(gestor, "La fábrica debe devolver un IGestorRespuesta no nulo");

        // Aceptar cualquiera de las implementaciones válidas por pool
        boolean enviadorValido = enviador instanceof EnviadorPeticiones
                || enviador instanceof EnviadorPeer
                || enviador instanceof EnviadorCliente;
        assertTrue(enviadorValido, "El enviador por defecto debe ser una implementación válida (EnviadorPeticiones/EnviadorPeer/EnviadorCliente)");

        boolean gestorValido = gestor instanceof GestorRespuesta
                || gestor instanceof GestorRespuestaPeer
                || gestor instanceof GestorRespuestaCliente;
        assertTrue(gestorValido, "El gestor por defecto debe ser una implementación válida (GestorRespuesta/GestorRespuestaPeer/GestorRespuestaCliente)");
    }
}
