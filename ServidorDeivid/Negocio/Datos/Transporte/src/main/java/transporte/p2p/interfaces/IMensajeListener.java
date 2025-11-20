package transporte.p2p.interfaces;

public interface IMensajeListener {
    // Se invoca cuando llega data pura por TCP
    void onMensajeRecibido(String mensaje, String origen);

    // Opcional: saber cuando alguien se conectó
    void onNuevaConexion(String origen);
}