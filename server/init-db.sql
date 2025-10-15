-- Script de inicialización de la base de datos Chat-Unillanos
-- MySQL 8.x

-- Otorgar permisos completos al usuario chatuser
GRANT ALL PRIVILEGES ON chat_unillanos.* TO 'chatuser'@'%';
FLUSH PRIVILEGES;

USE chat_unillanos;

-- Tabla: usuarios
CREATE TABLE IF NOT EXISTS usuarios (
    id VARCHAR(36) PRIMARY KEY,
    nombre_usuario VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    photo_id VARCHAR(255),
    ip_address VARCHAR(45),
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ultimo_acceso TIMESTAMP NULL,
    estado ENUM('ONLINE', 'OFFLINE', 'AWAY') DEFAULT 'OFFLINE',
    INDEX idx_email (email),
    INDEX idx_estado (estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla: canales
CREATE TABLE IF NOT EXISTS canales (
    id VARCHAR(36) PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT,
    creador_id VARCHAR(36) NOT NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    activo BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (creador_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    INDEX idx_nombre (nombre),
    INDEX idx_creador (creador_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla: canal_miembros (relación N:M entre usuarios y canales)
CREATE TABLE IF NOT EXISTS canal_miembros (
    canal_id VARCHAR(36) NOT NULL,
    usuario_id VARCHAR(36) NOT NULL,
    fecha_union TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    rol ENUM('ADMIN', 'MEMBER') DEFAULT 'MEMBER',
    PRIMARY KEY (canal_id, usuario_id),
    FOREIGN KEY (canal_id) REFERENCES canales(id) ON DELETE CASCADE,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    INDEX idx_canal (canal_id),
    INDEX idx_usuario (usuario_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla: mensajes (unificada para mensajes directos y de canal)
CREATE TABLE IF NOT EXISTS mensajes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    remitente_id VARCHAR(36) NOT NULL,
    destinatario_id VARCHAR(36),
    canal_id VARCHAR(36),
    tipo ENUM('DIRECT', 'CHANNEL') NOT NULL,
    contenido TEXT NOT NULL,
    file_id VARCHAR(255),
    fecha_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(20) DEFAULT 'ENVIADO',
    fecha_entrega TIMESTAMP NULL,
    fecha_lectura TIMESTAMP NULL,
    FOREIGN KEY (remitente_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (destinatario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (canal_id) REFERENCES canales(id) ON DELETE CASCADE,
    INDEX idx_remitente (remitente_id),
    INDEX idx_destinatario (destinatario_id),
    INDEX idx_canal (canal_id),
    INDEX idx_tipo (tipo),
    INDEX idx_fecha_envio (fecha_envio),
    INDEX idx_estado (estado),
    CHECK (
        (tipo = 'DIRECT' AND destinatario_id IS NOT NULL AND canal_id IS NULL) OR
        (tipo = 'CHANNEL' AND canal_id IS NOT NULL AND destinatario_id IS NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla: archivos
CREATE TABLE IF NOT EXISTS archivos (
    id VARCHAR(36) PRIMARY KEY,
    nombre_original VARCHAR(255) NOT NULL,
    nombre_almacenado VARCHAR(255) NOT NULL UNIQUE,
    hash_sha256 VARCHAR(64) NOT NULL UNIQUE,
    tamano_bytes BIGINT NOT NULL,
    tipo ENUM('IMAGE', 'VIDEO', 'AUDIO', 'DOCUMENT', 'OTHER') NOT NULL,
    usuario_id VARCHAR(36) NOT NULL,
    fecha_subida TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    INDEX idx_hash (hash_sha256),
    INDEX idx_usuario (usuario_id),
    INDEX idx_tipo (tipo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla: logs_sistema
CREATE TABLE IF NOT EXISTS logs_sistema (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tipo ENUM('LOGIN', 'LOGOUT', 'ERROR', 'INFO', 'SYSTEM') NOT NULL,
    usuario_id VARCHAR(36),
    ip_address VARCHAR(45),
    accion VARCHAR(100) NOT NULL,
    detalles TEXT,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL,
    INDEX idx_timestamp (timestamp),
    INDEX idx_tipo (tipo),
    INDEX idx_usuario (usuario_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla: chunk_sessions (para subida de archivos por trozos)
CREATE TABLE IF NOT EXISTS chunk_sessions (
    session_id VARCHAR(36) PRIMARY KEY,
    usuario_id VARCHAR(36) NOT NULL,
    nombre_archivo VARCHAR(255) NOT NULL,
    tipo_mime VARCHAR(100) NOT NULL,
    tamano_total BIGINT NOT NULL,
    total_chunks INT NOT NULL,
    chunks_recibidos TEXT,  -- JSON array [0,1,2,...]
    fecha_inicio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ultima_actividad TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    estado VARCHAR(20) DEFAULT 'ACTIVA',
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    INDEX idx_usuario (usuario_id),
    INDEX idx_estado (estado),
    INDEX idx_ultima_actividad (ultima_actividad)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Datos de prueba (opcional, para desarrollo)
-- Usuario de prueba: admin@unillanos.edu.co / Admin123!
INSERT INTO usuarios (id, nombre_usuario, email, password_hash, estado) 
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Admin Sistema',
    'admin@unillanos.edu.co',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYzQdpLx1uu',  -- BCrypt hash de "Admin123!"
    'OFFLINE'
);

-- Log de inicialización del sistema
INSERT INTO logs_sistema (tipo, accion, detalles)
VALUES ('SYSTEM', 'INIT_DB', 'Base de datos inicializada correctamente');
