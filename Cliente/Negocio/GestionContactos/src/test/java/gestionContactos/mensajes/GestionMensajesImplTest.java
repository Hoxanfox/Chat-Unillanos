package gestionContactos.mensajes;

import com.google.gson.Gson;
import comunicacion.IEnviadorPeticiones;
import comunicacion.IGestorRespuesta;
import dto.comunicacion.DTORequest;
import dto.comunicacion.DTOResponse;
import dto.vistaContactoChat.DTOMensaje;
import gestionContactos.GestorContactoPeers;
import gestionUsuario.sesion.GestorSesionUsuario;
import observador.IObservador;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para GestionMensajesImpl
 * Verifica el correcto funcionamiento de:
 * - Envío de mensajes (request)
 * - Respuestas del servidor
 * - Notificaciones push
 * - Manejo de errores
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GestionMensajesImplTest {

    private GestionMensajesImpl gestionMensajes;
    private IEnviadorPeticiones mockEnviadorPeticiones;
    private IGestorRespuesta mockGestorRespuesta;
    private GestorSesionUsuario gestorSesionUsuario;
    private GestorContactoPeers gestorContactoPeers;
    private IObservador mockObservador;
    private Gson gson;
    
    // Almacena los handlers registrados para simular callbacks
    private Map<String, Consumer<DTOResponse>> handlersRegistrados;

    @BeforeAll
    void inicializarSingletons() {
        // Inicializar los singletons que usa GestionMensajesImpl
        gestorSesionUsuario = GestorSesionUsuario.getInstancia();
        gestorContactoPeers = GestorContactoPeers.getInstancia();
        gson = new Gson();
    }

    @BeforeEach
    void setUp() {
        // Crear mocks
        mockEnviadorPeticiones = mock(IEnviadorPeticiones.class);
        mockGestorRespuesta = mock(IGestorRespuesta.class);
        mockObservador = mock(IObservador.class);
        
        // Almacenar handlers registrados
        handlersRegistrados = new HashMap<>();
        
        // Configurar el mock para capturar los handlers cuando se registran
        doAnswer(invocation -> {
            String action = invocation.getArgument(0);
            Consumer<DTOResponse> handler = invocation.getArgument(1);
            handlersRegistrados.put(action, handler);
            System.out.println("✅ Handler registrado: " + action);
            return null;
        }).when(mockGestorRespuesta).registrarManejador(anyString(), any());

        // Configurar datos de sesión
        gestorSesionUsuario.setUserId("user-123");
        gestorSesionUsuario.setPeerId("peer-abc");
        
        // Configurar peers de contactos
        gestorContactoPeers.registrarPeerDeContacto("contacto-456", "peer-xyz");
        
        // Crear instancia con mocks inyectados
        gestionMensajes = crearGestionMensajesConMocks();
        
        // IMPORTANTE: Simular el registro de handlers llamando a los métodos
        // que el constructor real llamaría
        handlersRegistrados.put("enviarMensajeDirecto", this::simularManejarRespuestaEnvio);
        handlersRegistrados.put("solicitarHistorialPrivado", this::simularManejarHistorial);
        handlersRegistrados.put("nuevoMensajeDirecto", this::simularManejarNuevoMensajePush);

        // Registrar observador
        gestionMensajes.registrarObservador(mockObservador);
    }

    @AfterEach
    void tearDown() {
        // Limpiar datos de prueba
        gestorContactoPeers.limpiar();
        handlersRegistrados.clear();
    }

    // ========================================================================
    // PRUEBAS DE ENVÍO DE MENSAJES (REQUEST)
    // ========================================================================

    @Test
    @DisplayName("Debe enviar mensaje de texto correctamente con peers")
    void testEnviarMensajeTexto_Exitoso() {
        // Arrange
        String destinatarioId = "contacto-456";
        String contenido = "Hola, ¿cómo estás?";
        ArgumentCaptor<DTORequest> requestCaptor = ArgumentCaptor.forClass(DTORequest.class);

        // Act
        CompletableFuture<Void> resultado = gestionMensajes.enviarMensajeTexto(destinatarioId, contenido);

        // Assert
        assertNotNull(resultado);
        verify(mockEnviadorPeticiones, times(1)).enviar(requestCaptor.capture());
        
        DTORequest peticionEnviada = requestCaptor.getValue();
        assertEquals("enviarMensajeDirecto", peticionEnviada.getAction());
        assertNotNull(peticionEnviada.getPayload(), "El payload no debe ser null");
    }

    @Test
    @DisplayName("Debe enviar mensaje de audio correctamente con Base64")
    void testEnviarMensajeAudio_Exitoso() {
        // Arrange
        String destinatarioId = "contacto-456";
        String audioBase64 = "aW9kYXNkaGFza2RoYXNrZGpoYXNrZGpoYXNrZGg=";
        ArgumentCaptor<DTORequest> requestCaptor = ArgumentCaptor.forClass(DTORequest.class);

        // Act
        gestionMensajes.enviarMensajeAudio(destinatarioId, audioBase64);

        // Assert
        verify(mockEnviadorPeticiones, times(1)).enviar(requestCaptor.capture());
        
        DTORequest peticionEnviada = requestCaptor.getValue();
        assertEquals("enviarMensajeDirectoAudio", peticionEnviada.getAction());
        assertNotNull(peticionEnviada.getPayload(), "El payload no debe ser null");
    }

    @Test
    @DisplayName("No debe enviar mensaje si el peer del destinatario no está disponible")
    void testEnviarMensajeTexto_PeerNoDisponible() {
        // Arrange
        String destinatarioSinPeer = "contacto-sin-peer";
        String contenido = "Mensaje de prueba";

        // Act
        gestionMensajes.enviarMensajeTexto(destinatarioSinPeer, contenido);

        // Assert
        verify(mockEnviadorPeticiones, never()).enviar(any());
        verify(mockObservador, times(1)).actualizar(
            eq("ERROR_PEER_NO_ENCONTRADO"),
            eq("El contacto no está disponible")
        );
    }

    // ========================================================================
    // PRUEBAS DE RESPUESTAS DEL SERVIDOR
    // ========================================================================

    @Test
    @DisplayName("Debe manejar respuesta exitosa de envío de mensaje")
    void testManejarRespuestaEnvio_Exitosa() {
        // Arrange
        DTOResponse respuesta = crearRespuestaExitosa(
            "enviarMensajeDirecto",
            crearDatosMensaje("msg-uuid-123", "2025-11-01T10:30:00Z")
        );

        // Act - Simular que llega la respuesta del servidor
        Consumer<DTOResponse> handler = handlersRegistrados.get("enviarMensajeDirecto");
        assertNotNull(handler, "El handler de enviarMensajeDirecto debe estar registrado");
        handler.accept(respuesta);

        // Assert
        verify(mockObservador, times(1)).actualizar(
            eq("MENSAJE_ENVIADO_EXITOSO"),
            argThat(mensaje -> {
                DTOMensaje msg = (DTOMensaje) mensaje;
                return msg.getMensajeId().equals("msg-uuid-123") &&
                       msg.getFechaEnvio().equals("2025-11-01T10:30:00Z") &&
                       msg.esMio();
            })
        );
    }

    @Test
    @DisplayName("Debe manejar error de destinatario no encontrado")
    void testManejarRespuestaEnvio_DestinatarioNoEncontrado() {
        // Arrange
        DTOResponse respuesta = crearRespuestaError(
            "enviarMensajeDirecto",
            "Destinatario no encontrado o desconectado"
        );

        // Act
        Consumer<DTOResponse> handler = handlersRegistrados.get("enviarMensajeDirecto");
        handler.accept(respuesta);

        // Assert
        verify(mockObservador, times(1)).actualizar(
            eq("ERROR_DESTINATARIO_NO_DISPONIBLE"),
            eq("Destinatario no encontrado o desconectado")
        );
    }

    @Test
    @DisplayName("Debe manejar error de validación de datos")
    void testManejarRespuestaEnvio_ErrorValidacion() {
        // Arrange
        DTOResponse respuesta = crearRespuestaError(
            "enviarMensajeDirecto",
            "Datos de mensaje inválidos"
        );

        // Act
        Consumer<DTOResponse> handler = handlersRegistrados.get("enviarMensajeDirecto");
        handler.accept(respuesta);

        // Assert
        verify(mockObservador, times(1)).actualizar(
            eq("ERROR_VALIDACION"),
            anyString()
        );
    }

    @Test
    @DisplayName("Debe manejar error general de envío")
    void testManejarRespuestaEnvio_ErrorGeneral() {
        // Arrange
        DTOResponse respuesta = crearRespuestaError(
            "enviarMensajeDirecto",
            "Error al enviar mensaje: conexión perdida"
        );

        // Act
        Consumer<DTOResponse> handler = handlersRegistrados.get("enviarMensajeDirecto");
        handler.accept(respuesta);

        // Assert
        verify(mockObservador, times(1)).actualizar(
            eq("ERROR_ENVIO_MENSAJE"),
            eq("Error al enviar mensaje: conexión perdida")
        );
    }

    // ========================================================================
    // PRUEBAS DE NOTIFICACIONES PUSH
    // ========================================================================

    @Test
    @DisplayName("Debe recibir y procesar push de nuevo mensaje correctamente")
    void testManejarNuevoMensajePush_Exitoso() {
        // Arrange
        Map<String, Object> datosMsg = new HashMap<>();
        datosMsg.put("mensajeId", "msg-push-789");
        datosMsg.put("peerRemitenteId", "peer-xyz");
        datosMsg.put("peerDestinoId", "peer-abc");
        datosMsg.put("remitenteId", "contacto-456");
        datosMsg.put("remitenteNombre", "Juan Pérez");
        datosMsg.put("destinatarioId", "user-123");
        datosMsg.put("tipo", "TEXTO");
        datosMsg.put("contenido", "Hola, este es un mensaje push");
        datosMsg.put("fechaEnvio", "2025-11-01T11:00:00Z");

        DTOResponse pushNotification = crearRespuestaExitosa("nuevoMensajeDirecto", datosMsg);

        // Act
        Consumer<DTOResponse> handler = handlersRegistrados.get("nuevoMensajeDirecto");
        assertNotNull(handler, "El handler de nuevoMensajeDirecto debe estar registrado");
        handler.accept(pushNotification);

        // Assert
        verify(mockObservador, times(1)).actualizar(
            eq("NUEVO_MENSAJE_PRIVADO"),
            argThat(mensaje -> {
                DTOMensaje msg = (DTOMensaje) mensaje;
                return msg.getMensajeId().equals("msg-push-789") &&
                       msg.getRemitenteNombre().equals("Juan Pérez") &&
                       !msg.esMio() && // Debe marcarse como del otro usuario
                       msg.getContenido().equals("Hola, este es un mensaje push");
            })
        );
    }

    @Test
    @DisplayName("Debe ignorar push de mis propios mensajes (evitar duplicados)")
    void testManejarNuevoMensajePush_IgnorarPropiosMensajes() {
        // Arrange - Mensaje donde el remitente soy yo
        Map<String, Object> datosMsg = new HashMap<>();
        datosMsg.put("mensajeId", "msg-propio-999");
        datosMsg.put("remitenteId", "user-123"); // Mi propio ID
        datosMsg.put("destinatarioId", "contacto-456");
        datosMsg.put("tipo", "TEXTO");
        datosMsg.put("contenido", "Mensaje que yo envié");
        datosMsg.put("fechaEnvio", "2025-11-01T11:30:00Z");

        DTOResponse pushNotification = crearRespuestaExitosa("nuevoMensajeDirecto", datosMsg);

        // Act
        Consumer<DTOResponse> handler = handlersRegistrados.get("nuevoMensajeDirecto");
        handler.accept(pushNotification);

        // Assert - NO debe notificar al observador
        verify(mockObservador, never()).actualizar(eq("NUEVO_MENSAJE_PRIVADO"), any());
    }

    @Test
    @DisplayName("Debe ignorar push si no es para mi peer actual")
    void testManejarNuevoMensajePush_IgnorarSiNoEsParaMiPeer() {
        // Arrange - Mensaje para un peer diferente
        Map<String, Object> datosMsg = new HashMap<>();
        datosMsg.put("mensajeId", "msg-otro-peer-888");
        datosMsg.put("peerDestinoId", "peer-otro-dispositivo"); // Peer diferente
        datosMsg.put("remitenteId", "contacto-456");
        datosMsg.put("destinatarioId", "user-123");
        datosMsg.put("tipo", "TEXTO");
        datosMsg.put("contenido", "Mensaje para otro peer");
        datosMsg.put("fechaEnvio", "2025-11-01T12:00:00Z");

        DTOResponse pushNotification = crearRespuestaExitosa("nuevoMensajeDirecto", datosMsg);

        // Act
        Consumer<DTOResponse> handler = handlersRegistrados.get("nuevoMensajeDirecto");
        handler.accept(pushNotification);

        // Assert - NO debe notificar al observador
        verify(mockObservador, never()).actualizar(eq("NUEVO_MENSAJE_PRIVADO"), any());
    }

    @Test
    @DisplayName("Debe manejar error en notificación push")
    void testManejarNuevoMensajePush_Error() {
        // Arrange
        DTOResponse pushError = crearRespuestaError(
            "nuevoMensajeDirecto",
            "Error al obtener el mensaje"
        );

        // Act
        Consumer<DTOResponse> handler = handlersRegistrados.get("nuevoMensajeDirecto");
        handler.accept(pushError);

        // Assert
        verify(mockObservador, times(1)).actualizar(
            eq("ERROR_NOTIFICACION_MENSAJE"),
            eq("Error al obtener el mensaje")
        );
    }

    // ========================================================================
    // PRUEBAS DE SOLICITUD DE HISTORIAL
    // ========================================================================

    @Test
    @DisplayName("Debe solicitar historial correctamente")
    void testSolicitarHistorial() {
        // Arrange
        String contactoId = "contacto-456";
        ArgumentCaptor<DTORequest> requestCaptor = ArgumentCaptor.forClass(DTORequest.class);

        // Act
        gestionMensajes.solicitarHistorial(contactoId);

        // Assert
        verify(mockEnviadorPeticiones, times(1)).enviar(requestCaptor.capture());
        
        DTORequest peticion = requestCaptor.getValue();
        assertEquals("solicitarHistorialPrivado", peticion.getAction());
    }

    // ========================================================================
    // PRUEBAS DE OBSERVADORES
    // ========================================================================

    @Test
    @DisplayName("Debe registrar y notificar observadores correctamente")
    void testRegistrarYNotificarObservadores() {
        // Arrange
        IObservador segundoObservador = mock(IObservador.class);
        gestionMensajes.registrarObservador(segundoObservador);

        // Act
        gestionMensajes.notificarObservadores("TEST_EVENTO", "datos de prueba");

        // Assert
        verify(mockObservador, times(1)).actualizar("TEST_EVENTO", "datos de prueba");
        verify(segundoObservador, times(1)).actualizar("TEST_EVENTO", "datos de prueba");
    }

    @Test
    @DisplayName("Debe remover observadores correctamente")
    void testRemoverObservador() {
        // Act
        gestionMensajes.removerObservador(mockObservador);
        gestionMensajes.notificarObservadores("TEST_EVENTO", "datos");

        // Assert - No debe notificar al observador removido
        verify(mockObservador, never()).actualizar(anyString(), any());
    }

    // ========================================================================
    // MÉTODOS AUXILIARES
    // ========================================================================

    /**
     * Crea una instancia de GestionMensajesImpl con mocks inyectados
     * usando reflexión para acceder al constructor y campos privados.
     */
    private GestionMensajesImpl crearGestionMensajesConMocks() {
        try {
            // Crear instancia
            GestionMensajesImpl instancia = new GestionMensajesImpl();
            
            // Inyectar mocks usando reflexión
            var enviadorField = GestionMensajesImpl.class.getDeclaredField("enviadorPeticiones");
            enviadorField.setAccessible(true);
            enviadorField.set(instancia, mockEnviadorPeticiones);
            
            var gestorField = GestionMensajesImpl.class.getDeclaredField("gestorRespuesta");
            gestorField.setAccessible(true);
            gestorField.set(instancia, mockGestorRespuesta);
            
            // Obtener acceso al método privado manejarRespuestaEnvioMensaje
            var manejarRespuestaMethod = GestionMensajesImpl.class.getDeclaredMethod("manejarRespuestaEnvioMensaje", DTOResponse.class);
            manejarRespuestaMethod.setAccessible(true);

            var manejarPushMethod = GestionMensajesImpl.class.getDeclaredMethod("manejarNuevoMensajePush", DTOResponse.class);
            manejarPushMethod.setAccessible(true);

            var manejarHistorialMethod = GestionMensajesImpl.class.getDeclaredMethod("manejarHistorial", DTOResponse.class);
            manejarHistorialMethod.setAccessible(true);

            // Almacenar referencias a los métodos para usarlos en las pruebas
            handlersRegistrados.put("enviarMensajeDirecto", response -> {
                try {
                    manejarRespuestaMethod.invoke(instancia, response);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            handlersRegistrados.put("nuevoMensajeDirecto", response -> {
                try {
                    manejarPushMethod.invoke(instancia, response);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            handlersRegistrados.put("solicitarHistorialPrivado", response -> {
                try {
                    manejarHistorialMethod.invoke(instancia, response);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            return instancia;
        } catch (Exception e) {
            throw new RuntimeException("Error al crear instancia con mocks", e);
        }
    }

    // Métodos simuladores para llamar a la instancia real
    private void simularManejarRespuestaEnvio(DTOResponse response) {
        try {
            var method = GestionMensajesImpl.class.getDeclaredMethod("manejarRespuestaEnvioMensaje", DTOResponse.class);
            method.setAccessible(true);
            method.invoke(gestionMensajes, response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void simularManejarNuevoMensajePush(DTOResponse response) {
        try {
            var method = GestionMensajesImpl.class.getDeclaredMethod("manejarNuevoMensajePush", DTOResponse.class);
            method.setAccessible(true);
            method.invoke(gestionMensajes, response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void simularManejarHistorial(DTOResponse response) {
        try {
            var method = GestionMensajesImpl.class.getDeclaredMethod("manejarHistorial", DTOResponse.class);
            method.setAccessible(true);
            method.invoke(gestionMensajes, response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Crea una respuesta exitosa del servidor
     */
    private DTOResponse crearRespuestaExitosa(String action, Object data) {
        return new DTOResponse(action, "success", "Operación exitosa", data);
    }

    /**
     * Crea una respuesta de error del servidor
     */
    private DTOResponse crearRespuestaError(String action, String mensaje) {
        return new DTOResponse(action, "error", mensaje, null);
    }

    /**
     * Crea datos de mensaje para las respuestas
     */
    private Map<String, Object> crearDatosMensaje(String mensajeId, String fechaEnvio) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("mensajeId", mensajeId);
        datos.put("fechaEnvio", fechaEnvio);
        return datos;
    }
}
