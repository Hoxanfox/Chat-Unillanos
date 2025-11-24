-- Tabla para almacenar metadatos de archivos
-- Los archivos físicos se guardan en Bucket/ y aquí solo se guardan las referencias

CREATE TABLE IF NOT EXISTS archivos (
    id VARCHAR(36) PRIMARY KEY,
    file_id VARCHAR(255) NOT NULL UNIQUE,
    nombre_archivo VARCHAR(255) NOT NULL,
    ruta_relativa VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100),
    tamanio BIGINT NOT NULL,
    hash_sha256 VARCHAR(64),
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_file_id (file_id),
    INDEX idx_fecha_creacion (fecha_creacion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Comentarios descriptivos
ALTER TABLE archivos
COMMENT = 'Almacena metadatos de archivos. Los binarios están en Bucket/';

