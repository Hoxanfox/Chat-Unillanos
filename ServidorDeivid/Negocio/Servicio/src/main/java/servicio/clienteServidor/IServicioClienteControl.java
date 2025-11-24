package servicio.clienteServidor;

import gestorClientes.servicios.ServicioNotificacionCliente;

/**
 * Contrato de Alto Nivel para el subsistema Cliente-Servidor.
 * Define las operaciones disponibles para controlar el servidor que atiende a los usuarios.
 */
public interface IServicioClienteControl {

    /**
     * Inicia el servidor de sockets para aceptar conexiones de clientes (Apps/Web).
     * @param puerto Puerto de escucha (ej. 8000).
     */
    void iniciarServidor(int puerto);

    /**
     * Detiene el servidor, cierra el puerto y desconecta a los clientes activos.
     */
    void detenerServidor();

    /**
     * Verifica si el servidor de clientes está activo y escuchando.
     * @return true si está en ejecución.
     */
    boolean estaCorriendo();

    /**
     * Obtiene el servicio de notificaciones PUSH.
     * Necesario para que el Orquestador Global conecte el puente de eventos P2P -> Clientes.
     */
    ServicioNotificacionCliente getServicioNotificacion();
}