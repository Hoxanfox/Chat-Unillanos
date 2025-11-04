package gestionUsuario.autenticacion;

import dto.comunicacion.DTOResponse;
import dto.vistaLogin.DTOAutenticacion;
import dominio.Usuario;
import comunicacion.IEnviadorPeticiones;
import comunicacion.IGestorRespuesta;
import gestionUsuario.especialista.IEspecialistaUsuarios;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Pruebas unitarias para AutenticarUsuario usando dobles simples (stubs).
 */
public class AutenticarUsuarioTest {

    private StubEnviador enviador;
    private StubGestorRespuesta gestor;
    private StubEspecialista especialista;
    private AutenticarUsuario autenticar;

    @BeforeEach
    public void setup() {
        enviador = new StubEnviador();
        gestor = new StubGestorRespuesta();
        especialista = new StubEspecialista();
        autenticar = new AutenticarUsuario(enviador, gestor, especialista);
    }

    @AfterEach
    public void tearDown() {
        gestor.clearHandlers();
    }

    @Test
    public void testAutenticar_success_standardFields() throws Exception {
        DTOAutenticacion dto = new DTOAutenticacion("juan@example.com", "pass");

        // preparar respuesta exitosa con campos estándar
        Map<String, Object> data = new HashMap<>();
        UUID id = UUID.randomUUID();
        data.put("userId", id.toString());
        data.put("nombre", "Juan Pérez");
        data.put("email", "juan@example.com");
        data.put("imagenBase64", "abcd1234");

        DTOResponse resp = new DTOResponse("authenticateUser", "success", "Autenticación exitosa", data);

        CompletableFuture<Boolean> futuro = autenticar.autenticar(dto);

        // simular la llegada de la respuesta desde el gestor
        gestor.emit("authenticateUser", resp);

        boolean result = futuro.get(2, TimeUnit.SECONDS);
        Assertions.assertTrue(result, "La autenticación debe reportar éxito");

        // verificar que el especialista guardó al usuario
        Usuario u = especialista.lastSaved;
        Assertions.assertNotNull(u);
        Assertions.assertEquals(id, u.getIdUsuario());
        Assertions.assertEquals("Juan Pérez", u.getNombre());
        Assertions.assertEquals("juan@example.com", u.getEmail());
    }

    /*
    @Test
    public void testAutenticar_success_alternateFields() throws Exception {
        DTOAutenticacion dto = new DTOAutenticacion("maria@example.com", "pass");

        Map<String, Object> data = new HashMap<>();
        UUID id = UUID.randomUUID();
        data.put("id", id.toString());
        data.put("nombreUsuario", "María");
        data.put("correo", "maria@example.com");
        data.put("photoId", "img-xyz");

        DTOResponse resp = new DTOResponse("authenticateUser", "success", "ok", data);

        CompletableFuture<Boolean> futuro = autenticar.autenticar(dto);
        gestor.emit("authenticateUser", resp);

        boolean result = futuro.get(2, TimeUnit.SECONDS);
        Assertions.assertTrue(result);

        Usuario u = especialista.lastSaved;
        Assertions.assertNotNull(u);
        Assertions.assertEquals(id, u.getIdUsuario());
        Assertions.assertEquals("María", u.getNombre());
        Assertions.assertEquals("maria@example.com", u.getEmail());
        Assertions.assertEquals("img-xyz", u.getPhotoIdServidor());
    }
    */

    @Test
    public void testAutenticar_error_withCampoMotivo() throws Exception {
        DTOAutenticacion dto = new DTOAutenticacion("bad@example.com", "pass");

        Map<String, Object> errorData = new HashMap<>();
        errorData.put("campo", "password");
        errorData.put("motivo", "Demasiado corta");

        DTOResponse resp = new DTOResponse("authenticateUser", "error", "Email o contraseña inválidos", errorData);

        CompletableFuture<Boolean> futuro = autenticar.autenticar(dto);
        gestor.emit("authenticateUser", resp);

        boolean result = futuro.get(2, TimeUnit.SECONDS);
        Assertions.assertFalse(result);
    }

    @Test
    public void testAutenticar_invalidCredentials() throws Exception {
        DTOAutenticacion dto = new DTOAutenticacion("noexist@example.com", "wrong");

        DTOResponse resp = new DTOResponse("authenticateUser", "error", "Credenciales incorrectas", null);

        CompletableFuture<Boolean> futuro = autenticar.autenticar(dto);
        gestor.emit("authenticateUser", resp);

        boolean result = futuro.get(2, TimeUnit.SECONDS);
        Assertions.assertFalse(result);
    }

    @Test
    public void testAutenticar_responseWithoutData() throws Exception {
        DTOAutenticacion dto = new DTOAutenticacion("x@example.com", "pass");

        // success pero sin data -> debe fallar al procesar
        DTOResponse resp = new DTOResponse("authenticateUser", "success", "Autenticación exitosa", null);

        CompletableFuture<Boolean> futuro = autenticar.autenticar(dto);
        gestor.emit("authenticateUser", resp);

        boolean result = futuro.get(2, TimeUnit.SECONDS);
        Assertions.assertFalse(result);
    }

    // --- Stubs simples ---

    static class StubEnviador implements IEnviadorPeticiones {
        public dto.comunicacion.DTORequest lastRequest;
        @Override
        public void enviar(dto.comunicacion.DTORequest request) {
            this.lastRequest = request;
        }
    }

    static class StubGestorRespuesta implements IGestorRespuesta {
        private final Map<String, Consumer<DTOResponse>> handlers = new HashMap<>();

        @Override
        public void iniciarEscucha() { }

        @Override
        public void detenerEscucha() { }

        @Override
        public void registrarManejador(String tipoOperacion, Consumer<DTOResponse> manejador) {
            handlers.put(tipoOperacion, manejador);
        }

        public void emit(String tipoOperacion, DTOResponse resp) {
            // intentar por nombre exacto
            Consumer<DTOResponse> h = handlers.get(tipoOperacion);
            if (h != null) {
                h.accept(resp);
                return;
            }
            // si no existe, intentar con 'login' o 'authenticateUser'
            h = handlers.get("login");
            if (h != null) h.accept(resp);
        }

        public void clearHandlers() {
            handlers.clear();
        }
    }

    static class StubEspecialista implements IEspecialistaUsuarios {
        public Usuario lastSaved = null;

        @Override
        public void guardarUsuario(Usuario usuario) {
            this.lastSaved = usuario;
        }

        @Override
        public Usuario obtenerUsuarioPorId(java.util.UUID idUsuario) {
            return null;
        }

        @Override
        public Usuario obtenerUsuarioPorEmail(String email) {
            return null;
        }

        @Override
        public void actualizarUsuario(Usuario usuario) {
            this.lastSaved = usuario;
        }

        @Override
        public void eliminarUsuario(java.util.UUID idUsuario) { }

        @Override
        public java.util.List<Usuario> obtenerTodosUsuarios() { return java.util.Collections.emptyList(); }

        @Override
        public boolean existeUsuarioPorEmail(String email) { return false; }

        @Override
        public void actualizarEstadoUsuario(java.util.UUID idUsuario, String nuevoEstado) { }
    }
}
