package com.busgallery.busgallery.service;

import com.busgallery.busgallery.controller.VehicleController;
import com.busgallery.busgallery.entity.VehicleFavorite;

import java.util.List;

public interface FavoriteService {

    FavoriteSummary toggle(Long vehicleId, Long userId);

    FavoriteSummary summary(Long vehicleId, Long currentUserId);

    List<VehicleController.VehicleDetailResponse> listFavorites(Long userId);

    record FavoriteSummary(boolean liked, long total, List<UserLike> topUsers) {
    }

    record UserLike(Long id, String username, String displayName, boolean isSelf) {
    }
}
