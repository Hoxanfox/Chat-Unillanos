package dto.gestionArchivos.response;

/** Wrapper reorganizado: response chunk de descarga */
public class DTODownloadChunk {
    private final dto.gestionArchivos.DTODownloadChunk original;

    public DTODownloadChunk(dto.gestionArchivos.DTODownloadChunk original) {
        this.original = original;
    }

    public dto.gestionArchivos.DTODownloadChunk getOriginal() { return original; }

    // Delegados
    public String getDownloadId() { return original.getDownloadId(); }
    public int getChunkNumber() { return original.getChunkNumber(); }
    public String getChunkData() { return original.getChunkData(); }
    public boolean isLast() { return original.isLast(); }
}
