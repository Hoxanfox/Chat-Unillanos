// java
// File: `dto/vistaContactoChat/DTOMensaje.java`
package dto.vistaContactoChat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DTOMensaje {
    private String mensajeId;
    private String fechaEnvio;
    private String remitenteId;
    private String remitenteNombre;
    private String destinatarioId;
    private String contenido;
    private String tipo;
    private String peerRemitenteId;
    private String peerDestinoId;
    private boolean esMio;

    // Nuevo: referencia al archivo/audio en el servidor
    private String fileId;
    private String fileName;  // Nombre del archivo (para archivos e imágenes)

    // Getters / setters (generar todos)
    public String getMensajeId() { return mensajeId; }
    public void setMensajeId(String mensajeId) { this.mensajeId = mensajeId; }
    public String getFechaEnvio() { return fechaEnvio; }
    public void setFechaEnvio(String fechaEnvio) { this.fechaEnvio = fechaEnvio; }
    public String getRemitenteId() { return remitenteId; }
    public void setRemitenteId(String remitenteId) { this.remitenteId = remitenteId; }
    public String getRemitenteNombre() { return remitenteNombre; }
    public void setRemitenteNombre(String remitenteNombre) { this.remitenteNombre = remitenteNombre; }
    public String getDestinatarioId() { return destinatarioId; }
    public void setDestinatarioId(String destinatarioId) { this.destinatarioId = destinatarioId; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getPeerRemitenteId() { return peerRemitenteId; }
    public void setPeerRemitenteId(String peerRemitenteId) { this.peerRemitenteId = peerRemitenteId; }
    public String getPeerDestinoId() { return peerDestinoId; }
    public void setPeerDestinoId(String peerDestinoId) { this.peerDestinoId = peerDestinoId; }
    public boolean isEsMio() { return esMio; }
    public void setEsMio(boolean esMio) { this.esMio = esMio; }
    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    // Métodos helper para compatibilidad con código existente
    public boolean esMio() { return esMio; }

    public boolean hasFileId() {
        return fileId != null && !fileId.isEmpty();
    }

    public boolean hasContenido() {
        return contenido != null && !contenido.isEmpty();
    }

    /**
     * Verifica si el mensaje es de tipo imagen.
     */
    public boolean esImagen() {
        return tipo != null && (tipo.equalsIgnoreCase("IMAGEN") || tipo.equalsIgnoreCase("IMAGE"));
    }

    /**
     * Verifica si el mensaje es de tipo archivo.
     */
    public boolean esArchivo() {
        return tipo != null && (tipo.equalsIgnoreCase("ARCHIVO") || tipo.equalsIgnoreCase("FILE"));
    }

    /**
     * Verifica si el mensaje es de tipo audio.
     */
    public boolean esAudio() {
        return tipo != null && tipo.equalsIgnoreCase("AUDIO");
    }

    /**
     * Verifica si el mensaje es de tipo texto.
     */
    public boolean esTexto() {
        return tipo != null && tipo.equalsIgnoreCase("TEXTO");
    }

    /**
     * Obtiene el nombre del autor junto con la fecha formateada.
     * Formato: "NombreAutor - HH:mm"
     */
    public String getAutorConFecha() {
        String autor = remitenteNombre != null ? remitenteNombre : "Desconocido";
        String fecha = formatearFecha();
        return autor + " - " + fecha;
    }

    /**
     * Formatea la fecha de envío en formato HH:mm.
     * Si la fecha no puede parsearse, retorna la fecha original.
     */
    private String formatearFecha() {
        if (fechaEnvio == null || fechaEnvio.isEmpty()) {
            return "";
        }

        try {
            // Intentar parsear ISO 8601 (ej: "2025-10-28T14:40:00Z")
            LocalDateTime dateTime = LocalDateTime.parse(fechaEnvio.replace("Z", ""),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            // Si falla, intentar otros formatos o retornar la fecha original
            try {
                // Intentar formato alternativo
                if (fechaEnvio.contains("T")) {
                    String timePart = fechaEnvio.split("T")[1];
                    if (timePart.contains(":")) {
                        String[] parts = timePart.split(":");
                        if (parts.length >= 2) {
                            return parts[0] + ":" + parts[1];
                        }
                    }
                }
            } catch (Exception ex) {
                // Ignorar
            }
            return fechaEnvio;
        }
    }

    @Override
    public String toString() {
        return "DTOMensaje{" +
                "mensajeId='" + mensajeId + '\'' +
                ", tipo='" + tipo + '\'' +
                ", esMio=" + esMio +
                ", remitenteNombre='" + remitenteNombre + '\'' +
                ", fileId='" + fileId + '\'' +
                ", fileName='" + fileName + '\'' +
                '}';
    }
}