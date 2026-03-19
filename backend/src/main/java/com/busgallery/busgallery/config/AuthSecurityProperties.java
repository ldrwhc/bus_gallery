package com.busgallery.busgallery.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "auth.security")
public class AuthSecurityProperties {

    private int codeLength = 6;
    private int otpTtlSeconds = 300;
    private int otpMaxAttempts = 5;
    private int otpResendCooldownSeconds = 60;
    private int resetTicketTtlSeconds = 600;
    private boolean mailEnabled = false;
    private String mailFrom = "";
    private boolean captchaEnabled = true;
    private int captchaTtlSeconds = 180;
    private int captchaCodeLength = 4;
    private int captchaMaxAttempts = 5;
    private int loginCaptchaFailureThreshold = 5;
    private int forgotCaptchaRequestThreshold = 6;
    private RateLimit rateLimit = new RateLimit();

    @Data
    public static class RateLimit {
        private int authGlobalPerMinute = 1200;
        private int sendCodeIpPerMinute = 30;
        private int sendCodeEmailPerHour = 12;
        private int sendCodeIpPerDay = 200;
        private int sendCodeEmailPerDay = 40;
        private int sendCodeAccountPerDay = 40;
        private int registerIpPerMinute = 20;
        private int registerEmailPerHour = 8;
        private int loginIpPerMinute = 120;
        private int loginUserPerMinute = 30;
        private int forgotIpPerMinute = 30;
        private int forgotIdentifierPerHour = 15;
    }
}
