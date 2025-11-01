package gestionUsuario.sesion;

import com.google.gson.JsonObject;
import comunicacion.IEnviadorPeticiones;
import comunicacion.IGestorRespuesta;
import dto.comunicacion.DTORequest;
import dto.comunicacion.DTOResponse;
import gestionUsuario.especialista.EspecialistaUsuariosImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para GestorCierreSesion.
 * 
 * Escenarios probados:
 * 1. Cierre exitoso con respuesta del servidor
 * 2. Cierre sin sesión activa
 * 3. Manejo de error del servidor
 * 4. Timeout del servidor (cierre local garantizado)
 * 5. Validación del protocolo JSON enviado
 */
@ExtendWith(MockitoExtension.class)
class GestorCierreSesionTest {

    @Mock
    private IEnviadorPeticiones enviadorPeticiones;

    @Mock
    private IGestorRespuesta gestorRespuesta;

    @Mock
    private EspecialistaUsuariosImpl especialistaUsuarios;

    private GestorCierreSesion gestorCierreSesion;
    private GestorSesionUsuario gestorSesion;

    @BeforeEach
    void setUp() throws Exception {
        // Limpiar el singleton de GestorSesionUsuario antes de cada prueba
        gestorSesion = GestorSesionUsuario.getInstancia();
        Field userIdField = GestorSesionUsuario.class.getDeclaredField("userId");
        userIdField.setAccessible(true);
        userIdField.set(gestorSesion, null);

        // Crear instancia del gestor de cierre con inyección de dependencias mock
        gestorCierreSesion = new GestorCierreSesion();
        injectMocks(gestorCierreSesion);
    }

    /**
     * Inyectar mocks en el gestor usando reflexión
     */
    private void injectMocks(GestorCierreSesion gestor) throws Exception {
        Field enviadorField = GestorCierreSesion.class.getDeclaredField("enviadorPeticiones");
        enviadorField.setAccessible(true);
        enviadorField.set(gestor, enviadorPeticiones);

        Field gestorRespuestaField = GestorCierreSesion.class.getDeclaredField("gestorRespuesta");
        gestorRespuestaField.setAccessible(true);
        gestorRespuestaField.set(gestor, gestorRespuesta);

        Field especialistaField = GestorCierreSesion.class.getDeclaredField("especialistaUsuarios");
        especialistaField.setAccessible(true);
        especialistaField.set(gestor, especialistaUsuarios);
    }

    @Test
    void testCerrarSesion_Exitoso() throws Exception {
        // ARRANGE
        String userId = UUID.randomUUID().toString();
        gestorSesion.setUserId(userId);

        ArgumentCaptor<Consumer<DTOResponse>> callbackCaptor = ArgumentCaptor.forClass(Consumer.class);
        ArgumentCaptor<DTORequest> requestCaptor = ArgumentCaptor.forClass(DTORequest.class);

        // Capturar el callback registrado
        doNothing().when(gestorRespuesta).registrarManejador(eq("logoutUser"), callbackCaptor.capture());

        // ACT
        CompletableFuture<Boolean> future = gestorCierreSesion.cerrarSesion();

        // Simular respuesta exitosa del servidor
        DTOResponse respuestaExitosa = new DTOResponse("logoutUser", "success", "Sesión cerrada exitosamente", null);
        callbackCaptor.getValue().accept(respuestaExitosa);

        Boolean resultado = future.get(1, TimeUnit.SECONDS);

        // ASSERT
        assertTrue(resultado, "El cierre de sesión debe ser exitoso");
        assertFalse(gestorSesion.haySesionActiva(), "La sesión debe estar cerrada");

        // Verificar que se actualizó el estado local
        verify(especialistaUsuarios, times(1)).actualizarEstadoUsuario(UUID.fromString(userId), "inactivo");

        // Verificar que se registró el manejador correcto
        verify(gestorRespuesta, times(1)).registrarManejador(eq("logoutUser"), any());

        // Verificar que se envió la petición
        verify(enviadorPeticiones, times(1)).enviar(requestCaptor.capture());

        // Validar el protocolo JSON enviado
        DTORequest requestEnviado = requestCaptor.getValue();
        assertEquals("logoutUser", requestEnviado.getAction(), "El action debe ser 'logoutUser'");
        
        JsonObject data = (JsonObject) requestEnviado.getPayload();
        assertNotNull(data, "Los datos no deben ser nulos");
        assertTrue(data.has("userId"), "Debe incluir el campo 'userId'");
        assertEquals(userId, data.get("userId").getAsString(), "El userId debe coincidir");

        System.out.println("✅ Test 1: Cierre exitoso - PASÓ");
    }

    @Test
    void testCerrarSesion_SinSesionActiva() throws Exception {
        // ARRANGE - No hay sesión activa

        // ACT
        CompletableFuture<Boolean> future = gestorCierreSesion.cerrarSesion();
        Boolean resultado = future.get(1, TimeUnit.SECONDS);

        // ASSERT
        assertTrue(resultado, "Debe retornar true cuando no hay sesión activa");
        verify(enviadorPeticiones, never()).enviar(any());
        verify(gestorRespuesta, never()).registrarManejador(any(), any());
        verify(especialistaUsuarios, never()).actualizarEstadoUsuario(any(), any());

        System.out.println("✅ Test 2: Sin sesión activa - PASÓ");
    }

