package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.auth.AuthContextHolder;
import com.busgallery.busgallery.auth.RequireLogin;
import com.busgallery.busgallery.auth.UserRole;
import com.busgallery.busgallery.auth.UserSession;
import com.busgallery.busgallery.dto.response.PageResponse;
import com.busgallery.busgallery.entity.VehicleComment;
import com.busgallery.busgallery.service.VehicleCommentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vehicles/{vehicleId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final VehicleCommentService commentService;

    @GetMapping
    public PageResponse<CommentResponse> list(@PathVariable Long vehicleId,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        List<CommentResponse> records = commentService.list(vehicleId, page, size)
                .stream()
                .map(CommentResponse::from)
                .collect(Collectors.toList());
        long total = commentService.count(vehicleId);
        return PageResponse.of(records, total, page, size);
    }

    @PostMapping
    @RequireLogin
    public CommentResponse create(@PathVariable Long vehicleId,
                                  @Valid @RequestBody CommentCreateRequest request) {
        UserSession session = AuthContextHolder.requireUser();
        VehicleComment saved = commentService.addComment(
                vehicleId,
                session.getUserId(),
                session.getUsername(),
                session.getDisplayName(),
                request.getContent()
        );
        return CommentResponse.from(saved);
    }

    @DeleteMapping("/{commentId}")
    @RequireLogin
    public void delete(@PathVariable Long vehicleId, @PathVariable Long commentId) {
        UserSession session = AuthContextHolder.requireUser();
        commentService.deleteComment(
                vehicleId,
                commentId,
                session.getUserId(),
                session.getRole() == UserRole.STATION
        );
    }

    @Data
    public static class CommentCreateRequest {
        @NotBlank(message = "Comment content is required")
        @Size(max = 500, message = "Comment is too long")
        private String content;
    }

    @Data
    public static class CommentResponse {
        private Long id;
        private Long vehicleId;
        private Long userId;
        private String displayName;
        private String username;
        private String content;
        private String createdAt;

        public static CommentResponse from(VehicleComment comment) {
            CommentResponse response = new CommentResponse();
            response.setId(comment.getId());
            response.setVehicleId(comment.getVehicleId());
            response.setUserId(comment.getUserId());
            response.setDisplayName(comment.getDisplayName());
            response.setUsername(comment.getUsername());
            response.setContent(comment.getContent());
            response.setCreatedAt(comment.getCreatedAt() != null ? comment.getCreatedAt().toString() : null);
            return response;
        }
    }
}
