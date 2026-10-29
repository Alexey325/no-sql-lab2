package com.example.nosqllab2.exception.custom;

public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(Class<?> entityType, Object id) {
        super(entityType.getSimpleName() + " with id " + id + " not found");
    }
}
