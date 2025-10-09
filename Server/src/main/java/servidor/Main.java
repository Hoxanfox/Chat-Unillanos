package servidor;

/**
 * Clase principal para iniciar el servidor de Chat-Unillanos.
 */
public class Main {
    
    private static final int PUERTO_DEFECTO = 8888;
    
    public static void main(String[] args) {
        int puerto = PUERTO_DEFECTO;
        
        // Permitir especificar el puerto como argumento
        if (args.length > 0) {
            try {
                puerto = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Puerto inválido. Usando puerto por defecto: " + PUERTO_DEFECTO);
            }
        }
        
        ServidorNetty servidor = new ServidorNetty(puerto);
        
        try {
            servidor.iniciar();
        } catch (Exception e) {
            System.err.println("❌ Error al iniciar el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

