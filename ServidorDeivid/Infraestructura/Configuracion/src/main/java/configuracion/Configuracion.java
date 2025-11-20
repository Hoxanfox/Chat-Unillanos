package configuracion;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Configuracion {
    private static Configuracion instance;
    private final Properties props;

    private Configuracion() {
        props = new Properties();
        // Intentamos cargar el archivo desde la raíz del proyecto
        try (FileInputStream fis = new FileInputStream("configuracion.txt")) {
            props.load(fis);
        } catch (IOException e) {
            System.out.println("[Config] ADVERTENCIA: No se encontró 'configuracion.txt'. Usando valores por defecto (127.0.0.1:9000).");
        }
    }

    public static synchronized Configuracion getInstance() {
        if (instance == null) {
            instance = new Configuracion();
        }
        return instance;
    }

    public String getProperty(String key) {
        return props.getProperty(key);
    }

    // --- Getters Tipados ---

    public String getPeerHost() {
        return props.getProperty("peer.host", "127.0.0.1");
    }

    public int getPeerPuerto() {
        // Puerto local donde este nodo escuchará
        return Integer.parseInt(props.getProperty("peer.puerto", "9000"));
    }

    // Retorna null si no está configurado (indicando que somos el primer nodo o Génesis)
    public String getPeerInicialHost() {
        String host = props.getProperty("peer.inicial.host");
        if (host != null && host.trim().isEmpty()) return null;
        return host;
    }

    public int getPeerInicialPuerto() {
        String port = props.getProperty("peer.inicial.puerto");
        return port != null && !port.isEmpty() ? Integer.parseInt(port) : 0;
    }
}