package dto.mensajeria;

/**
 * DTO para enviar mensajes con archivos adjuntos entre contactos.
 * Usado en la ruta: enviarmensajedirectoarchivo
 *
 * Este DTO contiene la información del archivo que se enviará como mensaje.
 */
public class DTOEnviarMensajeArchivo {
    private String remitenteId;
    private String destinatarioId;
    private String peerRemitenteId;
    private String peerDestinoId;
    private String fileName;
    private String mimeType;
    private long fileSize;
    private int totalChunks;
    private String descripcion; // Texto opcional que acompaña al archivo

    public DTOEnviarMensajeArchivo() {}

    public DTOEnviarMensajeArchivo(String remitenteId, String destinatarioId,
                                   String peerRemitenteId, String peerDestinoId,
                                   String fileName, String mimeType, long fileSize,
                                   int totalChunks, String descripcion) {
        this.remitenteId = remitenteId;
        this.destinatarioId = destinatarioId;
        this.peerRemitenteId = peerRemitenteId;
        this.peerDestinoId = peerDestinoId;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.totalChunks = totalChunks;
        this.descripcion = descripcion;
    }

    // Getters y Setters
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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}