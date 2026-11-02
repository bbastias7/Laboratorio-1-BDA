package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.Map;
import java.util.stream.Collectors;

/** Maps every failure to {"error": "..."}; DB business-rule errors are read from the SQLSTATE. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private static ResponseEntity<Map<String, String>> body(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage()).collect(Collectors.joining("; "));
        return body(HttpStatus.BAD_REQUEST, msg.isEmpty() ? "Datos inválidos" : msg);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    ResponseEntity<Map<String, String>> unreadable(Exception e) {
        return body(HttpStatus.BAD_REQUEST, "Datos inválidos");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<Map<String, String>> notFound(NoSuchElementException e) {
        return body(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, String>> conflict(IllegalStateException e) {
        return body(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(IdentidadNoDisponibleException.class)
    ResponseEntity<Map<String, String>> identidad(IdentidadNoDisponibleException e) {
        log.error("Keycloak admin error: {}", e.getMessage());
        return body(HttpStatus.SERVICE_UNAVAILABLE, "Servicio de identidad no disponible");
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Map<String, String>> denied(AccessDeniedException e) {
        return body(HttpStatus.FORBIDDEN, "No tiene permiso para esta operación");
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Map<String, String>> unauthenticated(AuthenticationException e) {
        return body(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<Map<String, String>> method(HttpRequestMethodNotSupportedException e) {
        return body(HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<Map<String, String>> noResource(NoResourceFoundException e) {
        return body(HttpStatus.NOT_FOUND, "Recurso no encontrado");
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<Map<String, String>> data(DataAccessException e) {
        return fromSql(findSql(e), e);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, String>> other(Exception e) {
        SQLException sql = findSql(e);
        if (sql != null) {
            return fromSql(sql, e);
        }
        log.error("Unhandled error", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno");
    }

    static SQLException findSql(Throwable t) {
        for (; t != null; t = t.getCause()) {
            if (t instanceof SQLException s) {
                return s;
            }
        }
        return null;
    }

    static ResponseEntity<Map<String, String>> fromSql(SQLException sql, Exception original) {
        String state = sql == null ? null : sql.getSQLState();
        if (state == null) {
            log.error("Database error without SQLSTATE", original);
            return body(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno");
        }
        return switch (state) {
            case "23514", "23505", "23503", "P0001" -> body(HttpStatus.CONFLICT, cleanMessage(sql));
            case "P0002" -> body(HttpStatus.NOT_FOUND, cleanMessage(sql));
            case "23502", "22P02" -> body(HttpStatus.BAD_REQUEST, cleanMessage(sql));
            default -> {
                log.error("Unmapped SQLSTATE {}", state, original);
                yield body(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno");
            }
        };
    }

    /** pgjdbc messages look like "ERROR: text\n  Where: ..."; keep only the first line's text. */
    static String cleanMessage(SQLException sql) {
        String m = sql.getMessage() == null ? "Error de base de datos" : sql.getMessage();
        int nl = m.indexOf('\n');
        if (nl >= 0) {
            m = m.substring(0, nl);
        }
        m = m.startsWith("ERROR: ") ? m.substring(7) : m;
        return m.isBlank() ? "Error de base de datos" : m.trim();
    }
}
