package servicio.contactos;

import dto.featureContactos.DTOContacto;
import fachada.FachadaGeneralImpl;
import observador.IObservador;
import fachada.gestionContactos.contactos.IFachadaContactos;
import servicio.archivos.IServicioArchivos;
import servicio.archivos.ServicioArchivosImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación del servicio de contactos con logging detallado.
 * Sincroniza contactos con la BD (vía Fachada) y descarga fotos automáticamente.
 */
public class ServicioContactosImpl implements IServicioContactos, IObservador {

    private final List<IObservador> observadores = new ArrayList<>();
    private final IFachadaContactos fachadaContactos;
    private final IServicioArchivos servicioArchivos;
    private List<DTOContacto> contactosCache = new ArrayList<>();

    public ServicioContactosImpl() {
        this.fachadaContactos = FachadaGeneralImpl.getInstancia().getFachadaContactos();
        this.servicioArchivos = new ServicioArchivosImpl();

        System.out.println("✅ [ServicioContactos]: Creado. Registrándose como observador en la FachadaContactos.");
        this.fachadaContactos.registrarObservador(this);
    }

    @Override
    public List<DTOContacto> getContactos() {
        System.out.println("ℹ️ [ServicioContactos]: getContactos() llamado. Devolviendo " + contactosCache.size() + " contactos desde la caché.");
        return new ArrayList<>(contactosCache);
    }

    @Override
    public void solicitarActualizacionContactos() {
        System.out.println("➡️ [ServicioContactos]: Delegando solicitud de actualización de contactos a la Fachada.");
        fachadaContactos.solicitarActualizacionContactos();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void actualizar(String tipoDeDato, Object datos) {
        System.out.println("🔔 [ServicioContactos]: ¡Notificación recibida de la Fachada! Tipo: " + tipoDeDato);
        if ("ACTUALIZAR_CONTACTOS".equals(tipoDeDato) && datos instanceof List) {
            this.contactosCache = (List<DTOContacto>) datos;
            System.out.println("  -> Caché de contactos actualizada con " + this.contactosCache.size() + " contactos.");

            // Sincronizar contactos con la base de datos (delegando a la Fachada)
            fachadaContactos.sincronizarContactosConBD(this.contactosCache);

            // Descargar fotos faltantes en segundo plano
            descargarFotosFaltantes(this.contactosCache);

            // Notificar a los observadores (controladores/vistas)
            notificarObservadores(tipoDeDato, this.contactosCache);
        } else {
            System.out.println("  -> La notificación no es para actualizar contactos o los datos son inválidos.");
        }
    }

    /**
     * Descarga las fotos faltantes de los contactos en segundo plano.
     * Reutiliza el servicio de archivos existente que verifica cache antes de descargar.
     */
    private void descargarFotosFaltantes(List<DTOContacto> contactos) {
        System.out.println("📸 [ServicioContactos]: Verificando y descargando fotos de contactos...");
        System.out.println("📸 [ServicioContactos]: Total de contactos a procesar: " + contactos.size());

        int fotosPendientes = 0;
        int sinFoto = 0;

        for (DTOContacto contacto : contactos) {
            String photoId = contacto.getPhotoId();

            System.out.println("📸 [ServicioContactos]: Procesando contacto " + contacto.getNombre() + " - photoId: " + (photoId != null ? photoId : "NULL"));

            if (photoId == null || photoId.isEmpty()) {
                sinFoto++;
                System.out.println("  ⚠️ Contacto " + contacto.getNombre() + " no tiene photoId definido");
                continue; // Este contacto no tiene foto
            }

            fotosPendientes++;

            // ✅ PROTECCIÓN: Primero verificar si ya existe localmente
            servicioArchivos.existeLocalmente(photoId)
                .thenCompose(existe -> {
                    if (existe) {
                        System.out.println("  ✓ Foto ya existe para contacto " + contacto.getNombre() + ", obteniendo ruta...");
                        return servicioArchivos.obtenerRutaLocal(photoId);
                    } else {
                        System.out.println("  ⬇️ Descargando foto para contacto " + contacto.getNombre() + "...");
                        return servicioArchivos.obtenerArchivoPorFileId(photoId);
                    }
                })
                .thenAccept(file -> {
                    if (file != null && file.exists()) {
                        System.out.println("  ✅ Foto lista para contacto " + contacto.getNombre() + ": " + file.getAbsolutePath());

                        // Actualizar el DTO con la ruta local de la foto
                        contacto.setLocalPhotoPath(file.getAbsolutePath());

                        // Notificar a los observadores que la foto de este contacto está lista
                        notificarObservadores("CONTACT_PHOTO_READY", contacto);
                    } else {
                        System.out.println("  ⚠️ No se pudo obtener foto para contacto " + contacto.getNombre() + " (photoId: " + photoId + ")");
                    }
                })
                .exceptionally(ex -> {
                    // ✅ PROTECCIÓN: No mostrar error si simplemente no existe el archivo
                    String mensaje = ex.getMessage();
                    if (mensaje != null && (mensaje.contains("no encontrado") || mensaje.contains("not found"))) {
                        System.out.println("  ℹ️ Foto no disponible para contacto " + contacto.getNombre() + " (photoId: " + photoId + ")");
                    } else {
                        System.err.println("  ❌ Error al obtener foto para contacto " + contacto.getNombre() + ": " + mensaje);
                    }
                    return null;
                });
        }

        if (fotosPendientes > 0) {
            System.out.println("📸 [ServicioContactos]: " + fotosPendientes + " fotos en proceso de verificación/descarga");
        } else {
            System.out.println("📸 [ServicioContactos]: No hay fotos para procesar (sin foto: " + sinFoto + ")");
        }
    }

    @Override
    public void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
            System.out.println("➕ [ServicioContactos]: Nuevo observador (Controlador/Vista) registrado. Total: " + observadores.size());
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        if(observadores.remove(observador)) {
            System.out.println("➖ [ServicioContactos]: Observador (Controlador/Vista) removido. Total: " + observadores.size());
        }
    }

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        System.out.println("📣 [ServicioContactos]: Notificando a " + observadores.size() + " observador(es) (Controlador/Vista)...");
        for (IObservador observador : observadores) {
            observador.actualizar(tipoDeDato, datos);
        }
    }
}
