package servicio.dashboard;

import dto.dashboard.DTORecursosSistema;
import observador.IObservador;

/**
 * Interfaz del Servicio de Dashboard
 * Define el contrato para el control del dashboard del sistema
 */
public interface IServicioDashboardControl {

    /**
     * Inicia el servicio de monitoreo del dashboard
     */
    void iniciar();

    /**
     * Detiene el servicio de monitoreo
     */
    void detener();

    /**
     * Actualiza el número de usuarios activos en el sistema
     */
    void actualizarUsuariosActivos(int cantidad);

    /**
     * Actualiza el número de conexiones P2P activas
     */
    void actualizarConexionesP2P(int cantidad);

    /**
     * Actualiza el número de conexiones de clientes activas
     */
    void actualizarConexionesClientes(int cantidad);

    /**
     * Registra una actividad reciente en el sistema
     */
    void registrarActividad(String actividad);

    /**
     * Obtiene el número actual de usuarios activos
     */
    int obtenerUsuariosActivos();

    /**
     * Obtiene el número actual de conexiones P2P
     */
    int obtenerConexionesP2P();

    /**
     * Obtiene el número actual de conexiones de clientes
     */
    int obtenerConexionesClientes();

    /**
     * Obtiene los datos de recursos del sistema
     */
    DTORecursosSistema obtenerRecursos();

    /**
     * Registra un observador para recibir actualizaciones
     */
    void registrarObservador(IObservador observador);

    /**
     * Elimina un observador
     */
    void removerObservador(IObservador observador);

    /**
     * Verifica si el servicio está activo
     */
    boolean estaActivo();
}
