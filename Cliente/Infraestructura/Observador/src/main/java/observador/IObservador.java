package observador;

/**
 * Contrato para cualquier clase que quiera actuar como un "Observador".
 * Define el método que será llamado por el "Sujeto" cuando ocurra un evento.
 * Esta interfaz vive en una capa transversal (infraestructura) para que pueda
 * ser implementada por la Presentación y utilizada por el Negocio.
 */
public interface IObservador {

    /**
     * Método que se invoca cuando el Sujeto notifica un cambio.
     * @param tipoDeDato Un identificador para que el observador sepa qué tipo de datos está recibiendo.
     * @param datos Los datos asociados a la actualización.
     */
    void actualizar(String tipoDeDato, Object datos);
}
