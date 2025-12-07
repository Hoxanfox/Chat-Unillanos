package gestionUsuario.sesion;

import com.google.gson.JsonObject;
import comunicacion.EnviadorPeticiones;
import comunicacion.GestorRespuesta;
import comunicacion.IEnviadorPeticiones;
import comunicacion.IGestorRespuesta;
import dto.comunicacion.DTORequest;
import dto.comunicacion.DTOResponse;
import gestionUsuario.especialista.EspecialistaUsuariosImpl;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Gestor responsable de manejar el cierre de sesión del usuario.
 * 
 * Responsabilidades:
 * 1. Enviar petición de logout al servidor con action: "logoutUser"
 * 2. Actualizar el estado del usuario en la base de datos local a "inactivo"
 * 3. Limpiar la sesión en memoria (GestorSesionUsuario)
 * 4. Manejar respuestas exitosas y errores del servidor
 * 5. Garantizar cierre de sesión local incluso si el servidor no responde
 */
public class GestorCierreSesion {

    private final GestorSesionUsuario gestorSesion;
    private final EspecialistaUsuariosImpl especialistaUsuarios;
    private final IEnviadorPeticiones enviadorPeticiones;
    private final IGestorRespuesta gestorRespuesta;

    public GestorCierreSesion() {
        this.gestorSesion = GestorSesionUsuario.getInstancia();
        this.especialistaUsuarios = new EspecialistaUsuariosImpl();
        this.enviadorPeticiones = new EnviadorPeticiones();
        this.gestorRespuesta = GestorRespuesta.getInstancia();
        System.out.println("✅ [GestorCierreSesion]: Inicializado correctamente.");
    }

    /**
     * Cierra la sesión del usuario actual.
     * 
     * Proceso:
     * 1. Valida que haya sesión activa
     * 2. Actualiza estado en BD local a "inactivo"
     * 3. Envía petición al servidor con action: "logoutUser"
     * 4. Espera respuesta del servidor (timeout 3 segundos)
     * 5. Limpia sesión en memoria
     * 
     * @return CompletableFuture que se completa con true si el logout fue exitoso
     */
    public CompletableFuture<Boolean> cerrarSesion() {
        System.out.println("🔄 [GestorCierreSesion]: Iniciando proceso de cierre de sesión...");

        CompletableFuture<Boolean> future = new CompletableFuture<>();

        // 1. Validar sesión activa
        if (!gestorSesion.haySesionActiva()) {
            System.out.println("⚠️ [GestorCierreSesion]: No hay sesión activa para cerrar");
            future.complete(true);
            return future;
        }

        String usuarioId = gestorSesion.getUserId();
        System.out.println("📋 [GestorCierreSesion]: Cerrando sesión del usuario: " + usuarioId);

        // 2. Actualizar estado local en BD antes de notificar al servidor
        try {
            UUID userId = UUID.fromString(usuarioId);
            especialistaUsuarios.actualizarEstadoUsuario(userId, "inactivo");
            System.out.println("✅ [GestorCierreSesion]: Estado en BD local actualizado a 'inactivo'");
        } catch (Exception e) {
            System.err.println("⚠️ [GestorCierreSesion]: Error al actualizar estado local: " + e.getMessage());
            // Continuar de todos modos
        }

        // 3. Preparar petición con el protocolo correcto: action = "logoutUser"
        JsonObject data = new JsonObject();
        data.addProperty("userId", usuarioId);

        DTORequest request = new DTORequest("logoutUser", data);

        // 4. Registrar manejador para la respuesta del servidor
        gestorRespuesta.registrarManejador("logoutUser", (DTOResponse respuesta) -> {
            System.out.println("📥 [GestorCierreSesion]: Respuesta recibida del servidor");
            
            if (respuesta.fueExitoso()) {
                System.out.println("✅ [GestorCierreSesion]: Servidor confirmó logout exitoso");
            } else {
                System.err.println("⚠️ [GestorCierreSesion]: Servidor reportó error: " + 
                                   respuesta.getMessage());
            }

            // Cerrar sesión local independientemente del resultado
            limpiarSesionLocal();
            future.complete(respuesta.fueExitoso());
        });

        // 5. Enviar petición al servidor
        try {
            enviadorPeticiones.enviar(request);
            System.out.println("📤 [GestorCierreSesion]: Petición 'logoutUser' enviada al servidor");
        } catch (Exception e) {
            System.err.println("❌ [GestorCierreSesion]: Error al enviar petición: " + e.getMessage());
            limpiarSesionLocal();
            future.complete(false);
            return future;
        }

        // 6. Timeout de seguridad (3 segundos)
        // Si el servidor no responde, cerrar sesión local de todos modos
        CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS).execute(() -> {
            if (!future.isDone()) {
                System.out.println("⏱️ [GestorCierreSesion]: Timeout esperando respuesta del servidor");
                System.out.println("🔒 [GestorCierreSesion]: Cerrando sesión local por seguridad");
                limpiarSesionLocal();
                future.complete(true); // Consideramos exitoso el cierre local
            }
        });

        return future;
    }

    /**
     * Limpia la sesión en memoria del usuario.
     * Esto elimina el userId y el objeto Usuario de la sesión.
     */
    private void limpiarSesionLocal() {
        gestorSesion.cerrarSesion();
        System.out.println("🧹 [GestorCierreSesion]: Sesión local limpiada (userId y Usuario eliminados)");
    }

    /**
     * Verifica si hay una sesión activa.
     * 
     * @return true si hay sesión activa, false en caso contrario
     */
    public boolean haySesionActiva() {
        return gestorSesion.haySesionActiva();
    }
}

