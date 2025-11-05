package gestionCanales.aceptarInvitacion;

import comunicacion.EnviadorPeticiones;
import comunicacion.GestorRespuesta;
import comunicacion.IEnviadorPeticiones;
import comunicacion.IGestorRespuesta;
import dto.comunicacion.DTORequest;
import dto.peticion.DTOUnirseCanal;
import gestionUsuario.sesion.GestorSesionUsuario;
import repositorio.canal.IRepositorioCanal; // Se necesita el repositorio

import java.util.concurrent.CompletableFuture;

/**
 * Implementación de la lógica para aceptar una invitación a un canal.
 * Ahora también persiste el cambio en la base de datos local.
 */
public class AceptadorInvitacion implements IAceptadorInvitacion {

    private final GestorSesionUsuario gestorSesion;
    private final IEnviadorPeticiones enviadorPeticiones;
    private final IGestorRespuesta gestorRespuesta;
    private final IRepositorioCanal repositorioCanal; // Inyectar el repositorio

    // Se asume que el repositorio se pasa a través del constructor o un método de fábrica.
    public AceptadorInvitacion(IRepositorioCanal repositorioCanal) {
        this.gestorSesion = GestorSesionUsuario.getInstancia();
        this.enviadorPeticiones = new EnviadorPeticiones();
        this.gestorRespuesta = GestorRespuesta.getInstancia();
        this.repositorioCanal = repositorioCanal; // Guardar la instancia del repositorio
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<Void> aceptarInvitacion(String canalId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        String usuarioId = gestorSesion.getUserId();

        if (usuarioId == null) {
            future.completeExceptionally(new IllegalStateException("El usuario no ha iniciado sesión."));
            return future;
        }

        DTOUnirseCanal payload = new DTOUnirseCanal(usuarioId, canalId);
        DTORequest request = new DTORequest("unirseCanal", payload);

        gestorRespuesta.registrarManejador(request.getAction(), (respuesta) -> {
            if ("success".equals(respuesta.getStatus())) {
                // ¡Paso clave añadido!
                // Si el servidor confirma, guardamos la relación en la BD local.
                repositorioCanal.agregarMiembroACanal(usuarioId, canalId)
                        .thenAccept(guardado -> {
                            if (guardado) {
                                future.complete(null); // Todo el proceso fue exitoso.
                            } else {
                                future.completeExceptionally(new RuntimeException("Se unió al canal en el servidor, pero falló el registro local."));
                            }
                        })
                        .exceptionally(ex -> {
                            future.completeExceptionally(ex);
                            return null;
                        });
            } else {
                future.completeExceptionally(new RuntimeException("No se pudo unir al canal: " + respuesta.getMessage()));
            }
        });

        enviadorPeticiones.enviar(request);

        return future;
    }
}

