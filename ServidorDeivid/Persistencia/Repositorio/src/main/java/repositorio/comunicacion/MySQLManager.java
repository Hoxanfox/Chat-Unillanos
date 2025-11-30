package repositorio.comunicacion;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import configuracion.Configuracion;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Driver;
import java.sql.DriverManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;

public class MySQLManager {
    private static final Logger logger = LoggerFactory.getLogger(MySQLManager.class);
    private static MySQLManager instance;
    private final HikariDataSource dataSource;

    private MySQLManager() {
        try {
            // ✅ NUEVO: Leer configuración desde configuracion.txt
            Configuracion config = Configuracion.getInstance();

            String host = config.getDbHost();
            int port = config.getDbPort();
            String db = config.getDbName();
            String user = config.getDbUser();
            String pass = config.getDbPass();
            int maxPool = config.getDbMaxPool();

            String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8",
                host, port, db);

            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(jdbcUrl);
            hikariConfig.setUsername(user);
            hikariConfig.setPassword(pass);
            hikariConfig.setMaximumPoolSize(maxPool);
            hikariConfig.setMinimumIdle(1);
            hikariConfig.setPoolName("HikariPool-Repositorio");
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            this.dataSource = new HikariDataSource(hikariConfig);
            logger.info("✅ HikariCP inicializado para MySQL: {}", jdbcUrl);
            logger.info("📊 Configuración DB: host={}, port={}, database={}, maxPool={}", host, port, db, maxPool);
        } catch (Exception e) {
            logger.error("❌ Error inicializando MySQLManager", e);
            throw new RuntimeException(e);
        }
    }

    public static synchronized MySQLManager getInstance() {
        if (instance == null) {
            instance = new MySQLManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("HikariCP cerrado");
        }
        try {
            AbandonedConnectionCleanupThread.checkedShutdown();
            logger.info("MySQL AbandonedConnectionCleanupThread shutdown requested");
        } catch (Throwable t) {
            logger.warn("No se pudo apagar AbandonedConnectionCleanupThread: {}", t.toString());
        }
        // Deregistrar drivers MySQL para evitar hilos residuales
        try {
            java.util.Enumeration<Driver> drivers = DriverManager.getDrivers();
            while (drivers.hasMoreElements()) {
                Driver driver = drivers.nextElement();
                String cname = driver.getClass().getName();
                if (cname != null && cname.startsWith("com.mysql")) {
                    try {
                        DriverManager.deregisterDriver(driver);
                        logger.info("Deregistered JDBC driver: {}", cname);
                    } catch (SQLException se) {
                        logger.warn("No se pudo desregistrar el driver {}: {}", cname, se.toString());
                    }
                }
            }
        } catch (Throwable t) {
            logger.warn("Error al desregistrar drivers JDBC: {}", t.toString());
        }
    }
}
