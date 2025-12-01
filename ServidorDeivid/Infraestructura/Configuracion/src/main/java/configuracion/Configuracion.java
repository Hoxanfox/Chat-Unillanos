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

    // --- Configuración para Clientes ---

    public String getClienteHost() {
        return props.getProperty("cliente.host", "127.0.0.1");
    }

    public int getClientePuerto() {
        return Integer.parseInt(props.getProperty("cliente.puerto", "8000"));
    }

    // --- Configuración para Base de Datos MySQL ---

    /**
     * Obtiene el host de la base de datos MySQL.
     * @return Host de la base de datos (por defecto: localhost)
     */
    public String getDbHost() {
        return props.getProperty("db.host", "localhost");
    }

    /**
     * Obtiene el puerto de la base de datos MySQL.
     * @return Puerto de la base de datos (por defecto: 3306)
     */
    public int getDbPort() {
        return Integer.parseInt(props.getProperty("db.port", "3306"));
    }

    /**
     * Obtiene el nombre de la base de datos.
     * @return Nombre de la base de datos (por defecto: chat_unillanos)
     */
    public String getDbName() {
        return props.getProperty("db.name", "chat_unillanos");
    }

    /**
     * Obtiene el usuario de la base de datos.
     * @return Usuario de la base de datos (por defecto: chatuser)
     */
    public String getDbUser() {
        return props.getProperty("db.user", "chatuser");
    }

    /**
     * Obtiene la contraseña de la base de datos.
     * @return Contraseña de la base de datos (por defecto: chatpass)
     */
    public String getDbPass() {
        return props.getProperty("db.pass", "chatpass");
    }

    /**
     * Obtiene el tamaño máximo del pool de conexiones HikariCP.
     * @return Tamaño máximo del pool (por defecto: 10)
     */
    public int getDbMaxPool() {
        return Integer.parseInt(props.getProperty("db.max.pool", "10"));
    }
}