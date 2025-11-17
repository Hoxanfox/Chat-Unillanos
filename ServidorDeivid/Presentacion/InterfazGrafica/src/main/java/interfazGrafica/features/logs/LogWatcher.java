package interfazGrafica.features.logs;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Observador simple que hace tail del fichero logs/app.log y escribe nuevas líneas en el LogsPanel.
 * No modifica el Logger central; sólo lee el fichero que LoggerCentral crea (si existe).
 */
public class LogWatcher {

    private final LogsPanel panel;
    private final File logFile;
    private final ExecutorService exec = Executors.newSingleThreadExecutor(r -> new Thread(r, "LogWatcher"));
    private final AtomicBoolean running = new AtomicBoolean(false);

    public LogWatcher(LogsPanel panel) {
        this(panel, new File("logs/app.log"));
    }

    public LogWatcher(LogsPanel panel, File logFile) {
        this.panel = panel;
        this.logFile = logFile;
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            exec.submit(this::watchLoop);
        }
    }

    public void stop() {
        running.set(false);
        exec.shutdownNow();
    }

    private void watchLoop() {
        try (RandomAccessFile raf = new RandomAccessFile(logFile, "r")) {
            long filePointer = raf.length();
            raf.seek(filePointer);
            while (running.get()) {
                String line = raf.readLine();
                if (line == null) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    // RandomAccessFile.readLine devuelve bytes ISO-8859-1, normalizamos a UTF-8
                    String utf8 = new String(line.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                    SwingUtilities.invokeLater(() -> panel.appendLog(utf8));
                }
            }
        } catch (IOException e) {
            // Si no existe el fichero o hay problemas, mostrar mensaje en panel
            SwingUtilities.invokeLater(() -> panel.appendLog("[LogWatcher] No se pudo abrir logs/app.log: " + e.getMessage()));
        }
    }
}

