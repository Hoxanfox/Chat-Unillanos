package com.arquitectura.DTO.p2p;

import java.util.UUID;

/**
 * DTO de request para enrutar un mensaje a través de la red P2P.
 * Permite que un peer envíe un mensaje a un usuario conectado en otro peer.
 */
public class RouteMessageRequestDto {
    
    private PeerOriginDto peerOrigen;
    private UUID destinatarioId;
    private MensajeDto mensaje;

    // Constructores
    public RouteMessageRequestDto() {
    }

    public RouteMessageRequestDto(PeerOriginDto peerOrigen, UUID destinatarioId, MensajeDto mensaje) {
        this.peerOrigen = peerOrigen;
        this.destinatarioId = destinatarioId;
        this.mensaje = mensaje;
    }

    // Getters y Setters
    public PeerOriginDto getPeerOrigen() {
        return peerOrigen;
    }

    public void setPeerOrigen(PeerOriginDto peerOrigen) {
        this.peerOrigen = peerOrigen;
    }

    public UUID getDestinatarioId() {
        return destinatarioId;
    }

    public void setDestinatarioId(UUID destinatarioId) {
        this.destinatarioId = destinatarioId;
    }

    public MensajeDto getMensaje() {
        return mensaje;
    }

    public void setMensaje(MensajeDto mensaje) {
        this.mensaje = mensaje;
    }

    /**
     * DTO anidado para información del peer origen
     */
    public static class PeerOriginDto {
        private UUID peerId;
        private String ip;
        private Integer puerto;

        public PeerOriginDto() {
        }

        public PeerOriginDto(UUID peerId, String ip, Integer puerto) {
            this.peerId = peerId;
            this.ip = ip;
            this.puerto = puerto;
        }

        public UUID getPeerId() {
            return peerId;
        }

        public void setPeerId(UUID peerId) {
            this.peerId = peerId;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public Integer getPuerto() {
            return puerto;
        }

        public void setPuerto(Integer puerto) {
            this.puerto = puerto;
        }
    }

    /**
     * DTO anidado para el mensaje a enrutar
     */
    public static class MensajeDto {
        private UUID remitenteId;
        private String remitenteUsername;
        private String tipo;
        private String contenido;
        private String fechaEnvio;

        public MensajeDto() {
        }

        public MensajeDto(UUID remitenteId, String remitenteUsername, String tipo, 
                         String contenido, String fechaEnvio) {
            this.remitenteId = remitenteId;
            this.remitenteUsername = remitenteUsername;
            this.tipo = tipo;
            this.contenido = contenido;
            this.fechaEnvio = fechaEnvio;
        }

        public UUID getRemitenteId() {
            return remitenteId;
        }

        public void setRemitenteId(UUID remitenteId) {
            this.remitenteId = remitenteId;
        }

        public String getRemitenteUsername() {
            return remitenteUsername;
        }

        public void setRemitenteUsername(String remitenteUsername) {
            this.remitenteUsername = remitenteUsername;
        }

        public String getTipo() {
            return tipo;
        }

        public void setTipo(String tipo) {
            this.tipo = tipo;
        }

        public String getContenido() {
            return contenido;
        }

        public void setContenido(String contenido) {
            this.contenido = contenido;
        }

        public String getFechaEnvio() {
            return fechaEnvio;
        }

        public void setFechaEnvio(String fechaEnvio) {
            this.fechaEnvio = fechaEnvio;
        }
    }

    @Override
    public String toString() {
        return "RouteMessageRequestDto{" +
                "peerOrigen=" + peerOrigen +
                ", destinatarioId=" + destinatarioId +
                ", mensaje=" + mensaje +
                '}';
    }
}
