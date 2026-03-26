package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.controller.CommentController;
import com.busgallery.busgallery.controller.VehicleController;
import com.busgallery.busgallery.entity.Vehicle;
import com.busgallery.busgallery.service.ImageAccessService;
import com.busgallery.busgallery.service.ImageService;
import com.busgallery.busgallery.service.FavoriteService;
import com.busgallery.busgallery.service.SnapshotService;
import com.busgallery.busgallery.service.VehicleCommentService;
import com.busgallery.busgallery.service.VehicleService;
import com.busgallery.busgallery.service.snapshot.SnapshotPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * SnapshotServiceImpl 类。
 */
@Service
@RequiredArgsConstructor
public class SnapshotServiceImpl implements SnapshotService {

    private final VehicleService vehicleService;
    private final ImageService imageService;
    private final ImageAccessService imageAccessService;
    private final VehicleCommentService commentService;
    private final FavoriteService favoriteService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration SNAPSHOT_TTL = Duration.ofMinutes(10);

    /**
     * getSnapshotByPlate 鏂规硶銆?
     * @param plateNumber 鍙傛暟
     * @return 杩斿洖鍊?
     */
    public SnapshotPayload getSnapshotByPlate(String plateNumber) {
        String normalized = normalizePlate(plateNumber);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        String latestKey = buildLatestKey(normalized);
        String version = redisTemplate.opsForValue().get(latestKey);
        if (StringUtils.hasText(version)) {
            SnapshotPayload cached = readVersion(normalized, version);
            if (cached != null) {
                return refreshSignedUrls(cached);
            }
        }
        SnapshotPayload stale = readStale(normalized);
        boolean locked = tryAcquireLock(normalized);
        if (!locked && stale != null) {
            return refreshSignedUrls(stale);
        }
        try {
            SnapshotPayload payload = buildSnapshot(normalized);
            cacheSnapshot(normalized, payload);
            return refreshSignedUrls(payload);
        } finally {
            if (locked) {
                releaseLock(normalized);
            }
        }
    }

    /**
     * listHotSnapshots 鏂规硶銆?
     * @param limit 鍙傛暟
     * @return 杩斿洖鍊?
     */
    public List<SnapshotPayload> listHotSnapshots(int limit) {
        int size = Math.max(1, Math.min(limit, 12));
        List<Vehicle> vehicles = vehicleService.listHotByViewCount(size);
        List<String> plateNumbers = vehicles.stream()
                .map(Vehicle::getPlateNumber)
                .filter(StringUtils::hasText)
                .map(this::normalizePlate)
                .distinct()
                .limit(size)
                .toList();
        return plateNumbers.stream()
                .map(this::getSnapshotByPlate)
                .filter(p -> p != null && !CollectionUtils.isEmpty(p.getVariants()))
                .collect(Collectors.toList());
    }

    private SnapshotPayload buildSnapshot(String normalizedPlate) {
        List<Vehicle> vehicles = vehicleService.listByPlateNumber(normalizedPlate);
        List<VehicleController.VehicleDetailResponse> variants = vehicles.stream()
                .map(v -> VehicleController.assembleDetail(
                        v,
                        vehicleService.findConfigByVehicleId(v.getId()),
                        imageService.listByVehicle(v.getId())
                ))
                .collect(Collectors.toList());

        List<CommentController.CommentResponse> comments = vehicles.isEmpty() ? List.of() :
                commentService.list(vehicles.get(0).getId(), 1, 30).stream()
                        .map(CommentController.CommentResponse::from)
                        .collect(Collectors.toList());

        FavoriteService.FavoriteSummary summary = vehicles.isEmpty()
                ? new FavoriteService.FavoriteSummary(false, 0, List.of())
                : favoriteService.summary(vehicles.get(0).getId(), null);

        List<VehicleController.VehicleSummary> recommendations = List.of();
        if (!vehicles.isEmpty() && vehicles.get(0).getCompany() != null && vehicles.get(0).getCompany().getId() != null) {
            Long companyId = vehicles.get(0).getCompany().getId();
            recommendations = vehicleService.listByCompany(companyId).stream()
                    .filter(v -> !normalizedPlate.equalsIgnoreCase(normalizePlate(v.getPlateNumber())))
                    .limit(6)
                    .map(v -> new VehicleController.VehicleSummary(
                            VehicleController.assembleDetail(v, vehicleService.findConfigByVehicleId(v.getId()), null).getVehicle(),
                            List.of()
                    ))
                    .collect(Collectors.toList());
        }

        SnapshotPayload payload = new SnapshotPayload();
        payload.setPlateNumber(normalizedPlate);
        payload.setVersion(System.currentTimeMillis());
        payload.setVariants(variants);
        payload.setComments(comments);
        payload.setRecommendations(recommendations);
        payload.setFavoriteSummary(summary);
        return payload;
    }

