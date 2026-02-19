package com.safewatch.exceptions;

public class InvalidIncidentException extends RuntimeException {
    public InvalidIncidentException(String message) {
        super(message);
    }
}
