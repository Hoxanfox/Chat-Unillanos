package gestorP2P.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import logger.LoggerCentral;

public class FileConfigReader implements IConfigReader {

    private final Map<String, String> values = new HashMap<>();

    public FileConfigReader() {
        this("configuracion.txt");
    }

    public FileConfigReader(String path) {
        try {
            LoggerCentral.debug("FileConfigReader: intentando cargar archivo de configuración desde: " + path);
            File f = new File(path);
            if (!f.exists()) {
                LoggerCentral.info("FileConfigReader: archivo de configuración no existe: " + path);
                return;
            }
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                int lineNo = 0;
                while ((line = br.readLine()) != null) {
                    lineNo++;
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        LoggerCentral.debug("FileConfigReader: saltando linea " + lineNo + " (vacía o comentario)");
                        continue;
                    }
                    int idx = line.indexOf('=');
                    if (idx <= 0) {
                        LoggerCentral.warn("FileConfigReader: linea " + lineNo + " ignorada (sin '=' válido): " + line);
                        continue;
                    }
                    String k = line.substring(0, idx).trim();
                    String v = line.substring(idx + 1).trim();
                    values.put(k, v);
                    LoggerCentral.debug("FileConfigReader: cargada clave='" + k + "' valor='" + v + "' (linea " + lineNo + ")");
                }
                LoggerCentral.info("FileConfigReader: carga finalizada. Entradas leídas=" + values.size());
            }
        } catch (Exception e) {
            LoggerCentral.error("FileConfigReader: error al leer archivo de configuración: " + e.getMessage(), e);
            // no bloquear si no hay config
        }
    }

    @Override
    public Optional<String> getString(String key) {
        String v = values.get(key);
        LoggerCentral.debug("FileConfigReader.getString(key='" + key + "') -> " + (v != null ? "'" + v + "'" : "<null>"));
        return Optional.ofNullable(v);
    }

    @Override
    public String getString(String key, String defaultValue) {
        String v = values.getOrDefault(key, defaultValue);
        LoggerCentral.debug("FileConfigReader.getString(key='" + key + "', default='" + defaultValue + "') -> '" + v + "'");
        return v;
    }

    @Override
    public int getInt(String key, int defaultValue) {
        String v = values.get(key);
        if (v == null || v.isEmpty()) {
            LoggerCentral.debug("FileConfigReader.getInt(key='" + key + "') -> usando default=" + defaultValue + " (no existe o vacío)");
            return defaultValue;
        }
        try {
            int val = Integer.parseInt(v);
            LoggerCentral.debug("FileConfigReader.getInt(key='" + key + "') -> parsed=" + val);
            return val;
        } catch (Exception e) {
            LoggerCentral.warn("FileConfigReader.getInt: no se pudo parsear entero para clave='" + key + "' valor='" + v + "', usando default=" + defaultValue);
            return defaultValue;
        }
    }
}
