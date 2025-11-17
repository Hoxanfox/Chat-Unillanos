package logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class LoggerCentral {

    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Level LEVEL;
    private static PrintWriter fileWriter;

    static {
        String lvl = System.getProperty("LOGGER_LEVEL");
        if (lvl == null) lvl = System.getenv("LOGGER_LEVEL");
        Level parsed = Level.INFO;
        if (lvl != null) {
            try { parsed = Level.valueOf(lvl.trim().toUpperCase()); } catch (Exception ignored) {}
        }
        LEVEL = parsed;

        // Intentar abrir fichero de log en logs/app.log (modo append)
        try {
            File dir = new File("logs");
            if (!dir.exists()) dir.mkdirs();
            File f = new File(dir, "app.log");
            fileWriter = new PrintWriter(new FileWriter(f, true), true);
        } catch (IOException e) {
            // No interrumpir la aplicación si no es posible crear el fichero
            fileWriter = null;
            System.err.println("[LoggerCentral] No se pudo abrir logs/app.log para escritura: " + e.getMessage());
        }
    }

    private LoggerCentral() {}

    private static String prefijo() {
        return "[" + TF.format(LocalDateTime.now()) + "] [" + Thread.currentThread().getName() + "] ";
    }

    private static synchronized void write(String s, boolean err) {
        if (err) System.err.println(s); else System.out.println(s);
        if (fileWriter != null) {
            fileWriter.println(s);
        }
    }

    public static void info(String msg) {
        if (LEVEL.level <= Level.INFO.level) write(prefijo() + "INFO: " + msg, false);
    }

    public static void debug(String msg) {
        if (LEVEL.level <= Level.DEBUG.level) write(prefijo() + "DEBUG: " + msg, false);
    }

    public static void warn(String msg) {
        if (LEVEL.level <= Level.WARN.level) write(prefijo() + "WARN: " + msg, true);
    }

    public static void error(String msg) {
        if (LEVEL.level <= Level.ERROR.level) write(prefijo() + "ERROR: " + msg, true);
    }

    public static void error(String msg, Throwable t) {
        if (LEVEL.level <= Level.ERROR.level) {
            String full = prefijo() + "ERROR: " + msg + " - " + t.getMessage();
            write(full, true);
            if (fileWriter != null) {
                t.printStackTrace(fileWriter);
            }
            t.printStackTrace(System.err);
        }
    }

    private enum Level {
        DEBUG(1), INFO(2), WARN(3), ERROR(4);
        final int level;
        Level(int level) { this.level = level; }
    }
}
