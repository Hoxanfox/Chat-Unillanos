package com.arquitectura.utils.network;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

@Component
public class NetworkUtils {

    /**
     * Obtiene la dirección IP local del servidor (no localhost/127.0.0.1).
     * Busca la primera interfaz de red activa que no sea loopback.
     * 
     * @return La dirección IP del servidor o "127.0.0.1" si no se encuentra ninguna
     */
    public String getServerIPAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                
                // Ignorar interfaces que están deshabilitadas o son loopback
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                
                // NUEVO: Ignorar interfaces de Docker y VirtualBox
                String interfaceName = networkInterface.getName().toLowerCase();
                String displayName = networkInterface.getDisplayName().toLowerCase();
                if (interfaceName.contains("docker") || interfaceName.contains("veth") || 
                    displayName.contains("docker") || displayName.contains("virtualbox") ||
                    interfaceName.startsWith("vEthernet") || interfaceName.contains("wsl")) {
                    System.out.println("[NetworkUtils] Ignorando interfaz virtual: " + networkInterface.getDisplayName());
                    continue;
                }
                
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    
                    // Buscamos direcciones IPv4 que no sean loopback ni link-local
                    if (!inetAddress.isLoopbackAddress() 
                            && !inetAddress.isLinkLocalAddress() 
                            && inetAddress.isSiteLocalAddress()) {
                        
                        String ipAddress = inetAddress.getHostAddress();
                        
                        // NUEVO: Ignorar redes de Docker (172.17-32.x.x) y otras virtuales
                        if (ipAddress.startsWith("172.1") || ipAddress.startsWith("172.2") || 
                            ipAddress.startsWith("172.3") || ipAddress.startsWith("169.254")) {
                            System.out.println("[NetworkUtils] Ignorando IP de red virtual Docker/Link-local: " + ipAddress);
                            continue;
                        }
                        
                        System.out.println("[NetworkUtils] Dirección IP del servidor detectada: " + ipAddress + 
                                         " en interfaz " + networkInterface.getDisplayName());
                        return ipAddress;
                    }
                }
            }
            
            // Si no encontramos ninguna IP específica, intentamos obtener la IP por hostname
            InetAddress localhost = InetAddress.getLocalHost();
            if (!localhost.isLoopbackAddress()) {
                String ipAddress = localhost.getHostAddress();
                System.out.println("[NetworkUtils] Dirección IP del servidor obtenida por hostname: " + ipAddress);
                return ipAddress;
            }
            
        } catch (SocketException e) {
            System.err.println("[NetworkUtils] Error al enumerar interfaces de red: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[NetworkUtils] Error al obtener la dirección IP del servidor: " + e.getMessage());
        }
        
        System.out.println("[NetworkUtils] No se pudo detectar la IP del servidor. Usando 127.0.0.1 por defecto.");
        return "127.0.0.1";
    }
    
    /**
     * Obtiene la dirección IP pública del servidor si está disponible.
     * En entornos locales, retorna la IP local.
     * 
     * @return La dirección IP pública o local del servidor
     */
    public String getPublicOrLocalIPAddress() {
        // Por ahora retornamos la IP local
        // En el futuro se podría implementar detección de IP pública consultando servicios externos
        return getServerIPAddress();
    }
}