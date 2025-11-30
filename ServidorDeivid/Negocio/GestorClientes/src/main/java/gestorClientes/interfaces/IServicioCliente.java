package gestorClientes.interfaces;

import conexion.clientes.interfaces.IGestorConexionesCliente;
import conexion.clientes.interfaces.IRouterMensajesCliente;

public interface IServicioCliente {

    /**
     * Inicializa el servicio inyectándole las herramientas de red.
     * Aquí se registran las acciones (ej: "login", "enviar_mensaje").
     */
    void inicializar(IGestorConexionesCliente gestor, IRouterMensajesCliente router);

    void iniciar();

    void detener();

    String getNombre();
}