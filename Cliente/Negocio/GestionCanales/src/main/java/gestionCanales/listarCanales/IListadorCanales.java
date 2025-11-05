package gestionCanales.listarCanales;

import dominio.Canal;
import observador.ISujeto;
import java.util.List;

/**
 * Contrato para el componente encargado de obtener y gestionar la lista de canales del usuario.
 * Actúa como un Sujeto para notificar a la UI sobre las actualizaciones.
 */
public interface IListadorCanales extends ISujeto {

    /**
     * Inicia el proceso para solicitar la lista de canales del usuario al servidor.
     * La actualización se propagará a través de una notificación a los observadores.
     */
    void solicitarCanalesUsuario();

    /**
     * Obtiene la lista de canales actualmente en caché.
     * @return Una lista de los canales.
     */
    List<Canal> getCanales();
}
