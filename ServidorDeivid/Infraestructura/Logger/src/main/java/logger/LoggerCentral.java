package logger;

import observador.IObservador;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class LoggerCentral {

    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("HH:mm:ss"); // Formato hora corta para consola
    private static final DateTimeFormatter FULL_TF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"); // Formato completo para logs
    private static final Level LEVEL;
    private static PrintWriter fileWriter;

    // Hook para la interfaz de usuario (Consola limpia)
    private static Consumer<String> printer;

    // Lista de observadores para logs en tiempo real
    private static final List<IObservador> observadores = new ArrayList<>();

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
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created) {
                    System.err.println("[LoggerCentral] No se pudo crear directorio logs/");
                }
            }
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

    // --- GESTIÓN DE OBSERVADORES ---

    public static void registrarObservador(IObservador observador) {
        synchronized (observadores) {
            if (!observadores.contains(observador)) {
                observadores.add(observador);
            }
        }
    }

    public static void removerObservador(IObservador observador) {
        synchronized (observadores) {
            observadores.remove(observador);
        }
    }

    private static void notificarObservadores(String tipoDeDato, Object datos) {
        synchronized (observadores) {
            for (IObservador obs : observadores) {
                obs.actualizar(tipoDeDato, datos);
            }
        }
    }

    private static String prefijo() {
        return GRIS + "[" + TF.format(LocalDateTime.now()) + "] [" + Thread.currentThread().getName() + "] " + RESET;
    }

    private static synchronized void write(String consoleMsg, String fileMsg, boolean err, String level, String source) {
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

        // 3. Notificar a observadores con información estructurada
        String timestamp = FULL_TF.format(LocalDateTime.now());
        String[] logData = {timestamp, level, source, extractMessage(fileMsg)};
        notificarObservadores("NUEVO_LOG", logData);
    }

    private static String extractMessage(String fileMsg) {
        // Extrae el mensaje eliminando el prefijo [LEVEL]
        if (fileMsg.startsWith("[")) {
            int endBracket = fileMsg.indexOf("]");
            if (endBracket > 0 && endBracket < fileMsg.length() - 1) {
                return fileMsg.substring(endBracket + 2);
            }
        }
        return fileMsg;
    }

    // --- MÉTODOS CON TAG (Para mantener compatibilidad con el código generado) ---

    public static void info(String tag, String msg) {
        if (LEVEL.level <= Level.INFO.level) {
            String fullMsg = "[" + tag + "] " + msg;
            String console = prefijo() + VERDE + "INFO: " + RESET + fullMsg;
            String file = "[INFO] " + fullMsg;
            write(console, file, false, "INFO", tag);
        }
    }

    public static void debug(String tag, String msg) {
        if (LEVEL.level <= Level.DEBUG.level) {
            String fullMsg = "[" + tag + "] " + msg;
            String console = prefijo() + MAGENTA + "DEBUG: " + RESET + fullMsg;
            String file = "[DEBUG] " + fullMsg;
            write(console, file, false, "DEBUG", tag);
        }
    }

    public static void warn(String tag, String msg) {
        if (LEVEL.level <= Level.WARN.level) {
            String fullMsg = "[" + tag + "] " + msg;
            String console = prefijo() + AMARILLO + "WARN: " + RESET + fullMsg;
            String file = "[WARN] " + fullMsg;
            write(console, file, true, "WARNING", tag);
        }
    }

    public static void error(String tag, String msg) {
        if (LEVEL.level <= Level.ERROR.level) {
            String fullMsg = "[" + tag + "] " + msg;
            String console = prefijo() + ROJO + "ERROR: " + RESET + fullMsg;
            String file = "[ERROR] " + fullMsg;
            write(console, file, true, "ERROR", tag);
        }
    }

    // --- MÉTODOS ORIGINALES ---

    public static void info(String msg) {
        if (LEVEL.level <= Level.INFO.level) {
            String console = prefijo() + VERDE + "INFO: " + RESET + msg;
            String file = "[INFO] " + msg;
            write(console, file, false, "INFO", "System");
        }
    }

    public static void debug(String msg) {
        if (LEVEL.level <= Level.DEBUG.level) {
            String console = prefijo() + MAGENTA + "DEBUG: " + RESET + msg;
            String file = "[DEBUG] " + msg;
            write(console, file, false, "DEBUG", "System");
        }
    }

    public static void warn(String msg) {
        if (LEVEL.level <= Level.WARN.level) {
            String console = prefijo() + AMARILLO + "WARN: " + RESET + msg;
            String file = "[WARN] " + msg;
            write(console, file, true, "WARNING", "System");
        }
    }

    public static void error(String msg) {
        if (LEVEL.level <= Level.ERROR.level) {
            String console = prefijo() + ROJO + "ERROR: " + RESET + msg;
            String file = "[ERROR] " + msg;
            write(console, file, true, "ERROR", "System");
        }
    }

    public static void error(String msg, Throwable t) {
        if (LEVEL.level <= Level.ERROR.level) {
            String console = prefijo() + ROJO + "ERROR: " + RESET + msg + " - " + t.getMessage();
            String file = "[ERROR] " + msg + " - " + t.getMessage();
            write(console, file, true, "ERROR", "System");
            if (fileWriter != null) t.printStackTrace(fileWriter);
        }
    }

    private enum Level {
        DEBUG(1), INFO(2), WARN(3), ERROR(4);
        final int level;
        Level(int level) { this.level = level; }
    }
}