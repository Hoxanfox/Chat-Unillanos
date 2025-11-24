package logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public final class LoggerCentral {

    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("HH:mm:ss"); // Formato hora corta para consola
    private static final Level LEVEL;
    private static PrintWriter fileWriter;

    // Hook para la interfaz de usuario (Consola limpia)
    private static Consumer<String> printer;

    // Colores ANSI
    private static final String RESET = "\u001B[0m";
    private static final String VERDE = "\u001B[32m";
    private static final String ROJO = "\u001B[31m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String GRIS = "\u001B[90m";

    static {
        String lvl = System.getProperty("LOGGER_LEVEL");
        if (lvl == null) lvl = System.getenv("LOGGER_LEVEL");
        Level parsed = Level.DEBUG; // Default a DEBUG para ver todo en desarrollo
        if (lvl != null) {
            try { parsed = Level.valueOf(lvl.trim().toUpperCase()); } catch (Exception ignored) {}
        }
        LEVEL = parsed;

        try {
            File dir = new File("logs");
            if (!dir.exists()) dir.mkdirs();
            File f = new File(dir, "app.log");
            fileWriter = new PrintWriter(new FileWriter(f, true), true);
        } catch (IOException e) {
            fileWriter = null;
            System.err.println("[LoggerCentral] No se pudo abrir logs/app.log: " + e.getMessage());
        }
    }

    private LoggerCentral() {}

    /**
     * Permite que la Vista se suscriba para manejar la impresión (borrar línea, etc).
     */
    public static void setPrinter(Consumer<String> printFunction) {
        printer = printFunction;
    }

    private static String prefijo() {
        return GRIS + "[" + TF.format(LocalDateTime.now()) + "] [" + Thread.currentThread().getName() + "] " + RESET;
    }

    private static synchronized void write(String consoleMsg, String fileMsg, boolean err) {
        // 1. Imprimir en Consola (vía Printer si existe, o System.out)
        if (printer != null) {
            printer.accept(consoleMsg);
        } else {
            if (err) System.err.println(consoleMsg); else System.out.println(consoleMsg);
        }

        // 2. Imprimir en Fichero (Sin colores ANSI)
        if (fileWriter != null) {
            fileWriter.println(fileMsg);
        }
    }

    // --- MÉTODOS CON TAG (Para mantener compatibilidad con el código generado) ---

    public static void info(String tag, String msg) {
        info("[" + tag + "] " + msg);
    }

    public static void debug(String tag, String msg) {
        debug("[" + tag + "] " + msg);
    }

    public static void warn(String tag, String msg) {
        warn("[" + tag + "] " + msg);
    }

    public static void error(String tag, String msg) {
        error("[" + tag + "] " + msg);
    }

    // --- MÉTODOS ORIGINALES ---

    public static void info(String msg) {
        if (LEVEL.level <= Level.INFO.level) {
            String console = prefijo() + VERDE + "INFO: " + RESET + msg;
            String file = "[INFO] " + msg;
            write(console, file, false);
        }
    }

    public static void debug(String msg) {
        if (LEVEL.level <= Level.DEBUG.level) {
            String console = prefijo() + MAGENTA + "DEBUG: " + RESET + msg;
            String file = "[DEBUG] " + msg;
            write(console, file, false);
        }
    }

    public static void warn(String msg) {
        if (LEVEL.level <= Level.WARN.level) {
            String console = prefijo() + AMARILLO + "WARN: " + RESET + msg;
            String file = "[WARN] " + msg;
            write(console, file, true);
        }
    }

    public static void error(String msg) {
        if (LEVEL.level <= Level.ERROR.level) {
            String console = prefijo() + ROJO + "ERROR: " + RESET + msg;
            String file = "[ERROR] " + msg;
            write(console, file, true);
        }
    }

    public static void error(String msg, Throwable t) {
        if (LEVEL.level <= Level.ERROR.level) {
            String console = prefijo() + ROJO + "ERROR: " + RESET + msg + " - " + t.getMessage();
            String file = "[ERROR] " + msg + " - " + t.getMessage();
            write(console, file, true);
            if (fileWriter != null) t.printStackTrace(fileWriter);
        }
    }

    private enum Level {
        DEBUG(1), INFO(2), WARN(3), ERROR(4);
        final int level;
        Level(int level) { this.level = level; }
    }
}