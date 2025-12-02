package gestorP2P.servicios.sincronizacion;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import conexion.p2p.interfaces.IGestorConexiones;
import dto.comunicacion.DTORequest;
import gestorP2P.servicios.ServicioNotificacionCambios;
import gestorP2P.servicios.ServicioTransferenciaArchivos;
import logger.LoggerCentral;
import observador.IObservador;
import repositorio.p2p.PeerRepositorio;

import java.util.List;

/**
 * Coordinador de Sincronización
 *
 * Orquesta las 6 fases de sincronización de manera secuencial:
 * 1. Construcción de Árboles Merkle
 * 2. Comparación de Hashes
 * 3. Solicitud de IDs
 * 4. Detección de Faltantes
 * 5. Comparación de Contenido
 * 6. Transferencia de Archivos
 */
public class CoordinadorSincronizacion {

    private static final String TAG = "CoordinadorSync";
    private static final String VERDE = "\u001B[32m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String ROJO = "\u001B[31m";
    private static final String AZUL = "\u001B[34m";
    private static final String CYAN = "\u001B[36m";
    private static final String RESET = "\u001B[0m";

    private static final int MAX_REINTENTOS = 3;
    private static final long INTERVALO_MIN_MS = 2000;

    // Fases
    private final Fase1ConstruccionArboles fase1;
    private final Fase2ComparacionHashes fase2;
    private final Fase3SolicitudIDs fase3;
    private final Fase4DeteccionFaltantes fase4;
    private final Fase5ComparacionContenido fase5;
    private final Fase6TransferenciaArchivos fase6;

    // Dependencias
    private final IGestorConexiones gestor;
    private final Gson gson;
    private final PeerRepositorio repoPeer;

    // ✅ NUEVO: Referencia al servicio padre para notificar a TODOS los observadores
    private observador.ISujeto servicioPadre;

    // Control de estado
    private volatile boolean sincronizacionEnProgreso = false;
    private volatile int contadorReintentos = 0;
    private volatile long ultimaSincronizacion = 0;
    private volatile boolean huboCambiosEnEsteCiclo = false;

    // Servicios externos
    private ServicioNotificacionCambios notificador;
    private IObservador servicioNotificacionCliente;

    public CoordinadorSincronizacion(IGestorConexiones gestor, Gson gson) {
        this.gestor = gestor;
        this.gson = gson;
        this.repoPeer = new PeerRepositorio();

        // Inicializar fases
        this.fase1 = new Fase1ConstruccionArboles();
        this.fase2 = new Fase2ComparacionHashes(fase1);
        this.fase3 = new Fase3SolicitudIDs(fase1, gestor, gson);
        this.fase4 = new Fase4DeteccionFaltantes(fase3, gestor, gson);
        this.fase5 = new Fase5ComparacionContenido(gestor, gson);
        this.fase6 = new Fase6TransferenciaArchivos();

        LoggerCentral.info(TAG, VERDE + "✓ Coordinador de sincronización inicializado" + RESET);
    }

    /**
     * ✅ NUEVO: Configura el servicio padre (ServicioSincronizacionDatos) para poder
     * notificar a TODOS sus observadores, no solo al servicioNotificacionCliente.
     */
    public void setServicioPadre(observador.ISujeto servicioPadre) {
        this.servicioPadre = servicioPadre;
        // ✅ NUEVO: Configurar también en Fase2 para notificaciones por tipo
        this.fase2.setServicioPadre(servicioPadre);
        LoggerCentral.info(TAG, VERDE + "✓ Servicio padre configurado para notificaciones globales" + RESET);
    }

    /**
     * Configura los servicios de notificación.
     */
    public void configurarNotificaciones(ServicioNotificacionCambios notificador,
            IObservador servicioNotificacionCliente) {
        this.notificador = notificador;
        this.servicioNotificacionCliente = servicioNotificacionCliente;
        LoggerCentral.info(TAG, VERDE + "✓ Servicios de notificación configurados" + RESET);
    }

    /**
     * Configura el servicio de transferencia de archivos.
     */
    public void configurarTransferenciaArchivos(ServicioTransferenciaArchivos servicio) {
        fase6.setServicioTransferencia(servicio);
    }

    /**
     * Reconstruye todos los árboles Merkle.
     */
    public void reconstruirArboles() {
        fase1.reconstruirTodosLosArboles();
    }

    /**
     * Inicia el proceso de sincronización general.
     */
    public void iniciarSincronizacion() {
        // Validaciones previas
        if (sincronizacionEnProgreso) {
            LoggerCentral.warn(TAG, AMARILLO + "⚠ Sincronización ya en progreso" + RESET);
            return;
        }

        long tiempoActual = System.currentTimeMillis();
        long tiempoTranscurrido = tiempoActual - ultimaSincronizacion;
        if (tiempoTranscurrido < INTERVALO_MIN_MS) {
            LoggerCentral.warn(TAG, AMARILLO + "⚠ Esperando intervalo mínimo: " +
                    (INTERVALO_MIN_MS - tiempoTranscurrido) + "ms" + RESET);
            return;
        }

        if (contadorReintentos >= MAX_REINTENTOS) {
            LoggerCentral.error(TAG, ROJO + "⚠ Límite de reintentos alcanzado (" + MAX_REINTENTOS + ")" + RESET);
            contadorReintentos = 0;
            sincronizacionEnProgreso = false;
            return;
        }

        List<PeerRepositorio.PeerInfo> peersOnline = repoPeer.listarPeersOnline();
        if (peersOnline.isEmpty()) {
            LoggerCentral.warn(TAG, AMARILLO + "⚠ No hay peers online" + RESET);
            sincronizacionEnProgreso = false;
            return;
        }

        // Iniciar sincronización
        sincronizacionEnProgreso = true;
        contadorReintentos++;
        ultimaSincronizacion = tiempoActual;

        LoggerCentral.info(TAG, AZUL + "=== INICIANDO SINCRONIZACIÓN ===" + RESET);
        LoggerCentral.info(TAG, String.format("Peers online: %d | Intento: %d/%d",
                peersOnline.size(), contadorReintentos, MAX_REINTENTOS));

        new Thread(this::ejecutarSincronizacion).start();
    }

    /**
     * Ejecuta el proceso de sincronización.
     */
    private void ejecutarSincronizacion() {
        try {
            // Thread.sleep(500); // REMOVED: Artificial delay removed for performance

            // Fase 1: Reconstruir árboles
            LoggerCentral.info(TAG, CYAN + "▶ FASE 1: Construyendo árboles Merkle" + RESET);
            fase1.reconstruirTodosLosArboles();

            // Enviar solicitud de comparación de hashes
            DTORequest req = new DTORequest("sync_check_all", null);
            String jsonReq = gson.toJson(req);

            LoggerCentral.info(TAG, VERDE + "📤 Enviando sync_check_all" + RESET);
            gestor.broadcast(jsonReq);

            // Liberar lock rápidamente
            liberarLockConRetraso(100);

        } catch (Exception e) {
            LoggerCentral.error(TAG, ROJO + "Error en sincronización: " + e.getMessage() + RESET);
            sincronizacionEnProgreso = false;
        }
    }

    /**
     * Procesa las diferencias detectadas tras comparar hashes (Fase 2).
     */
    public void procesarDiferencias(JsonObject hashesRemotos) {
        LoggerCentral.info(TAG, CYAN + "▶ FASE 2: Comparando hashes" + RESET);

        Fase2ComparacionHashes.ResultadoComparacion resultado = fase2.compararHashes(hashesRemotos);

        if (resultado.todoSincronizado()) {
            LoggerCentral.info(TAG, VERDE + "✔ SISTEMA TOTALMENTE SINCRONIZADO" + RESET);
            finalizarSincronizacionExitosa();
            return;
        }

        // Hay diferencias, procesar el primer tipo
        String primerTipo = resultado.getPrimerTipoConDiferencia();
        LoggerCentral.info(TAG, CYAN + "▶ FASE 3: Solicitando IDs para " + primerTipo + RESET);
        fase3.solicitarIDs(primerTipo);
    }

    /**
     * Procesa los IDs recibidos (Fase 4).
     */
    public void procesarIDsRecibidos(String tipo, JsonArray idsRemotos) {
        LoggerCentral.info(TAG, CYAN + "▶ FASE 4: Detectando entidades faltantes de " + tipo + RESET);

        Fase4DeteccionFaltantes.ResultadoDeteccion resultado = fase4.detectarYSolicitarFaltantes(tipo, idsRemotos);

        if (resultado.hayFaltantes()) {
            LoggerCentral.info(TAG, AMARILLO + String.format("⬇ Solicitadas %d entidades faltantes",
                    resultado.getCantidadFaltantes()) + RESET);
            // ✅ NUEVO: Marcar que hubo cambios porque hay entidades faltantes
            huboCambiosEnEsteCiclo = true;
            // Las entidades se solicitaron en fase4, esperamos que lleguen
        } else if (resultado.todosLosIDsCoinciden()) {
            // Tenemos todos los IDs pero los hashes difieren - comparar contenido
            LoggerCentral.info(TAG, CYAN + "▶ FASE 5: Comparando contenido de " + tipo + RESET);
            // ✅ NUEVO: Marcar que hubo actividad de sincronización (diferencias detectadas)
            huboCambiosEnEsteCiclo = true;
            fase5.iniciarComparaciones(tipo, idsRemotos);
        } else {
            // No hay diferencias reales, continuar
            iniciarSincronizacion();
        }
    }

    /**
     * Procesa una entidad recibida y guarda cambios.
     */
    public void procesarEntidadRecibida(String tipo, JsonElement data) {
        guardarEntidad(tipo, data);
        huboCambiosEnEsteCiclo = true;

        // Reiniciar sincronización para verificar siguiente tipo
        iniciarSincronizacion();
    }

    /**
     * Procesa una comparación de contenido (Fase 5).
     */
    public void procesarComparacion(String tipo, JsonElement dataRemota) {
        boolean huboCambios = fase5.compararYResolver(tipo, dataRemota);

        if (huboCambios) {
            huboCambiosEnEsteCiclo = true;
        }

        // Verificar si terminaron todas las comparaciones
        if (fase5.decrementarComparacion()) {
            LoggerCentral.info(TAG, VERDE + "✓ Comparaciones completadas para " + tipo + RESET);
            fase5.resetearComparaciones();
            iniciarSincronizacion();
        }
    }

    /**
     * Finaliza el proceso de sincronización exitosamente.
     */
    private void finalizarSincronizacionExitosa() {
        contadorReintentos = 0;
        sincronizacionEnProgreso = false;

        // Fase 6: Transferir archivos físicos
        if (fase6.estaConfigurado()) {
            LoggerCentral.info(TAG, CYAN + "▶ FASE 6: Verificando archivos físicos" + RESET);
            fase6.verificarYDescargarFaltantes();
        }

        // Notificar cambios si hubo
        if (huboCambiosEnEsteCiclo) {
            notificarCambios();
            huboCambiosEnEsteCiclo = false;
        }

        // Notificar finalización
        notificarFinalizacion();

        LoggerCentral.info(TAG, VERDE + "=== SINCRONIZACIÓN COMPLETADA ===" + RESET);
    }

    /**
     * Notifica cambios a clientes CS.
     */
    private void notificarCambios() {
        if (notificador != null) {
            LoggerCentral.info(TAG, AZUL + "📢 Notificando cambios a clientes CS..." + RESET);
            notificador.notificarCambio(
                    ServicioNotificacionCambios.TipoEvento.ACTUALIZACION_ESTADO,
                    null);
        }
    }

    /**
     * ✅ MEJORADO: Notifica finalización de sincronización a TODOS los observadores.
     * Esto incluye PanelUsuarios, ServicioNotificacionCliente, etc.
     */
    private void notificarFinalizacion() {
        LoggerCentral.info(TAG, AZUL + "📡 Notificando finalización de sincronización..." + RESET);

        // ✅ NUEVO: Notificar a TODOS los observadores del servicio padre
        if (servicioPadre != null) {
            try {
                LoggerCentral.info(TAG, VERDE + "📢 Notificando a TODOS los observadores (incluyendo UI)..." + RESET);
                servicioPadre.notificarObservadores("SINCRONIZACION_P2P_TERMINADA", huboCambiosEnEsteCiclo);
            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error notificando a observadores: " + e.getMessage());
            }
        }

        // Mantener compatibilidad con el observador individual (por si acaso)
        if (servicioNotificacionCliente != null) {
            try {
                servicioNotificacionCliente.actualizar("SINCRONIZACION_P2P_TERMINADA", huboCambiosEnEsteCiclo);
            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error notificando: " + e.getMessage());
            }
        }
    }

    /**
     * Guarda una entidad genérica.
     */
    private void guardarEntidad(String tipo, JsonElement data) {
        try {
            boolean guardado = fase5.compararYResolver(tipo, data);

            if (guardado) {
                LoggerCentral.info(TAG, VERDE + "✓ " + tipo + " guardado" + RESET);
            }
        } catch (Exception e) {
            LoggerCentral.error(TAG, ROJO + "Error guardando " + tipo + ": " + e.getMessage() + RESET);
        }
    }

    /**
     * Libera el lock de sincronización después de un retraso.
     */
    private void liberarLockConRetraso(long delayMs) {
        new Thread(() -> {
            try {
                Thread.sleep(delayMs);
                sincronizacionEnProgreso = false;
                LoggerCentral.debug(TAG, "Lock de sincronización liberado");
            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error liberando lock: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Fuerza una sincronización ignorando restricciones.
     */
    public void forzarSincronizacion() {
        LoggerCentral.warn(TAG, "Forzando sincronización manual...");
        huboCambiosEnEsteCiclo = false;
        contadorReintentos = 0;
        iniciarSincronizacion();
    }

    /**
     * Marca que hubo cambios en este ciclo.
     */
    public void marcarCambios() {
        huboCambiosEnEsteCiclo = true;
    }

    // Getters para las fases (útil para testing o acceso directo)
    public Fase1ConstruccionArboles getFase1() {
        return fase1;
    }

    public Fase2ComparacionHashes getFase2() {
        return fase2;
    }

    public Fase3SolicitudIDs getFase3() {
        return fase3;
    }

    public Fase4DeteccionFaltantes getFase4() {
        return fase4;
    }

    public Fase5ComparacionContenido getFase5() {
        return fase5;
    }

    public Fase6TransferenciaArchivos getFase6() {
        return fase6;
    }
}
