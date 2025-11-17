package gestorP2P.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FileConfigReader implements IConfigReader {

    private final Map<String, String> values = new HashMap<>();

    public FileConfigReader() {
        this("configuracion.txt");
    }

    public FileConfigReader(String path) {
        try {
            File f = new File(path);
            if (!f.exists()) return;
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int idx = line.indexOf('=');
                    if (idx <= 0) continue;
                    String k = line.substring(0, idx).trim();
                    String v = line.substring(idx + 1).trim();
                    values.put(k, v);
                }
            }
        } catch (Exception ignored) {
            // no bloquear si no hay config
        }
    }

    @Override
    public Optional<String> getString(String key) {
        return Optional.ofNullable(values.get(key));
    }

    @Override
    public String getString(String key, String defaultValue) {
        return values.getOrDefault(key, defaultValue);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        String v = values.get(key);
        if (v == null || v.isEmpty()) return defaultValue;
        try { return Integer.parseInt(v); } catch (Exception e) { return defaultValue; }
    }
}

