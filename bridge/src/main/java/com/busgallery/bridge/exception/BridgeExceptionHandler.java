package com.busgallery.bridge.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Unified bridge exception mapping.
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class BridgeExceptionHandler {

    private final ObjectMapper objectMapper;

    @ExceptionHandler(DownstreamServiceException.class)
    public ResponseEntity<?> handleDownstream(DownstreamServiceException ex) {
        int status = normalizeStatus(ex.getStatus());
        String body = ex.getBody();
        if (body != null && !body.isBlank()) {
            try {
                JsonNode node = objectMapper.readTree(body);
                return ResponseEntity.status(status)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(node);
            } catch (Exception ignored) {
                // fallback unified format below
            }
        }
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorBody("B0500", "Downstream service call failed", null));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleAuth(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorBody("A0401", ex.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAny(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorBody("B0500", "bridge internal error", null));
    }

    private int normalizeStatus(int status) {
        if (status >= 100 && status <= 599) {
            return status;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    private Map<String, Object> errorBody(String code, String message, Object data) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", code);
        map.put("message", message);
        map.put("data", data);
        return map;
    }
}
