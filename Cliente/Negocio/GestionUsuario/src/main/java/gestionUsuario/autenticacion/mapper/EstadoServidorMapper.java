package gestionUsuario.autenticacion.mapper;

/**
 * Mappea estados del servidor al formato usado en la BD local.
 */
public class EstadoServidorMapper {

    private EstadoServidorMapper() {}

    public static String mapearEstadoServidor(String estadoServidor) {
        if (estadoServidor == null) {
            return "activo"; // Default
        }

        return switch (estadoServidor.toUpperCase()) {
            case "ONLINE", "ACTIVE", "ACTIVO" -> "activo";
            case "OFFLINE", "INACTIVE", "INACTIVO" -> "inactivo";
            case "BANNED", "BANEADO" -> "baneado";
            default -> {
                System.out.println("⚠️ [EstadoServidorMapper]: Estado desconocido del servidor: " + estadoServidor + ", usando 'activo' por defecto");
                yield "activo";
            }
        };
    }
}
