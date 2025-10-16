package com.unillanos.server.repository.mappers;

import com.unillanos.server.entity.EstadoMensaje;
import com.unillanos.server.entity.MensajeEntity;
import com.unillanos.server.entity.TipoMensaje;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Clase utilitaria para mapear un ResultSet a un objeto MensajeEntity.
 */
public class MensajeMapper {

    private MensajeMapper() {
        // Evitar instanciación
    }

    /**
     * Mapea una fila del ResultSet a un objeto MensajeEntity.
     *
     * @param rs ResultSet con los datos del mensaje
     * @return MensajeEntity con los datos mapeados
     * @throws SQLException si hay error al leer los datos
     */
    public static MensajeEntity mapRow(ResultSet rs) throws SQLException {
        MensajeEntity mensaje = new MensajeEntity();
        
        mensaje.setId(rs.getLong("id"));
        mensaje.setRemitenteId(rs.getString("remitente_id"));
        mensaje.setDestinatarioId(rs.getString("destinatario_id"));
        mensaje.setCanalId(rs.getString("canal_id"));
        
        // Convertir String a TipoMensaje enum
        String tipoStr = rs.getString("tipo");
        mensaje.setTipo(TipoMensaje.fromString(tipoStr));
        
        mensaje.setContenido(rs.getString("contenido"));
        mensaje.setFileId(rs.getString("file_id"));
        
        // Convertir Timestamp a LocalDateTime
        Timestamp fechaEnvio = rs.getTimestamp("fecha_envio");
        if (fechaEnvio != null) {
            mensaje.setFechaEnvio(fechaEnvio.toLocalDateTime());
        }
        
        // Mapear campos de estado
        String estadoStr = rs.getString("estado");
        if (estadoStr != null && !estadoStr.trim().isEmpty()) {
            mensaje.setEstado(EstadoMensaje.fromString(estadoStr));
        } else {
            mensaje.setEstado(EstadoMensaje.ENVIADO); // Estado por defecto
        }
        
        Timestamp fechaEntrega = rs.getTimestamp("fecha_entrega");
        if (fechaEntrega != null) {
            mensaje.setFechaEntrega(fechaEntrega.toLocalDateTime());
        }
        
        Timestamp fechaLectura = rs.getTimestamp("fecha_lectura");
        if (fechaLectura != null) {
            mensaje.setFechaLectura(fechaLectura.toLocalDateTime());
        }
        
        return mensaje;
    }
}

