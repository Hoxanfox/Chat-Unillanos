package gestorP2P.servicios.sincronizacion;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import conexion.p2p.interfaces.IGestorConexiones;
import dominio.clienteServidor.Archivo;
import dominio.clienteServidor.Canal;
import dominio.clienteServidor.Mensaje;
import dominio.clienteServidor.Usuario;
import dominio.clienteServidor.relaciones.CanalMiembro;
import dominio.merkletree.IMerkleEntity;
import dto.comunicacion.DTORequest;
import logger.LoggerCentral;
import repositorio.clienteServidor.*;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fase 5: Comparación de Contenido
 *
 * Responsabilidad: Resolver conflictos cuando los IDs coinciden pero los hashes difieren.
 * Compara campo por campo y decide qué versión conservar basándose en timestamps.
 */
public class Fase5ComparacionContenido {

    private static final String TAG = "Fase5-Contenido";
    private static final String AMARILLO = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String VERDE = "\u001B[32m";
    private static final String ROJO = "\u001B[31m";
    private static final String AZUL = "\u001B[34m";
    private static final String RESET = "\u001B[0m";

    private final IGestorConexiones gestor;
    private final Gson gson;

    private final UsuarioRepositorio repoUsuario;
    private final CanalRepositorio repoCanal;
    private final CanalMiembroRepositorio repoMiembro;
    private final MensajeRepositorio repoMensaje;
    private final ArchivoRepositorio repoArchivo;

    // Control de comparaciones pendientes
    private final AtomicInteger comparacionesPendientes = new AtomicInteger(0);
    private volatile String tipoEnComparacion = null;

    public Fase5ComparacionContenido(IGestorConexiones gestor, Gson gson) {
        this.gestor = gestor;
        this.gson = gson;
        this.repoUsuario = new UsuarioRepositorio();
        this.repoCanal = new CanalRepositorio();
        this.repoMiembro = new CanalMiembroRepositorio();
        this.repoMensaje = new MensajeRepositorio();
        this.repoArchivo = new ArchivoRepositorio();
    }

    /**
     * Inicia la comparación de contenido para todas las entidades de un tipo.
     */
    public void iniciarComparaciones(String tipo, JsonArray idsRemotos) {
        int cantidad = idsRemotos.size();
        comparacionesPendientes.set(cantidad);
        tipoEnComparacion = tipo;

        LoggerCentral.info(TAG, CYAN + String.format("🔍 Iniciando %d comparaciones de contenido para %s",
            cantidad, tipo) + RESET);

        for (JsonElement el : idsRemotos) {
            String idRemoto = el.getAsString();
            solicitarParaComparacion(tipo, idRemoto);
        }
    }

    /**
     * Solicita una entidad específica para comparación de contenido.
     */
    private void solicitarParaComparacion(String tipo, String id) {
        LoggerCentral.info(TAG, AZUL + String.format("Solicitando %s ID: %s para comparación", tipo, id) + RESET);

        JsonObject payload = new JsonObject();
        payload.addProperty("tipo", tipo);
        payload.addProperty("id", id);
        payload.addProperty("compararContenido", true);

        DTORequest req = new DTORequest("sync_compare_entity", payload);
        String jsonReq = gson.toJson(req);

        gestor.broadcast(jsonReq);
    }

    /**
     * Compara una entidad remota con la local y resuelve conflictos.
     *
     * @return true si hubo cambios, false si no
     */
    public boolean compararYResolver(String tipo, JsonElement dataRemota) {
        boolean huboCambios = false;

        try {
            LoggerCentral.info(TAG, AZUL + "=== Comparando " + tipo + " ===" + RESET);

            switch (tipo) {
                case "USUARIO":
                    Usuario usuarioRemoto = gson.fromJson(dataRemota, Usuario.class);
                    huboCambios = compararUsuario(usuarioRemoto);
                    break;

                case "CANAL":
                    Canal canalRemoto = gson.fromJson(dataRemota, Canal.class);
                    huboCambios = compararCanal(canalRemoto);
                    break;

                case "MIEMBRO":
                    CanalMiembro miembroRemoto = gson.fromJson(dataRemota, CanalMiembro.class);
                    huboCambios = compararMiembro(miembroRemoto);
                    break;

                case "MENSAJE":
                    Mensaje mensajeRemoto = gson.fromJson(dataRemota, Mensaje.class);
                    huboCambios = compararMensaje(mensajeRemoto);
                    break;

                case "ARCHIVO":
                    Archivo archivoRemoto = gson.fromJson(dataRemota, Archivo.class);
                    huboCambios = compararArchivo(archivoRemoto);
                    break;

                default:
                    LoggerCentral.warn(TAG, AMARILLO + "Tipo no soportado para comparación: " + tipo + RESET);
            }

        } catch (Exception e) {
            LoggerCentral.error(TAG, ROJO + "Error comparando " + tipo + ": " + e.getMessage() + RESET);
            e.printStackTrace();
        }

        return huboCambios;
    }

