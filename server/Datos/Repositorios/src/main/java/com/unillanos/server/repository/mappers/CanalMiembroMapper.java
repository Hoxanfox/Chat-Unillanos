package com.unillanos.server.repository.mappers;

import com.unillanos.server.entity.CanalMiembroEntity;
import com.unillanos.server.entity.RolCanal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Mapper para convertir ResultSet a CanalMiembroEntity.
 */
public class CanalMiembroMapper {

    /**
     * Convierte una fila del ResultSet a un objeto CanalMiembroEntity.
     *
     * @param rs ResultSet posicionado en la fila a mapear
     * @return CanalMiembroEntity con los datos de la fila
     * @throws SQLException si hay un error al leer el ResultSet
     */
    public static CanalMiembroEntity mapRow(ResultSet rs) throws SQLException {
        CanalMiembroEntity miembro = new CanalMiembroEntity();
        
        miembro.setCanalId(rs.getString("canal_id"));
        miembro.setUsuarioId(rs.getString("usuario_id"));
        
        // Convertir Timestamp a LocalDateTime
        Timestamp fechaUnion = rs.getTimestamp("fecha_union");
        if (fechaUnion != null) {
            miembro.setFechaUnion(fechaUnion.toLocalDateTime());
        }
        
        // Convertir String a RolCanal enum
        String rolStr = rs.getString("rol");
        miembro.setRol(RolCanal.fromString(rolStr));
        
        return miembro;
    }
}

