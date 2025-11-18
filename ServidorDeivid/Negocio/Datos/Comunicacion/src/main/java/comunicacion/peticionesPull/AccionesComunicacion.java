package comunicacion.peticionesPull;

/**
 * Constantes de acciones usadas en la comunicación (peticiones/respuestas).
 * Se implementa como constantes String porque varios puntos del código construyen DTORequest con estas constantes.
 */
public final class AccionesComunicacion {
    public static final String PEER_PUSH = "PEER_PUSH";
    public static final String PEER_JOIN = "PEER_JOIN";
    public static final String PEER_LIST = "PEER_LIST";
    public static final String PEER_UPDATE = "PEER_UPDATE";

    private AccionesComunicacion() {}
}

