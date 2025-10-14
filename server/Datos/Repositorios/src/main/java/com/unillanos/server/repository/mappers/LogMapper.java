package com.unillanos.server.repository.mappers;

import com.unillanos.server.repository.models.LogEntity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Mapper para convertir ResultSet a LogEntity.
 */
public class LogMapper {

    /**
     * Convierte una fila del ResultSet a un objeto LogEntity.
     *
     * @param rs ResultSet posicionado en la fila a mapear
     * @return LogEntity con los datos de la fila
     * @throws SQLException si hay un error al leer el ResultSet
     */
    public static LogEntity mapRow(ResultSet rs) throws SQLException {
        LogEntity log = new LogEntity();
        
        log.setId(rs.getLong("id"));
        
        Timestamp timestamp = rs.getTimestamp("timestamp");
        if (timestamp != null) {
            log.setTimestamp(timestamp.toLocalDateTime());
        }
        
        log.setTipo(rs.getString("tipo"));
        log.setUsuarioId(rs.getString("usuario_id"));
        log.setIpAddress(rs.getString("ip_address"));
        log.setAccion(rs.getString("accion"));
        log.setDetalles(rs.getString("detalles"));
        
        return log;
    }
}

