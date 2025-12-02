package interfazGrafica.vistaTranscripcion;

import controlador.transcripcion.ControladorTranscripcion;
import dto.transcripcion.DTOAudioTranscripcion;
import interfazGrafica.vistaTranscripcion.componentes.*;
import logger.LoggerCentral;
import observador.IObservador;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Panel para gestionar transcripciones de audios
 * Permite visualizar, filtrar y transcribir audios del sistema
 */
public class PanelTranscripcionAudios extends JPanel implements IObservador {

    private static final String TAG = "PanelTranscripcionAudios";
    private final ControladorTranscripcion controlador;

    // Componentes de UI
    private PanelFiltros panelFiltros;
    private TablaAudios tablaAudios;
    private PanelDetalles panelDetalles;
    private PanelEstadisticas panelEstadisticas;

    // Datos
    private List<DTOAudioTranscripcion> audiosActuales;

    // Colores
    private static final Color COLOR_BACKGROUND = new Color(236, 240, 241);

    public PanelTranscripcionAudios(ControladorTranscripcion controlador) {
        this.controlador = controlador;

        // ✅ Suscribirse al controlador (para eventos locales)
        this.controlador.suscribirObservador(this);

        // ✅ NUEVO: Suscribirse directamente a la FachadaTranscripcion (para eventos del repositorio)
        this.controlador.suscribirAFachadaTranscripcion(this);

        inicializarUI();
        cargarDatos();
        LoggerCentral.info(TAG, "Panel de Transcripción de Audios inicializado");
        LoggerCentral.info(TAG, "✓ Suscrito a eventos de transcripción en tiempo real");
    }

    private void inicializarUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(COLOR_BACKGROUND);

        // Crear componentes
        panelFiltros = new PanelFiltros();
        tablaAudios = new TablaAudios();
        panelDetalles = new PanelDetalles();
        panelEstadisticas = new PanelEstadisticas();

        // Configurar listeners
        panelFiltros.setListenerBuscar(e -> aplicarFiltros());
        panelFiltros.setListenerLimpiar(e -> limpiarFiltros());
        tablaAudios.setListenerSeleccion(this::audioSeleccionado);
        panelDetalles.setListenerGuardar(e -> guardarTranscripcion());
        panelDetalles.setListenerReproducir(e -> reproducirAudio());
        panelDetalles.setListenerTranscribirAuto(e -> transcribirAutomaticamente()); // ✅ NUEVO

