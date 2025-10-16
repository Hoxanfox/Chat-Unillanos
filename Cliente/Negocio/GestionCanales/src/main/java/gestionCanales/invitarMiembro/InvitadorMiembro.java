package gestionCanales.invitarMiembro;

import comunicacion.EnviadorPeticiones;
import comunicacion.GestorRespuesta;
import comunicacion.IEnviadorPeticiones;
import comunicacion.IGestorRespuesta;
import dto.comunicacion.DTORequest;
import dto.peticion.DTOGestionarMiembro;
import gestionUsuario.sesion.GestorSesionUsuario;
import repositorio.canal.IRepositorioCanal;

import java.util.concurrent.CompletableFuture;

/**
 * Implementación de la lógica para invitar a un miembro a un canal.
 * Construye la petición usando la acción "gestionarMiembro" y la envía al servidor.
 */
public class InvitadorMiembro implements IInvitadorMiembro {

    private final GestorSesionUsuario gestorSesion;
    private final IEnviadorPeticiones enviadorPeticiones;
    private final IGestorRespuesta gestorRespuesta;
    private final IRepositorioCanal repositorioCanal; // Se inyecta por consistencia arquitectónica.

    /**
     * Constructor para la clase.
     * @param repositorioCanal El repositorio de canales, necesario para futuras operaciones
     * o para mantener la consistencia en la inyección de dependencias.
     */
    public InvitadorMiembro(IRepositorioCanal repositorioCanal) {
        this.gestorSesion = GestorSesionUsuario.getInstancia();
        this.enviadorPeticiones = new EnviadorPeticiones();
        this.gestorRespuesta = GestorRespuesta.getInstancia();
        this.repositorioCanal = repositorioCanal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<Void> invitarMiembro(String canalId, String contactoId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        String adminId = gestorSesion.getUserId();

        if (adminId == null) {
            future.completeExceptionally(new IllegalStateException("El usuario administrador no ha iniciado sesión."));
            return future;
        }

        DTOGestionarMiembro payload = new DTOGestionarMiembro(adminId, canalId, contactoId, "AGREGAR");
        DTORequest request = new DTORequest("gestionarMiembro", payload);

        gestorRespuesta.registrarManejador(request.getAction(), (respuesta) -> {
            if ("success".equals(respuesta.getStatus())) {
                // IMPORTANTE: No se actualiza el repositorio local aquí.
                // La acción de invitar solo envía la solicitud. La membresía del canal
                // no cambia hasta que el usuario invitado ACEPTA.
                //
                // El servidor, después de que el usuario acepte, enviará una notificación
                // "nuevoMiembro" a todos los integrantes del canal. El cliente (incluido este)
                // debe tener un manejador para esa notificación que SÍ actualizará el repositorio local.
                //
                // Esto asegura que el estado local solo refleje membresías confirmadas.
                future.complete(null);
            } else {
                future.completeExceptionally(new RuntimeException("Error al invitar al miembro: " + respuesta.getMessage()));
            }
        });

        enviadorPeticiones.enviar(request);

        return future;
    }
}

