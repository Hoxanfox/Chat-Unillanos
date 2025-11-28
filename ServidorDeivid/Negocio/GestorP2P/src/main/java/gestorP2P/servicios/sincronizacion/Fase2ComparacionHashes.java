package gestorP2P.servicios.sincronizacion;

import com.google.gson.JsonObject;
import logger.LoggerCentral;

import java.util.ArrayList;
import java.util.List;

/**
 * Fase 2: Comparación de Hashes
 *
 * Responsabilidad: Comparar hashes locales con hashes remotos para detectar diferencias.
 * Determina qué tipos de entidades requieren sincronización.
 */
public class Fase2ComparacionHashes {

    private static final String TAG = "Fase2-Comparacion";
    private static final String VERDE = "\u001B[32m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";

    private final Fase1ConstruccionArboles fase1;

    public Fase2ComparacionHashes(Fase1ConstruccionArboles fase1) {
        this.fase1 = fase1;
    }

    /**
     * Compara hashes locales con remotos y retorna lista de tipos con diferencias.
     *
     * @param hashesRemotos Hashes recibidos del peer remoto
     * @return Lista de tipos que tienen diferencias (necesitan sincronización)
     */
    public ResultadoComparacion compararHashes(JsonObject hashesRemotos) {
        LoggerCentral.info(TAG, CYAN + "=== Comparando hashes ===" + RESET);

        List<String> tiposConDiferencias = new ArrayList<>();
        List<String> tiposSincronizados = new ArrayList<>();

        for (String tipo : Fase1ConstruccionArboles.getOrdenSync()) {
            if (!hashesRemotos.has(tipo)) {
                LoggerCentral.warn(TAG, AMARILLO + "Tipo " + tipo + " no presente en hashes remotos" + RESET);
                continue;
            }

            String hashRemoto = hashesRemotos.get(tipo).getAsString();
            String hashLocal = fase1.obtenerHashPorTipo(tipo);

            if (hashLocal.isEmpty()) {
                LoggerCentral.warn(TAG, AMARILLO + "Hash local no disponible para " + tipo + RESET);
                continue;
            }

            LoggerCentral.debug(TAG, String.format("Comparando %s:", tipo));
            LoggerCentral.debug(TAG, String.format("  Local:  %s", hashLocal));
            LoggerCentral.debug(TAG, String.format("  Remoto: %s", hashRemoto));

            if (!hashLocal.equals(hashRemoto)) {
                String hLocalCorto = hashLocal.substring(0, Math.min(8, hashLocal.length()));
                String hRemotoCorto = hashRemoto.substring(0, Math.min(8, hashRemoto.length()));

                LoggerCentral.warn(TAG, AMARILLO + String.format("⚠ Diferencia en %s (L:%s != R:%s)",
                    tipo, hLocalCorto, hRemotoCorto) + RESET);

                tiposConDiferencias.add(tipo);
            } else {
                String hCorto = hashLocal.substring(0, Math.min(8, hashLocal.length()));
                LoggerCentral.debug(TAG, VERDE + String.format("✓ %s sincronizado (Hash: %s)", tipo, hCorto) + RESET);
                tiposSincronizados.add(tipo);
            }
        }

        return new ResultadoComparacion(tiposConDiferencias, tiposSincronizados);
    }

    /**
     * Clase que encapsula el resultado de la comparación.
     */
    public static class ResultadoComparacion {
        private final List<String> tiposConDiferencias;
        private final List<String> tiposSincronizados;

        public ResultadoComparacion(List<String> tiposConDiferencias, List<String> tiposSincronizados) {
            this.tiposConDiferencias = tiposConDiferencias;
            this.tiposSincronizados = tiposSincronizados;
        }

        public boolean hayDiferencias() {
            return !tiposConDiferencias.isEmpty();
        }

        public List<String> getTiposConDiferencias() {
            return tiposConDiferencias;
        }

        public List<String> getTiposSincronizados() {
            return tiposSincronizados;
        }

        public String getPrimerTipoConDiferencia() {
            return tiposConDiferencias.isEmpty() ? null : tiposConDiferencias.get(0);
        }

        public boolean todoSincronizado() {
            return tiposConDiferencias.isEmpty();
        }
    }
}

