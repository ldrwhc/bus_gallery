package com.busgallery.busgallery.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "busgallery.upload-security")
public class UploadSecurityProperties {

    private long maxFileBytes = 15 * 1024 * 1024;
    private int maxImageWidth = 8192;
    private int maxImageHeight = 8192;
    private long maxImagePixels = 24_000_000L;
    private int uploadIpPerMinute = 30;
    private int uploadUserPerMinute = 20;
    private int uploadGlobalPerMinute = 300;
    private int uploadUserPerDay = 120;
    private int pendingPerUserMax = 30;
    private int pendingGlobalMax = 5000;
    private List<String> allowedMimeTypes = List.of("image/jpeg", "image/png");
}
