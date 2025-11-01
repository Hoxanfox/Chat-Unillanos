package dto.vistaContactoChat;

/**
 * DTO para representar un mensaje según la API del servidor.
 * Incluye todos los campos enviados por el servidor.
 */
public class DTOMensaje {
    // Campos principales del mensaje
    private String mensajeId;           // UUID del mensaje (puede ser "id" en algunas respuestas)
    private Long id;                    // ID numérico (en algunas respuestas del servidor)
    private String peerRemitenteId;     // ← NUEVO: UUID del peer remitente WebRTC
    private String peerDestinoId;       // ← NUEVO: UUID del peer destino WebRTC
    private String remitenteId;         // UUID del remitente
    private String destinatarioId;      // UUID del destinatario
    private String remitenteNombre;     // Nombre del remitente
    private String destinatarioNombre;  // Nombre del destinatario

    // Contenido del mensaje
    private String contenido;           // Texto del mensaje
    private String tipo;                // "TEXTO", "IMAGEN", "AUDIO", "ARCHIVO", "VIDEO"

    // Archivos adjuntos
    private String fileId;              // UUID del archivo (null si es texto)
    private String fileName;            // Nombre del archivo (null si es texto)

    // Metadatos
    private String fechaEnvio;          // Timestamp ISO 8601
    private String estado;              // "ENVIADO", "ENTREGADO", "LEIDO"

    // Campo calculado (para compatibilidad con código existente)
    private boolean esMio;              // Para saber si alinear a la derecha o izquierda

    // Constructor vacío para Gson
    public DTOMensaje() {}

    // Constructor completo
    public DTOMensaje(String mensajeId, String remitenteId, String destinatarioId,
                      String remitenteNombre, String destinatarioNombre,
                      String contenido, String tipo, String fileId, String fileName,
                      String fechaEnvio, boolean esMio) {
        this.mensajeId = mensajeId;
        this.remitenteId = remitenteId;
        this.destinatarioId = destinatarioId;
        this.remitenteNombre = remitenteNombre;
        this.destinatarioNombre = destinatarioNombre;
        this.contenido = contenido;
        this.tipo = tipo;
        this.fileId = fileId;
        this.fileName = fileName;
        this.fechaEnvio = fechaEnvio;
        this.esMio = esMio;
    }

    // Getters y Setters
    public String getMensajeId() {
        return mensajeId != null ? mensajeId : (id != null ? String.valueOf(id) : null);
    }

    public void setMensajeId(String mensajeId) {
        this.mensajeId = mensajeId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPeerRemitenteId() {
        return peerRemitenteId;
    }

    public void setPeerRemitenteId(String peerRemitenteId) {
        this.peerRemitenteId = peerRemitenteId;
    }

    public String getPeerDestinoId() {
        return peerDestinoId;
    }

    public void setPeerDestinoId(String peerDestinoId) {
        this.peerDestinoId = peerDestinoId;
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

    public String getRemitenteNombre() {
        return remitenteNombre;
    }

    public void setRemitenteNombre(String remitenteNombre) {
        this.remitenteNombre = remitenteNombre;
    }

    public String getDestinatarioNombre() {
        return destinatarioNombre;
    }

    public void setDestinatarioNombre(String destinatarioNombre) {
        this.destinatarioNombre = destinatarioNombre;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(String fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public boolean esMio() {
        return esMio;
    }

    public void setEsMio(boolean esMio) {
        this.esMio = esMio;
    }

    // Métodos de utilidad

    /**
     * Retorna "Autor - HH:MM" para compatibilidad con código existente
     */
    public String getAutorConFecha() {
        String autor = remitenteNombre != null ? remitenteNombre : remitenteId;
        String hora = extraerHora(fechaEnvio);
        return autor + " - " + hora;
    }

    /**
     * Verifica si el mensaje tiene un archivo adjunto
     */
    public boolean tieneArchivo() {
        return fileId != null && !fileId.isEmpty();
    }

    /**
     * Retorna true si el mensaje es de tipo TEXTO
     */
    public boolean esTexto() {
        // Es texto SOLO si NO tiene fileId
        return (fileId == null || fileId.isEmpty());
    }

    /**
     * Retorna true si el mensaje es de tipo AUDIO
     */
    public boolean esAudio() {
        // 1. Detectar por tipo explícito
        if ("AUDIO".equalsIgnoreCase(tipo)) {
            return true;
        }

        // 2. Detectar por extensión de archivo (PRIORIDAD ALTA)
        if (fileName != null && !fileName.isEmpty()) {
            String lowerFileName = fileName.toLowerCase();
            if (lowerFileName.endsWith(".wav") ||
                lowerFileName.endsWith(".mp3") ||
                lowerFileName.endsWith(".m4a") ||
                lowerFileName.endsWith(".ogg") ||
                lowerFileName.contains("audio")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Retorna true si el mensaje es de tipo IMAGEN
     */
    public boolean esImagen() {
        // Es imagen si el tipo es IMAGEN
        if ("IMAGEN".equalsIgnoreCase(tipo)) {
            return true;
        }

        // También detectar por extensión de archivo
        if (fileId != null && !fileId.isEmpty() && fileName != null) {
            String lowerFileName = fileName.toLowerCase();
            return lowerFileName.endsWith(".jpg") ||
                   lowerFileName.endsWith(".jpeg") ||
                   lowerFileName.endsWith(".png") ||
                   lowerFileName.endsWith(".gif") ||
                   lowerFileName.endsWith(".bmp");
        }

        return false;
    }

    /**
     * Retorna true si el mensaje es de tipo ARCHIVO
     */
    public boolean esArchivo() {
        // Es archivo si el tipo es ARCHIVO
        if ("ARCHIVO".equalsIgnoreCase(tipo)) {
            return true;
        }

        // Es archivo si tiene fileId pero no es audio ni imagen
        if (fileId != null && !fileId.isEmpty() && !esAudio() && !esImagen()) {
            return true;
        }

        return false;
    }

    /**
     * Extrae la hora de un timestamp ISO 8601
     * Ej: "2025-10-17T10:35:00" -> "10:35"
     */
    private String extraerHora(String timestamp) {
        if (timestamp == null) return "00:00";
        try {
            // Formato: "2025-10-17T10:35:00"
            int tIndex = timestamp.indexOf('T');
            if (tIndex != -1) {
                String timePart = timestamp.substring(tIndex + 1);
                // Obtener HH:MM
                String[] parts = timePart.split(":");
                if (parts.length >= 2) {
                    return parts[0] + ":" + parts[1];
                }
            }
        } catch (Exception e) {
            return "00:00";
        }
        return "00:00";
    }

    @Override
    public String toString() {
        return "DTOMensaje{" +
                "mensajeId='" + getMensajeId() + '\'' +
                ", remitenteNombre='" + remitenteNombre + '\'' +
                ", contenido='" + contenido + '\'' +
                ", tipo='" + tipo + '\'' +
                ", fileId='" + fileId + '\'' +
                ", fechaEnvio='" + fechaEnvio + '\'' +
                ", esMio=" + esMio +
                '}';
    }
}
