package com.unillanos.server.repository.mappers;

import com.unillanos.server.entity.NotificacionEntity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Mapper para convertir ResultSet a NotificacionEntity.
 * Mapea los datos de la tabla 'notificaciones' a objetos Java.
 */
public class NotificacionMapper {
    
    /**
     * Mapea un ResultSet a NotificacionEntity.
     * 
     * @param rs ResultSet posicionado en la fila a mapear
     * @return NotificacionEntity con los datos del ResultSet
     * @throws SQLException si hay error al leer el ResultSet
     */
    public static NotificacionEntity mapToEntity(ResultSet rs) throws SQLException {
        NotificacionEntity notificacion = new NotificacionEntity();
        
        notificacion.setId(rs.getString("id"));
        notificacion.setUsuarioId(rs.getString("usuario_id"));
        notificacion.setTipo(rs.getString("tipo"));
        notificacion.setTitulo(rs.getString("titulo"));
        notificacion.setMensaje(rs.getString("mensaje"));
        notificacion.setRemitenteId(rs.getString("remitente_id"));
        notificacion.setCanalId(rs.getString("canal_id"));
        notificacion.setLeida(rs.getBoolean("leida"));
        
        // Mapeo de timestamp con manejo de NULL
        Timestamp timestamp = rs.getTimestamp("timestamp");
        notificacion.setTimestamp(timestamp != null ? timestamp.toLocalDateTime() : null);
        
        notificacion.setAccion(rs.getString("accion"));
        notificacion.setMetadata(rs.getString("metadata"));
        
        return notificacion;
    }
}
