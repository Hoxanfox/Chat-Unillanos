package repositorio.conexion;

import org.h2.jdbcx.JdbcConnectionPool; // 1. Importar el Pool de H2

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gestor de conexión a la base de datos H2 embebida.
 * Singleton para mantener un POOL de conexiones (solución al error "session closed").
 */
public class GestorConexionH2 {

    private static GestorConexionH2 instancia;

    // 2. Reemplazar la conexión única por un Pool
    // private Connection conexion; // <-- ESTO CAUSA EL ERROR
    private JdbcConnectionPool pool; // <-- ESTA ES LA SOLUCIÓN

    // Configuración de la base de datos H2
    private static final String DB_URL = "jdbc:h2:./data/chat_unillanos;AUTO_SERVER=TRUE";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    private GestorConexionH2() {
        inicializarBaseDatos();
    }

    public static synchronized GestorConexionH2 getInstancia() {
        if (instancia == null) {
            instancia = new GestorConexionH2();
        }
        return instancia;
    }

    /**
     * Inicializa el pool de conexiones y crea las tablas si no existen.
     */
    private void inicializarBaseDatos() {
        try {
            // Cargar el driver de H2
            Class.forName("org.h2.Driver");

            // 3. Establecer el POOL de conexiones
            pool = JdbcConnectionPool.create(DB_URL, DB_USER, DB_PASSWORD);
            pool.setMaxConnections(20); // Configura cuántas conexiones simultáneas permites

            System.out.println("✅ [GestorConexionH2]: Pool de conexiones H2 inicializado.");

            // Crear las tablas (usando una conexión del pool)
            crearTablas();

        } catch (ClassNotFoundException e) {
            System.err.println("❌ [GestorConexionH2]: Driver H2 no encontrado: " + e.getMessage());
            throw new RuntimeException("Error al cargar el driver de H2", e);
        } catch (SQLException e) {
            System.err.println("❌ [GestorConexionH2]: Error al crear tablas: " + e.getMessage());
            throw new RuntimeException("Error al inicializar la base de datos", e);
        }
    }

    /**
     * Crea todas las tablas necesarias según el esquema SQL.
     */
    private void crearTablas() throws SQLException {
        // 4. Pedir una conexión al pool SÓLO para este método
        // Se usa try-with-resources para que la conexión se devuelva al pool automáticamente
        try (Connection conn = pool.getConnection();
             Statement stmt = conn.createStatement()) {

            // Tabla Usuarios
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS usuarios (
                    id_usuario UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
                    nombre VARCHAR(255) NOT NULL,
                    email VARCHAR(255) NOT NULL UNIQUE,
                    estado VARCHAR(10) DEFAULT 'activo' CHECK (estado IN ('activo', 'inactivo', 'baneado')),
                    foto BLOB,
                    ip VARCHAR(45),
                    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    photoIdServidor VARCHAR(255)
                )
            """);

            // Tabla Canales
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS canales (
                    id_canal UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
                    nombre VARCHAR(255) NOT NULL UNIQUE,
                    id_administrador UUID,
                    FOREIGN KEY (id_administrador) REFERENCES usuarios(id_usuario) ON DELETE SET NULL
                )
            """);

            // Tabla Contactos
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS contactos (
                    id_contacto UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
                    nombre VARCHAR(255) NOT NULL,
                    email VARCHAR(255),
                    estado BOOLEAN DEFAULT TRUE,
                    photo_id VARCHAR(255),
                    peer_id VARCHAR(255),
                    fecha_registro VARCHAR(50)
                )
            """);

            // Tabla Invitaciones
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS invitaciones (
                    id_invitacion UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
                    estado BOOLEAN DEFAULT FALSE,
                    fecha_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Tabla Mensaje Enviado Canal
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mensaje_enviado_canal (
                    id_mensaje_enviado_canal UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
                    contenido BLOB,
                    fecha_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    tipo VARCHAR(50),
                    id_remitente UUID NOT NULL,
                    id_destinatario_canal UUID NOT NULL,
                    FOREIGN KEY (id_remitente) REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
                    FOREIGN KEY (id_destinatario_canal) REFERENCES canales(id_canal) ON DELETE CASCADE
                )
            """);

            // Tabla Mensaje Recibido Canal
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mensaje_recibido_canal (
                    id_mensaje UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
                    contenido BLOB,
                    fecha_envio TIMESTAMP,
                    tipo VARCHAR(50),
                    id_destinatario UUID NOT NULL,
                    id_remitente_canal UUID NOT NULL,
                    FOREIGN KEY (id_destinatario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
                    FOREIGN KEY (id_remitente_canal) REFERENCES canales(id_canal) ON DELETE CASCADE
                )
            """);

            // Tabla Mensaje Enviado Contacto
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mensaje_enviado_contacto (
                    id_mensaje_enviado_contacto UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
                    contenido BLOB,
                    fecha_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    tipo VARCHAR(50),
                    id_remitente UUID NOT NULL,
                    id_destinatario_usuario UUID NOT NULL,
                    FOREIGN KEY (id_remitente) REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
                    FOREIGN KEY (id_destinatario_usuario) REFERENCES contactos(id_contacto) ON DELETE CASCADE
                )
            """);

