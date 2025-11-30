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
        this.controlador.suscribirObservador(this);
        inicializarUI();
        cargarDatos();
        LoggerCentral.info(TAG, "Panel de Transcripción de Audios inicializado");
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

        if ("AUDIO_TRANSCRITO".equals(tipoDeDato)) {
            SwingUtilities.invokeLater(this::cargarDatos);
        } else if ("NUEVO_AUDIO_RECIBIDO".equals(tipoDeDato)) {
            LoggerCentral.info(TAG, "🔔 Nuevo audio recibido, actualizando tabla...");
            SwingUtilities.invokeLater(this::cargarDatos);
        } else if ("AUDIOS_CARGADOS".equals(tipoDeDato)) {
            LoggerCentral.info(TAG, "📁 Audios cargados desde BD, actualizando interfaz...");
            SwingUtilities.invokeLater(this::cargarDatos);
        }
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
