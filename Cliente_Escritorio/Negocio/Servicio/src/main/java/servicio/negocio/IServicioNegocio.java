package servicio.negocio;

/**
 * Contrato para un servicio de propósito general que gestiona
 * el ciclo de vida de la capa de negocio.
 */
public interface IServicioNegocio {

    /**
     * Inicializa todos los componentes de la capa de negocio
     * llamando a la Fachada General.
     */
    void inicializar();
}