            // Tabla Mensaje Recibido Contacto
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mensaje_recibido_contacto (
                    id_mensaje UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
                    contenido BLOB,
                    fecha_envio TIMESTAMP,
                    tipo VARCHAR(50),
                    id_destinatario UUID NOT NULL,
                    id_remitente_usuario UUID NOT NULL,
                    FOREIGN KEY (id_destinatario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
                    FOREIGN KEY (id_remitente_usuario) REFERENCES contactos(id_contacto) ON DELETE CASCADE
                )
            """);

            // Tabla Administrador (enlace)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS administrador (
                    id_administrador UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
                    id_usuario UUID NOT NULL,
                    id_canal UUID NOT NULL,
                    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
                    FOREIGN KEY (id_canal) REFERENCES canales(id_canal) ON DELETE CASCADE,
                    UNIQUE (id_usuario, id_canal)
                )
            """);

            // Tabla Invitacion Usuario (enlace)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS invitacion_usuario (
                    id_invitacion_usuario UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
                    id_usuario UUID NOT NULL,
                    id_invitacion UUID NOT NULL,
                    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
                    FOREIGN KEY (id_invitacion) REFERENCES invitaciones(id_invitacion) ON DELETE CASCADE
                )
            """);

            // Tabla Canal Invitacion (enlace)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS canal_invitacion (
                    id_canal_invitacion UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
                    id_canal UUID NOT NULL,
                    id_invitacion UUID NOT NULL,
                    FOREIGN KEY (id_canal) REFERENCES canales(id_canal) ON DELETE CASCADE,
                    FOREIGN KEY (id_invitacion) REFERENCES invitaciones(id_invitacion) ON DELETE CASCADE
                )
            """);

            // Tabla Canal Contacto (enlace)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS canal_contacto (
                    id_canal_contacto UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
                    id_canal UUID NOT NULL,
                    id_contacto UUID NOT NULL,
                    FOREIGN KEY (id_canal) REFERENCES canales(id_canal) ON DELETE CASCADE,
                    FOREIGN KEY (id_contacto) REFERENCES contactos(id_contacto) ON DELETE CASCADE
                )
            """);

            // Tabla Canal Usuario (miembros del canal)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS canal_usuario (
                    id_canal_usuario UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
                    id_canal UUID NOT NULL,
                    id_usuario UUID NOT NULL,
                    fecha_union TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    rol VARCHAR(50) DEFAULT 'miembro',
                    FOREIGN KEY (id_canal) REFERENCES canales(id_canal) ON DELETE CASCADE,
                    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
                    UNIQUE (id_canal, id_usuario)
                )
            """);

            // Tabla de Archivos
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS archivos (
                    id_archivo UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
                    file_id_servidor VARCHAR(255) NOT NULL UNIQUE,
                    nombre_archivo VARCHAR(500) NOT NULL,
                    mime_type VARCHAR(100),
                    tamanio_bytes BIGINT,
                    contenido_base64 CLOB,
                    hash_sha256 VARCHAR(64),
                    fecha_descarga TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    fecha_ultima_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    asociado_a VARCHAR(50),
                    id_asociado UUID,
                    estado VARCHAR(20) DEFAULT 'completo' CHECK (estado IN ('descargando', 'completo', 'error'))
                )
            """);

            // Índices para búsquedas rápidas en archivos
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_archivos_file_id_servidor ON archivos(file_id_servidor)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_archivos_asociado ON archivos(asociado_a, id_asociado)");

            System.out.println("✅ [GestorConexionH2]: Todas las tablas verificadas/creadas correctamente.");
        }
        // 5. La conexión se devuelve al pool automáticamente aquí
    }

    /**
     * Obtiene una conexión activa del pool.
     * @throws SQLException si hay un error al obtener la conexión.
     */
    public Connection getConexion() throws SQLException {
        // 6. Simplemente pedimos una conexión al pool.
        // El pool se encarga de darnos una que sea válida.
        return pool.getConnection();
    }

    /**
     * Cierra el pool de conexiones (llamar al cerrar la aplicación).
     */
    public void cerrarConexion() {
        // 7. Cierra todo el pool
        if (pool != null) {
            pool.dispose();
            System.out.println("✅ [GestorConexionH2]: Pool de conexiones cerrado.");
        }
    }
}