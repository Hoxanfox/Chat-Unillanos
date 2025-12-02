package transporte.p2p.interfaces;

public interface IMensajeListener {
    // Se invoca cuando llega data pura por TCP
    void onMensajeRecibido(String mensaje, String origen);

    // Se invoca cuando se establece una conexión nueva
    void onNuevaConexion(String origen);

    // NUEVO: Se invoca cuando se pierde/cierra una conexión
    void onDesconexion(String origen);
}