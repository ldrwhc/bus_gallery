package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.auth.AuthContextHolder;
import com.busgallery.busgallery.auth.RequireLogin;
import com.busgallery.busgallery.auth.UserSession;
import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import com.busgallery.busgallery.service.FavoriteService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PutMapping("/{vehicleId}")
    @RequireLogin
    public FavoriteResponse setLiked(@PathVariable Long vehicleId, @RequestBody SetFavoriteRequest request) {
        if (request == null || request.getLiked() == null) {
            throw new BizException(ErrorCode.INVALID_PARAM, "liked is required");
        }
        Long userId = AuthContextHolder.requireUser().getUserId();
        FavoriteService.FavoriteSummary summary = favoriteService.setLiked(vehicleId, userId, request.getLiked());
        return FavoriteResponse.from(summary);
    }

    @GetMapping("/{vehicleId}/summary")
    public FavoriteResponse summary(@PathVariable Long vehicleId) {
        UserSession session = AuthContextHolder.get();
        Long currentUserId = session != null ? session.getUserId() : null;
        FavoriteService.FavoriteSummary summary = favoriteService.summary(vehicleId, currentUserId);
        return FavoriteResponse.from(summary);
    }

    @Data
    public static class SetFavoriteRequest {
        private Boolean liked;
    }

    @GetMapping
    public List<VehicleController.VehicleDetailResponse> listFavorites(@RequestParam(required = false) Long userId) {
        Long target = userId;
        if (target == null) {
            // fallback to current user when authed
            target = AuthContextHolder.get() != null ? AuthContextHolder.get().getUserId() : null;
        }
        if (target == null) {
            throw new com.busgallery.busgallery.exception.BizException(
                    com.busgallery.busgallery.exception.ErrorCode.UNAUTHORIZED, "请先登录查看收藏");
        }
        return favoriteService.listFavorites(target);
    }

    @Data
    public static class FavoriteResponse {
        private boolean liked;
        private long total;
        private List<UserLike> topUsers;

        public static FavoriteResponse from(FavoriteService.FavoriteSummary summary) {
            FavoriteResponse resp = new FavoriteResponse();
            resp.setLiked(summary.liked());
            resp.setTotal(summary.total());
            resp.setTopUsers(summary.topUsers().stream()
                    .map(u -> new UserLike(u.id(), u.username(), u.displayName(), u.isSelf()))
                    .toList());
            return resp;
        }
    }

    @Data
    public static class UserLike {
        private Long id;
        private String username;
        private String displayName;
        private boolean isSelf;

        public UserLike(Long id, String username, String displayName, boolean isSelf) {
            this.id = id;
            this.username = username;
            this.displayName = displayName;
            this.isSelf = isSelf;
        }
    }
}
