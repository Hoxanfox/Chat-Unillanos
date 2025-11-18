package comunicacion;

import comunicacion.peticionesPull.AccionesComunicacion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AccionesComunicacionTest {

    @Test
    public void constantes_presentes() {
        assertNotNull(AccionesComunicacion.PEER_PUSH);
        assertNotNull(AccionesComunicacion.PEER_JOIN);
        assertNotNull(AccionesComunicacion.PEER_LIST);
        assertNotNull(AccionesComunicacion.PEER_UPDATE);

        assertEquals("PEER_PUSH", AccionesComunicacion.PEER_PUSH);
        assertEquals("PEER_JOIN", AccionesComunicacion.PEER_JOIN);
        assertEquals("PEER_LIST", AccionesComunicacion.PEER_LIST);
        assertEquals("PEER_UPDATE", AccionesComunicacion.PEER_UPDATE);
    }
}

