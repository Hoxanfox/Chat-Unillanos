package interfazGrafica.vistaLogs;

import controlador.logs.ControladorLogs;
import dto.logs.DTOLog;
import interfazGrafica.vistaLogs.componentes.BarraHerramientasLogs;
import interfazGrafica.vistaLogs.componentes.TablaLogs;
import observador.IObservador;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel principal que orquesta los componentes de la vista de logs
 * Muestra los registros del sistema con opciones de limpieza y exportación
 * Implementa IObservador para recibir actualizaciones en tiempo real
 */
public class PanelLogs extends JPanel implements IObservador {

    private BarraHerramientasLogs barraHerramientas;
    private TablaLogs tablaLogs;
    private DateTimeFormatter formateadorFecha;
    private ControladorLogs controlador;

    public PanelLogs() {
        formateadorFecha = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        configurarPanel();
        inicializarComponentes();
        configurarEventos();
    }

    /**
     * Configura el controlador y se registra como observador
     */
    public void setControlador(ControladorLogs controlador) {
        this.controlador = controlador;

        // Registrarse como observador para recibir logs en tiempo real
        controlador.registrarObservadorLogs(this);

        // Cargar logs existentes
        cargarLogsExistentes();
    }

    /**
     * Carga los logs existentes en memoria al inicio
     */
    private void cargarLogsExistentes() {
        try {
            List<DTOLog> logs = controlador.obtenerTodosLosLogs();
            for (DTOLog log : logs) {
                tablaLogs.agregarLog(log.toTableRow());
            }
        } catch (Exception e) {
            System.err.println("Error al cargar logs existentes: " + e.getMessage());
        }
    }

    // --- IMPLEMENTACIÓN DE IObservador ---

    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        if ("NUEVO_LOG".equals(tipoDeDato) && datos instanceof DTOLog) {
            DTOLog nuevoLog = (DTOLog) datos;

            // Actualizar la tabla en el hilo de Swing
            SwingUtilities.invokeLater(() -> {
                tablaLogs.agregarLog(nuevoLog.toTableRow());
            });
        }
    }

    private void configurarPanel() {
        this.setLayout(new BorderLayout(10, 10));
        this.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
    }

    private void inicializarComponentes() {
        // Instanciar componentes
        barraHerramientas = new BarraHerramientasLogs();
        tablaLogs = new TablaLogs();

        // Agregar al panel
        this.add(barraHerramientas, BorderLayout.NORTH);
        this.add(tablaLogs, BorderLayout.CENTER);
    }

    private void configurarEventos() {
        // Configurar listeners usando los métodos de los componentes
        barraHerramientas.setClearActionListener(e -> limpiarLogs());
        barraHerramientas.setExportActionListener(e -> exportarLogs());
    }

    /**
     * Limpia todos los logs de la tabla después de confirmación
     */
    private void limpiarLogs() {
        int cantidad = tablaLogs.obtenerCantidadLogs();

        if (cantidad == 0) {
            JOptionPane.showMessageDialog(this,
                "No hay logs para limpiar",
                "Clear Logs",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirmacion = JOptionPane.showConfirmDialog(this,
            "¿Está seguro de limpiar todos los logs (" + cantidad + " registros)?",
            "Confirmar limpieza",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirmacion == JOptionPane.YES_OPTION) {
            tablaLogs.limpiarLogs();

            // Limpiar también en el gestor si hay controlador configurado
            if (controlador != null) {
                try {
                    controlador.limpiarLogs();
                } catch (Exception ex) {
                    System.err.println("Error al limpiar logs en el gestor: " + ex.getMessage());
                }
            }

            JOptionPane.showMessageDialog(this,
                "Logs limpiados exitosamente",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Exporta los logs a un archivo de texto
     */
    private void exportarLogs() {
        if (tablaLogs.obtenerCantidadLogs() == 0) {
            JOptionPane.showMessageDialog(this,
                "No hay logs para exportar",
                "Export Logs",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Crear diálogo de selección de archivo
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Exportar Logs");

        // Sugerir nombre de archivo con fecha actual
        String nombreSugerido = "logs_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
        fileChooser.setSelectedFile(new File(nombreSugerido));

        // Filtro para archivos de texto
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Text Files (*.txt)", "txt");
        fileChooser.setFileFilter(filter);

        int resultado = fileChooser.showSaveDialog(this);

        if (resultado == JFileChooser.APPROVE_OPTION) {
            File archivo = fileChooser.getSelectedFile();

            // Asegurar extensión .txt
            if (!archivo.getName().toLowerCase().endsWith(".txt")) {
                archivo = new File(archivo.getAbsolutePath() + ".txt");
            }

            try {
                exportarLogsAArchivo(archivo);
                JOptionPane.showMessageDialog(this,
                    "Logs exportados exitosamente a:\n" + archivo.getAbsolutePath(),
                    "Export Success",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                    "Error al exportar logs: " + ex.getMessage(),
                    "Export Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Escribe los logs en un archivo
     */
    private void exportarLogsAArchivo(File archivo) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivo))) {
            // Escribir encabezado
            writer.write("=".repeat(100));
            writer.newLine();
            writer.write("SYSTEM LOGS EXPORT");
            writer.newLine();
            writer.write("Export Date: " + LocalDateTime.now().format(formateadorFecha));
            writer.newLine();
            writer.write("=".repeat(100));
            writer.newLine();
            writer.newLine();

            // Obtener y escribir logs
            Object[][] logs = tablaLogs.obtenerTodosLosLogs();

            for (Object[] log : logs) {
                writer.write(String.format("Time: %s | Level: %-8s | Source: %-20s | Message: %s",
                    log[0], log[1], log[2], log[3]));
                writer.newLine();
            }

            writer.newLine();
            writer.write("=".repeat(100));
            writer.newLine();
            writer.write("Total logs: " + logs.length);
        }
    }

    /**
     * Agrega un nuevo log al panel (método público para uso externo)
     */
    public void agregarLog(String level, String source, String message) {
        String time = LocalDateTime.now().format(formateadorFecha);
        Object[] nuevoLog = {time, level, source, message};
        tablaLogs.agregarLog(nuevoLog);
    }

    /**
     * Limpia el observador al destruir el panel
     */
    public void limpiar() {
        if (controlador != null) {
            controlador.removerObservadorLogs(this);
        }
    }
}
