package com.busgallery.gateway.util;

import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Utility to sign and verify internal auth headers.
 */
public final class HmacSigner {

    private static final String HMAC_ALGO = "HmacSHA256";

    private HmacSigner() {
    }

    /**
     * Sign payload with HMAC-SHA256 and return lower-case hex.
     *
     * @param secret  secret key
     * @param payload payload text
     * @return signature hex
     */
    public static String signHex(String secret, String payload) {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalArgumentException("internal secret must not be empty");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("sign internal auth payload failed", ex);
        }
    }
}
