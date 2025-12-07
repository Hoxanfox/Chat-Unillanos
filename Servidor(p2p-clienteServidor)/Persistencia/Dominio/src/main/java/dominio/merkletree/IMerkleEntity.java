package dominio.merkletree;

/**
 * Contrato para cualquier objeto que quiera ser parte de un Árbol de Merkle.
 */
public interface IMerkleEntity {
    /**
     * Identificador único del registro (ej. UUID).
     */
    String getId();

    /**
     * Datos que se usarán para generar el hash.
     * Debe incluir todo lo que consideres "contenido importante".
     * Ej: return this.id + this.texto + this.timestamp;
     */
    String getDatosParaHash();
}