package repositorio.comunicacion;

public class TestConexion {
    public static void main(String[] args) {
        try {
            MySQLManager mgr = MySQLManager.getInstance();
            try (java.sql.Connection conn = mgr.getConnection()) {
                System.out.println("Conexión establecida. URL: " + conn.getMetaData().getURL());
            }
            mgr.close();
        } catch (Exception e) {
            System.err.println("Fallo al conectar con la base de datos:");
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("Prueba finalizada.");
    }
}
