package fachada.gestionCanales;

import dominio.Canal;
import observador.IObservador;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Contrato para la Fachada que gestiona todas las operaciones relacionadas con canales.
 * Expone métodos para crear, listar, gestionar miembros y mensajes de canales.
 */
public interface IFachadaCanales {

    // === CREACIÓN DE CANALES ===
    CompletableFuture<Canal> crearCanal(String nombre, String descripcion);
    void registrarObservadorCreacion(IObservador observador);

    // === LISTADO DE CANALES ===
    void solicitarCanalesUsuario();
    List<Canal> obtenerCanalesCache();
    void registrarObservadorListado(IObservador observador);

    // === MENSAJES DE CANAL ===
    void solicitarHistorialCanal(String canalId, int limite);
    CompletableFuture<Void> enviarMensajeTexto(String canalId, String contenido);
    CompletableFuture<Void> enviarMensajeAudio(String canalId, String audioFileId);
    CompletableFuture<Void> enviarArchivo(String canalId, String fileId);
    void registrarObservadorMensajes(IObservador observador);
    void inicializarManejadoresMensajes();

    // === INVITACIÓN DE MIEMBROS ===
    CompletableFuture<Void> invitarMiembro(String canalId, String contactoId);

    // === LISTAR MIEMBROS ===
    void solicitarMiembrosCanal(String canalId);
    void registrarObservadorMiembros(IObservador observador);
}
