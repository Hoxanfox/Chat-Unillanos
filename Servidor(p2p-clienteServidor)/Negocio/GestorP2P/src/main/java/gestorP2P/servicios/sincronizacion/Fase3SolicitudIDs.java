package gestorP2P.servicios.sincronizacion;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import conexion.p2p.interfaces.IGestorConexiones;
import dominio.merkletree.IMerkleEntity;
import dto.comunicacion.DTORequest;
import logger.LoggerCentral;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Fase 3: Solicitud de IDs
 *
 * Responsabilidad: Solicitar y procesar listas de IDs de entidades remotas.
 * Prepara la información para detectar entidades faltantes.
 */
public class Fase3SolicitudIDs {

    private static final String TAG = "Fase3-IDs";
    private static final String CYAN = "\u001B[36m";
    private static final String VERDE = "\u001B[32m";
    private static final String RESET = "\u001B[0m";

    private final Fase1ConstruccionArboles fase1;
    private final IGestorConexiones gestor;
    private final Gson gson;

    public Fase3SolicitudIDs(Fase1ConstruccionArboles fase1, IGestorConexiones gestor, Gson gson) {
        this.fase1 = fase1;
        this.gestor = gestor;
        this.gson = gson;
    }

    /**
     * Solicita los IDs de un tipo específico a los peers remotos.
     */
    public void solicitarIDs(String tipo) {
        LoggerCentral.info(TAG, CYAN + "Solicitando IDs para tipo: " + tipo + RESET);

        DTORequest req = new DTORequest("sync_get_ids", gson.toJsonTree(tipo));
        String jsonReq = gson.toJson(req);

        LoggerCentral.debug(TAG, "Request JSON: " + jsonReq);
        gestor.broadcast(jsonReq);

        LoggerCentral.info(TAG, VERDE + "✓ Solicitud de IDs enviada para: " + tipo + RESET);
    }

    /**
     * Obtiene los IDs locales para un tipo dado.
     */
    public JsonArray obtenerIDsLocales(String tipo) {
        List<? extends IMerkleEntity> entidades = fase1.obtenerEntidadesPorTipo(tipo);
        JsonArray ids = new JsonArray();

        entidades.forEach(e -> ids.add(e.getId()));

        LoggerCentral.debug(TAG, String.format("IDs locales de %s: %d elementos", tipo, ids.size()));

        return ids;
    }

    /**
     * Convierte IDs locales a lista de strings para comparación.
     */
    public List<String> obtenerListaIDsLocales(String tipo) {
        List<? extends IMerkleEntity> entidades = fase1.obtenerEntidadesPorTipo(tipo);
        return entidades.stream()
            .map(IMerkleEntity::getId)
            .collect(Collectors.toList());
    }

    /**
     * Convierte JsonArray de IDs remotos a lista de strings.
     */
    public List<String> convertirIDsRemotos(JsonArray idsRemotos) {
        List<String> lista = new java.util.ArrayList<>();
        for (JsonElement el : idsRemotos) {
            lista.add(el.getAsString());
        }
        return lista;
    }
}

