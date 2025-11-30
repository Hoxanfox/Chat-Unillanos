package gestorP2P.servicios.sincronizacion;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import conexion.p2p.interfaces.IGestorConexiones;
import dto.comunicacion.DTORequest;
import logger.LoggerCentral;

import java.util.ArrayList;
import java.util.List;

/**
 * Fase 4: Detección de Entidades Faltantes
 *
 * Responsabilidad: Identificar qué entidades faltan localmente comparando IDs.
 * Solicitar las entidades faltantes a los peers remotos.
 */
public class Fase4DeteccionFaltantes {

    private static final String TAG = "Fase4-Faltantes";
    private static final String AMARILLO = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String VERDE = "\u001B[32m";
    private static final String RESET = "\u001B[0m";

    private final Fase3SolicitudIDs fase3;
    private final IGestorConexiones gestor;
    private final Gson gson;

    public Fase4DeteccionFaltantes(Fase3SolicitudIDs fase3, IGestorConexiones gestor, Gson gson) {
        this.fase3 = fase3;
        this.gestor = gestor;
        this.gson = gson;
    }

    /**
     * Analiza IDs remotos y solicita las entidades que faltan localmente.
     *
     * @return Resultado con información sobre entidades faltantes y coincidentes
     */
    public ResultadoDeteccion detectarYSolicitarFaltantes(String tipo, JsonArray idsRemotos) {
        LoggerCentral.info(TAG, CYAN + "Detectando entidades faltantes para: " + tipo + RESET);

        List<String> idsLocales = fase3.obtenerListaIDsLocales(tipo);
        List<String> idsRemotosList = fase3.convertirIDsRemotos(idsRemotos);

        LoggerCentral.debug(TAG, String.format("IDs locales: %d, IDs remotos: %d",
            idsLocales.size(), idsRemotosList.size()));

        List<String> idsFaltantes = new ArrayList<>();
        List<String> idsCoincidentes = new ArrayList<>();

        // Detectar faltantes
        for (String idRemoto : idsRemotosList) {
            if (idsLocales.contains(idRemoto)) {
                idsCoincidentes.add(idRemoto);
                LoggerCentral.debug(TAG, "  ✓ ID presente: " + idRemoto);
            } else {
                idsFaltantes.add(idRemoto);
                LoggerCentral.debug(TAG, "  ✗ ID faltante: " + idRemoto);
            }
        }

        // Solicitar entidades faltantes
        if (!idsFaltantes.isEmpty()) {
            LoggerCentral.info(TAG, AMARILLO + String.format("⬇ Solicitando %d entidades faltantes de %s",
                idsFaltantes.size(), tipo) + RESET);

            for (String idFaltante : idsFaltantes) {
                solicitarEntidad(tipo, idFaltante);
            }
        } else {
            LoggerCentral.info(TAG, VERDE + "✓ No hay entidades faltantes" + RESET);
        }

        return new ResultadoDeteccion(idsFaltantes, idsCoincidentes, idsRemotosList.size());
    }

    /**
     * Solicita una entidad específica por tipo e ID.
     */
    private void solicitarEntidad(String tipo, String id) {
        LoggerCentral.info(TAG, AMARILLO + String.format("Solicitando %s ID: %s", tipo, id) + RESET);

        JsonObject payload = new JsonObject();
        payload.addProperty("tipo", tipo);
        payload.addProperty("id", id);

        DTORequest req = new DTORequest("sync_get_entity", payload);
        String jsonReq = gson.toJson(req);

        LoggerCentral.debug(TAG, "Request: " + jsonReq);
        gestor.broadcast(jsonReq);
    }

    /**
     * Clase que encapsula el resultado de la detección.
     */
    public static class ResultadoDeteccion {
        private final List<String> idsFaltantes;
        private final List<String> idsCoincidentes;
        private final int totalRemoto;

        public ResultadoDeteccion(List<String> idsFaltantes, List<String> idsCoincidentes, int totalRemoto) {
            this.idsFaltantes = idsFaltantes;
            this.idsCoincidentes = idsCoincidentes;
            this.totalRemoto = totalRemoto;
        }

        public boolean hayFaltantes() {
            return !idsFaltantes.isEmpty();
        }

        public boolean todosLosIDsCoinciden() {
            return idsFaltantes.isEmpty() && !idsCoincidentes.isEmpty();
        }

        public List<String> getIdsFaltantes() {
            return idsFaltantes;
        }

        public List<String> getIdsCoincidentes() {
            return idsCoincidentes;
        }

        public int getTotalRemoto() {
            return totalRemoto;
        }

        public int getCantidadFaltantes() {
            return idsFaltantes.size();
        }
    }
}

