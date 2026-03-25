package com.busgallery.busgallery.messaging.sideeffect;

import com.busgallery.busgallery.messaging.payload.CommentCreatedEvent;
import com.busgallery.busgallery.service.SnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentSideEffectService {

    private static final List<String> SENSITIVE_WORDS = List.of("spam", "scam", "fake", "广告", "诈骗");

    private final StringRedisTemplate redisTemplate;
    private final SnapshotService snapshotService;

    public void process(CommentCreatedEvent event) {
        if (event == null || event.getVehicleId() == null) {
            return;
        }
        runBestEffort(() -> sendNotification(event), "notification");
        runBestEffort(() -> reviewSensitiveContent(event), "moderation");
        runBestEffort(() -> updateHeatStats(event), "heat-stats");
        runBestEffort(() -> updateRecommendationScore(event), "recommendation");
        runBestEffort(() -> prewarmSnapshot(event), "snapshot-prewarm");
    }

    private void sendNotification(CommentCreatedEvent event) {
        log.info("async-notify comment.created: commentId={}, vehicleId={}",
                event.getCommentId(), event.getVehicleId());
    }

    private void reviewSensitiveContent(CommentCreatedEvent event) {
        String content = StringUtils.hasText(event.getContent()) ? event.getContent().toLowerCase(Locale.ROOT) : "";
        boolean hit = SENSITIVE_WORDS.stream().anyMatch(content::contains);
        if (!hit || event.getCommentId() == null) {
            return;
        }
        String flagKey = "bg:comment:moderation:flag:" + event.getCommentId();
        redisTemplate.opsForValue().set(flagKey, "PENDING_REVIEW", Duration.ofDays(7));
    }

    private void updateHeatStats(CommentCreatedEvent event) {
        String vehicleHeatKey = "bg:vehicle:heat:engagement";
        redisTemplate.opsForZSet().incrementScore(vehicleHeatKey, String.valueOf(event.getVehicleId()), 1D);
    }

    private void updateRecommendationScore(CommentCreatedEvent event) {
        String key = "bg:vehicle:recommend:comment-signal:" + event.getVehicleId();
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofDays(7));
    }

    private void prewarmSnapshot(CommentCreatedEvent event) {
        if (!StringUtils.hasText(event.getPlateNumber())) {
            return;
        }
        try {
            snapshotService.getSnapshotByPlate(event.getPlateNumber());
        } catch (Exception e) {
            log.warn("snapshot prewarm failed for plate={}", event.getPlateNumber(), e);
        }
    }

    private void runBestEffort(Runnable runnable, String action) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.warn("comment.created side-effect skipped, action={}", action, e);
        }
    }
}
