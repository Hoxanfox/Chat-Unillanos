package com.arquitectura.fachada;

import com.arquitectura.fachada.archivo.IArchivoFachada;
import com.arquitectura.fachada.canal.ICanalFachada;
import com.arquitectura.fachada.mensaje.IMensajeFachada;
import com.arquitectura.fachada.p2p.IP2PFachada;
import com.arquitectura.fachada.sistema.ISistemaFachada;
import com.arquitectura.fachada.usuario.IUsuarioFachada;

/**
 * Fachada principal del sistema de chat.
 * Proporciona acceso a todas las fachadas especializadas del sistema.
 * 
 * Esta interfaz actúa como punto de entrada unificado, permitiendo
 * acceder a las subfachadas especializadas sin necesidad de múltiples inyecciones.
 */
public interface IChatFachada {
    
    /**
     * Obtiene la fachada de gestión de usuarios.
     * @return Fachada de usuarios
     */
    IUsuarioFachada usuarios();
    
    /**
     * Obtiene la fachada de gestión de canales.
     * @return Fachada de canales
     */
    ICanalFachada canales();
    
    /**
     * Obtiene la fachada de gestión de mensajes.
     * @return Fachada de mensajes
     */
    IMensajeFachada mensajes();
    
    /**
     * Obtiene la fachada de gestión de archivos.
     * @return Fachada de archivos
     */
    IArchivoFachada archivos();
    
    /**
     * Obtiene la fachada de operaciones P2P.
     * @return Fachada P2P
     */
    IP2PFachada p2p();
    
    /**
     * Obtiene la fachada de utilidades del sistema.
     * @return Fachada del sistema
     */
    ISistemaFachada sistema();
}