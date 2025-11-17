package main;

/**
 * Punto de entrada principal del módulo Presentacion/Main.
 * Delegamos al lanzador de la interfaz gráfica si está disponible.
 */
public class Main {
    public static void main(String[] args) {
        // Intentar delegar al launcher de la interfaz gráfica mediante reflexión
        try {
            Class<?> launcher = Class.forName("interfazGrafica.Launcher");
            java.lang.reflect.Method m = launcher.getMethod("main", String[].class);
            m.invoke(null, (Object) args);
        } catch (ClassNotFoundException cnf) {
            System.err.println("Launcher de interfaz gráfica no encontrado en el classpath. Ejecuta el módulo InterfazGrafica o añade la dependencia: " + cnf.getMessage());
            // Aquí puedes arrancar una alternativa o salir
        } catch (Throwable t) {
            System.err.println("Error al invocar el launcher de la interfaz gráfica: " + t.getMessage());
            t.printStackTrace(System.err);
        }
    }
}
