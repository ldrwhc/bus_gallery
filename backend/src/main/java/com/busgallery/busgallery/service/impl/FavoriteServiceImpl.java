package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.controller.VehicleController;
import com.busgallery.busgallery.entity.User;
import com.busgallery.busgallery.entity.VehicleFavorite;
import com.busgallery.busgallery.exception.NotFoundException;
import com.busgallery.busgallery.mapper.VehicleFavoriteMapper;
import com.busgallery.busgallery.repository.UserRepository;
import com.busgallery.busgallery.service.FavoriteService;
import com.busgallery.busgallery.service.ImageService;
import com.busgallery.busgallery.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final VehicleFavoriteMapper favoriteMapper;
    private final VehicleService vehicleService;
    private final ImageService imageService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public FavoriteSummary toggle(Long vehicleId, Long userId) {
        if (vehicleService.findById(vehicleId) == null) {
            throw new NotFoundException("Vehicle not found");
        }
        VehicleFavorite existing = favoriteMapper.select(vehicleId, userId);
        boolean liked;
        if (existing == null) {
            VehicleFavorite favorite = new VehicleFavorite();
            favorite.setVehicleId(vehicleId);
            favorite.setUserId(userId);
            favoriteMapper.insert(favorite);
            liked = true;
        } else {
            favoriteMapper.delete(vehicleId, userId);
            liked = false;
        }
        long total = favoriteMapper.countByVehicle(vehicleId);
        List<UserLike> topUsers = buildTopUsers(vehicleId, userId);
        return new FavoriteSummary(liked, total, topUsers);
    }

    @Override
    public FavoriteSummary summary(Long vehicleId, Long currentUserId) {
        VehicleFavorite existing = favoriteMapper.select(vehicleId, currentUserId);
        boolean liked = existing != null;
        long total = favoriteMapper.countByVehicle(vehicleId);
        List<UserLike> topUsers = buildTopUsers(vehicleId, currentUserId);
        return new FavoriteSummary(liked, total, topUsers);
    }

    @Override
    public List<VehicleController.VehicleDetailResponse> listFavorites(Long userId) {
        List<VehicleFavorite> favorites = favoriteMapper.selectByUser(userId);
        return favorites.stream()
                .map(fav -> {
                    var vehicle = vehicleService.findById(fav.getVehicleId());
                    if (vehicle == null) return null;
                    var config = vehicleService.findConfigByVehicleId(fav.getVehicleId());
                    var images = imageService.listByVehicle(fav.getVehicleId());
                    return VehicleController.assembleDetail(vehicle, config, images);
                })
                .filter(item -> item != null)
                .collect(Collectors.toList());
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
}
