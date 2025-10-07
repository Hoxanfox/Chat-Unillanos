package servicio.conexion;

/**
 * Implementación concreta del servicio de conexión.
 * Aquí vivirá la lógica real para conectarse al servidor.
 */
public class ServicioConexion implements IServicioConexion {

    /**
     * Simula un intento de conexión.
     * En una implementación real, aquí iría el código para crear sockets,
     * manejar timeouts, y verificar la respuesta del servidor.
     * @param ip La dirección IP del servidor.
     * @return true si la IP no está vacía (simulación de éxito).
     */
    @Override
    public boolean conectar(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            System.err.println("Error de conexión: La IP no puede estar vacía.");
            return false;
        }

        System.out.println("Intentando conectar al servidor en " + ip + "...");
        // Aquí iría la lógica de conexión real (ej. new Socket(ip, puerto))
        // Por ahora, simulamos que la conexión siempre es exitosa si la IP es válida.
        System.out.println("¡Conexión exitosa!");
        return true;
    }
}
