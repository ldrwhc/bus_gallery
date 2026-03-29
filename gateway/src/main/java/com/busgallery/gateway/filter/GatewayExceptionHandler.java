package com.busgallery.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Unified gateway exception mapper.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = resolveStatus(ex);
        String code = resolveCode(status);
        String message = resolveMessage(status, ex);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", null);

        byte[] payload;
        try {
            payload = objectMapper.writeValueAsBytes(body);
        } catch (Exception jsonEx) {
            payload = "{\"code\":\"B0500\",\"message\":\"gateway internal error\",\"data\":null}"
                    .getBytes(StandardCharsets.UTF_8);
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        if (status.is5xxServerError()) {
            log.error("gateway error path={}, reason={}", exchange.getRequest().getURI().getPath(), ex.getMessage(), ex);
        } else {
            log.warn("gateway request rejected path={}, status={}, reason={}",
                    exchange.getRequest().getURI().getPath(), status.value(), ex.getMessage());
        }
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(payload)));
    }

    private HttpStatus resolveStatus(Throwable ex) {
        if (ex instanceof ResponseStatusException statusException) {
            HttpStatus status = HttpStatus.resolve(statusException.getStatusCode().value());
            if (status != null) {
                return status;
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String resolveCode(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "A0400";
            case UNAUTHORIZED -> "A0401";
            case FORBIDDEN -> "A0403";
            case NOT_FOUND -> "A0404";
            case TOO_MANY_REQUESTS -> "A0429";
            default -> status.is4xxClientError() ? "A0400" : "B0500";
        };
    }

    private String resolveMessage(HttpStatus status, Throwable ex) {
        if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
            return "gateway internal error";
        }
        String reason = ex instanceof ResponseStatusException statusException ? statusException.getReason() : null;
        if (reason == null || reason.isBlank()) {
            return status.getReasonPhrase();
        }
        return reason;
    }
}