    private void cacheSnapshot(String normalizedPlate, SnapshotPayload payload) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(payload);
            byte[] gz = gzip(json);
            String base64 = Base64.getEncoder().encodeToString(gz);
            String version = String.valueOf(payload.getVersion());
            String versionKey = buildVersionKey(normalizedPlate, version);
            redisTemplate.opsForValue().set(versionKey, base64, SNAPSHOT_TTL);
            redisTemplate.opsForValue().set(buildLatestKey(normalizedPlate), version, SNAPSHOT_TTL);
            redisTemplate.opsForValue().set(buildStaleKey(normalizedPlate), base64, Duration.ofHours(1));
        } catch (Exception e) {
            // best effort
        }
    }

    private SnapshotPayload readVersion(String normalizedPlate, String version) {
        String versionKey = buildVersionKey(normalizedPlate, version);
        String base64 = redisTemplate.opsForValue().get(versionKey);
        if (!StringUtils.hasText(base64)) return null;
        try {
            byte[] gz = Base64.getDecoder().decode(base64.getBytes(StandardCharsets.UTF_8));
            byte[] json = ungzip(gz);
            SnapshotPayload payload = objectMapper.readValue(json, SnapshotPayload.class);
            payload.setVersion(Long.parseLong(version));
            return payload;
        } catch (Exception e) {
            return null;
        }
    }

    private String buildLatestKey(String plate) {
        return "bg:snapshot:plate:" + plate + ":latest";
    }

    private String buildVersionKey(String plate, String version) {
        return "bg:snapshot:plate:" + plate + ":v" + version;
    }

    private String buildStaleKey(String plate) {
        return "bg:snapshot:plate:" + plate + ":stale";
    }

    private String buildLockKey(String plate) {
        return "bg:snapshot:plate:" + plate + ":lock";
    }

    private boolean tryAcquireLock(String plate) {
        try {
            Boolean ok = redisTemplate.opsForValue().setIfAbsent(buildLockKey(plate), "1", Duration.ofSeconds(30));
            return Boolean.TRUE.equals(ok);
        } catch (Exception ignore) {
            return false;
        }
    }

    private void releaseLock(String plate) {
        try {
            redisTemplate.delete(buildLockKey(plate));
        } catch (Exception ignore) {
        }
    }

    private SnapshotPayload readStale(String normalizedPlate) {
        String base64 = redisTemplate.opsForValue().get(buildStaleKey(normalizedPlate));
        if (!StringUtils.hasText(base64)) return null;
        try {
            byte[] gz = Base64.getDecoder().decode(base64.getBytes(StandardCharsets.UTF_8));
            byte[] json = ungzip(gz);
            SnapshotPayload payload = objectMapper.readValue(json, SnapshotPayload.class);
            if (payload.getVersion() == 0) {
                payload.setVersion(System.currentTimeMillis());
            }
            return payload;
        } catch (Exception e) {
            return null;
        }
    }

    private SnapshotPayload refreshSignedUrls(SnapshotPayload payload) {
        if (payload == null || CollectionUtils.isEmpty(payload.getVariants())) {
            return payload;
        }
        payload.getVariants().forEach(variant -> {
            if (variant == null || CollectionUtils.isEmpty(variant.getImages())) {
                return;
            }
            variant.getImages().forEach(this::refreshSignedImage);
        });
        return payload;
    }

    private void refreshSignedImage(VehicleController.ImageDTO image) {
        if (image == null) {
            return;
        }
        String originalObjectName = imageAccessService.resolveObjectNameRef(image.getObjectName());
        String primaryObjectName = imageAccessService.resolveObjectNameRef(image.getUrl());
        String thumbObjectName = imageAccessService.resolveObjectNameRef(image.getThumbnailUrl());

        if (StringUtils.hasText(originalObjectName)
                && StringUtils.hasText(primaryObjectName)
                && StringUtils.hasText(thumbObjectName)
                && primaryObjectName.equals(thumbObjectName)
                && !primaryObjectName.equals(originalObjectName)) {
            // Fix legacy snapshot payloads where url had been downgraded to thumbnail.
            primaryObjectName = originalObjectName;
        }
        // Snapshot is used by detail pages. Keep high-quality path, do not downgrade to thumbnail.
        if (!StringUtils.hasText(primaryObjectName) && StringUtils.hasText(originalObjectName)) {
            primaryObjectName = originalObjectName;
        }
        if (!StringUtils.hasText(primaryObjectName) && StringUtils.hasText(thumbObjectName)) {
            primaryObjectName = thumbObjectName;
        }
        if (!StringUtils.hasText(primaryObjectName)) {
            return;
        }
        image.setUrl(imageAccessService.signPrimaryObject(primaryObjectName));

        if (!StringUtils.hasText(thumbObjectName)) {
            thumbObjectName = primaryObjectName;
        }
        image.setThumbnailUrl(imageAccessService.signThumbnailObject(thumbObjectName));
    }

    private String normalizePlate(String plate) {
        if (plate == null) return null;
        return plate.replaceAll("\\s+", "");
    }

    private byte[] gzip(byte[] input) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(baos)) {
            gos.write(input);
        }
        return baos.toByteArray();
    }

    private byte[] ungzip(byte[] input) throws Exception {
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(input))) {
            return gis.readAllBytes();
        }
    }
}
