package dto.featureNotificaciones;

import java.time.LocalDateTime;

/**
 * DTO que representa una notificación del sistema.
 */
public class DTONotificacion {

    private final String id;
    private final String tipo; // "MENCION", "MENSAJE", "SOLICITUD_AMISTAD", "INVITACION_CANAL"
    private final String titulo;
    private final String contenido;
    private final LocalDateTime fecha;
    private final boolean leida;
    private final String origenId; // ID del usuario/canal que originó la notificación

    public DTONotificacion(String id, String tipo, String titulo, String contenido,
                          LocalDateTime fecha, boolean leida, String origenId) {
        this.id = id;
        this.tipo = tipo;
        this.titulo = titulo;
        this.contenido = contenido;
        this.fecha = fecha;
        this.leida = leida;
        this.origenId = origenId;
    }

    public String getId() { return id; }
    public String getTipo() { return tipo; }
    public String getTitulo() { return titulo; }
    public String getContenido() { return contenido; }
    public LocalDateTime getFecha() { return fecha; }
    public boolean isLeida() { return leida; }
    public String getOrigenId() { return origenId; }

    public String getTiempoRelativo() {
        LocalDateTime ahora = LocalDateTime.now();
        long minutos = java.time.Duration.between(fecha, ahora).toMinutes();

        if (minutos < 1) return "Ahora mismo";
        if (minutos < 60) return minutos + " minutos ago";

        long horas = minutos / 60;
        if (horas < 24) return horas + " horas ago";

        long dias = horas / 24;
        return dias + " días ago";
    }
}

