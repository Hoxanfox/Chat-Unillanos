package transporte;

import java.io.OutputStream;
import java.io.PrintWriter;
import logger.LoggerCentral;

/**
 * PrintWriter que registra (debug) todo lo que se escribe antes de enviarlo.
 * Se utiliza para auditar/depurar el contenido enviado por el socket.
 */
public class LoggingPrintWriter extends PrintWriter {

    public LoggingPrintWriter(OutputStream out, boolean autoFlush) {
        super(out, autoFlush);
    }

    @Override
    public void println(String x) {
        LoggerCentral.debug("LoggingPrintWriter: println -> " + x);
        super.println(x);
    }

    @Override
    public void print(String s) {
        LoggerCentral.debug("LoggingPrintWriter: print -> " + s);
        super.print(s);
    }

    @Override
    public void write(char[] buf, int off, int len) {
        try {
            LoggerCentral.debug("LoggingPrintWriter: write -> " + new String(buf, off, len));
        } catch (Exception e) {
            LoggerCentral.debug("LoggingPrintWriter: write -> (error al construir string): " + e.getMessage());
        }
        super.write(buf, off, len);
    }

    @Override
    public void write(String s, int off, int len) {
        try {
            String snippet;
            if (s == null) {
                snippet = "null";
            } else {
                int end = Math.min(s.length(), off + len);
                if (off < 0 || off > end) {
                    snippet = "(invalid range)";
                } else {
                    snippet = s.substring(off, end);
                }
            }
            LoggerCentral.debug("LoggingPrintWriter: write(String) -> " + snippet);
        } catch (Exception e) {
            LoggerCentral.debug("LoggingPrintWriter: write(String) -> (error): " + e.getMessage());
        }
        // Evitar pasar null a super para prevenir NPE
        if (s == null) {
            // escribimos la palabra "null" trunqueda a la longitud solicitada
            String n = "null";
            int writeLen = Math.min(n.length(), Math.max(0, len));
            super.write(n, 0, writeLen);
        } else {
            super.write(s, off, len);
        }
    }

    @Override
    public void write(int c) {
        LoggerCentral.debug("LoggingPrintWriter: write(int) -> " + c);
        super.write(c);
    }

    @Override
    public PrintWriter printf(String format, Object... args) {
        try {
            String msg = String.format(format, args);
            LoggerCentral.debug("LoggingPrintWriter: printf -> " + msg);
        } catch (Exception e) {
            LoggerCentral.debug("LoggingPrintWriter: printf -> (error): " + e.getMessage());
        }
        return super.printf(format, args);
    }

    @Override
    public PrintWriter format(String format, Object... args) {
        try {
            String msg = String.format(format, args);
            LoggerCentral.debug("LoggingPrintWriter: format -> " + msg);
        } catch (Exception e) {
            LoggerCentral.debug("LoggingPrintWriter: format -> (error): " + e.getMessage());
        }
        return super.format(format, args);
    }
}
