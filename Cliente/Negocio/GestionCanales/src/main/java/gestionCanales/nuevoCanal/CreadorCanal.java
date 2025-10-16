package gestionCanales.nuevoCanal;

import comunicacion.EnviadorPeticiones;
import comunicacion.GestorRespuesta;
import comunicacion.IEnviadorPeticiones;
import comunicacion.IGestorRespuesta;
import dto.comunicacion.DTORequest;
import dto.peticion.DTOCrearCanal;
import dominio.Canal;
import gestionUsuario.sesion.GestorSesionUsuario;
import repositorio.canal.IRepositorioCanal;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación de la lógica para crear un nuevo canal.
 * Orquesta la comunicación con el servidor y la persistencia en el repositorio local.
 */
public class CreadorCanal implements ICreadorCanal {

    private final IRepositorioCanal repositorioCanal;
    private final GestorSesionUsuario gestorSesion;
    private final IEnviadorPeticiones enviadorPeticiones;
    private final IGestorRespuesta gestorRespuesta;

    /**
     * Constructor para inyección de dependencias.
     *
     * @param repositorioCanal   Componente para la persistencia de datos de canales.
     */
    public CreadorCanal(IRepositorioCanal repositorioCanal) {
        this.repositorioCanal = repositorioCanal;
        this.gestorSesion = GestorSesionUsuario.getInstancia();
        this.enviadorPeticiones = new EnviadorPeticiones();
        this.gestorRespuesta = GestorRespuesta.getInstancia();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<Canal> crearCanal(String nombre, String descripcion) {
        CompletableFuture<Canal> future = new CompletableFuture<>();

        String creadorId = gestorSesion.getUserId();
        if (creadorId == null) {
            future.completeExceptionally(new IllegalStateException("El usuario no ha iniciado sesión."));
            return future;
        }

        DTORequest request = new DTORequest("crearCanal", new DTOCrearCanal(creadorId, nombre, descripcion));

        // 1. Registrar el callback que procesará la respuesta del servidor.
        gestorRespuesta.registrarManejador(request.getAction(), (respuesta) -> {
            if (!"success".equals(respuesta.getStatus())) {
                future.completeExceptionally(new RuntimeException("Error del servidor: " + respuesta.getMessage()));
                return;
            }

            try {
                // Lógica de procesamiento y guardado local
                Map<String, Object> data = (Map<String, Object>) respuesta.getData();
                Canal canalDeDominio = new Canal(
                        UUID.fromString((String) data.get("id")),
                        (String) data.get("nombre"),
                        UUID.fromString((String) data.get("creadorId"))
                );

                repositorioCanal.guardar(canalDeDominio)
                        .thenAccept(guardado -> {
                            if (guardado) {
                                future.complete(canalDeDominio);
                            } else {
                                future.completeExceptionally(new RuntimeException("El canal se creó en el servidor, pero falló al guardarse localmente."));
                            }
                        })
                        .exceptionally(ex -> {
                            future.completeExceptionally(ex);
                            return null;
                        });
            } catch (Exception e) {
                future.completeExceptionally(new RuntimeException("Error al procesar la respuesta del servidor.", e));
            }
        });

        // 2. Enviar la petición al servidor.
        enviadorPeticiones.enviar(request);

        return future;
    }
}

