package com.unillanos.server.repository.mappers;

import com.unillanos.server.repository.models.ArchivoEntity;
import com.unillanos.server.repository.models.TipoArchivo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Clase utilitaria para mapear un ResultSet a un objeto ArchivoEntity.
 */
public class ArchivoMapper {

    private ArchivoMapper() {
        // Evitar instanciación
    }

    /**
     * Mapea una fila del ResultSet a un objeto ArchivoEntity.
     *
     * @param rs ResultSet con los datos del archivo
     * @return ArchivoEntity con los datos mapeados
     * @throws SQLException si hay error al leer los datos
     */
    public static ArchivoEntity mapRow(ResultSet rs) throws SQLException {
        ArchivoEntity archivo = new ArchivoEntity();
        
        archivo.setId(rs.getString("id"));
        archivo.setNombreOriginal(rs.getString("nombre_original"));
        archivo.setNombreAlmacenado(rs.getString("nombre_almacenado"));
        archivo.setTipoMime(rs.getString("tipo_mime"));
        
        // Convertir String a TipoArchivo enum
        String tipoStr = rs.getString("tipo_archivo");
        archivo.setTipoArchivo(TipoArchivo.fromString(tipoStr));
        
        archivo.setHashSha256(rs.getString("hash_sha256"));
        archivo.setTamanoBytes(rs.getLong("tamano_bytes"));
        archivo.setRutaAlmacenamiento(rs.getString("ruta_almacenamiento"));
        archivo.setUsuarioId(rs.getString("usuario_id"));
        
        // Convertir Timestamp a LocalDateTime
        Timestamp fechaSubida = rs.getTimestamp("fecha_subida");
        if (fechaSubida != null) {
            archivo.setFechaSubida(fechaSubida.toLocalDateTime());
        }
        
        return archivo;
    }
}

