package comunicacion;

import com.google.gson.Gson;
import dto.comunicacion.DTOResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class GestorRespuestaTest {

    @AfterEach
    public void cleanup() {
        // Asegurarnos de limpiar manejadores registrados entre tests
        GestorRespuesta.getInstancia().removerManejador("TEST_ACTION");
    }

    @Test
    public void procesarRespuesta_invocaManejadorEspecifico() throws Exception {
        GestorRespuesta gestor = GestorRespuesta.getInstancia();
        AtomicBoolean called = new AtomicBoolean(false);

        gestor.registrarManejador("TEST_ACTION", (DTOResponse resp) -> {
            called.set(true);
            assertEquals("TEST_ACTION", resp.getAction());
        });

        // Construir JSON de DTOResponse simple
        String json = "{\"action\":\"TEST_ACTION\",\"status\":\"success\",\"message\":\"ok\",\"data\":{\"requestId\":\"r1\"}}";

        // Llamar al método privado procesarRespuesta por reflexión
        Method procesar = GestorRespuesta.class.getDeclaredMethod("procesarRespuesta", String.class);
        procesar.setAccessible(true);
        procesar.invoke(gestor, json);

        assertTrue(called.get(), "El manejador registrado debe ser invocado al procesar la respuesta");

        gestor.removerManejador("TEST_ACTION");
    }
}

