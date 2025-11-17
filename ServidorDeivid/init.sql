-- init.sql: esquema inicial para MySQL

DROP TABLE IF EXISTS canal_miembros;
DROP TABLE IF EXISTS mensajes;
DROP TABLE IF EXISTS canales;
DROP TABLE IF EXISTS peers;
DROP TABLE IF EXISTS usuarios;

CREATE TABLE usuarios (
  id CHAR(36) NOT NULL PRIMARY KEY,
  nombre VARCHAR(200) NOT NULL,
  email VARCHAR(200),
  foto VARCHAR(500),
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
  CONSTRAINT fk_mensajes_remitente FOREIGN KEY (remitente_id) REFERENCES usuarios(id) ON DELETE SET NULL,
  CONSTRAINT fk_mensajes_destinatario_usuario FOREIGN KEY (destinatario_usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL,
  CONSTRAINT fk_mensajes_canal FOREIGN KEY (canal_id) REFERENCES canales(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE canal_miembros (
  canal_id CHAR(36) NOT NULL,
  usuario_id CHAR(36) NOT NULL,
  PRIMARY KEY (canal_id, usuario_id),
  CONSTRAINT fk_miembros_canal FOREIGN KEY (canal_id) REFERENCES canales(id) ON DELETE CASCADE,
  CONSTRAINT fk_miembros_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Índices recomendados
CREATE INDEX idx_usuarios_peerpadre ON usuarios(peer_padre);
CREATE INDEX idx_canales_peerpadre ON canales(peer_padre);
CREATE INDEX idx_mensajes_canal ON mensajes(canal_id);
CREATE INDEX idx_mensajes_remitente ON mensajes(remitente_id);
