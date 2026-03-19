package com.busgallery.busgallery.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaChallengeResponse {
    private String scene;
    private String captchaId;
    private String imageBase64;
    private int expireInSeconds;
}
