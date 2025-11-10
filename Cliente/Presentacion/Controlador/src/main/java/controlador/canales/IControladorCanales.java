package controlador.canales;
import dto.canales.DTOCanalCreado;
import observador.IObservador;
import java.util.List;
import java.util.concurrent.CompletableFuture;
public interface IControladorCanales {
    CompletableFuture<DTOCanalCreado> crearCanal(String nombre, String descripcion);
    void registrarObservadorCreacion(IObservador observador);
    void solicitarCanalesUsuario();
    List<DTOCanalCreado> obtenerCanalesCache();
    void registrarObservadorListado(IObservador observador);
    void solicitarHistorialCanal(String canalId, int limite);
    CompletableFuture<Void> enviarMensajeTexto(String canalId, String contenido);
    CompletableFuture<Void> enviarMensajeAudio(String canalId, String audioFileId);
    CompletableFuture<Void> enviarArchivo(String canalId, String fileId);
    void registrarObservadorMensajes(IObservador observador);
    void inicializarManejadoresMensajes();
    CompletableFuture<Void> invitarMiembro(String canalId, String contactoId);
    void solicitarMiembrosCanal(String canalId);
    void registrarObservadorMiembros(IObservador observador);

    // Métodos para gestión de invitaciones
    CompletableFuture<List<DTOCanalCreado>> solicitarInvitacionesPendientes();
    CompletableFuture<Void> responderInvitacion(String canalId, boolean aceptar);
    void registrarObservadorInvitaciones(IObservador observador);

    // Método para reproducir audio
    CompletableFuture<Void> reproducirAudio(String fileId);
}
