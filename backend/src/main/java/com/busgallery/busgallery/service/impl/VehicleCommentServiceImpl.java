package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.entity.Vehicle;
import com.busgallery.busgallery.entity.VehicleComment;
import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import com.busgallery.busgallery.exception.NotFoundException;
import com.busgallery.busgallery.messaging.BusEventPublisher;
import com.busgallery.busgallery.messaging.payload.CommentCreatedEvent;
import com.busgallery.busgallery.mapper.VehicleCommentMapper;
import com.busgallery.busgallery.mapper.VehicleMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.busgallery.busgallery.service.VehicleCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class VehicleCommentServiceImpl implements VehicleCommentService {

    private static final String COMMENT_SCOPE_PLATE_PREFIX = "plate:";
    private static final String COMMENT_SCOPE_VEHICLE_PREFIX = "vid:";

    private final VehicleCommentMapper commentMapper;
    private final VehicleMapper vehicleMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final BusEventPublisher busEventPublisher;

    @Value("${busgallery.cache.comments.ttl-seconds:30}")
    private long commentCacheTtlSeconds;

    
    @Override
    public List<VehicleComment> list(Long vehicleId, int page, int size) {
        if (vehicleId == null) {
            return Collections.emptyList();
        }
        String scopeKey = resolveCommentScopeKeySafe(vehicleId);
        if (!StringUtils.hasText(scopeKey)) {
            return Collections.emptyList();
        }
        int pageNo = Math.max(page, 1);
        int pageSize = Math.max(size, 1);
        int offset = (pageNo - 1) * pageSize;
        String version = getVersion(scopeKey);
        String cacheKey = listCacheKey(scopeKey, version, pageNo, pageSize);
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null && !cached.isBlank()) {
                return objectMapper.readValue(cached, new TypeReference<List<VehicleComment>>() {});
            }
        } catch (Exception ignore) {
        }
        List<VehicleComment> list = isPlateScope(scopeKey)
                ? commentMapper.selectByPlateNumber(extractScopeValue(scopeKey), offset, pageSize)
                : commentMapper.selectByVehicleId(vehicleId, offset, pageSize);
        try {
            String payload = objectMapper.writeValueAsString(list);
            stringRedisTemplate.opsForValue().set(cacheKey, payload, Duration.ofSeconds(commentCacheTtlSeconds));
        } catch (Exception ignore) {
        }
        return list;
    }

    @Override
    public long count(Long vehicleId) {
        if (vehicleId == null) {
            return 0;
        }
        String scopeKey = resolveCommentScopeKeySafe(vehicleId);
        if (!StringUtils.hasText(scopeKey)) {
            return 0;
        }
        String version = getVersion(scopeKey);
        String cacheKey = countCacheKey(scopeKey, version);
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null && !cached.isBlank()) {
                return Long.parseLong(cached);
            }
        } catch (Exception ignore) {
        }
        long total = isPlateScope(scopeKey)
                ? commentMapper.countByPlateNumber(extractScopeValue(scopeKey))
                : commentMapper.countByVehicleId(vehicleId);
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, String.valueOf(total), Duration.ofSeconds(commentCacheTtlSeconds));
        } catch (Exception ignore) {
        }
        return total;
    }

    @Override
    @Transactional
    public VehicleComment addComment(Long vehicleId, Long userId, String username, String displayName, String content) {
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("Comment content cannot be empty");
        }
        Vehicle vehicle = vehicleMapper.selectById(vehicleId);
        if (vehicle == null) {
            throw new NotFoundException("Vehicle not found");
        }
        VehicleComment comment = new VehicleComment();
        comment.setVehicleId(vehicleId);
        comment.setPlateNumber(normalizePlate(vehicle.getPlateNumber()));
        comment.setUserId(userId);
        comment.setUsername(username);
        comment.setDisplayName(displayName);
        comment.setContent(content.trim());
        commentMapper.insert(comment);
        String scopeKey = buildCommentScopeKey(vehicleId, vehicle.getPlateNumber());
        bumpVersion(scopeKey);
        VehicleComment latest = isPlateScope(scopeKey)
                ? commentMapper.selectByPlateNumber(extractScopeValue(scopeKey), 0, 1).stream().findFirst().orElse(comment)
                : commentMapper.selectByVehicleId(vehicleId, 0, 1).stream().findFirst().orElse(comment);
        busEventPublisher.publishCommentCreated(CommentCreatedEvent.builder()
                .commentId(latest.getId())
                .vehicleId(vehicleId)
                .plateNumber(vehicle.getPlateNumber())
                .userId(userId)
                .username(username)
                .displayName(displayName)
                .content(latest.getContent())
                .createdAt(latest.getCreatedAt())
                .build());
        return latest;
    }

    @Override
    @Transactional
    public void deleteComment(Long vehicleId, Long commentId, Long operatorUserId, boolean stationOperator) {
        if (vehicleId == null || commentId == null) {
            throw new BizException(ErrorCode.INVALID_PARAM, "Invalid vehicle/comment id");
        }
        VehicleComment comment = commentMapper.selectById(commentId);
        if (comment == null || !isSameCommentScope(vehicleId, comment)) {
            throw new NotFoundException("Comment not found");
        }
        boolean owner = operatorUserId != null && operatorUserId.equals(comment.getUserId());
        if (!stationOperator && !owner) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "No permission to delete this comment");
        }
        commentMapper.deleteById(commentId);
        bumpVersion(resolveCommentScopeKey(comment));
    }

    private boolean isSameCommentScope(Long vehicleId, VehicleComment comment) {
        if (comment == null) {
            return false;
        }
        String requestScope = resolveCommentScopeKeySafe(vehicleId);
        String commentScope = resolveCommentScopeKey(comment);
        if (StringUtils.hasText(requestScope) && StringUtils.hasText(commentScope)) {
            return requestScope.equals(commentScope);
        }
        return vehicleId.equals(comment.getVehicleId());
    }

    private String resolveCommentScopeKeySafe(Long vehicleId) {
        try {
            Vehicle vehicle = vehicleMapper.selectById(vehicleId);
            if (vehicle == null) {
                return null;
            }
            return buildCommentScopeKey(vehicleId, vehicle.getPlateNumber());
        } catch (Exception ex) {
            return COMMENT_SCOPE_VEHICLE_PREFIX + vehicleId;
        }
    }

    private String resolveCommentScopeKey(VehicleComment comment) {
        if (comment == null) {
            return null;
        }
        String plate = normalizePlate(comment.getPlateNumber());
        if (StringUtils.hasText(plate)) {
            return COMMENT_SCOPE_PLATE_PREFIX + plate;
        }
        if (comment.getVehicleId() != null) {
            return COMMENT_SCOPE_VEHICLE_PREFIX + comment.getVehicleId();
        }
        return null;
    }

    private String buildCommentScopeKey(Long vehicleId, String plateNumber) {
        String plate = normalizePlate(plateNumber);
        if (StringUtils.hasText(plate)) {
            return COMMENT_SCOPE_PLATE_PREFIX + plate;
        }
        return COMMENT_SCOPE_VEHICLE_PREFIX + vehicleId;
    }

    private String normalizePlate(String plateNumber) {
        if (!StringUtils.hasText(plateNumber)) {
            return null;
        }
        return plateNumber.replaceAll("\\s+", "");
    }

    private boolean isPlateScope(String scopeKey) {
        return StringUtils.hasText(scopeKey) && scopeKey.startsWith(COMMENT_SCOPE_PLATE_PREFIX);
    }

    private String extractScopeValue(String scopeKey) {
        if (!StringUtils.hasText(scopeKey)) {
            return "";
        }
        int idx = scopeKey.indexOf(':');
        return idx >= 0 ? scopeKey.substring(idx + 1) : scopeKey;
    }

    private String versionKey(String scopeKey) {
        return "bg:comments:ver:" + scopeKey;
    }

    private String getVersion(String scopeKey) {
        try {
            String v = stringRedisTemplate.opsForValue().get(versionKey(scopeKey));
            return (v == null || v.isBlank()) ? "1" : v;
        } catch (Exception ignore) {
            return "1";
        }
    }

    private void bumpVersion(String scopeKey) {
        if (!StringUtils.hasText(scopeKey)) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().increment(versionKey(scopeKey));
        } catch (Exception ignore) {
        }
    }

    private String listCacheKey(String scopeKey, String version, int page, int size) {
        return "bg:comments:list:v" + version + ":scope" + scopeKey + ":p" + page + ":s" + size;
    }

    private String countCacheKey(String scopeKey, String version) {
        return "bg:comments:count:v" + version + ":scope" + scopeKey;
    }
}
