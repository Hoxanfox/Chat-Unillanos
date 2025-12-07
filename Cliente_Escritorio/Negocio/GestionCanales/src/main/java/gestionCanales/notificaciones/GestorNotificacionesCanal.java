package gestionCanales.notificaciones;

import comunicacion.GestorRespuesta;
import comunicacion.IGestorRespuesta;
import observador.IObservador;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementación del gestor de notificaciones de canales.
 * Se encarga de registrar y procesar las notificaciones push del servidor
 * y notificar a los observadores (UI) sobre los cambios.
 */
@SuppressWarnings("unused")
public class GestorNotificacionesCanal implements IGestorNotificacionesCanal {

    private final List<IObservador> observadores = new ArrayList<>();
    private final IGestorRespuesta gestorRespuesta;

    public GestorNotificacionesCanal() {
        this.gestorRespuesta = GestorRespuesta.getInstancia();
    }

    @Override
    public void inicializarManejadores() {
        registrarManejadorInvitacion();
        registrarManejadorNuevoMiembro();
        System.out.println("Manejadores de notificaciones de canal inicializados.");
    }

    private void registrarManejadorInvitacion() {
        gestorRespuesta.registrarManejador("notificacionInvitacionCanal", (respuesta) -> {
            try {
                System.out.println("Notificación de invitación recibida: " + respuesta.getMessage());
                Object rawData = respuesta.getData();

                // Intentar interpretar data como Map<String, String> de forma segura
                if (rawData instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataObj = (Map<String, Object>) rawData;

                    // Convertir sólo las entradas necesarias a String si existen
                    // Crear un mapa simple String->String para pasar a los observadores
                    java.util.Map<String, String> data = new java.util.HashMap<>();
                    for (Map.Entry<String, Object> e : dataObj.entrySet()) {
                        data.put(e.getKey(), e.getValue() != null ? e.getValue().toString() : null);
                    }

                    // Notificar a los observadores (la UI) que ha llegado una nueva invitación.
                    notificarObservadores("NUEVA_INVITACION_CANAL", data);
                } else {
                    // Si no es un Map, simplemente reenviar el objeto tal cual a los observadores
                    notificarObservadores("NUEVA_INVITACION_CANAL", rawData);
                }

            } catch (Exception e) {
                System.err.println("Error procesando la notificación de invitación: " + e.getMessage());
            }
        });
    }

    private void registrarManejadorNuevoMiembro() {
        gestorRespuesta.registrarManejador("nuevoMiembro", (respuesta) -> {
            try {
                System.out.println("Notificación de nuevo miembro recibida: " + respuesta.getMessage());
                Object rawData = respuesta.getData();

                if (rawData instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataObj = (Map<String, Object>) rawData;
                    String canalId = dataObj.get("canalId") != null ? dataObj.get("canalId").toString() : null;
                    String nuevoUsuarioId = dataObj.get("usuarioId") != null ? dataObj.get("usuarioId").toString() : null;
                    String nombreUsuario = dataObj.get("nombreUsuario") != null ? dataObj.get("nombreUsuario").toString() : null;

                    // El repositorio actual no expone un método para agregar miembros directamente.
                    // Notificamos a los observadores para que la UI gestione la incorporación localmente.

                    System.out.println("Procesando nuevo miembro para canal: " + canalId + ", usuario: " + nuevoUsuarioId);

                    java.util.Map<String, String> info = new java.util.HashMap<>();
                    info.put("canalId", canalId);
                    info.put("usuarioId", nuevoUsuarioId);
                    info.put("nombreUsuario", nombreUsuario);

                    // Notificar a los observadores (la UI) que la lista de miembros de un canal ha cambiado.
                    notificarObservadores("NUEVO_MIEMBRO_EN_CANAL", info);

                } else {
                    // Si el payload no es un mapa, reenviarlo tal cual
                    notificarObservadores("NUEVO_MIEMBRO_EN_CANAL", rawData);
                }

            } catch (Exception e) {
                System.err.println("Error procesando la notificación de nuevo miembro: " + e.getMessage());
            }
        });
    }

    // --- Métodos del Patrón Observador ---
    @Override
    public void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
    }

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        // Se crea una copia para evitar problemas de concurrencia si un observador se desregistra durante la notificación.
        for (IObservador observador : new ArrayList<>(observadores)) {
            observador.actualizar(tipoDeDato, datos);
        }
    }
}
