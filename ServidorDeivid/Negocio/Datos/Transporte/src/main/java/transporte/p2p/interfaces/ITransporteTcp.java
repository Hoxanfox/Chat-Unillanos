package transporte.p2p.interfaces;

public interface ITransporteTcp {
    void iniciarEscucha(int puerto) throws InterruptedException;
    void conectarA(String host, int puerto);
    void enviarMensaje(String host, int puerto, String mensaje);

    // NUEVO: Método para cerrar una conexión específica
    void desconectar(String host, int puerto);

    void detener();
}