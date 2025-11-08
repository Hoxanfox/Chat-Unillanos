package gestionCanales.listarCanales;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import comunicacion.EnviadorPeticiones;
import comunicacion.GestorRespuesta;
import comunicacion.IEnviadorPeticiones;
import comunicacion.IGestorRespuesta;
import dominio.Canal;
import dto.canales.DTOCanalCreado;
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
import java.util.stream.Collectors;

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
        System.out.println("📥 [ListadorCanales]: Respuesta recibida - Status: " + respuesta.getStatus());

        if (!"success".equals(respuesta.getStatus())) {
            System.err.println("Error del servidor al listar canales: " + respuesta.getMessage());
            return;
        }

        try {
            System.out.println("📋 [ListadorCanales]: Parseando datos de la respuesta...");
            Type tipoLista = new TypeToken<ArrayList<Map<String, Object>>>() {}.getType();
            List<Map<String, Object>> listaDeMapas = gson.fromJson(gson.toJson(respuesta.getData()), tipoLista);
            System.out.println("✅ [ListadorCanales]: Se encontraron " + listaDeMapas.size() + " canales en la respuesta");

            List<Canal> canalesDelServidor = new ArrayList<>();
            for (Map<String, Object> mapa : listaDeMapas) {
                System.out.println("🔄 [ListadorCanales]: Convirtiendo canal - idCanal: " + mapa.get("idCanal") + ", nombreCanal: " + mapa.get("nombreCanal"));
                canalesDelServidor.add(convertirMapaACanal(mapa));
            }
            System.out.println("✅ [ListadorCanales]: Convertidos " + canalesDelServidor.size() + " canales a objetos de dominio");

            // Sincronizar con la base de datos local
            repositorioCanal.sincronizarCanales(canalesDelServidor)
                    .thenAccept(v -> {
                        System.out.println("✅ [ListadorCanales]: Sincronización completada");
                        // Actualizar caché
                        this.canalesCache = canalesDelServidor;

                        // ✅ SOLUCIÓN: Convertir Canal (dominio) a DTOCanalCreado para la UI
                        List<DTOCanalCreado> canalesDTO = canalesDelServidor.stream()
                                .map(canal -> new DTOCanalCreado(
                                        canal.getIdCanal().toString(),
                                        canal.getNombre()
                                ))
                                .collect(Collectors.toList());

                        System.out.println("✅ [ListadorCanales]: Convertidos " + canalesDTO.size() + " canales a DTOs");
                        System.out.println("📢 [ListadorCanales]: Notificando " + canalesDTO.size() + " canales a " + observadores.size() + " observadores");
                        notificarObservadores("CANALES_ACTUALIZADOS", canalesDTO);
                        System.out.println("Lista de canales sincronizada y UI notificada.");
                    })
                    .exceptionally(ex -> {
                        System.err.println("❌ [ListadorCanales]: Fallo al sincronizar los canales con la DB local: " + ex.getMessage());
                        ex.printStackTrace();
                        return null;
                    });

        } catch (Exception e) {
            System.err.println("❌ [ListadorCanales]: Error al procesar la respuesta de la lista de canales: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Canal convertirMapaACanal(Map<String, Object> data) {
        // El servidor envía "idCanal" y "nombreCanal", no "id" y "nombre"
        String id = (String) data.get("idCanal");
        String nombre = (String) data.get("nombreCanal");
        // El servidor envía "ownerId", no "creadorId"
        String idAdministrador = (String) data.getOrDefault("ownerId", null);

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

    // --- Métodos del Patrón Observador ---
    @Override
    public void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
            System.out.println("✅ [ListadorCanales]: Observador registrado. Total: " + observadores.size());
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
        System.out.println("🗑️ [ListadorCanales]: Observador removido. Total: " + observadores.size());
    }

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        System.out.println("📢 [ListadorCanales]: Notificando a " + observadores.size() + " observadores. Tipo: " + tipoDeDato);
        for (IObservador observador : observadores) {
            observador.actualizar(tipoDeDato, datos);
        }
    }
}
