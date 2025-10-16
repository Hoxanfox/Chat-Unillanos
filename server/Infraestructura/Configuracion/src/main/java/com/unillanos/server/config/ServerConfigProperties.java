package com.unillanos.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de configuración centralizadas para el servidor Chat-Unillanos.
 * Elimina números mágicos del código y proporciona configuración tipo-segura.
 */
@ConfigurationProperties(prefix = "server.chat")
public class ServerConfigProperties {
    
    private Netty netty = new Netty();
    private Archivos archivos = new Archivos();
    private Mensajeria mensajeria = new Mensajeria();
    private Seguridad seguridad = new Seguridad();
    
    // Getters y Setters
    public Netty getNetty() {
        return netty;
    }
    
    public void setNetty(Netty netty) {
        this.netty = netty;
    }
    
    public Archivos getArchivos() {
        return archivos;
    }
    
    public void setArchivos(Archivos archivos) {
        this.archivos = archivos;
    }
    
    public Mensajeria getMensajeria() {
        return mensajeria;
    }
    
    public void setMensajeria(Mensajeria mensajeria) {
        this.mensajeria = mensajeria;
    }
    
    public Seguridad getSeguridad() {
        return seguridad;
    }
    
    public void setSeguridad(Seguridad seguridad) {
        this.seguridad = seguridad;
    }
    
    /**
     * Configuración de Netty
     */
    public static class Netty {
        private int port = 8080;
        private int bossThreads = 1;
        private int workerThreads = 4;
        
        public int getPort() {
            return port;
        }
        
        public void setPort(int port) {
            this.port = port;
        }
        
        public int getBossThreads() {
            return bossThreads;
        }
        
        public void setBossThreads(int bossThreads) {
            this.bossThreads = bossThreads;
        }
        
        public int getWorkerThreads() {
            return workerThreads;
        }
        
        public void setWorkerThreads(int workerThreads) {
            this.workerThreads = workerThreads;
        }
    }
    
    /**
     * Configuración de archivos
     */
    public static class Archivos {
        private String directorioBase = "./uploads";
        private long maxTamanoImagen = 10485760; // 10 MB
        private long maxTamanoVideo = 52428800; // 50 MB
        private long maxTamanoAudio = 20971520; // 20 MB
        private long maxTamanoDocumento = 10485760; // 10 MB
        private int chunkSize = 2097152; // 2 MB
        private String directorioTemp = "./uploads/temp";
        private String directorioFinal = "./uploads/final";
        private int sesionExpiracionHoras = 24;
        
        public String getDirectorioBase() {
            return directorioBase;
        }
        
        public void setDirectorioBase(String directorioBase) {
            this.directorioBase = directorioBase;
        }
        
        public long getMaxTamanoImagen() {
            return maxTamanoImagen;
        }
        
        public void setMaxTamanoImagen(long maxTamanoImagen) {
            this.maxTamanoImagen = maxTamanoImagen;
        }
        
        public long getMaxTamanoVideo() {
            return maxTamanoVideo;
        }
        
        public void setMaxTamanoVideo(long maxTamanoVideo) {
            this.maxTamanoVideo = maxTamanoVideo;
        }
        
        public long getMaxTamanoAudio() {
            return maxTamanoAudio;
        }
        
        public void setMaxTamanoAudio(long maxTamanoAudio) {
            this.maxTamanoAudio = maxTamanoAudio;
        }
        
        public long getMaxTamanoDocumento() {
            return maxTamanoDocumento;
        }
        
        public void setMaxTamanoDocumento(long maxTamanoDocumento) {
            this.maxTamanoDocumento = maxTamanoDocumento;
        }
        
        public int getChunkSize() {
            return chunkSize;
        }
        
        public void setChunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
        }
        
        public String getDirectorioTemp() {
            return directorioTemp;
        }
        
        public void setDirectorioTemp(String directorioTemp) {
            this.directorioTemp = directorioTemp;
        }
        
        public String getDirectorioFinal() {
            return directorioFinal;
        }
        
        public void setDirectorioFinal(String directorioFinal) {
            this.directorioFinal = directorioFinal;
        }
        
        public int getSesionExpiracionHoras() {
            return sesionExpiracionHoras;
        }
        
        public void setSesionExpiracionHoras(int sesionExpiracionHoras) {
            this.sesionExpiracionHoras = sesionExpiracionHoras;
        }
    }
    
    /**
     * Configuración de mensajería
     */
    public static class Mensajeria {
        private int maxLongitudContenido = 5000;
        private int historialLimitePorDefecto = 50;
        
        public int getMaxLongitudContenido() {
            return maxLongitudContenido;
        }
        
        public void setMaxLongitudContenido(int maxLongitudContenido) {
            this.maxLongitudContenido = maxLongitudContenido;
        }
        
        public int getHistorialLimitePorDefecto() {
            return historialLimitePorDefecto;
        }
        
        public void setHistorialLimitePorDefecto(int historialLimitePorDefecto) {
            this.historialLimitePorDefecto = historialLimitePorDefecto;
        }
    }
    
    /**
     * Configuración de seguridad
     */
    public static class Seguridad {
        private int bcryptStrength = 12;
        private int longitudMinPassword = 8;
        
        public int getBcryptStrength() {
            return bcryptStrength;
        }
        
        public void setBcryptStrength(int bcryptStrength) {
            this.bcryptStrength = bcryptStrength;
        }
        
        public int getLongitudMinPassword() {
            return longitudMinPassword;
        }
        
        public void setLongitudMinPassword(int longitudMinPassword) {
            this.longitudMinPassword = longitudMinPassword;
        }
    }
}
