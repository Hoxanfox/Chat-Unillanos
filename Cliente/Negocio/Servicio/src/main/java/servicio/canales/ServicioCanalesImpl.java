package servicio.canales;

import dominio.Canal;
import fachada.FachadaGeneralImpl;
import fachada.gestionCanales.IFachadaCanales;
import observador.IObservador;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación del servicio de canales.
 * Delega las operaciones a la fachada correspondiente.
 */
public class ServicioCanalesImpl implements IServicioCanales {

    private final IFachadaCanales fachadaCanales;

    public ServicioCanalesImpl() {
        this.fachadaCanales = FachadaGeneralImpl.getInstancia().getFachadaCanales();
        System.out.println("✅ [ServicioCanales]: Inicializado con FachadaCanales");
    }

    @Override
    public CompletableFuture<Canal> crearCanal(String nombre, String descripcion) {
        System.out.println("➡️ [ServicioCanales]: Creando canal: " + nombre);
        return fachadaCanales.crearCanal(nombre, descripcion);
    }

    @Override
    public void registrarObservadorCreacion(IObservador observador) {
        System.out.println("🔔 [ServicioCanales]: Registrando observador de creación");
        fachadaCanales.registrarObservadorCreacion(observador);
    }

    @Override
    public void solicitarCanalesUsuario() {
        System.out.println("➡️ [ServicioCanales]: Solicitando lista de canales");
        fachadaCanales.solicitarCanalesUsuario();
    }

    @Override
    public List<Canal> obtenerCanalesCache() {
        return fachadaCanales.obtenerCanalesCache();
    }

    @Override
    public void registrarObservadorListado(IObservador observador) {
        System.out.println("🔔 [ServicioCanales]: Registrando observador de listado");
        fachadaCanales.registrarObservadorListado(observador);
    }

    @Override
    public void solicitarHistorialCanal(String canalId, int limite) {
        System.out.println("➡️ [ServicioCanales]: Solicitando historial del canal: " + canalId);
        fachadaCanales.solicitarHistorialCanal(canalId, limite);
    }

    @Override
    public CompletableFuture<Void> enviarMensajeTexto(String canalId, String contenido) {
        System.out.println("➡️ [ServicioCanales]: Enviando mensaje de texto al canal: " + canalId);
        return fachadaCanales.enviarMensajeTexto(canalId, contenido);
    }

    @Override
    public CompletableFuture<Void> enviarMensajeAudio(String canalId, String audioFileId) {
        System.out.println("➡️ [ServicioCanales]: Enviando mensaje de audio al canal: " + canalId);
        return fachadaCanales.enviarMensajeAudio(canalId, audioFileId);
    }

    @Override
    public CompletableFuture<Void> enviarArchivo(String canalId, String fileId) {
        System.out.println("➡️ [ServicioCanales]: Enviando archivo al canal: " + canalId);
        return fachadaCanales.enviarArchivo(canalId, fileId);
    }

    @Override
    public void registrarObservadorMensajes(IObservador observador) {
        System.out.println("🔔 [ServicioCanales]: Registrando observador de mensajes");
        fachadaCanales.registrarObservadorMensajes(observador);
    }

    @Override
    public void inicializarManejadoresMensajes() {
        System.out.println("➡️ [ServicioCanales]: Inicializando manejadores de mensajes");
        fachadaCanales.inicializarManejadoresMensajes();
    }

    @Override
    public CompletableFuture<Void> invitarMiembro(String canalId, String contactoId) {
        System.out.println("➡️ [ServicioCanales]: Invitando miembro al canal: " + canalId);
        return fachadaCanales.invitarMiembro(canalId, contactoId);
    }

    @Override
    public void solicitarMiembrosCanal(String canalId) {
        System.out.println("➡️ [ServicioCanales]: Solicitando miembros del canal: " + canalId);
        fachadaCanales.solicitarMiembrosCanal(canalId);
    }

    @Override
    public void registrarObservadorMiembros(IObservador observador) {
        System.out.println("🔔 [ServicioCanales]: Registrando observador de miembros");
        fachadaCanales.registrarObservadorMiembros(observador);
    }
}
