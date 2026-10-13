package org.example.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource) {
        super(resource + " nie istnieje.");
    }
}
