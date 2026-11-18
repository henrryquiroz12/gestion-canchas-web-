package com.canchas.web.exception;

public class DuplicateDataException extends RuntimeException {
    private final String field;

    public DuplicateDataException(String message) {
        this("global", message);
    }

    public DuplicateDataException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() { return field; }
}
