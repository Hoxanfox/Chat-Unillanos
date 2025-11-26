package servicio.clienteServidor;

import dto.cliente.DTOSesionCliente;
import gestorClientes.servicios.ServicioNotificacionCliente;
import gestorP2P.servicios.ServicioSincronizacionDatos;
import observador.ISujeto;

import java.util.List;

/**
 * Contrato de Alto Nivel para el subsistema Cliente-Servidor.
 * Define las operaciones disponibles para controlar el servidor que atiende a los usuarios.
 */
public interface IServicioClienteControl extends ISujeto {

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

    /**
     * Inyecta el servicio de sincronización P2P en los servicios de mensajería.
     */
    void setServicioSincronizacionP2P(ServicioSincronizacionDatos servicioSyncP2P);

    /**
     * Obtiene el número de clientes conectados actualmente.
     */
    int getNumeroClientesConectados();

    /**
     * Obtiene la lista de sesiones activas (DTOs transversales).
     */
    List<DTOSesionCliente> getSesionesActivas();
}