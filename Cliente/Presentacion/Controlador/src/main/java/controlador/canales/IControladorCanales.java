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
    void registrarObservadorMensajes(IObservador observador);
    void inicializarManejadoresMensajes();
    CompletableFuture<Void> invitarMiembro(String canalId, String contactoId);
    void solicitarMiembrosCanal(String canalId);
    void registrarObservadorMiembros(IObservador observador);
}
