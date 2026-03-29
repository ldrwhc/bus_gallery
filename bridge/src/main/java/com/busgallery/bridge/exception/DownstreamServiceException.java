package com.busgallery.bridge.exception;

import lombok.Getter;

/**
 * Exception raised by Feign decoder when downstream returns non-2xx.
 */
@Getter
public class DownstreamServiceException extends RuntimeException {
    private final int status;
    private final String body;

    public DownstreamServiceException(int status, String body) {
        super("downstream service call failed, status=" + status);
        this.status = status;
        this.body = body;
    }
}
