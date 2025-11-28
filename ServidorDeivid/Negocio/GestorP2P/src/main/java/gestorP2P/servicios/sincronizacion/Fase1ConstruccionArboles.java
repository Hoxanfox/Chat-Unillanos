package gestorP2P.servicios.sincronizacion;

import com.google.gson.JsonObject;
import dominio.merkletree.IMerkleEntity;
import dominio.merkletree.MerkleTree;
import logger.LoggerCentral;
import repositorio.clienteServidor.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fase 1: Construcción de Árboles Merkle
 *
 * Responsabilidad: Construir y mantener los árboles Merkle para cada tipo de entidad.
 * Genera hashes raíz para comparación con otros peers.
 */
public class Fase1ConstruccionArboles {

    private static final String TAG = "Fase1-Merkle";
    private static final String VERDE = "\u001B[32m";
    private static final String RESET = "\u001B[0m";
    private static final String AZUL = "\u001B[34m";

    private static final String[] ORDEN_SYNC = {"USUARIO", "CANAL", "MIEMBRO", "MENSAJE", "ARCHIVO"};

    private final Map<String, MerkleTree> bosqueMerkle;
    private final UsuarioRepositorio repoUsuario;
    private final CanalRepositorio repoCanal;
    private final CanalMiembroRepositorio repoMiembro;
    private final MensajeRepositorio repoMensaje;
    private final ArchivoRepositorio repoArchivo;

    public Fase1ConstruccionArboles() {
        this.bosqueMerkle = new HashMap<>();
        this.repoUsuario = new UsuarioRepositorio();
        this.repoCanal = new CanalRepositorio();
        this.repoMiembro = new CanalMiembroRepositorio();
        this.repoMensaje = new MensajeRepositorio();
        this.repoArchivo = new ArchivoRepositorio();
    }

    /**
     * Reconstruye todos los árboles Merkle con los datos actuales de BD.
     */
    public void reconstruirTodosLosArboles() {
        LoggerCentral.info(TAG, AZUL + "=== Reconstruyendo árboles Merkle ===" + RESET);

        for (String tipo : ORDEN_SYNC) {
            reconstruirArbol(tipo);
        }

        LoggerCentral.info(TAG, VERDE + "✓ Todos los árboles Merkle reconstruidos" + RESET);
    }

    /**
     * Reconstruye el árbol Merkle para un tipo específico.
     */
    public void reconstruirArbol(String tipo) {
        List<? extends IMerkleEntity> entidades = obtenerEntidadesPorTipo(tipo);
        MerkleTree arbol = new MerkleTree(entidades);
        bosqueMerkle.put(tipo, arbol);

        String hash = arbol.getRootHash();
        String hashCorto = hash.length() > 8 ? hash.substring(0, 8) : hash;

        LoggerCentral.debug(TAG, String.format("  - %s: %d entidades, hash: %s",
                tipo, entidades.size(), hashCorto));
    }

    /**
     * Obtiene todos los hashes raíz en formato JSON.
     */
    public JsonObject obtenerHashesRaiz() {
        JsonObject hashes = new JsonObject();

        for (String tipo : ORDEN_SYNC) {
            MerkleTree arbol = bosqueMerkle.get(tipo);
            if (arbol != null) {
                hashes.addProperty(tipo, arbol.getRootHash());
            }
        }

        return hashes;
    }

    /**
     * Obtiene el hash raíz de un tipo específico.
     */
    public String obtenerHashPorTipo(String tipo) {
        MerkleTree arbol = bosqueMerkle.get(tipo);
        return arbol != null ? arbol.getRootHash() : "";
    }

    /**
     * Obtiene la lista de entidades para un tipo dado.
     */
    public List<? extends IMerkleEntity> obtenerEntidadesPorTipo(String tipo) {
        switch (tipo) {
            case "USUARIO": return repoUsuario.obtenerTodosParaSync();
            case "CANAL": return repoCanal.obtenerTodosParaSync();
            case "MIEMBRO": return repoMiembro.obtenerTodosParaSync();
            case "MENSAJE": return repoMensaje.obtenerTodosParaSync();
            case "ARCHIVO": return repoArchivo.obtenerTodosParaSync();
            default:
                LoggerCentral.error(TAG, "Tipo desconocido: " + tipo);
                return List.of();
        }
    }

    /**
     * Busca una entidad específica por tipo e ID.
     */
    public IMerkleEntity buscarEntidad(String tipo, String id) {
        return obtenerEntidadesPorTipo(tipo).stream()
                .filter(e -> e.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public static String[] getOrdenSync() {
        return ORDEN_SYNC;
    }
}

