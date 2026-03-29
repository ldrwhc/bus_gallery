package com.busgallery.bridge.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper service for binary proxy and response rewrite.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BridgeProxyService {

    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            HttpHeaders.CONNECTION.toLowerCase(),
            "keep-alive",
            HttpHeaders.TRANSFER_ENCODING.toLowerCase(),
            HttpHeaders.PROXY_AUTHENTICATE.toLowerCase(),
            HttpHeaders.PROXY_AUTHORIZATION.toLowerCase(),
            "te",
            "trailer",
            HttpHeaders.UPGRADE.toLowerCase()
    );

    private static final List<String> FORWARDED_HEADERS = List.of(
            HttpHeaders.AUTHORIZATION,
            HttpHeaders.ACCEPT,
            "X-Request-Id",
            "X-Correlation-Id",
            "X-Internal-Auth",
            "X-User-Id",
            "X-Username",
            "X-Display-Name",
            "X-Role",
            "X-Review-Region",
            "X-Auth-Ts",
            "X-Auth-Signature"
    );

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Forward one binary GET request.
     *
     * @param targetUrl target URL
     * @param request servlet request
     * @return binary response
     */
    public ResponseEntity<byte[]> proxyBinaryGet(String targetUrl, HttpServletRequest request) {
        String url = appendQuery(targetUrl, request.getQueryString());
        HttpHeaders headers = buildForwardHeaders(request);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<byte[]> upstream = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
            return new ResponseEntity<>(upstream.getBody(), sanitizeResponseHeaders(upstream.getHeaders()), upstream.getStatusCode());
        } catch (HttpStatusCodeException ex) {
            HttpHeaders respHeaders = sanitizeResponseHeaders(ex.getResponseHeaders());
            return new ResponseEntity<>(ex.getResponseBodyAsByteArray(), respHeaders, ex.getStatusCode());
        }
    }

    /**
     * Rewrite trade record download URL to bridge endpoint.
     *
     * @param original original response JSON
     * @return rewritten JSON
     */
    public String rewriteRecordDownloadUrls(String original) {
        if (original == null || original.isBlank()) {
            return original;
        }
        try {
            Map<String, Object> root = objectMapper.readValue(original, new TypeReference<>() {});
            Object dataObj = root.get("data");
            if (!(dataObj instanceof List<?> list)) {
                return original;
            }
            for (Object itemObj : list) {
                if (!(itemObj instanceof Map<?, ?> itemMap)) {
                    continue;
                }
                Object recordIdObj = itemMap.get("recordId");
                if (recordIdObj == null) {
                    continue;
                }
                String recordId = String.valueOf(recordIdObj).trim();
                if (recordId.isEmpty()) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> mutable = (Map<String, Object>) itemMap;
                mutable.put("downloadUrl", "/api/bridge/purchases/" + recordId + "/download");
            }
            return objectMapper.writeValueAsString(root);
        } catch (Exception ex) {
            log.debug("rewrite record download url skipped: {}", ex.getMessage());
            return original;
        }
    }

    private HttpHeaders buildForwardHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        for (String name : FORWARDED_HEADERS) {
            String value = request.getHeader(name);
            if (value != null && !value.isBlank()) {
                headers.set(name, value);
            }
        }
        return headers;
    }

    private String appendQuery(String baseUrl, String queryString) {
        if (queryString == null || queryString.isBlank()) {
            return baseUrl;
        }
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + queryString;
    }

    private HttpHeaders sanitizeResponseHeaders(HttpHeaders upstreamHeaders) {
        HttpHeaders sanitized = new HttpHeaders();
        if (upstreamHeaders == null || upstreamHeaders.isEmpty()) {
            return sanitized;
        }
        for (Map.Entry<String, List<String>> entry : upstreamHeaders.entrySet()) {
            String name = entry.getKey();
            if (name == null) {
                continue;
            }
            String lowerName = name.toLowerCase();
            if (HOP_BY_HOP_HEADERS.contains(lowerName) || HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(name)) {
                continue;
            }
            sanitized.put(name, new ArrayList<>(entry.getValue()));
        }
        return sanitized;
    }
}
