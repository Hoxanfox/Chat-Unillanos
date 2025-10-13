package gestionContactos.mensajes;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import comunicacion.EnviadorPeticiones;
import comunicacion.GestorRespuesta;
import comunicacion.IEnviadorPeticiones;
import comunicacion.IGestorRespuesta;
import dto.comunicacion.DTORequest;
import dto.comunicacion.DTOResponse;
import dto.comunicacion.peticion.mensaje.DTOEnviarMensaje;
import dto.vistaContactoChat.DTOMensaje;
import gestionUsuario.sesion.GestorSesionUsuario;
import observador.IObservador;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación del gestor de mensajes. YA NO DEPENDE de otros gestores.
 */
public class GestionMensajesImpl implements IGestionMensajes {

    private final List<IObservador> observadores = new ArrayList<>();
    private final IEnviadorPeticiones enviadorPeticiones;
    private final IGestorRespuesta gestorRespuesta;
    private final GestorSesionUsuario gestorSesionUsuario;

    public GestionMensajesImpl() {
        this.enviadorPeticiones = new EnviadorPeticiones();
        this.gestorRespuesta = GestorRespuesta.getInstancia();
        this.gestorSesionUsuario = GestorSesionUsuario.getInstancia();

        this.gestorRespuesta.registrarManejador("nuevoMensajePrivado", this::manejarNuevoMensaje);
        this.gestorRespuesta.registrarManejador("respuestaHistorialPrivado", this::manejarHistorial);
    }

    // ... (manejarNuevoMensaje y manejarHistorial no cambian) ...

    @Override
    public void solicitarHistorial(String contactoId) {
        DTORequest peticion = new DTORequest("solicitarHistorialPrivado", contactoId);
        enviadorPeticiones.enviar(peticion);
    }

    @Override
    public CompletableFuture<Void> enviarMensajeTexto(String destinatarioId, String contenido) {
        String remitenteId = gestorSesionUsuario.getUserId();
        DTOEnviarMensaje payload = DTOEnviarMensaje.deTexto(remitenteId, destinatarioId, contenido);
        DTORequest peticion = new DTORequest("enviarMensajePrivado", payload);
        enviadorPeticiones.enviar(peticion);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> enviarMensajeAudio(String destinatarioId, String audioFileId) {
        String remitenteId = gestorSesionUsuario.getUserId();
        DTOEnviarMensaje payload = DTOEnviarMensaje.deAudio(remitenteId, destinatarioId, audioFileId);
        DTORequest peticion = new DTORequest("enviarMensajePrivado", payload);
        enviadorPeticiones.enviar(peticion);
        return CompletableFuture.completedFuture(null);
    }

    // ... (El resto de la clase no cambia) ...
    private void manejarNuevoMensaje(DTOResponse r) { if(r.fueExitoso()) notificarObservadores("NUEVO_MENSAJE_PRIVADO", new Gson().fromJson(new Gson().toJson(r.getData()), DTOMensaje.class)); }
    private void manejarHistorial(DTOResponse r) { if(r.fueExitoso()) { Type t = new TypeToken<ArrayList<DTOMensaje>>(){}.getType(); notificarObservadores("HISTORIAL_MENSAJES", new Gson().fromJson(new Gson().toJson(r.getData()), t)); }}
    @Override public void registrarObservador(IObservador o) { if (!observadores.contains(o)) observadores.add(o); }
    @Override public void removerObservador(IObservador o) { observadores.remove(o); }
    @Override public void notificarObservadores(String t, Object d) { for (IObservador o : observadores) o.actualizar(t, d); }
}

