package gestionCanales.notificaciones;

import observador.ISujeto;

/**
 * Define el contrato para un componente encargado de gestionar
 * las notificaciones push del servidor relacionadas con los canales.
 * Ahora también actúa como un Sujeto en el patrón Observer.
 */
public interface IGestorNotificacionesCanal extends ISujeto {

    /**
     * Registra los manejadores permanentes en el GestorRespuesta para escuchar
     * las notificaciones del servidor como invitaciones y nuevos miembros.
     * Este método debe ser llamado una vez, por ejemplo, después de que el usuario inicie sesión.
     */
    void inicializarManejadores();

}

