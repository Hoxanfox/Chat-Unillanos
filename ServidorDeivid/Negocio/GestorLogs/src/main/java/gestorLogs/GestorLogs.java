package gestorLogs;

import dto.logs.DTOLog;
import logger.LoggerCentral;
import observador.IObservador;
import observador.ISujeto;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Gestor de Logs - Capa de Negocio
 * Maneja la lógica de negocio relacionada con logs
 * Implementa ISujeto para notificar cambios a los observadores
 */
public class GestorLogs implements ISujeto {

    private static final String TAG = "GestorLogs";
    private final List<IObservador> observadores;
    private final List<DTOLog> logsEnMemoria;
    private final int maxLogsEnMemoria;
    private final String rutaArchivoLogs;

    public GestorLogs() {
        this(1000, "logs/app.log");
    }

    public GestorLogs(int maxLogsEnMemoria, String rutaArchivoLogs) {
        this.observadores = new ArrayList<>();
        this.logsEnMemoria = Collections.synchronizedList(new ArrayList<>());
        this.maxLogsEnMemoria = maxLogsEnMemoria;
        this.rutaArchivoLogs = rutaArchivoLogs;

        // Registrarse como observador del LoggerCentral
        LoggerCentral.registrarObservador(new IObservador() {
            @Override
            public void actualizar(String tipoDeDato, Object datos) {
                if ("NUEVO_LOG".equals(tipoDeDato) && datos instanceof String[]) {
                    String[] logData = (String[]) datos;
                    procesarNuevoLog(logData);
                }
            }
        });

        LoggerCentral.info(TAG, "GestorLogs inicializado");
    }

    /**
     * Procesa un nuevo log recibido del LoggerCentral
     */
    private void procesarNuevoLog(String[] logData) {
        if (logData.length >= 4) {
            DTOLog nuevoLog = new DTOLog(logData[0], logData[1], logData[2], logData[3]);
            agregarLogEnMemoria(nuevoLog);
            notificarObservadores("NUEVO_LOG", nuevoLog);
        }
    }

    /**
     * Agrega un log a la memoria limitada
     */
    private void agregarLogEnMemoria(DTOLog log) {
        synchronized (logsEnMemoria) {
            logsEnMemoria.add(log);
            // Limitar tamaño en memoria
            if (logsEnMemoria.size() > maxLogsEnMemoria) {
                logsEnMemoria.remove(0);
            }
        }
    }

    /**
     * Obtiene los logs recientes de memoria
     */
    public List<DTOLog> obtenerLogsRecientes(int cantidad) {
        synchronized (logsEnMemoria) {
            int size = logsEnMemoria.size();
            int fromIndex = Math.max(0, size - cantidad);
            return new ArrayList<>(logsEnMemoria.subList(fromIndex, size));
        }
    }

    /**
     * Obtiene todos los logs en memoria
     */
    public List<DTOLog> obtenerTodosLosLogsEnMemoria() {
        synchronized (logsEnMemoria) {
            return new ArrayList<>(logsEnMemoria);
        }
    }

    /**
     * Lee los logs del archivo físico
     */
    public List<DTOLog> leerLogsDesdeArchivo(int maxLineas) throws IOException {
        List<DTOLog> logs = new ArrayList<>();
        File archivo = new File(rutaArchivoLogs);

        if (!archivo.exists()) {
            LoggerCentral.warn(TAG, "Archivo de logs no existe: " + rutaArchivoLogs);
            return logs;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
            String linea;
            int contador = 0;

            while ((linea = reader.readLine()) != null && contador < maxLineas) {
                DTOLog log = parsearLineaLog(linea);
                if (log != null) {
                    logs.add(log);
                    contador++;
                }
            }
        }

        LoggerCentral.info(TAG, "Logs leídos desde archivo: " + logs.size());
        return logs;
    }