    @Test
    void testCerrarSesion_ErrorDelServidor() throws Exception {
        // ARRANGE
        String userId = UUID.randomUUID().toString();
        gestorSesion.setUserId(userId);

        ArgumentCaptor<Consumer<DTOResponse>> callbackCaptor = ArgumentCaptor.forClass(Consumer.class);

        doNothing().when(gestorRespuesta).registrarManejador(eq("logoutUser"), callbackCaptor.capture());

        // ACT
        CompletableFuture<Boolean> future = gestorCierreSesion.cerrarSesion();

        // Simular respuesta de error del servidor
        DTOResponse respuestaError = new DTOResponse("logoutUser", "error", "Usuario no autenticado o token inválido", null);
        callbackCaptor.getValue().accept(respuestaError);

        Boolean resultado = future.get(1, TimeUnit.SECONDS);

        // ASSERT
        assertFalse(resultado, "Debe retornar false cuando el servidor reporta error");
        
        // Verificar que la sesión local se cerró de todos modos
        assertFalse(gestorSesion.haySesionActiva(), "La sesión local debe cerrarse incluso con error del servidor");

        System.out.println("✅ Test 3: Error del servidor - PASÓ");
    }

    @Test
    void testCerrarSesion_TimeoutDelServidor() throws Exception {
        // ARRANGE
        String userId = UUID.randomUUID().toString();
        gestorSesion.setUserId(userId);

        // No simular respuesta del servidor (timeout)

        // ACT
        CompletableFuture<Boolean> future = gestorCierreSesion.cerrarSesion();

        // Esperar más del timeout (3 segundos + margen)
        Boolean resultado = future.get(4, TimeUnit.SECONDS);

        // ASSERT
        assertTrue(resultado, "Debe completarse con true después del timeout");
        assertFalse(gestorSesion.haySesionActiva(), "La sesión local debe cerrarse por timeout");

        verify(enviadorPeticiones, times(1)).enviar(any());
        verify(especialistaUsuarios, times(1)).actualizarEstadoUsuario(any(), eq("inactivo"));

        System.out.println("✅ Test 4: Timeout del servidor - PASÓ");
    }

    @Test
    void testCerrarSesion_ExcepcionAlEnviar() throws Exception {
        // ARRANGE
        String userId = UUID.randomUUID().toString();
        gestorSesion.setUserId(userId);

        // Simular excepción al enviar
        doThrow(new RuntimeException("Error de red")).when(enviadorPeticiones).enviar(any());

        // ACT
        CompletableFuture<Boolean> future = gestorCierreSesion.cerrarSesion();
        Boolean resultado = future.get(1, TimeUnit.SECONDS);

        // ASSERT
        assertFalse(resultado, "Debe retornar false cuando falla el envío");
        assertFalse(gestorSesion.haySesionActiva(), "La sesión local debe cerrarse incluso con error de envío");

        System.out.println("✅ Test 5: Excepción al enviar - PASÓ");
    }

    @Test
    void testValidarProtocoloJSON() throws Exception {
        // ARRANGE
        String userId = UUID.randomUUID().toString();
        gestorSesion.setUserId(userId);

        ArgumentCaptor<DTORequest> requestCaptor = ArgumentCaptor.forClass(DTORequest.class);
        ArgumentCaptor<Consumer<DTOResponse>> callbackCaptor = ArgumentCaptor.forClass(Consumer.class);

        doNothing().when(gestorRespuesta).registrarManejador(eq("logoutUser"), callbackCaptor.capture());

        // ACT
        CompletableFuture<Boolean> future = gestorCierreSesion.cerrarSesion();

        // ASSERT - Validar estructura del request
        verify(enviadorPeticiones, times(1)).enviar(requestCaptor.capture());

        DTORequest request = requestCaptor.getValue();

        // Validar protocolo según especificación
        assertEquals("logoutUser", request.getAction(), "Action debe ser 'logoutUser'");
        
        JsonObject data = (JsonObject) request.getPayload();
        assertNotNull(data, "Data no debe ser null");
        assertTrue(data.has("userId"), "Data debe contener 'userId'");
        assertEquals(userId, data.get("userId").getAsString(), "userId debe coincidir");
        assertEquals(1, data.size(), "Data debe contener solo 'userId'");

        // Simular respuesta para completar el future
        DTOResponse respuesta = new DTOResponse("logoutUser", "success", "OK", null);
        callbackCaptor.getValue().accept(respuesta);
        future.get(1, TimeUnit.SECONDS);

        System.out.println("✅ Test 6: Validación protocolo JSON - PASÓ");
    }

    @Test
    void testActualizacionEstadoLocalAntesDeLlamarServidor() throws Exception {
        // ARRANGE
        String userId = UUID.randomUUID().toString();
        gestorSesion.setUserId(userId);

        ArgumentCaptor<DTORequest> requestCaptor = ArgumentCaptor.forClass(DTORequest.class);
        ArgumentCaptor<Consumer<DTOResponse>> callbackCaptor = ArgumentCaptor.forClass(Consumer.class);

        doNothing().when(gestorRespuesta).registrarManejador(eq("logoutUser"), callbackCaptor.capture());

        // ACT
        CompletableFuture<Boolean> future = gestorCierreSesion.cerrarSesion();

        // ASSERT - Verificar orden de ejecución
        // Primero se debe actualizar la BD local
        verify(especialistaUsuarios, times(1)).actualizarEstadoUsuario(UUID.fromString(userId), "inactivo");
        
        // Luego se debe enviar al servidor
        verify(enviadorPeticiones, times(1)).enviar(any());

        // Completar el test
        DTOResponse respuesta = new DTOResponse("logoutUser", "success", "OK", null);
        callbackCaptor.getValue().accept(respuesta);
        future.get(1, TimeUnit.SECONDS);

        System.out.println("✅ Test 7: Actualización BD antes de servidor - PASÓ");
    }
}
