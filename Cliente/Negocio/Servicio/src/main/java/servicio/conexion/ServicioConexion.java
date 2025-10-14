package servicio.conexion;

<<<<<<< HEAD
import fachada.gestionConexion.FachadaConexionImpl;
import fachada.gestionConexion.IFachadaConexion;

import java.util.concurrent.CompletableFuture;

/**
 * Implementación del servicio de conexión.
 * Delega la tarea a la Fachada.
 */
public class ServicioConexion implements IServicioConexion {

    private final IFachadaConexion fachadaConexion;

    public ServicioConexion() {
        this.fachadaConexion = new FachadaConexionImpl();
    }

    @Override
    public CompletableFuture<Boolean> conectar() {
        return fachadaConexion.conectar();
=======
import comunicacion.GestorRespuesta;
import comunicacion.IGestorRespuesta;
import conexion.GestorConexion;
import dto.gestionConexion.conexion.DTOSesion;
import dto.gestionConexion.transporte.DTOConexion;
import transporte.ITransporte;
import transporte.TransporteTCP;

/**
 * Implementación concreta del servicio de conexión.
 * Gestiona la conexión real al servidor usando TransporteTCP.
 */
public class ServicioConexion implements IServicioConexion {

    private static final int PUERTO_SERVIDOR = 8888;
    
    private final ITransporte transporte;
    private final GestorConexion gestorConexion;
    private final IGestorRespuesta gestorRespuesta;

    public ServicioConexion() {
        this.transporte = new TransporteTCP();
        this.gestorConexion = GestorConexion.getInstancia();
        this.gestorRespuesta = new GestorRespuesta();
    }

    /**
     * Establece una conexión real con el servidor.
     * @param ip La dirección IP del servidor.
     * @return true si la conexión fue exitosa, false en caso contrario.
     */
    @Override
    public boolean conectar(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            System.err.println("Error de conexión: La IP no puede estar vacía.");
            return false;
        }

        try {
            System.out.println("Intentando conectar al servidor en " + ip + ":" + PUERTO_SERVIDOR);
            
            // Crear el DTO con los datos de conexión
            DTOConexion datosConexion = new DTOConexion(ip, PUERTO_SERVIDOR);
            
            // Establecer la conexión usando TransporteTCP
            DTOSesion sesion = transporte.conectar(datosConexion);
            
            if (sesion == null || !sesion.estaActiva()) {
                System.err.println("Error: No se pudo establecer la conexión.");
                return false;
            }
            
            // Guardar la sesión en el gestor
            gestorConexion.setSesionActiva(sesion);
            
            // Iniciar el gestor de respuestas para escuchar mensajes del servidor
            gestorRespuesta.iniciarEscucha();
            
            System.out.println("¡Conexión exitosa con el servidor!");
            return true;
            
        } catch (Exception e) {
            System.err.println("Error al conectar con el servidor: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
>>>>>>> 3c366e1 (Cree un servidor sencillo para que ponga en prueba el cliente, tambien se creo scripts para automatizar el despliegue del server son los .sh)
    }
}

