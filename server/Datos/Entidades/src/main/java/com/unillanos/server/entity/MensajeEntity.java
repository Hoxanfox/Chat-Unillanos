package com.unillanos.server.entity;

import com.unillanos.server.dto.DTOMensaje;

import java.time.LocalDateTime;

/**
 * Entidad que representa un mensaje en la base de datos.
 */
public class MensajeEntity {
    
    private Long id;                    // ID autoincremental
    private String remitenteId;
    private String destinatarioId;      // null si es mensaje de canal
    private String canalId;             // null si es mensaje directo
    private TipoMensaje tipo;           // DIRECT o CHANNEL
    private String contenido;
    private String fileId;              // null si no tiene archivo (para Épica 5)
    private LocalDateTime fechaEnvio;
    private EstadoMensaje estado;       // ENVIADO, ENTREGADO, LEIDO
    private LocalDateTime fechaEntrega;
    private LocalDateTime fechaLectura;
    
    // Constructores
    public MensajeEntity() {
    }
    
    public MensajeEntity(Long id, String remitenteId, String destinatarioId, String canalId,
                         TipoMensaje tipo, String contenido, String fileId, LocalDateTime fechaEnvio) {
        this.id = id;
        this.remitenteId = remitenteId;
        this.destinatarioId = destinatarioId;
        this.canalId = canalId;
        this.tipo = tipo;
        this.contenido = contenido;
        this.fileId = fileId;
        this.fechaEnvio = fechaEnvio;
    }
    
    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRemitenteId() {
        return remitenteId;
    }

    public void setRemitenteId(String remitenteId) {
        this.remitenteId = remitenteId;
    }

    public String getDestinatarioId() {
        return destinatarioId;
    }

    public void setDestinatarioId(String destinatarioId) {
        this.destinatarioId = destinatarioId;
    }

    public String getCanalId() {
        return canalId;
    }

    public void setCanalId(String canalId) {
        this.canalId = canalId;
    }

    public TipoMensaje getTipo() {
        return tipo;
    }

    public void setTipo(TipoMensaje tipo) {
        this.tipo = tipo;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public EstadoMensaje getEstado() {
        return estado;
    }

    public void setEstado(EstadoMensaje estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(LocalDateTime fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public LocalDateTime getFechaLectura() {
        return fechaLectura;
    }

    public void setFechaLectura(LocalDateTime fechaLectura) {
        this.fechaLectura = fechaLectura;
    }
    
    /**
     * Convierte la entidad a DTO.
     * Requiere información adicional de Usuario y Canal para nombres.
     *
     * @param remitenteNombre Nombre del remitente
     * @param destinatarioNombre Nombre del destinatario (puede ser null)
     * @param canalNombre Nombre del canal (puede ser null)
     * @param fileName Nombre del archivo adjunto (puede ser null)
     * @return DTOMensaje con todos los datos del mensaje
     */
    public DTOMensaje toDTO(String remitenteNombre, String destinatarioNombre, 
                            String canalNombre, String fileName) {
        DTOMensaje dto = new DTOMensaje();
        dto.setId(this.id);
        dto.setRemitenteId(this.remitenteId);
        dto.setRemitenteNombre(remitenteNombre);
        dto.setDestinatarioId(this.destinatarioId);
        dto.setDestinatarioNombre(destinatarioNombre);
        dto.setCanalId(this.canalId);
        dto.setCanalNombre(canalNombre);
        dto.setTipo(this.tipo != null ? this.tipo.name() : TipoMensaje.DIRECT.name());
        dto.setContenido(this.contenido);
        dto.setFileId(this.fileId);
        dto.setFileName(fileName);
        dto.setFechaEnvio(this.fechaEnvio != null ? this.fechaEnvio.toString() : null);
        dto.setEstado(this.estado != null ? this.estado.toString() : EstadoMensaje.ENVIADO.toString());
        dto.setFechaEntrega(this.fechaEntrega != null ? this.fechaEntrega.toString() : null);
        dto.setFechaLectura(this.fechaLectura != null ? this.fechaLectura.toString() : null);
        return dto;
    }

    @Override
    public String toString() {
        return "MensajeEntity{" +
                "id=" + id +
                ", remitenteId='" + remitenteId + '\'' +
                ", destinatarioId='" + destinatarioId + '\'' +
                ", canalId='" + canalId + '\'' +
                ", tipo=" + tipo +
                ", contenido='" + (contenido != null && contenido.length() > 50 ? 
                    contenido.substring(0, 50) + "..." : contenido) + '\'' +
                ", fechaEnvio=" + fechaEnvio +
                ", estado=" + estado +
                ", fechaEntrega=" + fechaEntrega +
                ", fechaLectura=" + fechaLectura +
                '}';
    }
}
