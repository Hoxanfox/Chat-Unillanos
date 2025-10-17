package fachada.gestionCanales;

import dominio.Canal;
import gestionCanales.invitarMiembro.IInvitadorMiembro;
import gestionCanales.invitarMiembro.InvitadorMiembro;
import gestionCanales.listarCanales.IListadorCanales;
import gestionCanales.listarCanales.ListadorCanales;
import gestionCanales.listarMiembros.IListadorMiembros;
import gestionCanales.listarMiembros.ListadorMiembros;
import gestionCanales.mensajes.GestorMensajesCanalImpl;
import gestionCanales.mensajes.IGestorMensajesCanal;
import gestionCanales.nuevoCanal.CreadorCanal;
import gestionCanales.nuevoCanal.ICreadorCanal;
import observador.IObservador;
import repositorio.canal.IRepositorioCanal;
import repositorio.canal.RepositorioCanalImpl;
import repositorio.mensaje.IRepositorioMensajeCanal;
import repositorio.mensaje.RepositorioMensajeCanalImpl;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación de la Fachada que gestiona todas las operaciones de canales.
 * Orquesta los diferentes gestores de negocio relacionados con canales.
 * PATRÓN SINGLETON para mantener los observadores registrados.
 */
public class FachadaCanalesImpl implements IFachadaCanales {

    private static FachadaCanalesImpl instancia;

    private final ICreadorCanal creadorCanal;
    private final IListadorCanales listadorCanales;
    private final IGestorMensajesCanal gestorMensajes;
    private final IInvitadorMiembro invitadorMiembro;
    private final IListadorMiembros listadorMiembros;

    private FachadaCanalesImpl() {
        IRepositorioCanal repositorioCanal = new RepositorioCanalImpl();
        IRepositorioMensajeCanal repositorioMensajes = new RepositorioMensajeCanalImpl();

        this.creadorCanal = new CreadorCanal(repositorioCanal);
        this.listadorCanales = new ListadorCanales(repositorioCanal);
        this.gestorMensajes = new GestorMensajesCanalImpl(repositorioMensajes);
        this.invitadorMiembro = new InvitadorMiembro(repositorioCanal);
        this.listadorMiembros = new ListadorMiembros(repositorioCanal);

        // ✅ SOLUCIÓN: Configurar la referencia para actualizaciones automáticas
        ((CreadorCanal) this.creadorCanal).setListadorCanales(this.listadorCanales);

        System.out.println("✅ [FachadaCanales]: Inicializada con todos los gestores (SINGLETON)");
    }

    public static synchronized FachadaCanalesImpl getInstancia() {
        if (instancia == null) {
            instancia = new FachadaCanalesImpl();
        }
        return instancia;
    }

    @Override
    public CompletableFuture<Canal> crearCanal(String nombre, String descripcion) {
        return creadorCanal.crearCanal(nombre, descripcion);
    }

    @Override
    public void registrarObservadorCreacion(IObservador observador) {
        System.out.println("🔔 [FachadaCanales]: Registrando observador en CreadorCanal");
        creadorCanal.registrarObservador(observador);
    }

    @Override
    public void solicitarCanalesUsuario() {
        listadorCanales.solicitarCanalesUsuario();
    }

    @Override
    public List<Canal> obtenerCanalesCache() {
        return listadorCanales.getCanales();
    }

    @Override
    public void registrarObservadorListado(IObservador observador) {
        System.out.println("🔔 [FachadaCanales]: Registrando observador en ListadorCanales");
        listadorCanales.registrarObservador(observador);
    }

    @Override
    public void solicitarHistorialCanal(String canalId, int limite) {
        gestorMensajes.solicitarHistorialCanal(canalId, limite);
    }

    @Override
    public CompletableFuture<Void> enviarMensajeTexto(String canalId, String contenido) {
        return gestorMensajes.enviarMensajeTexto(canalId, contenido);
    }

    @Override
    public CompletableFuture<Void> enviarMensajeAudio(String canalId, String audioFileId) {
        return gestorMensajes.enviarMensajeAudio(canalId, audioFileId);
    }

    @Override
    public CompletableFuture<Void> enviarArchivo(String canalId, String fileId) {
        return gestorMensajes.enviarArchivo(canalId, fileId);
    }

    @Override
    public void registrarObservadorMensajes(IObservador observador) {
        System.out.println("🔔 [FachadaCanales]: Registrando observador en GestorMensajes");
        gestorMensajes.registrarObservador(observador);
    }

    @Override
    public void inicializarManejadoresMensajes() {
        gestorMensajes.inicializarManejadores();
    }

    @Override
    public CompletableFuture<Void> invitarMiembro(String canalId, String contactoId) {
        return invitadorMiembro.invitarMiembro(canalId, contactoId);
    }

    @Override
    public void solicitarMiembrosCanal(String canalId) {
        listadorMiembros.solicitarMiembros(canalId);
    }

    @Override
    public void registrarObservadorMiembros(IObservador observador) {
        System.out.println("🔔 [FachadaCanales]: Registrando observador en ListadorMiembros");
        listadorMiembros.registrarObservador(observador);
    }
}
