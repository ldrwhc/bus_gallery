package com.busgallery.busgallery.service;

import com.busgallery.busgallery.auth.UserSession;
import com.busgallery.busgallery.dto.response.AuthChallengeResponse;
import com.busgallery.busgallery.dto.response.AuthTicketResponse;

public interface AuthSecurityService {

    void checkLoginRateLimit(String clientIp, String username);

    void checkRegisterSubmitRateLimit(String clientIp, String email);

    AuthChallengeResponse sendRegisterCode(String username, String email, String clientIp);

    void verifyRegisterCode(String challengeId, String email, String emailCode);

    AuthChallengeResponse sendPasswordChangeCode(UserSession session, String currentPassword, String clientIp);

    void changePassword(UserSession session, String challengeId, String emailCode, String newPassword, String confirmPassword);

    AuthChallengeResponse sendForgotPasswordCode(String usernameOrEmail, String clientIp);

    AuthTicketResponse verifyForgotPasswordCode(String challengeId, String emailCode);

    void resetForgotPassword(String resetTicket, String newPassword, String confirmPassword);

    AuthChallengeResponse sendBindEmailCode(UserSession session, String email, String currentPassword, String clientIp);

    void confirmBindEmail(UserSession session, String challengeId, String email, String emailCode);
}