        // Agregar componentes al panel principal
        add(panelFiltros, BorderLayout.NORTH);
        add(tablaAudios, BorderLayout.CENTER);
        add(panelDetalles, BorderLayout.EAST);
        add(panelEstadisticas, BorderLayout.SOUTH);
    }

    private void cargarDatos() {
        try {
            audiosActuales = controlador.obtenerAudios();
            tablaAudios.actualizarTabla(audiosActuales);
            panelEstadisticas.actualizarEstadisticas(audiosActuales);
            LoggerCentral.info(TAG, "Datos cargados: " + audiosActuales.size() + " audios");
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al cargar datos: " + e.getMessage());
            mostrarError("Error al cargar audios: " + e.getMessage());
        }
    }

    private void aplicarFiltros() {
        try {
            String textoBusqueda = panelFiltros.getTextoBusqueda();
            String tipoSeleccionado = panelFiltros.getTipoSeleccionado();

            List<DTOAudioTranscripcion> audiosFiltrados;

            // Primero filtrar por tipo
            if ("Canales".equals(tipoSeleccionado)) {
                audiosFiltrados = controlador.filtrarPorTipo("CANAL");
            } else if ("Contactos".equals(tipoSeleccionado)) {
                audiosFiltrados = controlador.filtrarPorTipo("CONTACTO");
            } else {
                audiosFiltrados = controlador.obtenerAudios();
            }

            // Luego aplicar búsqueda por texto
            if (!textoBusqueda.isEmpty()) {
                audiosFiltrados = controlador.buscarAudios(textoBusqueda);
            }

            tablaAudios.actualizarTabla(audiosFiltrados);
            LoggerCentral.debug(TAG, "Filtros aplicados: " + audiosFiltrados.size() + " resultados");
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al aplicar filtros: " + e.getMessage());
            mostrarError("Error al filtrar: " + e.getMessage());
        }
    }

    private void limpiarFiltros() {
        panelFiltros.limpiar();
        cargarDatos();
    }

    private void audioSeleccionado() {
        String audioId = tablaAudios.getAudioIdSeleccionado();
        if (audioId != null) {
            DTOAudioTranscripcion audio = audiosActuales.stream()
                    .filter(a -> a.getAudioId().equals(audioId))
                    .findFirst()
                    .orElse(null);

            panelDetalles.mostrarAudio(audio);
            LoggerCentral.debug(TAG, "Audio seleccionado: " + audioId);
        } else {
            panelDetalles.limpiar();
        }
    }

    /**
     * ✅ NUEVO: Transcribe automáticamente el audio seleccionado usando Vosk
     */
    private void transcribirAutomaticamente() {
        DTOAudioTranscripcion audio = panelDetalles.getAudioActual();
        if (audio == null) {
            mostrarAdvertencia("Debe seleccionar un audio");
            return;
        }

        if (audio.isTranscrito()) {
            mostrarAdvertencia("Este audio ya está transcrito");
            return;
        }

        // Confirmar acción
        int confirmacion = JOptionPane.showConfirmDialog(
            this,
            "¿Desea transcribir automáticamente este audio?\nEsto puede tomar unos minutos.",
            "Confirmar Transcripción Automática",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            LoggerCentral.info(TAG, "🎤 Iniciando transcripción automática para: " + audio.getAudioId());

            // Iniciar transcripción automática (se procesa en segundo plano)
            boolean encolado = controlador.iniciarTranscripcionAutomatica(audio.getAudioId());

            if (encolado) {
                mostrarInfo("Audio encolado para transcripción automática.\nSe notificará cuando termine.");
                LoggerCentral.info(TAG, "✅ Audio encolado para transcripción");
            } else {
                mostrarError("No se pudo iniciar la transcripción automática.\nVerifique que el modelo Vosk esté configurado.");
                LoggerCentral.error(TAG, "❌ Error al encolar audio para transcripción");
            }

        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al transcribir automáticamente: " + e.getMessage());
            mostrarError("Error al iniciar transcripción: " + e.getMessage());
        }
    }

    private void guardarTranscripcion() {
        DTOAudioTranscripcion audio = panelDetalles.getAudioActual();
        if (audio == null) {
            mostrarAdvertencia("Debe seleccionar un audio");
            return;
        }

        String transcripcion = panelDetalles.getTextoTranscripcion();
        if (transcripcion.isEmpty()) {
            mostrarAdvertencia("La transcripción no puede estar vacía");
            return;
        }

        boolean exito = controlador.transcribirAudio(audio.getAudioId(), transcripcion);
        if (exito) {
            mostrarExito("Transcripción guardada exitosamente");
            cargarDatos();
            panelDetalles.limpiar();
        } else {
            mostrarError("Error al guardar la transcripción");
        }
    }

    private void reproducirAudio() {
        DTOAudioTranscripcion audio = panelDetalles.getAudioActual();
        if (audio == null) {
            mostrarAdvertencia("Debe seleccionar un audio");
            return;
        }

        // TODO: Implementar reproducción de audio
        mostrarInfo("Función de reproducción en desarrollo");
        LoggerCentral.info(TAG, "Reproduciendo audio: " + audio.getRutaArchivo());
    }

    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        LoggerCentral.debug(TAG, "Evento recibido: " + tipoDeDato);

        SwingUtilities.invokeLater(() -> {
            switch (tipoDeDato) {
                case "TRANSCRIPCION_ACTUALIZADA":
                    // ✅ NUEVO: Cuando se actualiza una transcripción en la BD, recargar la tabla
                    LoggerCentral.info(TAG, "🔔 Transcripción actualizada, recargando datos...");
                    cargarDatos();
                    break;

                case "TRANSCRIPCION_COMPLETADA":
                    LoggerCentral.info(TAG, "✓ Transcripción completada");
                    cargarDatos();
                    if (datos instanceof DTOAudioTranscripcion) {
                        DTOAudioTranscripcion audio = (DTOAudioTranscripcion) datos;
                        mostrarInfo("Transcripción completada para: " + audio.getAudioId());
                    }
                    break;

                case "TRANSCRIPCION_ENCOLADA":
                    LoggerCentral.info(TAG, "Transcripción encolada");
                    break;

                case "TRANSCRIPCION_ERROR":
                    LoggerCentral.error(TAG, "Error en transcripción");
                    if (datos instanceof DTOAudioTranscripcion) {
                        DTOAudioTranscripcion audio = (DTOAudioTranscripcion) datos;
                        mostrarError("Error al transcribir: " + audio.getAudioId());
                    }
                    break;

                case "NUEVO_AUDIO_RECIBIDO":
                    LoggerCentral.info(TAG, "🔔 Nuevo audio recibido, recargando datos...");
                    cargarDatos();
                    break;

                case "AUDIOS_CARGADOS":
                    LoggerCentral.info(TAG, "Audios cargados: " + datos);
                    break;

                default:
                    LoggerCentral.debug(TAG, "Evento no manejado: " + tipoDeDato);
            }
        });
    }

    // Métodos auxiliares para mostrar mensajes
    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void mostrarAdvertencia(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Advertencia", JOptionPane.WARNING_MESSAGE);
    }

    private void mostrarExito(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Éxito", JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostrarInfo(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Información", JOptionPane.INFORMATION_MESSAGE);
    }
}
