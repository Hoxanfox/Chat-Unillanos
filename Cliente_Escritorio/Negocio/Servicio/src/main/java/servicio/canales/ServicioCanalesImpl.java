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
 * Implementa IObservador para escuchar notificaciones de invitaciones aceptadas.
 */
public class ServicioCanalesImpl implements IServicioCanales, IObservador {

    private final IFachadaCanales fachadaCanales;

    public ServicioCanalesImpl() {
        this.fachadaCanales = FachadaGeneralImpl.getInstancia().getFachadaCanales();

        // ✅ SOLUCIÓN: Registrarse como observador de notificaciones para escuchar invitaciones aceptadas
        FachadaGeneralImpl.getInstancia().getFachadaNotificaciones().registrarObservador(this);

        System.out.println("✅ [ServicioCanales]: Inicializado con FachadaCanales y registrado como observador de notificaciones");
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

    @Override
    public CompletableFuture<List<dto.canales.DTOCanalCreado>> solicitarInvitacionesPendientes() {
        System.out.println("➡️ [ServicioCanales]: Solicitando invitaciones pendientes");
        return fachadaCanales.solicitarInvitacionesPendientes();
    }

    @Override
    public CompletableFuture<Void> responderInvitacion(String canalId, boolean aceptar) {
        System.out.println("➡️ [ServicioCanales]: Respondiendo invitación - Canal: " + canalId + ", Aceptar: " + aceptar);
        return fachadaCanales.responderInvitacion(canalId, aceptar);
    }

    @Override
    public void registrarObservadorInvitaciones(IObservador observador) {
        System.out.println("🔔 [ServicioCanales]: Registrando observador de invitaciones");
        fachadaCanales.registrarObservadorInvitaciones(observador);
    }

    @Override
    public CompletableFuture<Void> reproducirAudio(String fileId) {
        System.out.println("🎵 [ServicioCanales]: Reproduciendo audio - FileId: " + fileId);
        return fachadaCanales.reproducirAudio(fileId);
    }

    // 🆕 Método para establecer el canal activo
    @Override
    public void setCanalActivo(String canalId) {
        System.out.println("📍 [ServicioCanales]: Estableciendo canal activo: " + canalId);
        fachadaCanales.setCanalActivo(canalId);
    }

    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        System.out.println("🔔 [ServicioCanales]: Notificación recibida - Tipo: " + tipoDeDato);

        // ✅ Cuando se acepta una invitación, refrescar la lista de canales automáticamente
        if ("INVITACION_CANAL_ACEPTADA".equals(tipoDeDato) || "CANAL_UNIDO".equals(tipoDeDato)) {
            System.out.println("🔄 [ServicioCanales]: Invitación aceptada detectada, refrescando lista de canales...");
            solicitarCanalesUsuario();
        }
    }
}
