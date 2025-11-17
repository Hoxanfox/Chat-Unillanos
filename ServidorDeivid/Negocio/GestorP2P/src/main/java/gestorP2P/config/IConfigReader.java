package gestorP2P.config;

import java.util.Optional;

public interface IConfigReader {
    Optional<String> getString(String key);
    String getString(String key, String defaultValue);
    int getInt(String key, int defaultValue);
}

