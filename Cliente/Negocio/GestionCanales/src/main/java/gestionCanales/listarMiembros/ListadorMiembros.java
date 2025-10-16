package gestionCanales.listarMiembros;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import comunicacion.EnviadorPeticiones;
import comunicacion.GestorRespuesta;
import comunicacion.IEnviadorPeticiones;
import comunicacion.IGestorRespuesta;
import dto.canales.DTOMiembroCanal;
import dto.comunicacion.DTORequest;
import dto.peticion.DTOListarMiembros;
import gestionUsuario.sesion.GestorSesionUsuario;
import repositorio.canal.IRepositorioCanal;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación de la lógica para listar los miembros de un canal.
 */
public class ListadorMiembros implements IListadorMiembros {

    private final IEnviadorPeticiones enviadorPeticiones;
    private final IGestorRespuesta gestorRespuesta;
    private final IRepositorioCanal repositorioCanal;
    private final GestorSesionUsuario gestorSesion;
    private final Gson gson;

    public ListadorMiembros(IRepositorioCanal repositorioCanal) {
        this.repositorioCanal = repositorioCanal;
        this.enviadorPeticiones = new EnviadorPeticiones();
        this.gestorRespuesta = GestorRespuesta.getInstancia();
        this.gestorSesion = GestorSesionUsuario.getInstancia();
        this.gson = new Gson();
    }

    @Override
    public CompletableFuture<List<DTOMiembroCanal>> solicitarMiembros(String canalId) {
        CompletableFuture<List<DTOMiembroCanal>> future = new CompletableFuture<>();

        String solicitanteId = gestorSesion.getUserId();
        if (solicitanteId == null) {
            future.completeExceptionally(new IllegalStateException("El usuario no ha iniciado sesión."));
            return future;
        }

        DTOListarMiembros payload = new DTOListarMiembros(canalId, solicitanteId);
        DTORequest peticion = new DTORequest("listarMiembros", payload);

        gestorRespuesta.registrarManejador(peticion.getAction(), (respuesta) -> {
            if (!"success".equals(respuesta.getStatus())) {
                future.completeExceptionally(new RuntimeException("Error del servidor al listar miembros: " + respuesta.getMessage()));
                return;
            }

            try {
                Type tipoLista = new TypeToken<ArrayList<DTOMiembroCanal>>() {}.getType();
                List<DTOMiembroCanal> miembros = gson.fromJson(gson.toJson(respuesta.getData()), tipoLista);

                // Sincronizar la lista de miembros en la base de datos local
                repositorioCanal.sincronizarMiembros(canalId, miembros)
                        .thenAccept(v -> {
                            future.complete(miembros); // Completar el futuro con la lista de miembros
                            System.out.println("Lista de miembros del canal " + canalId + " sincronizada.");
                        })
                        .exceptionally(ex -> {
                            future.completeExceptionally(new RuntimeException("Fallo al sincronizar miembros en la DB local", ex));
                            return null;
                        });

            } catch (Exception e) {
                future.completeExceptionally(new RuntimeException("Error al procesar la respuesta de la lista de miembros.", e));
            }
        });

        enviadorPeticiones.enviar(peticion);
        return future;
    }
}
