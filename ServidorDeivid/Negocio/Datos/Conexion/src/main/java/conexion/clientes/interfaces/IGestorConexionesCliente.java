package conexion.clientes.interfaces;

import dto.cliente.DTOSesionCliente;
import java.util.List;

public interface IGestorConexionesCliente {

    /**
     * Inicia el servidor socket para clientes (ej. puerto 8000).
     */
    void iniciarServidor(int puertoEscucha);

    /**
     * Envía un mensaje a una sesión específica (socket abierto).
     */
    void enviarMensaje(String idSesion, String mensaje);

    /**
     * Envía un mensaje a un usuario específico (busca su sesión activa).
     */
    void enviarMensajeAUsuario(String idUsuario, String mensaje);

    void broadcast(String mensaje);
    void desconectar(String idSesion);

    /**
     * Vincula una conexión anónima con un usuario de la BD.
     * Se llama tras un Login exitoso.
     */
    boolean registrarUsuarioEnSesion(String idSesion, String idUsuario);

    /**
     * Desvincula un usuario de su sesión (logout).
     */
    void desregistrarUsuarioEnSesion(String idSesion);

    /**
     * Obtiene el ID del usuario asociado a una sesión.
     * @param idSesion ID de la sesión
     * @return ID del usuario o null si no está autenticado
     */
    String obtenerUsuarioDeSesion(String idSesion);

    DTOSesionCliente obtenerSesion(String idSesion);
    List<DTOSesionCliente> obtenerClientesConectados();

    void apagar();
}