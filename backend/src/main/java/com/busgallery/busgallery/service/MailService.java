package com.busgallery.busgallery.service;

public interface MailService {
    void sendOtpCode(String toEmail, String sceneLabel, String code, int expireSeconds);
}

