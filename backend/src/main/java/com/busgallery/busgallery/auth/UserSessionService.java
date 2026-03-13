package com.busgallery.busgallery.auth;

import com.busgallery.busgallery.config.AuthProperties;
import com.busgallery.busgallery.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSessionService {

    private static final String KEY_PREFIX = "busgallery:sessions:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final AuthProperties authProperties;

    public String createSession(User user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        UserSession session = UserSession.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .token(token)
                .build();
        save(token, session);
        return token;
    }

    public UserSession getSession(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        String key = buildKey(token);
        String payload = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(payload)) {
            return null;
        }
        try {
            UserSession session = objectMapper.readValue(payload, UserSession.class);
            session.setToken(token);
            return session;
        } catch (JsonProcessingException e) {
            stringRedisTemplate.delete(key);
            return null;
        }
    }

    public void deleteSession(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        stringRedisTemplate.delete(buildKey(token));
    }

    private void save(String token, UserSession session) {
        try {
            Duration ttl = Duration.ofSeconds(Math.max(60L, authProperties.getTtlSeconds()));
            stringRedisTemplate.opsForValue()
                    .set(buildKey(token), objectMapper.writeValueAsString(session), ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("无法序列化用户会话", e);
        }
    }

    private String buildKey(String token) {
        return KEY_PREFIX + token;
    }
}
