package com.busgallery.busgallery.service;

import com.busgallery.busgallery.controller.VehicleController;
import com.busgallery.busgallery.dto.response.PageResponse;

import java.util.List;

public interface FavoriteService {

    FavoriteSummary setLiked(Long vehicleId, Long userId, boolean liked);

    FavoriteSummary summary(Long vehicleId, Long currentUserId);

    PageResponse<VehicleController.VehicleDetailResponse> listFavorites(Long userId, int page, int size);

    record FavoriteSummary(boolean liked, long total, List<UserLike> topUsers) {
    }

    record UserLike(Long id, String username, String displayName, boolean isSelf) {
    }
}
