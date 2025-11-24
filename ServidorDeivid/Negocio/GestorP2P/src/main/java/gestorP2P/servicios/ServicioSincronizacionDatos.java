package gestorP2P.servicios;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import conexion.p2p.interfaces.IRouterMensajes;
import conexion.p2p.interfaces.IGestorConexiones;
import dominio.clienteServidor.Canal;
import dominio.clienteServidor.relaciones.CanalMiembro;
import dominio.clienteServidor.Mensaje;
import dominio.clienteServidor.Usuario;
import dominio.merkletree.IMerkleEntity;
import dominio.merkletree.MerkleTree;
import dto.comunicacion.DTORequest;
import dto.comunicacion.DTOResponse;
import gestorP2P.interfaces.IServicioP2P;
import logger.LoggerCentral;
import observador.IObservador;
import repositorio.clienteServidor.CanalMiembroRepositorio;
import repositorio.clienteServidor.CanalRepositorio;
import repositorio.clienteServidor.MensajeRepositorio;
import repositorio.clienteServidor.UsuarioRepositorio;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ServicioSincronizacionDatos implements IServicioP2P, IObservador {

    private static final String TAG = "SyncDatos";

    // --- COLORES ANSI ---
    private static final String RESET = "\u001B[0m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String AZUL = "\u001B[34m";
    private static final String CYAN = "\u001B[36m";
    private static final String VERDE = "\u001B[32m";
    private static final String ROJO = "\u001B[31m";

    private static final String[] ORDEN_SYNC = {"USUARIO", "CANAL", "MIEMBRO", "MENSAJE"};

    private IGestorConexiones gestor;
    private final Gson gson;
    private ServicioNotificacionCambios notificador;

    private final UsuarioRepositorio repoUsuario;
    private final CanalRepositorio repoCanal;
    private final CanalMiembroRepositorio repoMiembro;
    private final MensajeRepositorio repoMensaje;
    private final Map<String, MerkleTree> bosqueMerkle;

    // Flag para saber si hicimos cambios durante este ciclo y debemos avisar al final
    private boolean huboCambiosEnEsteCiclo = false;

    public ServicioSincronizacionDatos() {
        this.gson = new Gson();
        this.repoUsuario = new UsuarioRepositorio();
        this.repoCanal = new CanalRepositorio();
        this.repoMiembro = new CanalMiembroRepositorio();
        this.repoMensaje = new MensajeRepositorio();
        this.bosqueMerkle = new HashMap<>();
    }

    public void setNotificador(ServicioNotificacionCambios notificador) {
        this.notificador = notificador;
    }

    @Override
    public String getNombre() { return "ServicioSincronizacionDatos"; }

    public void forzarSincronizacion() {
        if (gestor == null) return;
        LoggerCentral.warn(TAG, "Forzando sincronización manual...");
        huboCambiosEnEsteCiclo = false; // Reset flag
        iniciarSincronizacionGeneral();
    }

    public void onBaseDeDatosCambio() {
        new Thread(this::reconstruirTodosLosArboles).start();
    }

    private void reconstruirTodosLosArboles() {
        bosqueMerkle.put("USUARIO", new MerkleTree(repoUsuario.obtenerTodosParaSync()));
        bosqueMerkle.put("CANAL", new MerkleTree(repoCanal.obtenerTodosParaSync()));
        bosqueMerkle.put("MIEMBRO", new MerkleTree(repoMiembro.obtenerTodosParaSync()));
        bosqueMerkle.put("MENSAJE", new MerkleTree(repoMensaje.obtenerTodosParaSync()));
    }

    @Override
    public void inicializar(IGestorConexiones gestor, IRouterMensajes router) {
        this.gestor = gestor;

        // 1. CHECK MULTIPLE
        router.registrarAccion("sync_check_all", (datos, origen) -> {
            reconstruirTodosLosArboles();
            JsonObject hashes = new JsonObject();
            for(String tipo : ORDEN_SYNC) {
                hashes.addProperty(tipo, bosqueMerkle.get(tipo).getRootHash());
            }
            return new DTOResponse("sync_check_all", "success", "Hashes Locales", hashes);
        });

        router.registrarManejadorRespuesta("sync_check_all", (resp) -> {
            if(resp.fueExitoso() && resp.getData() != null) {
                procesarDiferenciasEnOrden(resp.getData().getAsJsonObject());
            }
        });

        // 2. GET IDs
        router.registrarAccion("sync_get_ids", (datos, origen) -> {
            String tipo = datos.getAsString();
            List<? extends IMerkleEntity> lista = obtenerListaPorTipo(tipo);
            JsonArray ids = new JsonArray();
            lista.forEach(e -> ids.add(e.getId()));
            JsonObject result = new JsonObject();
            result.addProperty("tipo", tipo);
            result.add("ids", ids);
            return new DTOResponse("sync_get_ids", "success", "IDs", result);
        });

        router.registrarManejadorRespuesta("sync_get_ids", (resp) -> {
            if(resp.fueExitoso()) {
                JsonObject res = resp.getData().getAsJsonObject();
                solicitarEntidadesFaltantes(res.get("tipo").getAsString(), res.get("ids").getAsJsonArray());
            }
        });

        // 3. GET ENTITY
        router.registrarAccion("sync_get_entity", (datos, origen) -> {
            JsonObject req = datos.getAsJsonObject();
            String tipo = req.get("tipo").getAsString();
            IMerkleEntity entidad = buscarEntidad(tipo, req.get("id").getAsString());
            if (entidad != null) {
                JsonObject env = new JsonObject();
                env.addProperty("tipo", tipo);
                env.add("data", gson.toJsonTree(entidad));
                return new DTOResponse("sync_get_entity", "success", "Found", env);
            }
            return new DTOResponse("sync_get_entity", "error", "Not found", null);
        });

        router.registrarManejadorRespuesta("sync_get_entity", (resp) -> {
            if(resp.fueExitoso()) {
                JsonObject env = resp.getData().getAsJsonObject();
                guardarEntidadGenerica(env.get("tipo").getAsString(), env.get("data"));
                // Marcamos que hubo cambios, pero NO notificamos aún
                huboCambiosEnEsteCiclo = true;
                iniciarSincronizacionGeneral(); // Siguiente ciclo de verificación
            }
        });
    }

    private void procesarDiferenciasEnOrden(JsonObject hashesRemotos) {
        for (String tipo : ORDEN_SYNC) {
            if (!hashesRemotos.has(tipo)) continue;

            String hashRemoto = hashesRemotos.get(tipo).getAsString();
            String hashLocal = bosqueMerkle.get(tipo).getRootHash();

            if (!hashLocal.equals(hashRemoto)) {
                String hLocalCorto = hashLocal.length() > 8 ? hashLocal.substring(0, 8) : hashLocal;
                String hRemotoCorto = hashRemoto.length() > 8 ? hashRemoto.substring(0, 8) : hashRemoto;
                LoggerCentral.warn(TAG, "Diferencia en " + AMARILLO + tipo + RESET + " (L:" + hLocalCorto + " != R:" + hRemotoCorto + "). Reparando...");

                DTORequest req = new DTORequest("sync_get_ids", gson.toJsonTree(tipo));
                gestor.broadcast(gson.toJson(req));
                return; // Detener aquí y reparar este nivel
            }
        }

        // SI LLEGAMOS AQUÍ, ES QUE TODO ESTÁ SINCRONIZADO
        LoggerCentral.info(TAG, VERDE + "✔ Sistema totalmente sincronizado." + RESET);

        // Aquí enviamos el PUSH solo si realmente trajimos datos nuevos
        if (huboCambiosEnEsteCiclo && notificador != null) {
            LoggerCentral.info(TAG, "Notificando actualización masiva a clientes CS...");
            notificador.notificarCambio(
                    ServicioNotificacionCambios.TipoEvento.ACTUALIZACION_ESTADO,
                    null
            );
            huboCambiosEnEsteCiclo = false; // Reset para el futuro
        }
    }

    private void solicitarEntidadesFaltantes(String tipo, JsonArray idsRemotos) {
        List<? extends IMerkleEntity> locales = obtenerListaPorTipo(tipo);
        List<String> misIds = locales.stream().map(IMerkleEntity::getId).collect(Collectors.toList());

        int faltantes = 0;
        for(JsonElement el : idsRemotos) {
            String idRemoto = el.getAsString();
            if(!misIds.contains(idRemoto)) {
                faltantes++;
                LoggerCentral.debug(TAG, "Solicitando " + CYAN + tipo + RESET + ": " + idRemoto);
                JsonObject reqPayload = new JsonObject();
                reqPayload.addProperty("tipo", tipo);
                reqPayload.addProperty("id", idRemoto);
                DTORequest req = new DTORequest("sync_get_entity", reqPayload);
                gestor.broadcast(gson.toJson(req));
            }
        }
        if (faltantes == 0) {
            LoggerCentral.info(TAG, "Tenemos todos los IDs de " + tipo + ". (Diferencia de contenido).");
        }
    }

    // --- Helpers y Guardado ---

    private List<? extends IMerkleEntity> obtenerListaPorTipo(String tipo) {
        switch (tipo) {
            case "USUARIO": return repoUsuario.obtenerTodosParaSync();
            case "CANAL": return repoCanal.obtenerTodosParaSync();
            case "MIEMBRO": return repoMiembro.obtenerTodosParaSync();
            case "MENSAJE": return repoMensaje.obtenerTodosParaSync();
            default: return List.of();
        }
    }

    private IMerkleEntity buscarEntidad(String tipo, String id) {
        return obtenerListaPorTipo(tipo).stream().filter(e -> e.getId().equals(id)).findFirst().orElse(null);
    }

    private void guardarEntidadGenerica(String tipo, JsonElement data) {
        try {
            boolean guardado = false;
            switch (tipo) {
                case "USUARIO": guardado = repoUsuario.guardar(gson.fromJson(data, Usuario.class)); break;
                case "CANAL": guardado = repoCanal.guardar(gson.fromJson(data, Canal.class)); break;
                case "MIEMBRO": guardado = repoMiembro.guardar(gson.fromJson(data, CanalMiembro.class)); break;
                case "MENSAJE": guardado = repoMensaje.guardar(gson.fromJson(data, Mensaje.class)); break;
            }

            if (guardado) {
                LoggerCentral.info(TAG, "Guardado exitoso (Sync): " + CYAN + tipo + RESET);
                // NOTA: Ya NO notificamos aquí individualmente para evitar spam de push
            }
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error guardando " + tipo + ": " + e.getMessage());
        }
    }

    @Override
    public void iniciar() {
        LoggerCentral.info(TAG, "Iniciando servicio de sincronización...");
        reconstruirTodosLosArboles();
    }

    @Override public void detener() {}

    private void iniciarSincronizacionGeneral() {
        new Thread(() -> {
            try { Thread.sleep(500); } catch (Exception e) {}
            reconstruirTodosLosArboles();
            DTORequest req = new DTORequest("sync_check_all", null);
            gestor.broadcast(gson.toJson(req));
        }).start();
    }

    @Override
    public void actualizar(String tipo, Object datos) {
        if ("PEER_CONECTADO".equals(tipo)) {
            LoggerCentral.info(TAG, "Peer conectado (" + datos + "). Verificando integridad...");
            huboCambiosEnEsteCiclo = false; // Iniciamos ciclo limpio
            iniciarSincronizacionGeneral();
        }
    }
}