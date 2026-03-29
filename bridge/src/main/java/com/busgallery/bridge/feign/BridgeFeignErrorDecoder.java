package com.busgallery.bridge.feign;

import com.busgallery.bridge.exception.DownstreamServiceException;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Map Feign downstream errors into domain exception for unified response.
 */
@Component
public class BridgeFeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        String body = "";
        try {
            if (response.body() != null) {
                body = Util.toString(response.body().asReader(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {
            body = "";
        }
        return new DownstreamServiceException(response.status(), body);
    }
}
