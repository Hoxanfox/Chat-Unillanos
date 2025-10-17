-- Migración: Agregar tipo 'WARN' a la tabla logs_sistema
-- Fecha: 2025-10-17
-- Descripción: Permite registrar advertencias de validación de negocio

USE chat_unillanos;

-- Modificar la columna tipo para incluir 'WARN'
ALTER TABLE logs_sistema
MODIFY COLUMN tipo ENUM('LOGIN', 'LOGOUT', 'ERROR', 'WARN', 'INFO', 'SYSTEM') NOT NULL;

-- Verificar el cambio
SHOW COLUMNS FROM logs_sistema WHERE Field = 'tipo';