    /**
     * Compara dos usuarios campo por campo.
     */
    private boolean compararUsuario(Usuario remoto) {
        Usuario local = repoUsuario.buscarPorId(remoto.getId());

        if (local == null) {
            LoggerCentral.warn(TAG, AMARILLO + "Usuario no existe localmente. Guardando..." + RESET);
            repoUsuario.guardar(remoto);
            return true;
        }

        boolean hayDiferencias = false;

        // Comparar campos
        if (!local.getNombre().equals(remoto.getNombre())) {
            LoggerCentral.warn(TAG, AMARILLO + "  Diferencia en NOMBRE" + RESET);
            LoggerCentral.warn(TAG, "    Local: " + local.getNombre());
            LoggerCentral.warn(TAG, "    Remoto: " + remoto.getNombre());
            hayDiferencias = true;
        }

        if (!local.getEmail().equals(remoto.getEmail())) {
            LoggerCentral.warn(TAG, AMARILLO + "  Diferencia en EMAIL" + RESET);
            hayDiferencias = true;
        }

        if (!Objects.equals(local.getFoto(), remoto.getFoto())) {
            LoggerCentral.warn(TAG, AMARILLO + "  Diferencia en FOTO" + RESET);
            hayDiferencias = true;
        }

        if (!Objects.equals(local.getContrasena(), remoto.getContrasena())) {
            LoggerCentral.warn(TAG, AMARILLO + "  Diferencia en CONTRASEÑA" + RESET);
            hayDiferencias = true;
        }

        if (hayDiferencias) {
            return resolverConflictoTemporal(
                local.getFechaCreacion(),
                remoto.getFechaCreacion(),
                () -> repoUsuario.guardar(remoto),
                "Usuario"
            );
        }

        LoggerCentral.debug(TAG, VERDE + "  ✓ Usuario idéntico" + RESET);
        return false;
    }

    /**
     * Compara canales campo por campo.
     */
    private boolean compararCanal(Canal remoto) {
        // Buscar en la lista de entidades
        List<? extends IMerkleEntity> canales = repoCanal.obtenerTodosParaSync();
        Canal local = (Canal) canales.stream()
            .filter(c -> c.getId().equals(remoto.getId()))
            .findFirst()
            .orElse(null);

        if (local == null) {
            LoggerCentral.warn(TAG, AMARILLO + "Canal no existe localmente. Guardando..." + RESET);
            repoCanal.guardar(remoto);
            return true;
        }

        boolean hayDiferencias = false;

        // Comparar solo campos disponibles
        if (!local.getNombre().equals(remoto.getNombre())) {
            LoggerCentral.warn(TAG, AMARILLO + "  Diferencia en NOMBRE" + RESET);
            LoggerCentral.warn(TAG, "    Local: " + local.getNombre());
            LoggerCentral.warn(TAG, "    Remoto: " + remoto.getNombre());
            hayDiferencias = true;
        }

        // Si hay diferencias, resolver por timestamp
        if (hayDiferencias) {
            return resolverConflictoTemporal(
                local.getFechaCreacion(),
                remoto.getFechaCreacion(),
                () -> repoCanal.guardar(remoto),
                "Canal"
            );
        }

        LoggerCentral.debug(TAG, VERDE + "  ✓ Canal idéntico" + RESET);
        return false;
    }

    /**
     * Compara miembros campo por campo.
     */
    private boolean compararMiembro(CanalMiembro remoto) {
        // Buscar en la lista de entidades
        List<? extends IMerkleEntity> miembros = repoMiembro.obtenerTodosParaSync();
        CanalMiembro local = (CanalMiembro) miembros.stream()
            .filter(m -> m.getId().equals(remoto.getId()))
            .findFirst()
            .orElse(null);

        if (local == null) {
            LoggerCentral.warn(TAG, AMARILLO + "Miembro no existe localmente. Guardando..." + RESET);
            repoMiembro.guardar(remoto);
            return true;
        }

        boolean hayDiferencias = false;

        // Comparar campos disponibles
        if (!Objects.equals(local.getUsuarioId(), remoto.getUsuarioId())) {
            LoggerCentral.warn(TAG, AMARILLO + "  Diferencia en USUARIO_ID" + RESET);
            hayDiferencias = true;
        }

        if (!Objects.equals(local.getCanalId(), remoto.getCanalId())) {
            LoggerCentral.warn(TAG, AMARILLO + "  Diferencia en CANAL_ID" + RESET);
            hayDiferencias = true;
        }

        // Si hay diferencias, simplemente guardar el remoto (ya que no hay timestamp)
        if (hayDiferencias) {
            LoggerCentral.warn(TAG, ROJO + "  ⚠ Actualizando con versión remota..." + RESET);
            repoMiembro.guardar(remoto);
            return true;
        }

        LoggerCentral.debug(TAG, VERDE + "  ✓ Miembro idéntico" + RESET);
        return false;
    }

