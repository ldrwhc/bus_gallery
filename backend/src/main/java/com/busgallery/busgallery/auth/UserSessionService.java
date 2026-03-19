package com.busgallery.busgallery.auth;

import com.busgallery.busgallery.config.AuthProperties;
import com.busgallery.busgallery.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSessionService {

    private static final String KEY_PREFIX = "busgallery:sessions:";
    private static final String USER_TOKENS_KEY_PREFIX = "busgallery:user:sessions:";
    private static final int LOCAL_SESSION_MAX_SIZE = 20_000;
    private static final long REDIS_FALLBACK_WARN_INTERVAL_MS = 60_000L;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final AuthProperties authProperties;
    private final Map<String, LocalSessionRecord> localSessionStore = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> localUserTokens = new ConcurrentHashMap<>();
    private volatile long lastRedisFallbackWarnAt = 0L;

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
        try {
            String payload = stringRedisTemplate.opsForValue().get(key);
            if (StringUtils.hasText(payload)) {
                UserSession session = objectMapper.readValue(payload, UserSession.class);
                session.setToken(token);
                return session;
            }
        } catch (JsonProcessingException e) {
            try {
                stringRedisTemplate.delete(key);
            } catch (Exception ex) {
                logRedisFallback("delete-bad-session", ex);
            }
        } catch (Exception ex) {
            logRedisFallback("get-session", ex);
        }
        return getLocalSession(token);
    }

    public void deleteSession(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        UserSession session = getSession(token);
        try {
            stringRedisTemplate.delete(buildKey(token));
        } catch (Exception ex) {
            logRedisFallback("delete-session", ex);
        }
        if (session != null && session.getUserId() != null && StringUtils.hasText(token)) {
            try {
                stringRedisTemplate.opsForSet().remove(buildUserTokensKey(session.getUserId()), token);
            } catch (Exception ex) {
                logRedisFallback("unbind-session-token", ex);
            }
        }
        removeLocalToken(token, session != null ? session.getUserId() : null);
    }

    public void deleteAllSessionsByUserId(Long userId) {
        if (userId == null) {
            return;
        }
        String key = buildUserTokensKey(userId);
        try {
            Set<String> tokens = stringRedisTemplate.opsForSet().members(key);
            if (tokens != null && !tokens.isEmpty()) {
                for (String token : tokens) {
                    if (StringUtils.hasText(token)) {
                        stringRedisTemplate.delete(buildKey(token));
                    }
                }
            }
            stringRedisTemplate.delete(key);
        } catch (Exception ex) {
            logRedisFallback("delete-all-sessions", ex);
        }
        removeAllLocalTokensByUserId(userId);
    }

    public void refreshDisplayNameByUserId(Long userId, String displayName) {
        if (userId == null || !StringUtils.hasText(displayName)) {
            return;
        }
        Set<String> allTokens = new HashSet<>();
        try {
            Set<String> redisTokens = stringRedisTemplate.opsForSet().members(buildUserTokensKey(userId));
            if (redisTokens != null && !redisTokens.isEmpty()) {
                allTokens.addAll(redisTokens);
            }
        } catch (Exception ex) {
            logRedisFallback("read-user-tokens", ex);
        }
        Set<String> localTokens = localUserTokens.get(userId);
        if (localTokens != null && !localTokens.isEmpty()) {
            allTokens.addAll(localTokens);
        }
        if (allTokens.isEmpty()) {
            return;
        }
        for (String token : allTokens) {
            if (!StringUtils.hasText(token)) {
                continue;
            }
            UserSession session = getSession(token);
            if (session == null) {
                continue;
            }
            session.setDisplayName(displayName);
            Duration ttl = resolveSessionTtl(token);
            save(token, session, ttl);
            bindTokenToUser(userId, token, ttl);
        }
    }

    private void save(String token, UserSession session, Duration ttl) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(session);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize user session", e);
        }
        try {
            stringRedisTemplate.opsForValue().set(buildKey(token), payload, ttl);
            removeLocalToken(token, session != null ? session.getUserId() : null);
        } catch (Exception ex) {
            logRedisFallback("save-session", ex);
            saveLocalSession(token, session, ttl);
        }
    }

    private void bindTokenToUser(Long userId, String token, Duration ttl) {
        if (userId == null || !StringUtils.hasText(token)) {
            return;
        }
        String key = buildUserTokensKey(userId);
        try {
            stringRedisTemplate.opsForSet().add(key, token);
            stringRedisTemplate.expire(key, ttl);
            Set<String> localTokens = localUserTokens.get(userId);
            if (localTokens != null) {
                localTokens.remove(token);
                if (localTokens.isEmpty()) {
                    localUserTokens.remove(userId);
                }
            }
        } catch (Exception ex) {
            logRedisFallback("bind-session-token", ex);
            localUserTokens.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(token);
        }
    }

    private String buildKey(String token) {
        return KEY_PREFIX + token;
    }

    private String buildUserTokensKey(Long userId) {
        return USER_TOKENS_KEY_PREFIX + userId;
    }

    private Duration resolveSessionTtl(String token) {
        if (!StringUtils.hasText(token)) {
            return Duration.ofSeconds(Math.max(60L, authProperties.getTtlSeconds()));
        }
        try {
            Long ttlSeconds = stringRedisTemplate.getExpire(buildKey(token));
            if (ttlSeconds != null && ttlSeconds > 0) {
                return Duration.ofSeconds(ttlSeconds);
            }
        } catch (Exception ex) {
            logRedisFallback("resolve-session-ttl", ex);
        }
        LocalSessionRecord local = localSessionStore.get(token);
        if (local != null) {
            long remainMs = local.expireAt - System.currentTimeMillis();
            if (remainMs > 0) {
                return Duration.ofMillis(remainMs);
            }
        }
        return Duration.ofSeconds(Math.max(60L, authProperties.getTtlSeconds()));
    }

    private void saveLocalSession(String token, UserSession session, Duration ttl) {
        if (!StringUtils.hasText(token) || session == null) {
            return;
        }
        long expireAt = System.currentTimeMillis() + Math.max(1L, ttl.toMillis());
        localSessionStore.put(token, new LocalSessionRecord(copySession(session, token), expireAt));
        if (session.getUserId() != null) {
            localUserTokens.computeIfAbsent(session.getUserId(), ignored -> ConcurrentHashMap.newKeySet()).add(token);
        }
        cleanupLocalSessionsIfNeeded();
    }

    private UserSession getLocalSession(String token) {
        LocalSessionRecord record = localSessionStore.get(token);
        if (record == null) {
            return null;
        }
        if (record.expireAt <= System.currentTimeMillis()) {
            removeLocalToken(token, record.session != null ? record.session.getUserId() : null);
            return null;
        }
        return copySession(record.session, token);
    }

    private void removeLocalToken(String token, Long userId) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        LocalSessionRecord removed = localSessionStore.remove(token);
        Long uid = userId != null ? userId : (removed != null && removed.session != null ? removed.session.getUserId() : null);
        if (uid == null) {
            return;
        }
        Set<String> tokens = localUserTokens.get(uid);
        if (tokens != null) {
            tokens.remove(token);
            if (tokens.isEmpty()) {
                localUserTokens.remove(uid);
            }
        }
    }

    private void removeAllLocalTokensByUserId(Long userId) {
        Set<String> tokens = localUserTokens.remove(userId);
        if (tokens == null || tokens.isEmpty()) {
            return;
        }
        for (String token : tokens) {
            localSessionStore.remove(token);
        }
    }

    private UserSession copySession(UserSession source, String token) {
        if (source == null) {
            return null;
        }
        return UserSession.builder()
                .userId(source.getUserId())
                .username(source.getUsername())
                .displayName(source.getDisplayName())
                .avatarUrl(source.getAvatarUrl())
                .role(source.getRole())
                .reviewRegionId(source.getReviewRegionId())
                .token(token)
                .build();
    }

    private void cleanupLocalSessionsIfNeeded() {
        if (localSessionStore.size() <= LOCAL_SESSION_MAX_SIZE && ThreadLocalRandom.current().nextInt(100) != 0) {
            return;
        }
        long now = System.currentTimeMillis();
        localSessionStore.entrySet().removeIf(entry -> entry.getValue().expireAt <= now);
        if (localSessionStore.size() <= LOCAL_SESSION_MAX_SIZE) {
            return;
        }
        int removed = 0;
        for (String token : localSessionStore.keySet()) {
            LocalSessionRecord record = localSessionStore.remove(token);
            if (record != null && record.session != null && record.session.getUserId() != null) {
                Set<String> tokenSet = localUserTokens.get(record.session.getUserId());
                if (tokenSet != null) {
                    tokenSet.remove(token);
                    if (tokenSet.isEmpty()) {
                        localUserTokens.remove(record.session.getUserId());
                    }
                }
            }
            removed++;
            if (localSessionStore.size() <= LOCAL_SESSION_MAX_SIZE || removed >= 1024) {
                break;
            }
        }
    }

    private void logRedisFallback(String operation, Exception ex) {
        long now = System.currentTimeMillis();
        if (now - lastRedisFallbackWarnAt < REDIS_FALLBACK_WARN_INTERVAL_MS) {
            return;
        }
        lastRedisFallbackWarnAt = now;
        log.warn("Redis session operation fallback [{}]: {}", operation, ex.getMessage());
    }

    private static final class LocalSessionRecord {
        private final UserSession session;
        private final long expireAt;

        private LocalSessionRecord(UserSession session, long expireAt) {
            this.session = session;
            this.expireAt = expireAt;
        }
    }
}
