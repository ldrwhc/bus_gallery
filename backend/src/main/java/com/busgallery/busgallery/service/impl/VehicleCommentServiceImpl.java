package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.entity.Vehicle;
import com.busgallery.busgallery.entity.VehicleComment;
import com.busgallery.busgallery.exception.NotFoundException;
import com.busgallery.busgallery.mapper.VehicleCommentMapper;
import com.busgallery.busgallery.mapper.VehicleMapper;
import com.busgallery.busgallery.service.VehicleCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleCommentServiceImpl implements VehicleCommentService {

    private final VehicleCommentMapper commentMapper;
    private final VehicleMapper vehicleMapper;

    @Override
    public List<VehicleComment> list(Long vehicleId, int page, int size) {
        if (vehicleId == null) {
            return Collections.emptyList();
        }
        int pageNo = Math.max(page, 1);
        int pageSize = Math.max(size, 1);
        int offset = (pageNo - 1) * pageSize;
        return commentMapper.selectByVehicleId(vehicleId, offset, pageSize);
    }

    @Override
    public long count(Long vehicleId) {
        if (vehicleId == null) {
            return 0;
        }
        return commentMapper.countByVehicleId(vehicleId);
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
        return commentMapper.selectByVehicleId(vehicleId, 0, 1).stream().findFirst().orElse(comment);
    }
}
