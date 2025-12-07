-- =====================================================================
-- TABLAS PRINCIPALES (ENTIDADES)
-- =====================================================================

-- Tabla de Usuarios (Adaptada para H2)
CREATE TABLE usuarios (
                          id_usuario UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
                          nombre VARCHAR(255) NOT NULL,
                          email VARCHAR(255) NOT NULL UNIQUE,
                          estado VARCHAR(10) DEFAULT 'activo' CHECK (estado IN ('activo', 'inactivo', 'baneado')),
                          foto BLOB,
                          ip VARCHAR(45),
                          fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          photoIdServidor VARCHAR(255)
);

-- Tabla de Canales (Adaptada para H2)
CREATE TABLE canales (
                         id_canal UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
                         nombre VARCHAR(255) NOT NULL UNIQUE,
                         id_administrador UUID,
                         FOREIGN KEY (id_administrador) REFERENCES usuarios(id_usuario) ON DELETE SET NULL
);

-- Tabla de Contactos (Adaptada para H2)
CREATE TABLE contactos (
                           id_contacto UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
                           nombre VARCHAR(255) NOT NULL,
                           estado BOOLEAN DEFAULT TRUE
);

-- Tabla de Invitaciones (Adaptada para H2)
CREATE TABLE invitaciones (
                              id_invitacion UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
                              estado BOOLEAN DEFAULT FALSE,
                              fecha_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================================
-- TABLAS DE MENSAJES (Adaptadas para H2)
-- =====================================================================

CREATE TABLE mensaje_enviado_canal (
                                       id_mensaje_enviado_canal UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
                                       contenido BLOB,
                                       fecha_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                       tipo VARCHAR(50),
                                       id_remitente UUID NOT NULL,
                                       id_destinatario_canal UUID NOT NULL,
                                       FOREIGN KEY (id_remitente) REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
                                       FOREIGN KEY (id_destinatario_canal) REFERENCES canales(id_canal) ON DELETE CASCADE
);

CREATE TABLE mensaje_recibido_canal (
                                        id_mensaje UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
                                        contenido BLOB,
                                        fecha_envio TIMESTAMP,
                                        tipo VARCHAR(50),
                                        id_destinatario UUID NOT NULL,
                                        id_remitente_canal UUID NOT NULL,
                                        FOREIGN KEY (id_destinatario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
                                        FOREIGN KEY (id_remitente_canal) REFERENCES canales(id_canal) ON DELETE CASCADE
);

CREATE TABLE mensaje_enviado_contacto (
                                          id_mensaje_enviado_contacto UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
                                          contenido BLOB,
                                          fecha_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                          tipo VARCHAR(50),
                                          id_remitente UUID NOT NULL,
                                          id_destinatario_usuario UUID NOT NULL,
                                          FOREIGN KEY (id_remitente) REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
                                          FOREIGN KEY (id_destinatario_usuario) REFERENCES contactos(id_contacto) ON DELETE CASCADE
);

CREATE TABLE mensaje_recibido_contacto (
                                           id_mensaje UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
                                           contenido BLOB,
                                           fecha_envio TIMESTAMP,
                                           tipo VARCHAR(50),
                                           id_destinatario UUID NOT NULL,
                                           id_remitente_usuario UUID NOT NULL,
                                           FOREIGN KEY (id_destinatario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
                                           FOREIGN KEY (id_remitente_usuario) REFERENCES contactos(id_contacto) ON DELETE CASCADE
);

-- =====================================================================
-- TABLAS DE ENLACE (Adaptadas para H2)
-- =====================================================================

CREATE TABLE administrador (
                               id_administrador UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
                               id_usuario UUID NOT NULL,
                               id_canal UUID NOT NULL,
                               FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
                               FOREIGN KEY (id_canal) REFERENCES canales(id_canal) ON DELETE CASCADE,
                               UNIQUE (id_usuario, id_canal)
);

CREATE TABLE invitacion_usuario (
                                    id_invitacion_usuario UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
                                    id_usuario UUID NOT NULL,
                                    id_invitacion UUID NOT NULL,
                                    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
                                    FOREIGN KEY (id_invitacion) REFERENCES invitaciones(id_invitacion) ON DELETE CASCADE
);

CREATE TABLE canal_invitacion (
                                  id_canal_invitacion UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
                                  id_canal UUID NOT NULL,
                                  id_invitacion UUID NOT NULL,
                                  FOREIGN KEY (id_canal) REFERENCES canales(id_canal) ON DELETE CASCADE,
                                  FOREIGN KEY (id_invitacion) REFERENCES invitaciones(id_invitacion) ON DELETE CASCADE
);

CREATE TABLE canal_contacto (
                                id_canal_contacto UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
                                id_canal UUID NOT NULL,
                                id_contacto UUID NOT NULL,
                                FOREIGN KEY (id_canal) REFERENCES canales(id_canal) ON DELETE CASCADE,
                                FOREIGN KEY (id_contacto) REFERENCES contactos(id_contacto) ON DELETE CASCADE
);
-- Tabla para relacionar usuarios con canales (miembros del canal)
CREATE TABLE canal_usuario (
    id_canal_usuario UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
    id_canal UUID NOT NULL,
    id_usuario UUID NOT NULL,
    fecha_union TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_canal) REFERENCES canales(id_canal) ON DELETE CASCADE,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    UNIQUE (id_canal, id_usuario)
);

-- Agregar campo rol a la tabla canal_usuario
ALTER TABLE canal_usuario ADD COLUMN IF NOT EXISTS rol VARCHAR(50) DEFAULT 'miembro';


-- =====================================================================
-- TABLA DE ARCHIVOS (Para almacenar archivos localmente con Base64)
-- =====================================================================
CREATE TABLE archivos (
    id_archivo UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
    file_id_servidor VARCHAR(255) NOT NULL UNIQUE,
    nombre_archivo VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100),
    tamanio_bytes BIGINT,
    contenido_base64 CLOB,
    hash_sha256 VARCHAR(64),
    fecha_descarga TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_ultima_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    asociado_a VARCHAR(50),  -- 'perfil', 'mensaje', 'canal', etc.
    id_asociado UUID,  -- ID de la entidad asociada
    estado VARCHAR(20) DEFAULT 'completo' CHECK (estado IN ('descargando', 'completo', 'error'))
);
-- Índices para búsquedas rápidas
CREATE INDEX idx_archivos_file_id_servidor ON archivos(file_id_servidor);
CREATE INDEX idx_archivos_asociado ON archivos(asociado_a, id_asociado);

