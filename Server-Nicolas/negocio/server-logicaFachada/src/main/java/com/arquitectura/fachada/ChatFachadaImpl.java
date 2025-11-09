package com.arquitectura.fachada;

import com.arquitectura.fachada.archivo.IArchivoFachada;
import com.arquitectura.fachada.canal.ICanalFachada;
import com.arquitectura.fachada.mensaje.IMensajeFachada;
import com.arquitectura.fachada.p2p.IP2PFachada;
import com.arquitectura.fachada.sistema.ISistemaFachada;
import com.arquitectura.fachada.usuario.IUsuarioFachada;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementación de la fachada principal del sistema.
 * Actúa como punto de acceso unificado a todas las fachadas especializadas.
 * 
 * Esta implementación es mucho más simple y limpia, simplemente expone
 * las fachadas especializadas sin necesidad de delegar cada método.
 */
@Component
public class ChatFachadaImpl implements IChatFachada {

    private final IUsuarioFachada usuarioFachada;
    private final ICanalFachada canalFachada;
    private final IMensajeFachada mensajeFachada;
    private final IArchivoFachada archivoFachada;
    private final IP2PFachada p2pFachada;
    private final ISistemaFachada sistemaFachada;

    @Autowired
    public ChatFachadaImpl(IUsuarioFachada usuarioFachada,
                          ICanalFachada canalFachada,
                          IMensajeFachada mensajeFachada,
                          IArchivoFachada archivoFachada,
                          IP2PFachada p2pFachada,
                          ISistemaFachada sistemaFachada) {
        this.usuarioFachada = usuarioFachada;
        this.canalFachada = canalFachada;
        this.mensajeFachada = mensajeFachada;
        this.archivoFachada = archivoFachada;
        this.p2pFachada = p2pFachada;
        this.sistemaFachada = sistemaFachada;
    }

    @Override
    public IUsuarioFachada usuarios() {
        return usuarioFachada;
    }

    @Override
    public ICanalFachada canales() {
        return canalFachada;
    }

    @Override
    public IMensajeFachada mensajes() {
        return mensajeFachada;
    }

    @Override
    public IArchivoFachada archivos() {
        return archivoFachada;
    }

    @Override
    public IP2PFachada p2p() {
        return p2pFachada;
    }

    @Override
    public ISistemaFachada sistema() {
        return sistemaFachada;
    }
}