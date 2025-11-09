package com.arquitectura.fachada.sistema;

import com.arquitectura.utils.logs.ILogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Implementación de la fachada del sistema.
 * Coordina las operaciones de utilidades y sistema.
 */
@Component
public class SistemaFachadaImpl implements ISistemaFachada {

    private final ILogService logService;

    @Autowired
    public SistemaFachadaImpl(ILogService logService) {
        this.logService = logService;
    }

    @Override
    public String getLogContents() throws IOException {
        return logService.getLogContents();
    }
}

