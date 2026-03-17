package com.busgallery.busgallery.service.snapshot;

import com.busgallery.busgallery.controller.VehicleController;
import com.busgallery.busgallery.controller.CommentController;
import com.busgallery.busgallery.service.FavoriteService;
import lombok.Data;

import java.util.List;

/**
 * SnapshotPayload 类。
 */
@Data
public class SnapshotPayload {
    private String plateNumber;
    private long version;
    private List<VehicleController.VehicleDetailResponse> variants;
    private List<CommentController.CommentResponse> comments;
    private List<VehicleController.VehicleSummary> recommendations;
    private FavoriteService.FavoriteSummary favoriteSummary;
}
