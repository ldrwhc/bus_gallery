package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.entity.Vehicle;
import com.busgallery.busgallery.entity.VehicleComment;
import com.busgallery.busgallery.exception.NotFoundException;
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

    private final VehicleCommentMapper commentMapper;
    private final VehicleMapper vehicleMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${busgallery.cache.comments.ttl-seconds:30}")
    private long commentCacheTtlSeconds;

    
    @Override
    public List<VehicleComment> list(Long vehicleId, int page, int size) {
        if (vehicleId == null) {
            return Collections.emptyList();
        }
        int pageNo = Math.max(page, 1);
        int pageSize = Math.max(size, 1);
        int offset = (pageNo - 1) * pageSize;
        String version = getVersion(vehicleId);
        String cacheKey = listCacheKey(vehicleId, version, pageNo, pageSize);
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null && !cached.isBlank()) {
                return objectMapper.readValue(cached, new TypeReference<List<VehicleComment>>() {});
            }
        } catch (Exception ignore) {
        }
        List<VehicleComment> list = commentMapper.selectByVehicleId(vehicleId, offset, pageSize);
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
        String version = getVersion(vehicleId);
        String cacheKey = countCacheKey(vehicleId, version);
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null && !cached.isBlank()) {
                return Long.parseLong(cached);
            }
        } catch (Exception ignore) {
        }
        long total = commentMapper.countByVehicleId(vehicleId);
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
        comment.setUserId(userId);
        comment.setUsername(username);
        comment.setDisplayName(displayName);
        comment.setContent(content.trim());
        commentMapper.insert(comment);
        bumpVersion(vehicleId);
        return commentMapper.selectByVehicleId(vehicleId, 0, 1).stream().findFirst().orElse(comment);
    }

    private String versionKey(Long vehicleId) {
        return "bg:comments:ver:" + vehicleId;
    }

    private String getVersion(Long vehicleId) {
        try {
            String v = stringRedisTemplate.opsForValue().get(versionKey(vehicleId));
            return (v == null || v.isBlank()) ? "1" : v;
        } catch (Exception ignore) {
            return "1";
        }
    }

    private void bumpVersion(Long vehicleId) {
        try {
            stringRedisTemplate.opsForValue().increment(versionKey(vehicleId));
        } catch (Exception ignore) {
        }
    }

    private String listCacheKey(Long vehicleId, String version, int page, int size) {
        return "bg:comments:list:v" + version + ":vid" + vehicleId + ":p" + page + ":s" + size;
    }

    private String countCacheKey(Long vehicleId, String version) {
        return "bg:comments:count:v" + version + ":vid" + vehicleId;
    }
}
