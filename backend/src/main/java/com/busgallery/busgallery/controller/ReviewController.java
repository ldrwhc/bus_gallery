package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.auth.AuthContextHolder;
import com.busgallery.busgallery.auth.RequireLogin;
import com.busgallery.busgallery.auth.RoleGuard;
import com.busgallery.busgallery.auth.UserRole;
import com.busgallery.busgallery.auth.UserSession;
import com.busgallery.busgallery.dto.request.ReviewApproveRequest;
import com.busgallery.busgallery.dto.request.ReviewRejectRequest;
import com.busgallery.busgallery.dto.request.VehicleUpsertPayload;
import com.busgallery.busgallery.dto.response.SubmissionResponse;
import com.busgallery.busgallery.entity.Image;
import com.busgallery.busgallery.entity.VehicleSubmission;
import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import com.busgallery.busgallery.service.ImageService;
import com.busgallery.busgallery.service.SubmissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final SubmissionService submissionService;
    private final ImageService imageService;
    private final ObjectMapper objectMapper;

    @GetMapping("/inbox")
    @RequireLogin
    public List<SubmissionResponse> inbox() {
        UserSession session = AuthContextHolder.requireUser();
        return submissionService.listInbox(session).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/pending")
    @RequireLogin
    public List<SubmissionResponse> pending() {
        UserSession session = RoleGuard.requireReviewerOrStation();
        return submissionService.listPendingForReview(session).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @PostMapping("/submissions/update")
    @RequireLogin
    public SubmissionResponse submitUpdate(@RequestBody UpdateSubmissionRequest request) {
        UserSession session = AuthContextHolder.requireUser();
        if (request == null || request.getPayload() == null) {
            throw new BizException(ErrorCode.INVALID_PARAM, "Update payload is required");
        }
        if (session.getRole() != UserRole.USER) {
            throw new BizException(ErrorCode.INVALID_PARAM, "Only normal users submit update requests here");
        }
        VehicleUpsertPayload payload = request.getPayload();
        Long vehicleId = request.getVehicleId() != null ? request.getVehicleId() : payload.getVehicleId();
        if (vehicleId == null) {
            throw new BizException(ErrorCode.INVALID_PARAM, "Vehicle ID is required");
        }
        Long imageId = request.getImageId();
        if (imageId != null) {
            Image image = imageService.findById(imageId);
            if (image == null || !session.getUserId().equals(image.getUploaderId())) {
                throw new BizException(ErrorCode.UNAUTHORIZED, "Image does not belong to current user");
            }
        } else {
            Image ownImage = imageService.listByVehicle(vehicleId).stream()
                    .filter(item -> session.getUserId().equals(item.getUploaderId()))
                    .findFirst()
                    .orElse(null);
            if (ownImage == null) {
                throw new BizException(ErrorCode.UNAUTHORIZED, "Vehicle does not contain your uploaded image");
            }
            imageId = ownImage.getId();
        }
        VehicleSubmission submission = submissionService.submitUpdate(
                session,
                payload,
                vehicleId,
                imageId
        );
        return toResponse(submission);
    }

    @PostMapping("/{id}/approve")
    @RequireLogin
    public SubmissionResponse approve(@PathVariable Long id,
                                      @RequestBody(required = false) ReviewApproveRequest request) {
        UserSession reviewer = RoleGuard.requireReviewerOrStation();
        VehicleSubmission submission = submissionService.approve(
                reviewer,
                id,
                request == null ? null : request.getPayload()
        );
        return toResponse(submission);
    }

    @PostMapping("/{id}/reject")
    @RequireLogin
    public SubmissionResponse reject(@PathVariable Long id,
                                     @RequestBody ReviewRejectRequest request) {
        UserSession reviewer = RoleGuard.requireReviewerOrStation();
        if (request == null || !StringUtils.hasText(request.getRejectReason())) {
            throw new BizException(ErrorCode.INVALID_PARAM, "Reject reason is required");
        }
        VehicleSubmission submission = submissionService.reject(
                reviewer,
                id,
                request.getRejectCode(),
                request.getRejectReason()
        );
        return toResponse(submission);
    }

    private SubmissionResponse toResponse(VehicleSubmission item) {
        Image image = item.getImageId() == null ? null : imageService.findById(item.getImageId());
        return SubmissionResponse.builder()
                .id(item.getId())
                .actionType(item.getActionType())
                .status(item.getStatus())
                .submitterId(item.getSubmitterId())
                .submitterUsername(item.getSubmitterUsername())
                .submitterDisplayName(item.getSubmitterDisplayName())
                .reviewerId(item.getReviewerId())
                .reviewerUsername(item.getReviewerUsername())
                .reviewerDisplayName(item.getReviewerDisplayName())
                .regionId(item.getRegionId())
                .provinceRegionId(item.getProvinceRegionId())
                .cityRegionId(item.getCityRegionId())
                .vehicleId(item.getVehicleId())
                .imageId(item.getImageId())
                .imageUrl(image != null ? image.getUrl() : null)
                .imageThumbnailUrl(image != null ? image.getThumbnailUrl() : null)
                .rejectCode(item.getRejectCode())
                .rejectReason(item.getRejectReason())
                .requestPayload(readPayload(item.getRequestPayload()))
                .reviewPayload(readPayload(item.getReviewPayload()))
                .createdAt(item.getCreatedAt())
                .reviewedAt(item.getReviewedAt())
                .build();
    }

    private VehicleUpsertPayload readPayload(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, VehicleUpsertPayload.class);
        } catch (Exception ex) {
            return null;
        }
    }

    @lombok.Data
    public static class UpdateSubmissionRequest {
        private Long vehicleId;
        private Long imageId;
        private VehicleUpsertPayload payload;
    }
}
