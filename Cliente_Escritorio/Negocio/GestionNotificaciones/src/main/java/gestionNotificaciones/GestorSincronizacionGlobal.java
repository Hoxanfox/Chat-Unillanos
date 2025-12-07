package gestionNotificaciones;

import comunicacion.GestorRespuesta;
import comunicacion.IGestorRespuesta;
import dto.comunicacion.DTOResponse;
import observador.IObservador;
import observador.ISujeto;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestor centralizado que maneja la señal SIGNAL_UPDATE del servidor.
 * Cuando recibe esta señal, dispara la actualización de todos los componentes
 * de la interfaz (contactos, canales, mensajes, etc.).
 *
 * <p>Este gestor actúa como coordinador central para mantener la aplicación
 * sincronizada con el estado del servidor.</p>
 */
public class GestorSincronizacionGlobal implements ISujeto {

    private static GestorSincronizacionGlobal instancia;
    private final IGestorRespuesta gestorRespuesta;
    private final List<IObservador> observadores;
    private boolean inicializado = false;

    private GestorSincronizacionGlobal() {
        this.gestorRespuesta = GestorRespuesta.getInstancia();
        this.observadores = new ArrayList<>();
        System.out.println("✅ [GestorSincronizacionGlobal]: Instancia creada");
    }

    /**
     * Obtiene la instancia única del gestor (Singleton).
     */
    public static synchronized GestorSincronizacionGlobal getInstancia() {
        if (instancia == null) {
            instancia = new GestorSincronizacionGlobal();
        }
        return instancia;
    }

    /**
     * Inicializa el gestor registrando los manejadores necesarios.
     * Este método debe llamarse una sola vez durante el inicio de la aplicación.
     */
    public void inicializar() {
        if (inicializado) {
            System.out.println("⚠️ [GestorSincronizacionGlobal]: Ya está inicializado");
            return;
        }

        System.out.println("🔧 [GestorSincronizacionGlobal]: Inicializando y registrando manejadores...");

        // Registrar manejador para SIGNAL_UPDATE
        gestorRespuesta.registrarManejador("SIGNAL_UPDATE", this::manejarSignalUpdate);

        inicializado = true;
        System.out.println("✅ [GestorSincronizacionGlobal]: Inicialización completa");
    }

    /**
     * Maneja la señal SIGNAL_UPDATE enviada por el servidor.
     * Esta señal indica que hay cambios en el servidor y se debe refrescar todo.
     */
    private void manejarSignalUpdate(DTOResponse respuesta) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🔔 [GestorSincronizacionGlobal]: ⚡ SIGNAL_UPDATE RECIBIDA ⚡");
        System.out.println("=".repeat(80));
        System.out.println("📋 Timestamp: " + java.time.LocalDateTime.now());
        System.out.println("📋 Thread: " + Thread.currentThread().getName());

