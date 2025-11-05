package com.unillanos.server.exception;

/**
 * Excepción lanzada cuando un recurso solicitado no se encuentra.
 * Por ejemplo: usuario no encontrado, canal no encontrado, etc.
 */
public class NotFoundException extends ChatServerException {
    
    private final String resourceType;
    private final String resourceId;

    public NotFoundException(String resourceType, String resourceId) {
        super(String.format("%s no encontrado: %s", resourceType, resourceId), 
              "NOT_FOUND", 
              resourceId);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public NotFoundException(String message, String resourceType, String resourceId) {
        super(message, "NOT_FOUND", resourceId);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    @Override
    public String toString() {
        return String.format("NotFoundException[type=%s, id=%s, message=%s]", 
                resourceType, resourceId, getMessage());
    }
}

