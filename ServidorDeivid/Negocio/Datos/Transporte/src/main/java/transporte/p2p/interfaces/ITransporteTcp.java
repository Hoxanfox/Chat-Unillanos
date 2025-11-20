package transporte.p2p.interfaces;

public interface ITransporteTcp {
    // Inicia el modo servidor para escuchar conexiones entrantes
    void iniciarEscucha(int puerto) throws InterruptedException;

    // Conecta a otro nodo (actúa como cliente)
    void conectarA(String host, int puerto);

    // Envía un mensaje a todos o a una conexión específica (simplificado para el ejemplo)
    void enviarMensaje(String host, int puerto, String mensaje);

    // Detiene todo
    void detener();
}