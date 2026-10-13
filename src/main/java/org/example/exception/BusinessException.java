package org.example.exception;

public class BusinessException extends RuntimeException {
    private final String field;

    public BusinessException(String message) {
        this(null, message);
    }

    public BusinessException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
