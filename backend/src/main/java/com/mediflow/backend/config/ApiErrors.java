package com.mediflow.backend.config;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@RestControllerAdvice
public class ApiErrors {
    public record Body(String code, String message, Map<String,String> fieldErrors, OffsetDateTime timestamp) {}

    private static Body body(String code, String message, Map<String,String> fields) {
        return new Body(code, message, fields, OffsetDateTime.now(ZoneId.of("Asia/Kolkata")));
    }

    static void writeSecurity(HttpServletResponse response, ObjectMapper mapper,
                              HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        if (status == HttpStatus.UNAUTHORIZED) response.setHeader("WWW-Authenticate", "Bearer");
        response.getOutputStream().write(mapper.writeValueAsBytes(body(status.name(), message, Map.of())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Body> fields(MethodArgumentNotValidException ex) {
        Map<String,String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(body("VALIDATION_ERROR","Please correct the invalid fields.",fields));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Body> handle(Exception ex) {
        if (ex instanceof org.springframework.http.converter.HttpMessageNotReadableException
                || ex instanceof org.springframework.beans.TypeMismatchException) {
            return ResponseEntity.badRequest().body(body("VALIDATION_ERROR",
                    "Check the request fields, date format and enum values.",Map.of()));
        }
        if (ex instanceof ErrorResponse error) {
            var status = HttpStatus.valueOf(error.getStatusCode().value());
            String message = ex instanceof ResponseStatusException r && r.getReason() != null
                    ? r.getReason() : status.getReasonPhrase();
            String code = status == HttpStatus.BAD_REQUEST ? "VALIDATION_ERROR" : status.name();
            if (status == HttpStatus.CONFLICT && message.startsWith("This slot is unavailable")) code = "SLOT_ALREADY_BOOKED";
            return ResponseEntity.status(status).headers(error.getHeaders()).body(body(code,message,Map.of()));
        }
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && sql.getErrorCode() == 1062) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(body("CONFLICT","A record with this value already exists.",Map.of()));
            }
        }
        LoggerFactory.getLogger(ApiErrors.class).error("Unhandled API error", ex);
        return ResponseEntity.internalServerError()
                .body(body("INTERNAL_SERVER_ERROR","Unable to complete the request.",Map.of()));
    }
}
