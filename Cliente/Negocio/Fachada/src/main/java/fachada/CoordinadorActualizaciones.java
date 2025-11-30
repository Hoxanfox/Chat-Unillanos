package fachada;

import gestionNotificaciones.GestorSincronizacionGlobal;
import observador.IObservador;

/**
 * Coordinador que conecta el GestorSincronizacionGlobal con las fachadas.
 * Se registra como observador del gestor y dispara las actualizaciones
 * correspondientes en cada fachada cuando recibe señales.
 */
public class CoordinadorActualizaciones implements IObservador {

    private static CoordinadorActualizaciones instancia;
    private final IFachadaGeneral fachadaGeneral;

    private CoordinadorActualizaciones(IFachadaGeneral fachadaGeneral) {
        this.fachadaGeneral = fachadaGeneral;
        System.out.println("✅ [CoordinadorActualizaciones]: Instancia creada");
    }

    /**
     * Obtiene la instancia única del coordinador (Singleton).
     * Requiere la instancia de FachadaGeneral.
     */
    public static synchronized CoordinadorActualizaciones getInstancia(IFachadaGeneral fachadaGeneral) {
        if (instancia == null) {
            instancia = new CoordinadorActualizaciones(fachadaGeneral);
        }
        return instancia;
    }

    /**
     * Inicializa el coordinador registrándose como observador del
     * GestorSincronizacionGlobal.
     */
    public void inicializar() {
        System.out.println("🔧 [CoordinadorActualizaciones]: Registrándose en GestorSincronizacionGlobal...");
        GestorSincronizacionGlobal.getInstancia().registrarObservador(this);
        System.out.println("✅ [CoordinadorActualizaciones]: Inicialización completa");
    }

    /**
     * Recibe notificaciones del GestorSincronizacionGlobal y dispara
     * las actualizaciones correspondientes en las fachadas.
     */
    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║ 📡 [CoordinadorActualizaciones]: SEÑAL RECIBIDA              ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println("    Tipo: " + tipoDeDato);
        System.out.println("    Timestamp: " + java.time.LocalDateTime.now());
        System.out.println("    Thread: " + Thread.currentThread().getName());
        System.out.println("    Datos: " + (datos != null ? datos.toString() : "null"));
        System.out.println("─".repeat(67));

