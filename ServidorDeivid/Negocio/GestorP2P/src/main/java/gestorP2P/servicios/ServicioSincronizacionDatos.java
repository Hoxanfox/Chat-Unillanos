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
import gestorP2P.utils.GsonUtil;
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
        this.gson = GsonUtil.crearGson();
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
        if (gestor == null) {
            LoggerCentral.error(TAG, ROJO + "No se puede forzar sincronización: gestor es null" + RESET);
            return;
        }
        LoggerCentral.warn(TAG, "Forzando sincronización manual...");
        huboCambiosEnEsteCiclo = false; // Reset flag
        iniciarSincronizacionGeneral();
    }

    public void onBaseDeDatosCambio() {
        LoggerCentral.info(TAG, AZUL + "Base de datos cambió. Reconstruyendo árboles Merkle..." + RESET);
        new Thread(this::reconstruirTodosLosArboles).start();
    }

    private void reconstruirTodosLosArboles() {
        LoggerCentral.debug(TAG, "Iniciando reconstrucción de todos los árboles Merkle...");

        bosqueMerkle.put("USUARIO", new MerkleTree(repoUsuario.obtenerTodosParaSync()));
        String hashUsuario = bosqueMerkle.get("USUARIO").getRootHash();
        LoggerCentral.debug(TAG, "- Árbol USUARIO reconstruido. Hash: " +
            hashUsuario.substring(0, Math.min(8, hashUsuario.length())));

        bosqueMerkle.put("CANAL", new MerkleTree(repoCanal.obtenerTodosParaSync()));
        String hashCanal = bosqueMerkle.get("CANAL").getRootHash();
        LoggerCentral.debug(TAG, "- Árbol CANAL reconstruido. Hash: " +
            hashCanal.substring(0, Math.min(8, hashCanal.length())));

        bosqueMerkle.put("MIEMBRO", new MerkleTree(repoMiembro.obtenerTodosParaSync()));
        String hashMiembro = bosqueMerkle.get("MIEMBRO").getRootHash();
        LoggerCentral.debug(TAG, "- Árbol MIEMBRO reconstruido. Hash: " +
            hashMiembro.substring(0, Math.min(8, hashMiembro.length())));

        bosqueMerkle.put("MENSAJE", new MerkleTree(repoMensaje.obtenerTodosParaSync()));
        String hashMensaje = bosqueMerkle.get("MENSAJE").getRootHash();
        LoggerCentral.debug(TAG, "- Árbol MENSAJE reconstruido. Hash: " +
            hashMensaje.substring(0, Math.min(8, hashMensaje.length())));

        LoggerCentral.info(TAG, VERDE + "Todos los árboles Merkle reconstruidos exitosamente" + RESET);
    }

    @Override
    public void inicializar(IGestorConexiones gestor, IRouterMensajes router) {
        LoggerCentral.info(TAG, AZUL + "Inicializando ServicioSincronizacionDatos..." + RESET);
        this.gestor = gestor;

        // 1. CHECK MULTIPLE
        router.registrarAccion("sync_check_all", (datos, origen) -> {
            LoggerCentral.debug(TAG, "Recibido sync_check_all desde: " + origen);
            reconstruirTodosLosArboles();
            JsonObject hashes = new JsonObject();
            for(String tipo : ORDEN_SYNC) {
                hashes.addProperty(tipo, bosqueMerkle.get(tipo).getRootHash());
            }
            LoggerCentral.debug(TAG, "Enviando hashes locales a: " + origen);
            return new DTOResponse("sync_check_all", "success", "Hashes Locales", hashes);
        });

        router.registrarManejadorRespuesta("sync_check_all", (resp) -> {
            LoggerCentral.info(TAG, CYAN + ">>> Manejador sync_check_all activado <<<" + RESET);
            LoggerCentral.debug(TAG, "Respuesta exitosa: " + resp.fueExitoso());
            LoggerCentral.debug(TAG, "Respuesta tiene datos: " + (resp.getData() != null));

            if(resp.fueExitoso() && resp.getData() != null) {
                LoggerCentral.info(TAG, "✓ Respuesta sync_check_all recibida. Procesando diferencias...");
                LoggerCentral.debug(TAG, "Datos recibidos: " + resp.getData().toString());
                procesarDiferenciasEnOrden(resp.getData().getAsJsonObject());
            } else {
                LoggerCentral.warn(TAG, AMARILLO + "Respuesta sync_check_all no exitosa o sin datos" + RESET);
                if (!resp.fueExitoso()) {
                    LoggerCentral.warn(TAG, "Status: " + resp.getStatus());
                }
            }
        });

        // 2. GET IDs
        router.registrarAccion("sync_get_ids", (datos, origen) -> {
            String tipo = datos.getAsString();
            LoggerCentral.debug(TAG, "Recibido sync_get_ids para tipo: " + CYAN + tipo + RESET + " desde: " + origen);
            List<? extends IMerkleEntity> lista = obtenerListaPorTipo(tipo);
            JsonArray ids = new JsonArray();
            lista.forEach(e -> ids.add(e.getId()));
            LoggerCentral.debug(TAG, "Enviando " + ids.size() + " IDs de tipo " + tipo);
            LoggerCentral.debug(TAG, "IDs: " + ids.toString());
            JsonObject result = new JsonObject();
            result.addProperty("tipo", tipo);
            result.add("ids", ids);
            return new DTOResponse("sync_get_ids", "success", "IDs", result);
        });

        router.registrarManejadorRespuesta("sync_get_ids", (resp) -> {
            LoggerCentral.info(TAG, CYAN + ">>> Manejador sync_get_ids activado <<<" + RESET);
            LoggerCentral.debug(TAG, "Respuesta exitosa: " + resp.fueExitoso());

            if(resp.fueExitoso()) {
                JsonObject res = resp.getData().getAsJsonObject();
                String tipo = res.get("tipo").getAsString();
                int cantidadIds = res.get("ids").getAsJsonArray().size();
                LoggerCentral.info(TAG, "✓ Recibidos " + cantidadIds + " IDs de tipo " + CYAN + tipo + RESET);
                LoggerCentral.debug(TAG, "IDs recibidos: " + res.get("ids").toString());
                solicitarEntidadesFaltantes(tipo, res.get("ids").getAsJsonArray());
            } else {
                LoggerCentral.error(TAG, ROJO + "Error en respuesta sync_get_ids: " + resp.getStatus() + RESET);
            }
        });

        // 3. GET ENTITY
        router.registrarAccion("sync_get_entity", (datos, origen) -> {
            JsonObject req = datos.getAsJsonObject();
            String tipo = req.get("tipo").getAsString();
            String id = req.get("id").getAsString();
            LoggerCentral.debug(TAG, "Recibido sync_get_entity: " + CYAN + tipo + RESET + " ID: " + id);
            IMerkleEntity entidad = buscarEntidad(tipo, id);
            if (entidad != null) {
                LoggerCentral.debug(TAG, "Entidad encontrada. Enviando " + tipo + " ID: " + id);
                JsonObject env = new JsonObject();
                env.addProperty("tipo", tipo);
                env.add("data", gson.toJsonTree(entidad));
                return new DTOResponse("sync_get_entity", "success", "Found", env);
            }
            LoggerCentral.warn(TAG, AMARILLO + "Entidad NO encontrada: " + tipo + " ID: " + id + RESET);
            return new DTOResponse("sync_get_entity", "error", "Not found", null);
        });

        router.registrarManejadorRespuesta("sync_get_entity", (resp) -> {
            if(resp.fueExitoso()) {
                JsonObject env = resp.getData().getAsJsonObject();
                String tipo = env.get("tipo").getAsString();
                LoggerCentral.info(TAG, VERDE + "Entidad recibida para guardar: " + tipo + RESET);
                guardarEntidadGenerica(tipo, env.get("data"));
                // Marcamos que hubo cambios, pero NO notificamos aún
                huboCambiosEnEsteCiclo = true;
                iniciarSincronizacionGeneral(); // Siguiente ciclo de verificación
            } else {
                LoggerCentral.error(TAG, ROJO + "Error en respuesta sync_get_entity: " + resp.getStatus() + RESET);
            }
        });

        LoggerCentral.info(TAG, VERDE + "ServicioSincronizacionDatos inicializado correctamente" + RESET);
    }

    private void procesarDiferenciasEnOrden(JsonObject hashesRemotos) {
        LoggerCentral.info(TAG, AZUL + "=== Procesando diferencias en orden ===" + RESET);
        LoggerCentral.debug(TAG, "Hashes remotos recibidos: " + hashesRemotos.toString());

        for (String tipo : ORDEN_SYNC) {
            if (!hashesRemotos.has(tipo)) {
                LoggerCentral.warn(TAG, AMARILLO + "Tipo " + tipo + " no presente en hashes remotos" + RESET);
                continue;
            }

            String hashRemoto = hashesRemotos.get(tipo).getAsString();
            String hashLocal = bosqueMerkle.get(tipo).getRootHash();

            LoggerCentral.debug(TAG, "Comparando " + tipo + ":");
            LoggerCentral.debug(TAG, "  - Hash Local:  " + hashLocal);
            LoggerCentral.debug(TAG, "  - Hash Remoto: " + hashRemoto);
            LoggerCentral.debug(TAG, "  - Son iguales: " + hashLocal.equals(hashRemoto));

            if (!hashLocal.equals(hashRemoto)) {
                String hLocalCorto = hashLocal.length() > 8 ? hashLocal.substring(0, 8) : hashLocal;
                String hRemotoCorto = hashRemoto.length() > 8 ? hashRemoto.substring(0, 8) : hashRemoto;
                LoggerCentral.warn(TAG, "Diferencia en " + AMARILLO + tipo + RESET + " (L:" + hLocalCorto + " != R:" + hRemotoCorto + "). Reparando...");

                DTORequest req = new DTORequest("sync_get_ids", gson.toJsonTree(tipo));
                String jsonReq = gson.toJson(req);
                LoggerCentral.debug(TAG, "Enviando request sync_get_ids: " + jsonReq);
                gestor.broadcast(jsonReq);
                LoggerCentral.info(TAG, "Solicitando IDs para tipo: " + CYAN + tipo + RESET);
                LoggerCentral.info(TAG, AMARILLO + "⏸ Deteniendo verificación. Esperando respuesta de IDs para " + tipo + RESET);
                return; // Detener aquí y reparar este nivel
            } else {
                String hCorto = hashLocal.length() > 8 ? hashLocal.substring(0, 8) : hashLocal;
                LoggerCentral.debug(TAG, VERDE + "✓ " + tipo + " sincronizado (Hash: " + hCorto + ")" + RESET);
            }
        }

        // SI LLEGAMOS AQUÍ, ES QUE TODO ESTÁ SINCRONIZADO
        LoggerCentral.info(TAG, VERDE + "✔ Sistema totalmente sincronizado." + RESET);
        LoggerCentral.info(TAG, "Flag huboCambiosEnEsteCiclo: " + huboCambiosEnEsteCiclo);
        LoggerCentral.info(TAG, "Notificador disponible: " + (notificador != null));

        // Aquí enviamos el PUSH solo si realmente trajimos datos nuevos
        if (huboCambiosEnEsteCiclo && notificador != null) {
            LoggerCentral.info(TAG, AZUL + "Notificando actualización masiva a clientes CS..." + RESET);
            notificador.notificarCambio(
                    ServicioNotificacionCambios.TipoEvento.ACTUALIZACION_ESTADO,
                    null
            );
            huboCambiosEnEsteCiclo = false; // Reset para el futuro
            LoggerCentral.info(TAG, VERDE + "Notificación de cambios enviada exitosamente" + RESET);
        } else if (huboCambiosEnEsteCiclo && notificador == null) {
            LoggerCentral.warn(TAG, AMARILLO + "Hubo cambios pero el notificador es null" + RESET);
        } else if (!huboCambiosEnEsteCiclo) {
            LoggerCentral.debug(TAG, "No hubo cambios en este ciclo. No se envía notificación.");
        }
    }

    private void solicitarEntidadesFaltantes(String tipo, JsonArray idsRemotos) {
        LoggerCentral.info(TAG, "Verificando entidades faltantes para tipo: " + CYAN + tipo + RESET);
        List<? extends IMerkleEntity> locales = obtenerListaPorTipo(tipo);
        List<String> misIds = locales.stream().map(IMerkleEntity::getId).collect(Collectors.toList());

        LoggerCentral.debug(TAG, "IDs locales (" + misIds.size() + "): " + misIds.toString());

        // Convertir idsRemotos a lista para logging
        List<String> idsRemotosList = new java.util.ArrayList<>();
        for(JsonElement el : idsRemotos) {
            idsRemotosList.add(el.getAsString());
        }
        LoggerCentral.debug(TAG, "IDs remotos (" + idsRemotosList.size() + "): " + idsRemotosList.toString());

        int faltantes = 0;
        for(JsonElement el : idsRemotos) {
            String idRemoto = el.getAsString();
            boolean loTengo = misIds.contains(idRemoto);
            LoggerCentral.debug(TAG, "  Verificando ID remoto: " + idRemoto + " -> ¿Lo tengo?: " + loTengo);

            if(!loTengo) {
                faltantes++;
                LoggerCentral.info(TAG, AMARILLO + "⬇ Solicitando " + CYAN + tipo + RESET + AMARILLO + " faltante ID: " + idRemoto + RESET);
                JsonObject reqPayload = new JsonObject();
                reqPayload.addProperty("tipo", tipo);
                reqPayload.addProperty("id", idRemoto);
                DTORequest req = new DTORequest("sync_get_entity", reqPayload);
                String jsonReq = gson.toJson(req);
                LoggerCentral.debug(TAG, "Request JSON: " + jsonReq);
                gestor.broadcast(jsonReq);
            }
        }

        if (faltantes == 0) {
            LoggerCentral.warn(TAG, AMARILLO + "⚠ Tenemos todos los IDs de " + tipo + " pero los hashes difieren." + RESET);
            LoggerCentral.warn(TAG, AMARILLO + "   Esto indica diferencia de contenido en las entidades existentes." + RESET);
            // Aquí deberíamos reiniciar verificación
            LoggerCentral.info(TAG, "Reiniciando verificación para detectar cambios de contenido...");
            iniciarSincronizacionGeneral();
        } else {
            LoggerCentral.info(TAG, VERDE + "✓ Solicitadas " + faltantes + " entidades faltantes de tipo " + tipo + RESET);
        }
    }

    // --- Helpers y Guardado ---

    private List<? extends IMerkleEntity> obtenerListaPorTipo(String tipo) {
        LoggerCentral.debug(TAG, "Obteniendo lista para tipo: " + tipo);
        List<? extends IMerkleEntity> lista;
        switch (tipo) {
            case "USUARIO": lista = repoUsuario.obtenerTodosParaSync(); break;
            case "CANAL": lista = repoCanal.obtenerTodosParaSync(); break;
            case "MIEMBRO": lista = repoMiembro.obtenerTodosParaSync(); break;
            case "MENSAJE": lista = repoMensaje.obtenerTodosParaSync(); break;
            default:
                LoggerCentral.error(TAG, ROJO + "Tipo desconocido: " + tipo + RESET);
                lista = List.of();
        }
        LoggerCentral.debug(TAG, "Lista obtenida para " + tipo + ": " + lista.size() + " elementos");
        return lista;
    }

    private IMerkleEntity buscarEntidad(String tipo, String id) {
        LoggerCentral.debug(TAG, "Buscando entidad " + tipo + " con ID: " + id);
        IMerkleEntity resultado = obtenerListaPorTipo(tipo).stream()
            .filter(e -> e.getId().equals(id))
            .findFirst()
            .orElse(null);

        if (resultado != null) {
            LoggerCentral.debug(TAG, VERDE + "Entidad encontrada: " + tipo + " ID: " + id + RESET);
        } else {
            LoggerCentral.debug(TAG, AMARILLO + "Entidad no encontrada: " + tipo + " ID: " + id + RESET);
        }

        return resultado;
    }

    private void guardarEntidadGenerica(String tipo, JsonElement data) {
        LoggerCentral.debug(TAG, "Intentando guardar entidad de tipo: " + CYAN + tipo + RESET);
        try {
            boolean guardado = false;
            switch (tipo) {
                case "USUARIO":
                    Usuario usuario = gson.fromJson(data, Usuario.class);
                    LoggerCentral.debug(TAG, "Guardando Usuario: " + usuario.getId());
                    guardado = repoUsuario.guardar(usuario);
                    break;
                case "CANAL":
                    Canal canal = gson.fromJson(data, Canal.class);
                    LoggerCentral.debug(TAG, "Guardando Canal: " + canal.getId());
                    guardado = repoCanal.guardar(canal);
                    break;
                case "MIEMBRO":
                    CanalMiembro miembro = gson.fromJson(data, CanalMiembro.class);
                    LoggerCentral.debug(TAG, "Guardando Miembro: " + miembro.getId());
                    guardado = repoMiembro.guardar(miembro);
                    break;
                case "MENSAJE":
                    Mensaje mensaje = gson.fromJson(data, Mensaje.class);
                    LoggerCentral.debug(TAG, "Guardando Mensaje: " + mensaje.getId());
                    guardado = repoMensaje.guardar(mensaje);
                    break;
                default:
                    LoggerCentral.error(TAG, ROJO + "Tipo desconocido para guardar: " + tipo + RESET);
            }

            if (guardado) {
                LoggerCentral.info(TAG, VERDE + "✓ Guardado exitoso (Sync): " + CYAN + tipo + RESET);
                // NOTA: Ya NO notificamos aquí individualmente para evitar spam de push
            } else {
                LoggerCentral.warn(TAG, AMARILLO + "No se guardó (posiblemente ya existía): " + tipo + RESET);
            }
        } catch (Exception e) {
            LoggerCentral.error(TAG, ROJO + "Error guardando " + tipo + ": " + e.getMessage() + RESET);
            e.printStackTrace();
        }
    }

    @Override
    public void iniciar() {
        LoggerCentral.info(TAG, AZUL + "======================================" + RESET);
        LoggerCentral.info(TAG, AZUL + "Iniciando servicio de sincronización..." + RESET);
        LoggerCentral.info(TAG, AZUL + "======================================" + RESET);
        reconstruirTodosLosArboles();
        LoggerCentral.info(TAG, VERDE + "Servicio de sincronización iniciado correctamente" + RESET);
    }

    @Override
    public void detener() {
        LoggerCentral.info(TAG, ROJO + "Deteniendo servicio de sincronización..." + RESET);
    }

    private void iniciarSincronizacionGeneral() {
        LoggerCentral.info(TAG, AZUL + "Programando sincronización general..." + RESET);
        new Thread(() -> {
            try {
                Thread.sleep(500);
                LoggerCentral.debug(TAG, "Esperando 500ms antes de sincronizar...");
            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error en sleep: " + e.getMessage());
            }
            reconstruirTodosLosArboles();
            DTORequest req = new DTORequest("sync_check_all", null);
            gestor.broadcast(gson.toJson(req));
            LoggerCentral.info(TAG, VERDE + "Broadcast de sync_check_all enviado" + RESET);
        }).start();
    }

    @Override
    public void actualizar(String tipo, Object datos) {
        LoggerCentral.debug(TAG, "Actualización recibida - Tipo: " + tipo + ", Datos: " + datos);
        if ("PEER_CONECTADO".equals(tipo)) {
            LoggerCentral.info(TAG, VERDE + "=== Peer conectado (" + datos + ") ===" + RESET);
            LoggerCentral.info(TAG, "Verificando integridad con el nuevo peer...");
            huboCambiosEnEsteCiclo = false; // Iniciamos ciclo limpio
            iniciarSincronizacionGeneral();
        } else {
            LoggerCentral.debug(TAG, "Tipo de actualización no manejado: " + tipo);
        }
    }
}