    /**
     * Parsea una línea del archivo de logs
     */
    private DTOLog parsearLineaLog(String linea) {
        try {
            // Formato esperado: [LEVEL] [TAG] mensaje
            // o simplemente: [LEVEL] mensaje
            if (!linea.startsWith("[")) {
                return null;
            }

            int primerCierre = linea.indexOf("]");
            if (primerCierre == -1) {
                return null;
            }

            String level = linea.substring(1, primerCierre).trim();
            String resto = linea.substring(primerCierre + 1).trim();

            String source = "System";
            String message = resto;

            // Intentar extraer el tag/source si existe
            if (resto.startsWith("[")) {
                int segundoCierre = resto.indexOf("]");
                if (segundoCierre > 0) {
                    source = resto.substring(1, segundoCierre).trim();
                    message = resto.substring(segundoCierre + 1).trim();
                }
            }

            return new DTOLog(level, source, message);

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Filtra logs por nivel
     */
    public List<DTOLog> filtrarPorNivel(String nivel) {
        synchronized (logsEnMemoria) {
            return logsEnMemoria.stream()
                    .filter(log -> log.getLevel().equalsIgnoreCase(nivel))
                    .toList();
        }
    }

    /**
     * Filtra logs por source/tag
     */
    public List<DTOLog> filtrarPorSource(String source) {
        synchronized (logsEnMemoria) {
            return logsEnMemoria.stream()
                    .filter(log -> log.getSource().contains(source))
                    .toList();
        }
    }

    /**
     * Busca logs que contengan un texto específico
     */
    public List<DTOLog> buscarPorTexto(String texto) {
        synchronized (logsEnMemoria) {
            String textoBusqueda = texto.toLowerCase();
            return logsEnMemoria.stream()
                    .filter(log -> log.getMessage().toLowerCase().contains(textoBusqueda))
                    .toList();
        }
    }

    /**
     * Limpia los logs en memoria
     */
    public void limpiarLogsEnMemoria() {
        synchronized (logsEnMemoria) {
            logsEnMemoria.clear();
        }
        LoggerCentral.info(TAG, "Logs en memoria limpiados");
    }

    /**
     * Obtiene estadísticas de logs
     */
    public EstadisticasLogs obtenerEstadisticas() {
        synchronized (logsEnMemoria) {
            int total = logsEnMemoria.size();
            long info = logsEnMemoria.stream().filter(l -> "INFO".equals(l.getLevel())).count();
            long warning = logsEnMemoria.stream().filter(l -> "WARNING".equals(l.getLevel())).count();
            long error = logsEnMemoria.stream().filter(l -> "ERROR".equals(l.getLevel())).count();
            long debug = logsEnMemoria.stream().filter(l -> "DEBUG".equals(l.getLevel())).count();

            return new EstadisticasLogs(total, (int)info, (int)warning, (int)error, (int)debug);
        }
    }

    // --- IMPLEMENTACIÓN DE ISujeto ---

    @Override
    public void registrarObservador(IObservador observador) {
        synchronized (observadores) {
            if (!observadores.contains(observador)) {
                observadores.add(observador);
                LoggerCentral.info(TAG, "Observador registrado para logs");
            }
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        synchronized (observadores) {
            observadores.remove(observador);
            LoggerCentral.info(TAG, "Observador removido de logs");
        }
    }

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        synchronized (observadores) {
            for (IObservador obs : observadores) {
                obs.actualizar(tipoDeDato, datos);
            }
        }
    }

    /**
     * Clase interna para estadísticas de logs
     */
    public static class EstadisticasLogs {
        private final int total;
        private final int info;
        private final int warning;
        private final int error;
        private final int debug;

        public EstadisticasLogs(int total, int info, int warning, int error, int debug) {
            this.total = total;
            this.info = info;
            this.warning = warning;
            this.error = error;
            this.debug = debug;
        }

        public int getTotal() { return total; }
        public int getInfo() { return info; }
        public int getWarning() { return warning; }
        public int getError() { return error; }
        public int getDebug() { return debug; }

        @Override
        public String toString() {
            return String.format("Total: %d | INFO: %d | WARNING: %d | ERROR: %d | DEBUG: %d",
                    total, info, warning, error, debug);
        }
    }
}

