package com.example.demo.config;

/** Keycloak could not be reached or answered unexpectedly; mapped to 503 without leaking details. */
public class IdentidadNoDisponibleException extends RuntimeException {
    public IdentidadNoDisponibleException(String message, Throwable cause) {
        super(message, cause);
    }

    public IdentidadNoDisponibleException(String message) {
        super(message);
    }
}
