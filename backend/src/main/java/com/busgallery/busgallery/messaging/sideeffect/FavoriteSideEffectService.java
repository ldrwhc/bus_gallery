package com.busgallery.busgallery.messaging.sideeffect;

import com.busgallery.busgallery.messaging.payload.FavoriteToggledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteSideEffectService {

    private final StringRedisTemplate redisTemplate;

    public void process(FavoriteToggledEvent event) {
        if (event == null || event.getVehicleId() == null) {
            return;
        }
        runBestEffort(() -> updateLeaderboard(event), "leaderboard");
        runBestEffort(() -> updateRecommendationSignal(event), "recommendation");
        runBestEffort(() -> sendNotification(event), "notification");
    }

    private void updateLeaderboard(FavoriteToggledEvent event) {
        String key = "bg:vehicle:heat:favorites";
        redisTemplate.opsForZSet().add(key, String.valueOf(event.getVehicleId()), event.getTotalFavorites());
    }

    private void updateRecommendationSignal(FavoriteToggledEvent event) {
        String key = "bg:vehicle:recommend:favorite-signal:" + event.getVehicleId();
        long delta = event.isLiked() ? 1 : -1;
        redisTemplate.opsForValue().increment(key, delta);
        redisTemplate.expire(key, Duration.ofDays(7));
    }

    private void sendNotification(FavoriteToggledEvent event) {
        log.info("async-notify favorite.toggled: vehicleId={}, userId={}, liked={}, total={}",
                event.getVehicleId(), event.getUserId(), event.isLiked(), event.getTotalFavorites());
    }

    private void runBestEffort(Runnable runnable, String action) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.warn("favorite.toggled side-effect skipped, action={}", action, e);
        }
    }
}
