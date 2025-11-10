package servicio.canales;

import dominio.Canal;
import observador.IObservador;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Contrato para el servicio de canales.
 * Es el punto de entrada desde el Controlador a la capa de Negocio para operaciones de canales.
 */
public interface IServicioCanales {

    // === CREACIÓN ===
    CompletableFuture<Canal> crearCanal(String nombre, String descripcion);
    void registrarObservadorCreacion(IObservador observador);

    // === LISTADO ===
    void solicitarCanalesUsuario();
    List<Canal> obtenerCanalesCache();
    void registrarObservadorListado(IObservador observador);

    // === MENSAJES ===
    void solicitarHistorialCanal(String canalId, int limite);
    CompletableFuture<Void> enviarMensajeTexto(String canalId, String contenido);
    CompletableFuture<Void> enviarMensajeAudio(String canalId, String audioFileId);
    CompletableFuture<Void> enviarArchivo(String canalId, String fileId);
    void registrarObservadorMensajes(IObservador observador);
    void inicializarManejadoresMensajes();

    // === MIEMBROS ===
    CompletableFuture<Void> invitarMiembro(String canalId, String contactoId);
    void solicitarMiembrosCanal(String canalId);
    void registrarObservadorMiembros(IObservador observador);

    // === INVITACIONES ===
    CompletableFuture<List<dto.canales.DTOCanalCreado>> solicitarInvitacionesPendientes();
    CompletableFuture<Void> responderInvitacion(String canalId, boolean aceptar);
    void registrarObservadorInvitaciones(IObservador observador);

    // === AUDIO ===
    CompletableFuture<Void> reproducirAudio(String fileId);
}
