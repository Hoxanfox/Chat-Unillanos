package gestorP2P.servicios.sincronizacion;

import com.google.gson.JsonObject;
import dominio.merkletree.IMerkleEntity;
import dominio.merkletree.MerkleTree;
import logger.LoggerCentral;
import repositorio.clienteServidor.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fase 1: Construcción de Árboles Merkle
 *
 * Responsabilidad: Construir y mantener los árboles Merkle para cada tipo de entidad.
 * Genera hashes raíz para comparación con otros peers.
 */
public class Fase1ConstruccionArboles {

    private static final String TAG = "Fase1ConstruccionArboles";
    public static final String TIPO_USUARIO = "USUARIO";
    public static final String TIPO_CANAL = "CANAL";
    public static final String TIPO_MENSAJE = "MENSAJE";
    public static final String TIPO_CANAL_MIEMBRO = "CANAL_MIEMBRO";
    public static final String TIPO_CANAL_INVITACION = "CANAL_INVITACION"; // nuevo tipo
    public static final String TIPO_ARCHIVO = "ARCHIVO";


    // Orden de sincronización
    private static final String[] ORDEN_SYNC = {
            TIPO_USUARIO,
            TIPO_CANAL,
            TIPO_CANAL_MIEMBRO,
            TIPO_CANAL_INVITACION,
            TIPO_MENSAJE,
            TIPO_ARCHIVO
    };

    // Mapa de árboles por tipo (MerkleTree no es genérico)
    private final Map<String, MerkleTree> arbolesMerkle;

    // Repositorios
    private final UsuarioRepositorio usuarioRepo;
    private final CanalRepositorio canalRepo;
    private final MensajeRepositorio mensajeRepo;
    private final CanalMiembroRepositorio canalMiembroRepo;
    private final CanalInvitacionRepositorio canalInvitacionRepo;
    private final ArchivoRepositorio repoArchivo;


    public Fase1ConstruccionArboles() {
        this.arbolesMerkle = new ConcurrentHashMap<>();

        this.usuarioRepo = new UsuarioRepositorio();
        this.canalRepo = new CanalRepositorio();
        this.mensajeRepo = new MensajeRepositorio();
        this.canalMiembroRepo = new CanalMiembroRepositorio();
        this.canalInvitacionRepo = new CanalInvitacionRepositorio();
        this.repoArchivo = new ArchivoRepositorio();
    }

    /** Reconstruye todos los árboles Merkle desde la base de datos. */
    public void reconstruirTodosLosArboles() {
        LoggerCentral.info(TAG, "Construyendo árboles Merkle...");

        arbolesMerkle.clear();

        for (String tipo : ORDEN_SYNC) {
            List<? extends IMerkleEntity> entidades = obtenerEntidadesPorTipo(tipo);
            if (entidades == null) {
                continue;
            }
            MerkleTree arbol = new MerkleTree(entidades);
            arbolesMerkle.put(tipo, arbol);

            String hash = arbol.getRootHash();
            String hashCorto = hash.length() > 8 ? hash.substring(0, 8) : hash;
            LoggerCentral.debug(TAG, String.format("  - %s: %d entidades, hash: %s",
                    tipo, entidades.size(), hashCorto));
        }

        LoggerCentral.info(TAG, "Árboles Merkle construidos exitosamente.");
    }

    /** Reconstruye el árbol Merkle para un tipo específico. */
    public void reconstruirArbol(String tipo) {
        List<? extends IMerkleEntity> entidades = obtenerEntidadesPorTipo(tipo);
        MerkleTree arbol = new MerkleTree(entidades);
        arbolesMerkle.put(tipo, arbol);

        String hash = arbol.getRootHash();
        String hashCorto = hash.length() > 8 ? hash.substring(0, 8) : hash;

        LoggerCentral.debug(TAG, String.format("  - %s: %d entidades, hash: %s",
                tipo, entidades.size(), hashCorto));
    }

    /** Obtiene todos los hashes raíz en formato JSON. */
    public JsonObject obtenerHashesRaiz() {
        JsonObject hashes = new JsonObject();

        for (String tipo : ORDEN_SYNC) {
            MerkleTree arbol = arbolesMerkle.get(tipo);
            if (arbol != null) {
                hashes.addProperty(tipo, arbol.getRootHash());
            }
        }

        return hashes;
    }

    /** Obtiene el hash raíz de un tipo específico. */
    public String obtenerHashPorTipo(String tipo) {
        MerkleTree arbol = arbolesMerkle.get(tipo);
        return arbol != null ? arbol.getRootHash() : "";
    }

    /** Obtiene la lista de entidades para un tipo dado. */
    public List<? extends IMerkleEntity> obtenerEntidadesPorTipo(String tipo) {
        switch (tipo) {
            case TIPO_USUARIO:
                return usuarioRepo.obtenerTodosParaSync();
            case TIPO_CANAL:
                return canalRepo.obtenerTodosParaSync();
            case TIPO_CANAL_MIEMBRO:
                return canalMiembroRepo.obtenerTodosParaSync();
            case TIPO_CANAL_INVITACION:
                return canalInvitacionRepo.obtenerTodosParaSync();
            case TIPO_MENSAJE:
                return mensajeRepo.obtenerTodosParaSync();
            case TIPO_ARCHIVO:
                return repoArchivo.obtenerTodosParaSync();
            default:
                LoggerCentral.error(TAG, "Tipo desconocido: " + tipo);
                return List.of();
        }
    }

    /** Busca una entidad específica por tipo e ID. */
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
