package gestionCanales.listarCanales;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import comunicacion.EnviadorPeticiones;
import comunicacion.GestorRespuesta;
import comunicacion.IEnviadorPeticiones;
import comunicacion.IGestorRespuesta;
import dominio.Canal;
import dto.comunicacion.DTORequest;
import dto.peticion.DTOListarCanales;
import gestionUsuario.sesion.GestorSesionUsuario;
import observador.IObservador;
import repositorio.canal.IRepositorioCanal;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementación de la lógica para listar los canales de un usuario.
 */
public class ListadorCanales implements IListadorCanales {

    private final List<IObservador> observadores = new CopyOnWriteArrayList<>();
    private List<Canal> canalesCache = new ArrayList<>();
    private final IEnviadorPeticiones enviadorPeticiones;
    private final IGestorRespuesta gestorRespuesta;
    private final IRepositorioCanal repositorioCanal;
    private final GestorSesionUsuario gestorSesion;
    private final Gson gson;

    public ListadorCanales(IRepositorioCanal repositorioCanal) {
        this.repositorioCanal = repositorioCanal;
        this.enviadorPeticiones = new EnviadorPeticiones();
        this.gestorRespuesta = GestorRespuesta.getInstancia();
        this.gestorSesion = GestorSesionUsuario.getInstancia();
        this.gson = new Gson();
        this.gestorRespuesta.registrarManejador("listarCanales", this::manejarRespuestaListado);
    }

    @Override
    public void solicitarCanalesUsuario() {
        String usuarioId = gestorSesion.getUserId();
        if (usuarioId == null) {
            System.err.println("No se puede solicitar la lista de canales, el usuario no ha iniciado sesión.");
            return;
        }

        DTOListarCanales payload = new DTOListarCanales(usuarioId, 100, 0); // Limite 100, offset 0 por defecto
        DTORequest peticion = new DTORequest("listarCanales", payload);
        enviadorPeticiones.enviar(peticion);
    }

    private void manejarRespuestaListado(dto.comunicacion.DTOResponse respuesta) {
        if (!"success".equals(respuesta.getStatus())) {
            System.err.println("Error del servidor al listar canales: " + respuesta.getMessage());
            return;
        }

        try {
            Type tipoLista = new TypeToken<ArrayList<Map<String, Object>>>() {}.getType();
            List<Map<String, Object>> listaDeMapas = gson.fromJson(gson.toJson(respuesta.getData()), tipoLista);

            List<Canal> canalesDelServidor = new ArrayList<>();
            for (Map<String, Object> mapa : listaDeMapas) {
                canalesDelServidor.add(convertirMapaACanal(mapa));
            }

            // Sincronizar con la base de datos local
            repositorioCanal.sincronizarCanales(canalesDelServidor)
                    .thenAccept(v -> {
                        // Actualizar caché y notificar a la UI
                        this.canalesCache = canalesDelServidor;
                        notificarObservadores("CANALES_ACTUALIZADOS", new ArrayList<>(this.canalesCache));
                        System.out.println("Lista de canales sincronizada y UI notificada.");
                    })
                    .exceptionally(ex -> {
                        System.err.println("Fallo al sincronizar los canales con la DB local: " + ex.getMessage());
                        return null;
                    });

        } catch (Exception e) {
            System.err.println("Error al procesar la respuesta de la lista de canales: " + e.getMessage());
        }
    }

    private Canal convertirMapaACanal(Map<String, Object> data) {
        String id = (String) data.get("id");
        String nombre = (String) data.get("nombre");
        // El creadorId puede no estar presente en todos los DTOs, manejarlo con cuidado
        String idAdministrador = (String) data.getOrDefault("creadorId", null);

        return new Canal(
                UUID.fromString(id),
                nombre,
                (idAdministrador != null) ? UUID.fromString(idAdministrador) : null
        );
    }

    @Override
    public List<Canal> getCanales() {
        return new ArrayList<>(canalesCache);
    }

    @Override
    public void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) observadores.add(observador);
    }

    @Override
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
    }

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        for (IObservador observador : observadores) {
            observador.actualizar(tipoDeDato, datos);
        }
    }
}
