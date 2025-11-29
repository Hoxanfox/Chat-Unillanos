-- init.sql: esquema inicial para MySQL

DROP TABLE IF EXISTS transcripciones;
DROP TABLE IF EXISTS canal_invitaciones;
DROP TABLE IF EXISTS canal_miembros;
DROP TABLE IF EXISTS mensajes;
DROP TABLE IF EXISTS canales;
DROP TABLE IF EXISTS archivos;
DROP TABLE IF EXISTS peers;
DROP TABLE IF EXISTS usuarios;

CREATE TABLE usuarios (
                          id CHAR(36) NOT NULL PRIMARY KEY,
                          nombre VARCHAR(200) NOT NULL,
                          email VARCHAR(200),
                          foto VARCHAR(500) COMMENT 'FileId relativo desde Bucket/ (ej: user_photos/uuid_foto.jpg)',
                          peer_padre CHAR(36),
                          contrasena VARCHAR(200),
                          ip VARCHAR(100),
                          estado ENUM('OFFLINE','ONLINE') NOT NULL DEFAULT 'OFFLINE',
                          fecha_creacion DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE peers (
                       id CHAR(36) NOT NULL PRIMARY KEY,
                       ip VARCHAR(100),
                       socket_info VARCHAR(500),
                       estado ENUM('OFFLINE','ONLINE') NOT NULL DEFAULT 'OFFLINE',
                       fecha_creacion DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE archivos (
                          id CHAR(36) NOT NULL PRIMARY KEY,
                          file_id VARCHAR(255) NOT NULL UNIQUE COMMENT 'ID único del archivo (ej: user_photos/uuid_foto.jpg)',
                          nombre_archivo VARCHAR(255) NOT NULL,
                          ruta_relativa VARCHAR(500) NOT NULL COMMENT 'Ruta desde Bucket/',
                          mime_type VARCHAR(100),
                          tamanio BIGINT NOT NULL,
                          hash_sha256 VARCHAR(64),
                          fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          INDEX idx_file_id (file_id),
                          INDEX idx_fecha_creacion (fecha_creacion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Metadatos de archivos. Binarios en Bucket/';

CREATE TABLE canales (
                         id CHAR(36) NOT NULL PRIMARY KEY,
                         peer_padre CHAR(36),
                         creador_id CHAR(36) NOT NULL,
                         nombre VARCHAR(200),
                         tipo ENUM('PRIVADO','PUBLICO') NOT NULL DEFAULT 'PUBLICO',
                         fecha_creacion DATETIME(6) NOT NULL,
                         CONSTRAINT fk_canales_creador FOREIGN KEY (creador_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mensajes (
                          id CHAR(36) NOT NULL PRIMARY KEY,
                          remitente_id CHAR(36) DEFAULT NULL,
                          destinatario_usuario_id CHAR(36) DEFAULT NULL,
                          canal_id CHAR(36) DEFAULT NULL,
                          tipo ENUM('AUDIO','TEXTO') NOT NULL DEFAULT 'TEXTO',
                          contenido TEXT,
                          fecha_envio DATETIME(6) NOT NULL,
                          peer_remitente_id VARCHAR(255) NULL COMMENT 'ID del peer WebRTC del remitente',
                          peer_destino_id VARCHAR(255) NULL COMMENT 'ID del peer WebRTC del destinatario',
                          CONSTRAINT fk_mensajes_remitente FOREIGN KEY (remitente_id) REFERENCES usuarios(id) ON DELETE SET NULL,
                          CONSTRAINT fk_mensajes_destinatario_usuario FOREIGN KEY (destinatario_usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL,
                          CONSTRAINT fk_mensajes_canal FOREIGN KEY (canal_id) REFERENCES canales(id) ON DELETE CASCADE,
                          INDEX idx_peer_remitente (peer_remitente_id),
                          INDEX idx_peer_destino (peer_destino_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE canal_miembros (
                                canal_id CHAR(36) NOT NULL,
                                usuario_id CHAR(36) NOT NULL,
                                PRIMARY KEY (canal_id, usuario_id),
                                CONSTRAINT fk_miembros_canal FOREIGN KEY (canal_id) REFERENCES canales(id) ON DELETE CASCADE,
                                CONSTRAINT fk_miembros_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE canal_invitaciones (
                                    id CHAR(36) NOT NULL PRIMARY KEY,
                                    canal_id CHAR(36) NOT NULL,
                                    invitador_id CHAR(36) NOT NULL COMMENT 'Usuario que envía la invitación (admin del canal)',
                                    invitado_id CHAR(36) NOT NULL COMMENT 'Usuario que recibe la invitación',
                                    fecha_creacion DATETIME(6) NOT NULL,
                                    estado ENUM('PENDIENTE','ACEPTADA','RECHAZADA') NOT NULL DEFAULT 'PENDIENTE',
                                    CONSTRAINT fk_invitaciones_canal FOREIGN KEY (canal_id) REFERENCES canales(id) ON DELETE CASCADE,
                                    CONSTRAINT fk_invitaciones_invitador FOREIGN KEY (invitador_id) REFERENCES usuarios(id) ON DELETE CASCADE,
                                    CONSTRAINT fk_invitaciones_invitado FOREIGN KEY (invitado_id) REFERENCES usuarios(id) ON DELETE CASCADE,
                                    INDEX idx_invitaciones_invitado (invitado_id),
                                    INDEX idx_invitaciones_estado (estado),
                                    INDEX idx_invitaciones_canal (canal_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Invitaciones a canales';

-- ✅ TABLA TRANSCRIPCIONES: Movida después de mensajes para resolver dependencias
CREATE TABLE transcripciones (
                                 id CHAR(36) NOT NULL PRIMARY KEY,
                                 archivo_id CHAR(36) NOT NULL COMMENT 'Referencia al archivo de audio',
                                 mensaje_id CHAR(36) DEFAULT NULL COMMENT 'Referencia al mensaje si aplica',
                                 transcripcion TEXT COMMENT 'Texto transcrito del audio',
                                 estado ENUM('PENDIENTE','PROCESANDO','COMPLETADA','ERROR') NOT NULL DEFAULT 'PENDIENTE',
                                 duracion_segundos DECIMAL(10,2) COMMENT 'Duración del audio en segundos',
                                 idioma VARCHAR(10) DEFAULT 'es' COMMENT 'Idioma detectado/configurado',
                                 confianza DECIMAL(5,2) COMMENT 'Nivel de confianza de la transcripción (0-100)',
                                 fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                 fecha_procesamiento TIMESTAMP NULL COMMENT 'Fecha cuando se procesó',
                                 fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 CONSTRAINT fk_transcripciones_archivo FOREIGN KEY (archivo_id) REFERENCES archivos(id) ON DELETE CASCADE,
                                 CONSTRAINT fk_transcripciones_mensaje FOREIGN KEY (mensaje_id) REFERENCES mensajes(id) ON DELETE SET NULL,
                                 INDEX idx_transcripciones_archivo (archivo_id),
                                 INDEX idx_transcripciones_mensaje (mensaje_id),
                                 INDEX idx_transcripciones_estado (estado),
                                 INDEX idx_transcripciones_fecha (fecha_creacion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Transcripciones de archivos de audio';

-- Índices recomendados
CREATE INDEX idx_usuarios_peerpadre ON usuarios(peer_padre);
CREATE INDEX idx_canales_peerpadre ON canales(peer_padre);
CREATE INDEX idx_mensajes_canal ON mensajes(canal_id);
CREATE INDEX idx_mensajes_remitente ON mensajes(remitente_id);
