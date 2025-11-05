package com.unillanos.server.exception;

/**
 * Excepción lanzada cuando se intenta crear un recurso que ya existe.
 * Por ejemplo: usuario con email duplicado, canal con nombre duplicado, etc.
 */
public class DuplicateResourceException extends ChatServerException {
    
    private final String resourceType;
    private final String duplicateField;
    private final String duplicateValue;

    public DuplicateResourceException(String resourceType, String duplicateField, String duplicateValue) {
        super(String.format("%s ya existe con %s: %s", resourceType, duplicateField, duplicateValue), 
              "DUPLICATE_RESOURCE",
              duplicateValue);
        this.resourceType = resourceType;
        this.duplicateField = duplicateField;
        this.duplicateValue = duplicateValue;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getDuplicateField() {
        return duplicateField;
    }

    public String getDuplicateValue() {
        return duplicateValue;
    }

    @Override
    public String toString() {
        return String.format("DuplicateResourceException[type=%s, field=%s, value=%s]", 
                resourceType, duplicateField, duplicateValue);
    }
}