    /**
     * Compara mensajes campo por campo.
     */
    private boolean compararMensaje(Mensaje remoto) {
        // Buscar en la lista de entidades
        List<? extends IMerkleEntity> mensajes = repoMensaje.obtenerTodosParaSync();
        Mensaje local = (Mensaje) mensajes.stream()
            .filter(m -> m.getId().equals(remoto.getId()))
            .findFirst()
            .orElse(null);

        if (local == null) {
            LoggerCentral.warn(TAG, AMARILLO + "Mensaje no existe localmente. Guardando..." + RESET);
            repoMensaje.guardar(remoto);
            return true;
        }

        boolean hayDiferencias = false;

        // Comparar contenido
        if (!local.getContenido().equals(remoto.getContenido())) {
            LoggerCentral.warn(TAG, AMARILLO + "  Diferencia en CONTENIDO" + RESET);
            LoggerCentral.warn(TAG, "    Local: " + local.getContenido().substring(0, Math.min(50, local.getContenido().length())));
            LoggerCentral.warn(TAG, "    Remoto: " + remoto.getContenido().substring(0, Math.min(50, remoto.getContenido().length())));
            hayDiferencias = true;
        }

        if (!Objects.equals(local.getCanalId(), remoto.getCanalId())) {
            LoggerCentral.warn(TAG, AMARILLO + "  Diferencia en CANAL_ID" + RESET);
            hayDiferencias = true;
        }

        if (hayDiferencias) {
            return resolverConflictoTemporal(
                local.getFechaEnvio(),
                remoto.getFechaEnvio(),
                () -> repoMensaje.guardar(remoto),
                "Mensaje"
            );
        }

        LoggerCentral.debug(TAG, VERDE + "  ✓ Mensaje idéntico" + RESET);
        return false;
    }

    /**
     * Compara archivos campo por campo.
     */
    private boolean compararArchivo(Archivo remoto) {
        try {
            Archivo local = repoArchivo.buscarPorId(java.util.UUID.fromString(remoto.getId()));

            if (local == null) {
                LoggerCentral.warn(TAG, AMARILLO + "Archivo no existe localmente. Guardando..." + RESET);
                repoArchivo.guardar(remoto);
                return true;
            }

            boolean hayDiferencias = false;

            // Comparar tamaño (campo disponible)
            if (local.getTamanio() != remoto.getTamanio()) {
                LoggerCentral.warn(TAG, AMARILLO + "  Diferencia en TAMAÑO" + RESET);
                LoggerCentral.warn(TAG, "    Local: " + local.getTamanio());
                LoggerCentral.warn(TAG, "    Remoto: " + remoto.getTamanio());
                hayDiferencias = true;
            }

            // Si hay diferencias, guardar el remoto
            if (hayDiferencias) {
                LoggerCentral.warn(TAG, ROJO + "  ⚠ Actualizando con versión remota..." + RESET);
                repoArchivo.guardar(remoto);
                return true;
            }

            LoggerCentral.debug(TAG, VERDE + "  ✓ Archivo idéntico" + RESET);
            return false;
        } catch (Exception e) {
            LoggerCentral.error(TAG, ROJO + "Error comparando archivo: " + e.getMessage() + RESET);
            return false;
        }
    }

    /**
     * Resuelve conflicto basándose en timestamps.
     * Regla: El que tiene fecha de creación más antigua (creado primero) gana.
     */
    private boolean resolverConflictoTemporal(Instant fechaLocal, Instant fechaRemota,
                                              Runnable guardarRemoto, String tipoEntidad) {
        LoggerCentral.info(TAG, AZUL + "  Comparando timestamps:" + RESET);
        LoggerCentral.info(TAG, "    Local:  " + fechaLocal);
        LoggerCentral.info(TAG, "    Remoto: " + fechaRemota);

        if (fechaRemota.isBefore(fechaLocal)) {
            LoggerCentral.warn(TAG, ROJO + "  ⚠ Versión REMOTA es más antigua. Actualizando..." + RESET);
            guardarRemoto.run();
            LoggerCentral.info(TAG, VERDE + "  ✓ " + tipoEntidad + " actualizado" + RESET);
            return true;
        } else if (fechaRemota.isAfter(fechaLocal)) {
            LoggerCentral.info(TAG, VERDE + "  ✓ Versión LOCAL es más antigua. Manteniendo local." + RESET);
            return false;
        } else {
            LoggerCentral.warn(TAG, AMARILLO + "  ⚠ Misma fecha. Manteniendo local por defecto." + RESET);
            return false;
        }
    }

    /**
     * Decrementa el contador de comparaciones pendientes.
     *
     * @return true si ya no hay comparaciones pendientes
     */
    public boolean decrementarComparacion() {
        int restantes = comparacionesPendientes.decrementAndGet();
        LoggerCentral.info(TAG, CYAN + "Comparaciones restantes: " + restantes + RESET);
        return restantes <= 0;
    }

    /**
     * Resetea el contador de comparaciones.
     */
    public void resetearComparaciones() {
        comparacionesPendientes.set(0);
        tipoEnComparacion = null;
    }

    public int getComparacionesPendientes() {
        return comparacionesPendientes.get();
    }

    public String getTipoEnComparacion() {
        return tipoEnComparacion;
    }
}