        try {
            // Extraer el recurso - ahora viene directamente en el campo 'resource'
            String recurso = respuesta.getResource();

            if (recurso == null || recurso.isEmpty()) {
                // Fallback: intentar extraer desde data
                recurso = extraerRecurso(respuesta);
            }

            System.out.println("📡 Recurso actualizado: [" + recurso + "]");
            System.out.println("📡 Status: " + respuesta.getStatus());
            System.out.println("📡 Message: " + respuesta.getMessage());

            // SIEMPRE disparar actualización global cuando llega SIGNAL_UPDATE
            // No importa el recurso específico, actualizamos todo
            System.out.println("\n💬 INICIANDO ACTUALIZACIÓN GLOBAL COMPLETA...");
            System.out.println("   ➤ Total de observadores registrados: " + observadores.size());

            long startTime = System.currentTimeMillis();
            dispararActualizacionGlobal();
            long endTime = System.currentTimeMillis();

            System.out.println("\n✅ SIGNAL_UPDATE procesada en " + (endTime - startTime) + "ms");
            System.out.println("=".repeat(80) + "\n");

        } catch (Exception e) {
            System.err.println("❌ [GestorSincronizacionGlobal]: Error al procesar SIGNAL_UPDATE: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Extrae el campo 'resource' de la respuesta (método de respaldo).
     */
    private String extraerRecurso(DTOResponse respuesta) {
        // Intentar obtener desde varios campos posibles
        if (respuesta.getData() != null) {
            if (respuesta.getData() instanceof String) {
                return (String) respuesta.getData();
            }
            if (respuesta.getData() instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) respuesta.getData();
                if (dataMap.containsKey("resource")) {
                    return String.valueOf(dataMap.get("resource"));
                }
            }
        }

        return "DESCONOCIDO";
    }

    /**
     * Dispara una actualización global de todos los componentes.
     * Notifica a todos los observadores registrados para que soliciten
     * datos frescos del servidor.
     */
    private void dispararActualizacionGlobal() {
        System.out.println("🔄 [GestorSincronizacionGlobal]: Iniciando actualización global de la aplicación");

        // Notificar a todos los observadores con diferentes tipos de eventos
        // para que cada uno actualice su parte correspondiente

        // 1. Actualizar lista de contactos
        System.out.println("   📇 Solicitando actualización de contactos...");
        notificarObservadores("ACTUALIZAR_CONTACTOS", null);

        // 2. Actualizar lista de canales
        System.out.println("   📢 Solicitando actualización de canales...");
        notificarObservadores("ACTUALIZAR_CANALES", null);

        // 3. Actualizar mensajes privados (si hay un chat abierto)
        System.out.println("   💬 Solicitando actualización de mensajes privados...");
        notificarObservadores("ACTUALIZAR_MENSAJES_PRIVADOS", null);

        // 4. Actualizar mensajes de canales (si hay un canal abierto)
        System.out.println("   📨 Solicitando actualización de mensajes de canales...");
        notificarObservadores("ACTUALIZAR_MENSAJES_CANALES", null);

        // 5. Actualizar notificaciones
        System.out.println("   🔔 Solicitando actualización de notificaciones...");
        notificarObservadores("ACTUALIZAR_NOTIFICACIONES", null);

        // Evento general para componentes que necesiten refrescarse
        notificarObservadores("SINCRONIZACION_GLOBAL", System.currentTimeMillis());

        System.out.println("✅ [GestorSincronizacionGlobal]: Actualización global completada");
    }

    // ==================== IMPLEMENTACIÓN DE ISujeto ====================

    @Override
    public void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
            System.out.println("[GestorSincronizacionGlobal] Observador registrado: " +
                observador.getClass().getSimpleName() + " (Total: " + observadores.size() + ")");
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
        System.out.println("[GestorSincronizacionGlobal] Observador removido: " +
            observador.getClass().getSimpleName());
    }

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        if (observadores.isEmpty()) {
            System.out.println("⚠️ [GestorSincronizacionGlobal] No hay observadores registrados para notificar");
            return;
        }

        System.out.println("      📢 Notificando a " + observadores.size() + " observador(es) - Tipo: " + tipoDeDato);

        int contador = 0;
        for (IObservador observador : observadores) {
            try {
                contador++;
                String nombreObservador = observador.getClass().getSimpleName();
                System.out.println("         [" + contador + "/" + observadores.size() + "] → " + nombreObservador);
                observador.actualizar(tipoDeDato, datos);
                System.out.println("         ✓ " + nombreObservador + " actualizado");
            } catch (Exception e) {
                System.err.println("         ❌ Error al notificar observador " +
                    observador.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Permite disparar manualmente una actualización global.
     * Útil para testing o para forzar una sincronización.
     */
    public void forzarActualizacion() {
        System.out.println("🔄 [GestorSincronizacionGlobal]: Actualización manual forzada");
        dispararActualizacionGlobal();
    }
}
