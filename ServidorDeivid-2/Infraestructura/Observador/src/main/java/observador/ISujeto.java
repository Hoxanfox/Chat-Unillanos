package observador;

/**
 * Contrato para cualquier clase que quiera actuar como un "Sujeto" notificable.
 * Define los métodos para gestionar una lista de observadores y notificarles
 * de los cambios.
 */
public interface ISujeto {

    /**
     * Permite que un observador se suscriba para recibir notificaciones.
     * @param observador El objeto observador que desea ser notificado.
     */
    void registrarObservador(IObservador observador);

    /**
     * Permite que un observador se dé de baja de las notificaciones.
     * @param observador El objeto observador que ya no desea ser notificado.
     */
    void removerObservador(IObservador observador);

    /**
     * Notifica a todos los observadores suscritos que ha ocurrido un cambio.
     * @param tipoDeDato Un identificador para el tipo de actualización.
     * @param datos Los datos asociados a la actualización.
     */
    void notificarObservadores(String tipoDeDato, Object datos);
}
