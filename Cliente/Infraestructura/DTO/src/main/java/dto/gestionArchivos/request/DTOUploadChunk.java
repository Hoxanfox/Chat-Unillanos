package dto.gestionArchivos.request;

/** Wrapper reorganizado: request para upload chunk */
public class DTOUploadChunk {
    private final dto.gestionArchivos.DTOUploadChunk original;

    public DTOUploadChunk(dto.gestionArchivos.DTOUploadChunk original) { this.original = original; }
    public dto.gestionArchivos.DTOUploadChunk getOriginal() { return original; }
}
