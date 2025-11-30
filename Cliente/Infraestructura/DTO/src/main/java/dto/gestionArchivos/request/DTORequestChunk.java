package dto.gestionArchivos.request;

/** Wrapper reorganizado: request para solicitar chunk */
public class DTORequestChunk {
    private final dto.gestionArchivos.DTORequestChunk original;

    public DTORequestChunk(dto.gestionArchivos.DTORequestChunk original) { this.original = original; }
    public dto.gestionArchivos.DTORequestChunk getOriginal() { return original; }
}
