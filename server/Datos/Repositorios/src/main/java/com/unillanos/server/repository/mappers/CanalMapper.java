package com.unillanos.server.repository.mappers;

import com.unillanos.server.repository.models.CanalEntity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Mapper para convertir ResultSet a CanalEntity.
 */
public class CanalMapper {

    /**
     * Convierte una fila del ResultSet a un objeto CanalEntity.
     *
     * @param rs ResultSet posicionado en la fila a mapear
     * @return CanalEntity con los datos de la fila
     * @throws SQLException si hay un error al leer el ResultSet
     */
    public static CanalEntity mapRow(ResultSet rs) throws SQLException {
        CanalEntity canal = new CanalEntity();
        
        canal.setId(rs.getString("id"));
        canal.setNombre(rs.getString("nombre"));
        canal.setDescripcion(rs.getString("descripcion"));
        canal.setCreadorId(rs.getString("creador_id"));
        
        // Convertir Timestamp a LocalDateTime
        Timestamp fechaCreacion = rs.getTimestamp("fecha_creacion");
        if (fechaCreacion != null) {
            canal.setFechaCreacion(fechaCreacion.toLocalDateTime());
        }
        
        canal.setActivo(rs.getBoolean("activo"));
        
        return canal;
    }
}