        try {
            switch (tipoDeDato) {
                case "ACTUALIZAR_CONTACTOS":
                    System.out.println("    → Procesando: ACTUALIZAR_CONTACTOS");
                    actualizarContactos();
                    break;

                case "ACTUALIZAR_CANALES":
                    System.out.println("    → Procesando: ACTUALIZAR_CANALES");
                    actualizarCanales();
                    break;

                case "ACTUALIZAR_MENSAJES_PRIVADOS":
                    System.out.println("    → Procesando: ACTUALIZAR_MENSAJES_PRIVADOS");
                    actualizarMensajesPrivados();
                    break;

                case "ACTUALIZAR_MENSAJES_CANALES":
                    System.out.println("    → Procesando: ACTUALIZAR_MENSAJES_CANALES");
                    actualizarMensajesCanales();
                    break;

                case "ACTUALIZAR_NOTIFICACIONES":
                    System.out.println("    → Procesando: ACTUALIZAR_NOTIFICACIONES");
                    actualizarNotificaciones();
                    break;

                case "SINCRONIZACION_GLOBAL":
                    System.out.println("    → Procesando: SINCRONIZACION_GLOBAL");
                    System.out.println("    🔄 Sincronización global iniciada - actualizando TODO");
                    actualizarContactos();
                    actualizarCanales();
                    actualizarMensajesPrivados();
                    actualizarMensajesCanales();
                    actualizarNotificaciones();
                    System.out.println("    ✅ Sincronización global completada");
                    break;

                case "AUTENTICACION_EXITOSA":
                    System.out.println("    → Procesando: AUTENTICACION_EXITOSA");
                    manejarAutenticacionExitosa(datos);
                    break;

                default:
                    System.out.println("    ⚠️ Tipo de actualización no reconocido: " + tipoDeDato);
            }

            System.out.println("╚═══════════════════════════════════════════════════════════════╝\n");

        } catch (Exception e) {
            System.err.println("❌ [CoordinadorActualizaciones]: Error al procesar actualización: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Maneja el evento de autenticación exitosa.
     * Solicita automáticamente las invitaciones a canales pendientes y otras notificaciones.
     */
    private void manejarAutenticacionExitosa(Object datos) {
        System.out.println("\n    ┌─ AUTENTICACIÓN EXITOSA ───────────────────────────────┐");
        System.out.println("    │ 🎉 Usuario autenticado correctamente                  │");
        System.out.println("    │ Iniciando carga de datos iniciales...                │");
        System.out.println("    └────────────────────────────────────────────────────────┘");

        try {
            // 1. Solicitar lista de contactos
            System.out.println("    [1/4] 📇 Solicitando lista de contactos...");
            fachadaGeneral.getFachadaContactos().solicitarActualizacionContactos();

            // 2. Solicitar lista de canales
            System.out.println("    [2/4] 📢 Solicitando lista de canales...");
            fachadaGeneral.getFachadaCanales().solicitarCanalesUsuario();

            // 3. Solicitar invitaciones a canales pendientes
            System.out.println("    [3/4] 📨 Solicitando invitaciones a canales pendientes...");
            fachadaGeneral.getFachadaCanales().solicitarInvitacionesPendientes()
                .thenAccept(invitaciones -> {
                    System.out.println("    │ ✓ Invitaciones recibidas: " + invitaciones.size());
                    if (!invitaciones.isEmpty()) {
                        System.out.println("    │   → Tienes " + invitaciones.size() + " invitación(es) pendiente(s)");
                        for (int i = 0; i < invitaciones.size() && i < 3; i++) {
                            System.out.println("    │   → [" + (i+1) + "] " + invitaciones.get(i).getNombre());
                        }
                        if (invitaciones.size() > 3) {
                            System.out.println("    │   → ... y " + (invitaciones.size() - 3) + " más");
                        }
                    }
                })
                .exceptionally(e -> {
                    System.err.println("    │ ⚠️ Error al obtener invitaciones: " + e.getMessage());
                    return null;
                });

            // 4. Solicitar notificaciones generales
            System.out.println("    [4/4] 🔔 Solicitando notificaciones generales...");
            fachadaGeneral.getFachadaNotificaciones().obtenerNotificaciones();

            System.out.println("\n    ┌─────────────────────────────────────────────────────────┐");
            System.out.println("    │ ✅ Carga inicial de datos completada                   │");
            System.out.println("    └─────────────────────────────────────────────────────────┘");

        } catch (Exception e) {
            System.err.println("    ❌ Error en carga inicial de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Solicita actualización de la lista de contactos.
     */
    private void actualizarContactos() {
        try {
            System.out.println("    ┌─ ACTUALIZANDO CONTACTOS ──────────────────────────────┐");
            System.out.println("    │ Solicitando lista de contactos al servidor...         │");
            fachadaGeneral.getFachadaContactos().solicitarActualizacionContactos();
            System.out.println("    │ ✓ Petición enviada                                     │");
            System.out.println("    └────────────────────────────────────────────────────────┘");
        } catch (Exception e) {
            System.err.println("    ❌ Error al actualizar contactos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Solicita actualización de la lista de canales.
     */
    private void actualizarCanales() {
        try {
            System.out.println("    ┌─ ACTUALIZANDO CANALES ────────────────────────────────┐");
            System.out.println("    │ Solicitando lista de canales al servidor...           │");
            fachadaGeneral.getFachadaCanales().solicitarCanalesUsuario();
            System.out.println("    │ ✓ Petición enviada                                     │");
            System.out.println("    └────────────────────────────────────────────────────────┘");
        } catch (Exception e) {
            System.err.println("    ❌ Error al actualizar canales: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Solicita actualización de notificaciones.
     */
    private void actualizarNotificaciones() {
        try {
            System.out.println("   🔔 Solicitando actualización de notificaciones...");

            // Nota: obtenerNotificaciones() actualmente devuelve lista vacía
            // ya que el servidor no implementa esta acción
            fachadaGeneral.getFachadaNotificaciones().obtenerNotificaciones();

            // IMPORTANTE: Solicitar invitaciones a canales pendientes
            // Las invitaciones a canales son parte de las notificaciones del sistema
            actualizarInvitacionesCanales();

            System.out.println("   ✅ Actualización de notificaciones solicitada");
        } catch (Exception e) {
            System.err.println("   ❌ Error al actualizar notificaciones: " + e.getMessage());
        }
    }

    /**
     * Solicita actualización de invitaciones a canales pendientes.
     * Las invitaciones a canales son notificaciones importantes que el usuario debe ver.
     */
    private void actualizarInvitacionesCanales() {
        try {
            System.out.println("   📨 Solicitando invitaciones a canales pendientes...");
            fachadaGeneral.getFachadaCanales().solicitarInvitacionesPendientes();
            System.out.println("   ✅ Invitaciones a canales solicitadas");
        } catch (Exception e) {
            System.err.println("   ❌ Error al actualizar invitaciones a canales: " + e.getMessage());
        }
    }

    /**
     * Notifica a las vistas de chat para que actualicen sus mensajes.
     * Las vistas se encargan de solicitar su propio historial si están activas.
     */
    private void actualizarMensajesPrivados() {
        try {
            System.out.println("    ┌─ ACTUALIZANDO MENSAJES PRIVADOS ─────────────────────┐");
            System.out.println("    │ Enviando señal REFRESCAR_MENSAJES a vistas...        │");
            // Notificar a través de la fachada de contactos/chat
            // Las vistas activas recibirán la señal y actualizarán su historial
            fachadaGeneral.getFachadaContactos().notificarObservadores("REFRESCAR_MENSAJES", null);
            System.out.println("    │ ✓ Señal enviada a todas las vistas de chat activas   │");
            System.out.println("    └────────────────────────────────────────────────────────┘");
        } catch (Exception e) {
            System.err.println("    ❌ Error al actualizar mensajes privados: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Notifica a las vistas de canales para que actualicen sus mensajes.
     * Las vistas se encargan de solicitar su propio historial si están activas.
     */
    private void actualizarMensajesCanales() {
        try {
            System.out.println("    ┌─ ACTUALIZANDO MENSAJES DE CANALES ───────────────────┐");
            System.out.println("    │ Las vistas de canal recibirán actualización...       │");
            System.out.println("    │ ℹ️  Las vistas activas se actualizan automáticamente  │");
            System.out.println("    │ ✓ Proceso completado                                  │");
            System.out.println("    └────────────────────────────────────────────────────────┘");
        } catch (Exception e) {
            System.err.println("    ❌ Error al actualizar mensajes de canales: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
