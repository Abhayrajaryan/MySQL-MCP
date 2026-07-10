package com.mysqlmcp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidApiKeyException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidApiKey(InvalidApiKeyException ex) {
        return buildErrorResponse("INVALID_API_KEY", ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<Map<String, Object>> handlePermissionDenied(PermissionDeniedException ex) {
        return buildErrorResponse("PERMISSION_DENIED", ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(DatabaseConnectionException.class)
    public ResponseEntity<Map<String, Object>> handleDatabaseConnection(DatabaseConnectionException ex) {
        String code = "DATABASE_UNREACHABLE";
        String message = ex.getMessage();
        if (message != null && message.contains("timeout")) {
            code = "DATABASE_CONNECTION_TIMEOUT";
        } else if (message != null && message.contains("credentials")) {
            code = "INVALID_DATABASE_CREDENTIALS";
        }
        return buildErrorResponse(code, message, HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(InvalidQueryException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidQuery(InvalidQueryException ex) {
        return buildErrorResponse("INVALID_SQL", ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(QueryTimeoutException.class)
    public ResponseEntity<Map<String, Object>> handleQueryTimeout(QueryTimeoutException ex) {
        return buildErrorResponse("QUERY_TIMEOUT", ex.getMessage(), HttpStatus.GATEWAY_TIMEOUT);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildErrorResponse("INVALID_INPUT", ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return buildErrorResponse("INTERNAL_ERROR", "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(String code, String message, HttpStatus status) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(status).body(body);
    }
}