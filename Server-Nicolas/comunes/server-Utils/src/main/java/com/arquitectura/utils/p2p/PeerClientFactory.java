package com.arquitectura.utils.p2p;

import com.google.gson.Gson;

/**
 * Factory para crear instancias de PeerClient y PeerConnectionPool
 * con configuraciones predefinidas.
 */
public class PeerClientFactory {

    private static PeerConnectionPool poolInstance;
    private static final Object lock = new Object();

    /**
     * Crea un nuevo PeerClient con configuración por defecto.
     *
     * @return Nueva instancia de PeerClient
     */
    public static PeerClient crearCliente() {
        return new PeerClient();
    }

    /**
     * Crea un nuevo PeerClient con Gson personalizado.
     *
     * @param gson Instancia de Gson a usar
     * @return Nueva instancia de PeerClient
     */
    public static PeerClient crearCliente(Gson gson) {
        return new PeerClient(gson);
    }

    /**
     * Obtiene la instancia singleton del PeerConnectionPool.
     * Si no existe, la crea con configuración por defecto.
     *
     * @return Instancia singleton del pool
     */
    public static PeerConnectionPool obtenerPool() {
        if (poolInstance == null) {
            synchronized (lock) {
                if (poolInstance == null) {
                    poolInstance = new PeerConnectionPool();
                }
            }
        }
        return poolInstance;
    }

    /**
     * Obtiene o crea el pool con un número específico de threads.
     *
     * @param maxThreads Número máximo de threads
     * @return Instancia del pool
     */
    public static PeerConnectionPool obtenerPool(int maxThreads) {
        if (poolInstance == null) {
            synchronized (lock) {
                if (poolInstance == null) {
                    poolInstance = new PeerConnectionPool(maxThreads);
                }
            }
        }
        return poolInstance;
    }

    /**
     * Reinicia el pool (cierra el actual y crea uno nuevo).
     *
     * @param maxThreads Número máximo de threads para el nuevo pool
     * @return Nueva instancia del pool
     */
    public static PeerConnectionPool reiniciarPool(int maxThreads) {
        synchronized (lock) {
            if (poolInstance != null) {
                poolInstance.cerrar();
            }
            poolInstance = new PeerConnectionPool(maxThreads);
            return poolInstance;
        }
    }

    /**
     * Cierra el pool actual si existe.
     */
    public static void cerrarPool() {
        synchronized (lock) {
            if (poolInstance != null) {
                poolInstance.cerrar();
                poolInstance = null;
            }
        }
    }

    /**
     * Verifica si el pool está inicializado.
     *
     * @return true si el pool existe
     */
    public static boolean poolInicializado() {
        return poolInstance != null;
    }
}
