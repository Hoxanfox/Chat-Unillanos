package com.unillanos.server.repository.mappers;

import com.unillanos.server.entity.EstadoUsuario;
import com.unillanos.server.entity.UsuarioEntity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Mapper para convertir ResultSet a UsuarioEntity.
 */
public class UsuarioMapper {

    /**
     * Convierte una fila del ResultSet a un objeto UsuarioEntity.
     *
     * @param rs ResultSet posicionado en la fila a mapear
     * @return UsuarioEntity con los datos de la fila
     * @throws SQLException si hay un error al leer el ResultSet
     */
    public static UsuarioEntity mapRow(ResultSet rs) throws SQLException {
        UsuarioEntity usuario = new UsuarioEntity();
        
        usuario.setId(rs.getString("id"));
        usuario.setNombre(rs.getString("nombre_usuario"));
        usuario.setEmail(rs.getString("email"));
        usuario.setPasswordHash(rs.getString("password_hash"));
        usuario.setPhotoId(rs.getString("photo_id"));
        usuario.setIpAddress(rs.getString("ip_address"));
        
        // Convertir Timestamp a LocalDateTime
        Timestamp fechaRegistro = rs.getTimestamp("fecha_registro");
        if (fechaRegistro != null) {
            usuario.setFechaRegistro(fechaRegistro.toLocalDateTime());
        }
        
        // Mapear ultimo_acceso
        Timestamp ultimoAcceso = rs.getTimestamp("ultimo_acceso");
        if (ultimoAcceso != null) {
            usuario.setUltimoAcceso(ultimoAcceso.toLocalDateTime());
        }

        // Convertir String a EstadoUsuario enum
        String estadoStr = rs.getString("estado");
        usuario.setEstado(EstadoUsuario.fromString(estadoStr));
        
        return usuario;
    }
}
