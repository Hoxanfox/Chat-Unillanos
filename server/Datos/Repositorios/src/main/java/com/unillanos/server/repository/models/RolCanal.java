package com.unillanos.server.repository.models;

/**
 * Enum que representa los posibles roles de un usuario en un canal.
 */
public enum RolCanal {
    ADMIN,    // Administrador del canal (puede agregar/remover miembros, cambiar roles)
    MEMBER;   // Miembro regular del canal
    
    /**
     * Convierte un String a RolCanal.
     * Si el String no es válido, retorna MEMBER por defecto.
     *
     * @param rol String con el nombre del rol
     * @return RolCanal correspondiente o MEMBER si no es válido
     */
    public static RolCanal fromString(String rol) {
        if (rol == null || rol.trim().isEmpty()) {
            return MEMBER; // Por defecto
        }
        
        try {
            return RolCanal.valueOf(rol.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MEMBER; // Por defecto si el valor no es válido
        }
    }
}

