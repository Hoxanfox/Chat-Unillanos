package controlador.canales;

import dto.canales.DTOCanalCreado;
import observador.IObservador;
import servicio.canales.IServicioCanales;
import servicio.canales.ServicioCanalesImpl;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ControladorCanalesImpl implements IControladorCanales {
    private final IServicioCanales servicioCanales;

    public ControladorCanalesImpl() {
        this.servicioCanales = new ServicioCanalesImpl();
        System.out.println("✅ [ControladorCanales]: Inicializado");
    }

    @Override
    public CompletableFuture<DTOCanalCreado> crearCanal(String nombre, String descripcion) {
        return servicioCanales.crearCanal(nombre, descripcion)
            .thenApply(canal -> new DTOCanalCreado(canal.getIdCanal().toString(), canal.getNombre()));
    }

    @Override
    public void registrarObservadorCreacion(IObservador observador) {
        servicioCanales.registrarObservadorCreacion(observador);
    }

    @Override
    public void solicitarCanalesUsuario() {
        servicioCanales.solicitarCanalesUsuario();
    }

    @Override
    public List<DTOCanalCreado> obtenerCanalesCache() {
        return servicioCanales.obtenerCanalesCache().stream()
            .map(canal -> new DTOCanalCreado(canal.getIdCanal().toString(), canal.getNombre()))
            .collect(Collectors.toList());
    }

    @Override
    public void registrarObservadorListado(IObservador observador) {
        servicioCanales.registrarObservadorListado(observador);
    }

    @Override
    public void solicitarHistorialCanal(String canalId, int limite) {
        servicioCanales.solicitarHistorialCanal(canalId, limite);
    }

    @Override
    public CompletableFuture<Void> enviarMensajeTexto(String canalId, String contenido) {
        return servicioCanales.enviarMensajeTexto(canalId, contenido);
    }

    @Override
    public CompletableFuture<Void> enviarMensajeAudio(String canalId, String audioFileId) {
        System.out.println("🎤 [ControladorCanales]: Delegando envío de audio al servicio");
        return servicioCanales.enviarMensajeAudio(canalId, audioFileId);
    }

    @Override
    public CompletableFuture<Void> enviarArchivo(String canalId, String fileId) {
        System.out.println("📎 [ControladorCanales]: Delegando envío de archivo al servicio");
        return servicioCanales.enviarArchivo(canalId, fileId);
    }

    @Override
    public void registrarObservadorMensajes(IObservador observador) {
        servicioCanales.registrarObservadorMensajes(observador);
    }

    @Override
    public void inicializarManejadoresMensajes() {
        servicioCanales.inicializarManejadoresMensajes();
    }

    @Override
    public CompletableFuture<Void> invitarMiembro(String canalId, String contactoId) {
        return servicioCanales.invitarMiembro(canalId, contactoId);
    }

    @Override
    public void solicitarMiembrosCanal(String canalId) {
        System.out.println("🎮 [ControladorCanales]: Solicitando miembros del canal: " + canalId);
        servicioCanales.solicitarMiembrosCanal(canalId);
    }

    @Override
    public void registrarObservadorMiembros(IObservador observador) {
        System.out.println("🔔 [ControladorCanales]: Registrando observador de miembros");
        servicioCanales.registrarObservadorMiembros(observador);
    }

    @Override
    public CompletableFuture<List<DTOCanalCreado>> solicitarInvitacionesPendientes() {
        System.out.println("📨 [ControladorCanales]: Solicitando invitaciones pendientes");
        return servicioCanales.solicitarInvitacionesPendientes();
    }

    @Override
    public CompletableFuture<Void> responderInvitacion(String canalId, boolean aceptar) {
        System.out.println((aceptar ? "✓" : "✗") + " [ControladorCanales]: Respondiendo invitación - Aceptar: " + aceptar);
        return servicioCanales.responderInvitacion(canalId, aceptar);
    }

    @Override
    public void registrarObservadorInvitaciones(IObservador observador) {
        System.out.println("🔔 [ControladorCanales]: Registrando observador de invitaciones");
        servicioCanales.registrarObservadorInvitaciones(observador);
    }
}
