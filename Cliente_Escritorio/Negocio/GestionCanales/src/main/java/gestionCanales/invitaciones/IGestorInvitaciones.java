package gestionCanales.invitaciones;

import observador.IObservador;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interfaz para el gestor de invitaciones a canales.
 * Maneja las operaciones de solicitar, aceptar y rechazar invitaciones.
 */
public interface IGestorInvitaciones {
    
    /**
     * Solicita la lista de invitaciones pendientes del usuario actual.
     * @return CompletableFuture con la lista de canales a los que el usuario está invitado
     */
    CompletableFuture<List<dto.canales.DTOCanalCreado>> solicitarInvitacionesPendientes();
    
    /**
     * Responde a una invitación de canal (aceptar o rechazar).
     * @param canalId ID del canal
     * @param aceptar true para aceptar, false para rechazar
     * @return CompletableFuture que se completa cuando la respuesta es procesada
     */
    CompletableFuture<Void> responderInvitacion(String canalId, boolean aceptar);
    
    /**
     * Registra un observador para recibir notificaciones sobre invitaciones.
     * @param observador El observador a registrar
     */
    void registrarObservador(IObservador observador);
    
    /**
     * Remueve un observador.
     * @param observador El observador a remover
     */
    void removerObservador(IObservador observador);
    
    /**
     * Notifica a todos los observadores registrados.
     * @param tipoDeDato Tipo de notificación
     * @param datos Datos asociados a la notificación
     */
    void notificarObservadores(String tipoDeDato, Object datos);
}

