package com.unillanos.server.service.impl;

import com.unillanos.server.repository.interfaces.ILogRepository;
import com.unillanos.server.entity.LogEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servicio de logging centralizado.
 * Registra eventos en consola (SLF4J) Y en base de datos de forma asíncrona.
 * Usa hilos virtuales de Java 21 para escritura no bloqueante.
 */
@Service
public class LoggerService {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggerService.class);
    private final ILogRepository logRepository;
    private final NotificationManager notificationManager;
    private final ExecutorService virtualThreadExecutor;
    private final java.util.List<java.util.function.Consumer<String>> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    public LoggerService(ILogRepository logRepository, NotificationManager notificationManager) {
        this.logRepository = logRepository;
        this.notificationManager = notificationManager;
        // Usar hilos virtuales de Java 21 para operaciones asíncronas
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Registra un evento de login.
     *
     * @param usuarioId ID del usuario
     * @param ipAddress Dirección IP
     */
    public void logLogin(String usuarioId, String ipAddress) {
        logger.info("LOGIN - Usuario: {}, IP: {}", usuarioId, ipAddress);
        persistLogAsync("LOGIN", usuarioId, ipAddress, "login", "Usuario autenticado exitosamente");
        
        // Notificar a clientes GUI suscritos
        notificationManager.notificar("LOG", Map.of(
            "tipo", "LOGIN",
            "usuarioId", usuarioId,
            "ipAddress", ipAddress,
            "mensaje", "Usuario autenticado exitosamente"
        ));
    }

    /**
     * Registra un evento de logout.
     *
     * @param usuarioId ID del usuario
     * @param ipAddress Dirección IP
     */
    public void logLogout(String usuarioId, String ipAddress) {
        logger.info("LOGOUT - Usuario: {}, IP: {}", usuarioId, ipAddress);
        persistLogAsync("LOGOUT", usuarioId, ipAddress, "logout", "Usuario desconectado");
        
        // Notificar a clientes GUI suscritos
        notificationManager.notificar("LOG", Map.of(
            "tipo", "LOGOUT",
            "usuarioId", usuarioId,
            "ipAddress", ipAddress,
            "mensaje", "Usuario desconectado"
        ));
    }

    /**
     * Registra un error.
     *
     * @param accion Acción que causó el error
     * @param detalles Detalles del error
     */
    public void logError(String accion, String detalles) {
        logger.error("ERROR - Acción: {}, Detalles: {}", accion, detalles);
        persistLogAsync("ERROR", null, null, accion, detalles);
    }

    /**
     * Registra información general.
     *
     * @param accion Acción realizada
     * @param detalles Detalles de la acción
     */
    public void logInfo(String accion, String detalles) {
        logger.info("INFO - Acción: {}, Detalles: {}", accion, detalles);
        persistLogAsync("INFO", null, null, accion, detalles);
    }

    /**
     * Registra un evento del sistema.
     *
     * @param accion Acción del sistema
     * @param detalles Detalles de la acción
     */
    public void logSystem(String accion, String detalles) {
        logger.info("SYSTEM - Acción: {}, Detalles: {}", accion, detalles);
        persistLogAsync("SYSTEM", null, null, accion, detalles);
        
        // Notificar a clientes GUI suscritos
        notificationManager.notificar("LOG", Map.of(
            "tipo", "SYSTEM",
            "accion", accion,
            "detalles", detalles,
            "mensaje", "Evento del sistema"
        ));
    }

    /**
     * Persiste un log en la base de datos de forma asíncrona usando hilos virtuales.
     * No bloquea el hilo principal.
     *
     * @param tipo Tipo de log (LOGIN, LOGOUT, ERROR, INFO, SYSTEM)
     * @param usuarioId ID del usuario (puede ser null)
     * @param ipAddress Dirección IP (puede ser null)
     * @param accion Acción realizada
     * @param detalles Detalles de la acción
     */
    private void persistLogAsync(String tipo, String usuarioId, String ipAddress, String accion, String detalles) {
        // Ejecutar en hilo virtual para no bloquear
        virtualThreadExecutor.submit(() -> {
            try {
                LogEntity log = new LogEntity(
                    LocalDateTime.now(),
                    tipo,
                    usuarioId,
                    ipAddress,
                    accion,
                    detalles
                );
                logRepository.save(log);
                    notifyListeners(String.format("%s [%s] %s - %s", log.getTimestamp(), tipo, accion, detalles));
            } catch (Exception e) {
                // Solo registrar en consola si falla la persistencia
                logger.error("Error al persistir log en BD: tipo={}, accion={}", tipo, accion, e);
            }
        });
    }

    public void addListener(java.util.function.Consumer<String> listener) {
        listeners.add(listener);
    }

    public void removeListener(java.util.function.Consumer<String> listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(String line) {
        for (var l : listeners) {
            try {
                l.accept(line);
            } catch (Exception ignored) {}
        }
    }
}

