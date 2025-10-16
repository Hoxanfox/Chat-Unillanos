package gestionCanales.nuevoCanal;

import comunicacion.EnviadorPeticiones;
import comunicacion.GestorRespuesta;
import comunicacion.IEnviadorPeticiones;
import comunicacion.IGestorRespuesta;
import dto.comunicacion.DTORequest;
import dto.peticion.DTOCrearCanal;
import dominio.Canal;
import gestionUsuario.sesion.GestorSesionUsuario;
import observador.IObservador;
import repositorio.canal.IRepositorioCanal;

import java.util.ArrayList;
import java.util.List;
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

    // Patrón Observador
    private final List<IObservador> observadores;

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
        this.observadores = new ArrayList<>();
        System.out.println("✅ [CreadorCanal]: Inicializado con Observador");
    }

    // Implementación del patrón Observador
    @Override
    public void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
            System.out.println("[CreadorCanal] Observador registrado: " + observador.getClass().getSimpleName());
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
        System.out.println("[CreadorCanal] Observador removido: " + observador.getClass().getSimpleName());
    }

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        System.out.println("[CreadorCanal] Notificando a " + observadores.size() + " observadores - Tipo: " + tipoDeDato);
        for (IObservador observador : observadores) {
            observador.actualizar(tipoDeDato, datos);
        }
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
            notificarObservadores("CANAL_ERROR", "Usuario no autenticado");
            return future;
        }

        // Notificar inicio de creación
        notificarObservadores("CANAL_CREACION_INICIADA", nombre);

        DTORequest request = new DTORequest("crearCanal", new DTOCrearCanal(creadorId, nombre, descripcion));

        // 1. Registrar el callback que procesará la respuesta del servidor.
        gestorRespuesta.registrarManejador(request.getAction(), (respuesta) -> {
            if (!"success".equals(respuesta.getStatus())) {
                String mensajeError = "Error del servidor: " + respuesta.getMessage();
                future.completeExceptionally(new RuntimeException(mensajeError));
                notificarObservadores("CANAL_ERROR", mensajeError);
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
                                System.out.println("✅ [CreadorCanal]: Canal creado y guardado: " + canalDeDominio.getNombre());
                                notificarObservadores("CANAL_CREADO_EXITOSAMENTE", canalDeDominio);
                                future.complete(canalDeDominio);
                            } else {
                                String mensajeError = "El canal se creó en el servidor, pero falló al guardarse localmente.";
                                future.completeExceptionally(new RuntimeException(mensajeError));
                                notificarObservadores("CANAL_ERROR", mensajeError);
                            }
                        })
                        .exceptionally(ex -> {
                            notificarObservadores("CANAL_ERROR", "Error al guardar: " + ex.getMessage());
                            future.completeExceptionally(ex);
                            return null;
                        });
            } catch (Exception e) {
                String mensajeError = "Error al procesar la respuesta del servidor.";
                future.completeExceptionally(new RuntimeException(mensajeError, e));
                notificarObservadores("CANAL_ERROR", mensajeError);
            }
        });

        // 2. Enviar la petición al servidor.
        enviadorPeticiones.enviar(request);

        return future;
    }
}
