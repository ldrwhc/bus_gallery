package com.busgallery.busgallery.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthChallengeResponse {
    private String challengeId;
    private long expireInSeconds;
    private long resendAfterSeconds;
}

