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
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSessionService {

    private static final String KEY_PREFIX = "busgallery:sessions:";
    private static final String USER_TOKENS_KEY_PREFIX = "busgallery:user:sessions:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final AuthProperties authProperties;

    public String createSession(User user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        Duration ttl = Duration.ofSeconds(Math.max(60L, authProperties.getTtlSeconds()));
        UserSession session = UserSession.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .reviewRegionId(user.getReviewRegionId())
                .token(token)
                .build();
        save(token, session, ttl);
        bindTokenToUser(user.getId(), token, ttl);
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
        UserSession session = getSession(token);
        stringRedisTemplate.delete(buildKey(token));
        if (session != null && session.getUserId() != null) {
            stringRedisTemplate.opsForSet().remove(buildUserTokensKey(session.getUserId()), token);
        }
    }

    public void deleteAllSessionsByUserId(Long userId) {
        if (userId == null) {
            return;
        }
        String key = buildUserTokensKey(userId);
        Set<String> tokens = stringRedisTemplate.opsForSet().members(key);
        if (tokens != null && !tokens.isEmpty()) {
            for (String token : tokens) {
                if (StringUtils.hasText(token)) {
                    stringRedisTemplate.delete(buildKey(token));
                }
            }
        }
        stringRedisTemplate.delete(key);
    }

    private void save(String token, UserSession session, Duration ttl) {
        try {
            stringRedisTemplate.opsForValue().set(buildKey(token), objectMapper.writeValueAsString(session), ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize user session", e);
        }
    }

    private void bindTokenToUser(Long userId, String token, Duration ttl) {
        if (userId == null || !StringUtils.hasText(token)) {
            return;
        }
        String key = buildUserTokensKey(userId);
        stringRedisTemplate.opsForSet().add(key, token);
        stringRedisTemplate.expire(key, ttl);
    }

    private String buildKey(String token) {
        return KEY_PREFIX + token;
    }

    private String buildUserTokensKey(Long userId) {
        return USER_TOKENS_KEY_PREFIX + userId;
    }
}

