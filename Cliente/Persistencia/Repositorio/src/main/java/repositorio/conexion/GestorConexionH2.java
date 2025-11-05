package repositorio.conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gestor de conexión a la base de datos H2 embebida.
 * Singleton para mantener una única conexión durante la ejecución.
 */
public class GestorConexionH2 {

    private static GestorConexionH2 instancia;
    private Connection conexion;

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
     * Inicializa la conexión y crea las tablas si no existen.
     */
    private void inicializarBaseDatos() {
        try {
            // Cargar el driver de H2
            Class.forName("org.h2.Driver");

            // Establecer conexión
            conexion = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("✅ [GestorConexionH2]: Conexión establecida con la base de datos.");

            // Crear las tablas
            crearTablas();

        } catch (ClassNotFoundException e) {
            System.err.println("❌ [GestorConexionH2]: Driver H2 no encontrado: " + e.getMessage());
            throw new RuntimeException("Error al cargar el driver de H2", e);
        } catch (SQLException e) {
            System.err.println("❌ [GestorConexionH2]: Error al conectar con la base de datos: " + e.getMessage());
            throw new RuntimeException("Error al inicializar la base de datos", e);
        }
    }

    /**
     * Crea todas las tablas necesarias según el esquema SQL.
     */
    private void crearTablas() throws SQLException {
        try (Statement stmt = conexion.createStatement()) {

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
                    estado BOOLEAN DEFAULT TRUE
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
    }

    /**
     * Obtiene la conexión activa.
     */
    public Connection getConexion() {
        try {
            // Verificar si la conexión está cerrada y reconectar si es necesario
            if (conexion == null || conexion.isClosed()) {
                System.out.println("🔄 [GestorConexionH2]: Reconectando...");
                conexion = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            }
        } catch (SQLException e) {
            System.err.println("❌ [GestorConexionH2]: Error al verificar/reconectar: " + e.getMessage());
            throw new RuntimeException("Error con la conexión a la base de datos", e);
        }
        return conexion;
    }

    /**
     * Cierra la conexión con la base de datos.
     */
    public void cerrarConexion() {
        try {
            if (conexion != null && !conexion.isClosed()) {
                conexion.close();
                System.out.println("✅ [GestorConexionH2]: Conexión cerrada correctamente.");
            }
        } catch (SQLException e) {
            System.err.println("❌ [GestorConexionH2]: Error al cerrar conexión: " + e.getMessage());
        }
    }
}
