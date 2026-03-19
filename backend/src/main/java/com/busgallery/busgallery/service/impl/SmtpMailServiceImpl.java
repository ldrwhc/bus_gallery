package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.config.AuthSecurityProperties;
import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import com.busgallery.busgallery.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SmtpMailServiceImpl implements MailService {

    private final JavaMailSender mailSender;
    private final AuthSecurityProperties authSecurityProperties;

    @Override
    public void sendOtpCode(String toEmail, String sceneLabel, String code, int expireSeconds) {
        if (!authSecurityProperties.isMailEnabled()) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "邮箱验证码服务未开启");
        }
        if (!StringUtils.hasText(authSecurityProperties.getMailFrom())) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "邮箱发件人未配置");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(authSecurityProperties.getMailFrom().trim());
        message.setTo(toEmail);
        message.setSubject("Bus Gallery 验证码");
        message.setText(buildBody(sceneLabel, code, expireSeconds));
        mailSender.send(message);
    }

    private String buildBody(String sceneLabel, String code, int expireSeconds) {
        return "您正在进行 " + sceneLabel + "。\n"
                + "验证码：" + code + "\n"
                + "有效期：" + expireSeconds + " 秒。\n"
                + "如果不是本人操作，请忽略此邮件。";
    }
}

