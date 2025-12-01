package repositorio.comunicacion;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
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
            String host = System.getenv().getOrDefault("DB_HOST", "localhost");
            String port = System.getenv().getOrDefault("DB_PORT", "12000");
            String db = System.getenv().getOrDefault("DB_NAME", "chat_unillanos");
            String user = System.getenv().getOrDefault("DB_USER", "chatuser");
            String pass = System.getenv().getOrDefault("DB_PASS", "chatpass");
            int maxPool = Integer.parseInt(System.getenv().getOrDefault("DB_MAX_POOL", "10"));

            String jdbcUrl = String.format("jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8", host, port, db);

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(user);
            config.setPassword(pass);
            config.setMaximumPoolSize(maxPool);
            config.setMinimumIdle(1);
            config.setPoolName("HikariPool-Repositorio");
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            this.dataSource = new HikariDataSource(config);
            logger.info("Inicializado HikariCP para MySQL: {}", jdbcUrl);
        } catch (Exception e) {
            logger.error("Error inicializando MySQLManager", e);
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
