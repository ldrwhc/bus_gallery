package com.busgallery.busgallery.service;

import com.busgallery.busgallery.entity.VehicleComment;

import java.util.List;

public interface VehicleCommentService {

    List<VehicleComment> list(Long vehicleId, int page, int size);

    long count(Long vehicleId);

    VehicleComment addComment(Long vehicleId, Long userId, String username, String displayName, String content);
}
