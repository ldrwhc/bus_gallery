package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.controller.VehicleController;
import com.busgallery.busgallery.dto.response.PageResponse;
import com.busgallery.busgallery.entity.User;
import com.busgallery.busgallery.entity.VehicleFavorite;
import com.busgallery.busgallery.exception.NotFoundException;
import com.busgallery.busgallery.messaging.BusEventPublisher;
import com.busgallery.busgallery.messaging.payload.FavoriteToggledEvent;
import com.busgallery.busgallery.mapper.VehicleFavoriteMapper;
import com.busgallery.busgallery.repository.UserRepository;
import com.busgallery.busgallery.service.FavoriteService;
import com.busgallery.busgallery.service.ImageService;
import com.busgallery.busgallery.service.VehicleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final VehicleFavoriteMapper favoriteMapper;
    private final VehicleService vehicleService;
    private final ImageService imageService;
    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final BusEventPublisher busEventPublisher;

    @Value("${busgallery.cache.favorites.summary-ttl-seconds:30}")
    private long favoriteSummaryTtlSeconds;

    @Value("${busgallery.cache.favorites.liked-ttl-seconds:30}")
    private long favoriteLikedTtlSeconds;


    @Override
    @Transactional
    public FavoriteSummary setLiked(Long vehicleId, Long userId, boolean liked) {
        var vehicle = vehicleService.findById(vehicleId);
        if (vehicle == null) {
            throw new NotFoundException("Vehicle not found");
        }
        VehicleFavorite existing = favoriteMapper.select(vehicleId, userId);
        boolean changed = false;
        if (liked) {
            if (existing == null) {
                VehicleFavorite favorite = new VehicleFavorite();
                favorite.setVehicleId(vehicleId);
                favorite.setUserId(userId);
                try {
                    favoriteMapper.insert(favorite);
                    changed = true;
                } catch (DuplicateKeyException ignore) {
                    // Concurrent set-liked requests may race; treat duplicate insert as idempotent no-op.
                }
            }
        } else {
            if (existing != null) {
                int affected = favoriteMapper.delete(vehicleId, userId);
                changed = affected > 0;
            }
        }
        long total = favoriteMapper.countByVehicle(vehicleId);
        List<UserLike> topUsers = buildTopUsers(vehicleId, userId);
        FavoriteSummary summary = new FavoriteSummary(liked, total, topUsers);
        cacheSummary(vehicleId, summary);
        cacheLiked(vehicleId, userId, liked);
        if (changed) {
            busEventPublisher.publishFavoriteToggled(FavoriteToggledEvent.builder()
                    .vehicleId(vehicleId)
                    .plateNumber(vehicle.getPlateNumber())
                    .userId(userId)
                    .liked(liked)
                    .totalFavorites(total)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
        return summary;
    }

    @Override
    public FavoriteSummary summary(Long vehicleId, Long currentUserId) {
        FavoriteSummary cached = getCachedSummary(vehicleId);
        Boolean liked = currentUserId != null ? getCachedLiked(vehicleId, currentUserId) : null;
        if (liked == null && currentUserId != null) {
            VehicleFavorite existing = favoriteMapper.select(vehicleId, currentUserId);
            liked = existing != null;
            cacheLiked(vehicleId, currentUserId, liked);
        }
        if (cached != null) {
            return new FavoriteSummary(Boolean.TRUE.equals(liked), cached.total(), cached.topUsers());
        }
        long total = favoriteMapper.countByVehicle(vehicleId);
        List<UserLike> topUsers = buildTopUsers(vehicleId, currentUserId);
        FavoriteSummary summary = new FavoriteSummary(Boolean.TRUE.equals(liked), total, topUsers);
        cacheSummary(vehicleId, summary);
        return summary;
    }

    @Override
    public PageResponse<VehicleController.VehicleDetailResponse> listFavorites(Long userId, int page, int size) {
        int pageNo = Math.max(page, 1);
        int pageSize = Math.max(size, 1);
        int offset = (pageNo - 1) * pageSize;
        List<VehicleFavorite> favorites = favoriteMapper.selectByUserPage(userId, offset, pageSize);
        List<VehicleController.VehicleDetailResponse> records = favorites.stream()
                .map(fav -> {
                    var vehicle = vehicleService.findById(fav.getVehicleId());
                    if (vehicle == null) return null;
                    var config = vehicleService.findConfigByVehicleId(fav.getVehicleId());
                    var images = imageService.listByVehicle(fav.getVehicleId());
                    return VehicleController.assembleDetail(vehicle, config, images);
                })
                .filter(item -> item != null)
                .collect(Collectors.toList());
        long total = favoriteMapper.countByUser(userId);
        return PageResponse.of(records, total, pageNo, pageSize);
    }

    private List<UserLike> buildTopUsers(Long vehicleId, Long currentUserId) {
        List<VehicleFavorite> top = favoriteMapper.selectByVehicle(vehicleId, 5);
        return top.stream()
                .map(fav -> {
                    User user = userRepository.findById(fav.getUserId()).orElse(null);
                    return new UserLike(
                            fav.getUserId(),
                            user != null ? user.getUsername() : null,
                            user != null ? user.getDisplayName() : null,
                            currentUserId != null && currentUserId.equals(fav.getUserId())
                    );
                })
                .collect(Collectors.toList());
    }

    private String summaryCacheKey(Long vehicleId) {
        return "bg:fav:summary:" + vehicleId;
    }

    private String likedCacheKey(Long vehicleId, Long userId) {
        return "bg:fav:liked:" + vehicleId + ":" + userId;
    }

    private FavoriteSummary getCachedSummary(Long vehicleId) {
        try {
            String json = stringRedisTemplate.opsForValue().get(summaryCacheKey(vehicleId));
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json, FavoriteSummary.class);
        } catch (Exception ignore) {
            return null;
        }
    }

    private void cacheSummary(Long vehicleId, FavoriteSummary summary) {
        try {
            String json = objectMapper.writeValueAsString(summary);
            stringRedisTemplate.opsForValue().set(
                    summaryCacheKey(vehicleId),
                    json,
                    Duration.ofSeconds(favoriteSummaryTtlSeconds)
            );
        } catch (Exception ignore) {
        }
    }

    private Boolean getCachedLiked(Long vehicleId, Long userId) {
        try {
            String value = stringRedisTemplate.opsForValue().get(likedCacheKey(vehicleId, userId));
            if (value == null || value.isBlank()) {
                return null;
            }
            return Boolean.parseBoolean(value);
        } catch (Exception ignore) {
            return null;
        }
    }

    private void cacheLiked(Long vehicleId, Long userId, boolean liked) {
        if (userId == null) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(
                    likedCacheKey(vehicleId, userId),
                    String.valueOf(liked),
                    Duration.ofSeconds(favoriteLikedTtlSeconds)
            );
        } catch (Exception ignore) {
        }
    }
}
