package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.auth.UserRole;
import com.busgallery.busgallery.auth.UserSession;
import com.busgallery.busgallery.dto.request.VehicleUpsertPayload;
import com.busgallery.busgallery.entity.*;
import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import com.busgallery.busgallery.repository.VehicleSubmissionRepository;
import com.busgallery.busgallery.service.RegionService;
import com.busgallery.busgallery.service.SubmissionService;
import com.busgallery.busgallery.service.VehicleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final VehicleSubmissionRepository submissionRepository;
    private final VehicleService vehicleService;
    private final RegionService regionService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public VehicleSubmission submitCreate(UserSession session, VehicleUpsertPayload payload, Long imageId) {
        payload.validate();
        return createSubmission(session, payload, imageId, null, SubmissionActionType.CREATE);
    }

    @Override
    @Transactional
    public VehicleSubmission submitUpdate(UserSession session,
                                          VehicleUpsertPayload payload,
                                          Long vehicleId,
                                          Long imageId) {
        if (vehicleId == null) {
            throw new BizException(ErrorCode.INVALID_PARAM, "Vehicle ID is required for update submission");
        }
        payload.setVehicleId(vehicleId);
        payload.validate();
        return createSubmission(session, payload, imageId, vehicleId, SubmissionActionType.UPDATE);
    }

    @Override
    public List<VehicleSubmission> listInbox(UserSession session) {
        if (session.getRole() == UserRole.STATION || session.getRole() == UserRole.REVIEWER) {
            return listPendingForReview(session);
        }
        List<VehicleSubmission> own = submissionRepository.findBySubmitterIdOrderByCreatedAtDesc(session.getUserId());
        return own.stream()
                .filter(item -> item.getStatus() == SubmissionStatus.APPROVED || item.getStatus() == SubmissionStatus.REJECTED)
                .toList();
    }

    @Override
    public List<VehicleSubmission> listPendingForReview(UserSession session) {
        if (session.getRole() == UserRole.STATION) {
            return submissionRepository.findByStatusOrderByCreatedAtAsc(SubmissionStatus.PENDING);
        }
        if (session.getRole() != UserRole.REVIEWER || session.getReviewRegionId() == null) {
            return Collections.emptyList();
        }
        List<Long> scopeIds = resolveReviewScopeRegionIds(session.getReviewRegionId());
        if (scopeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return submissionRepository.findByStatusAndRegionIdInOrderByCreatedAtAsc(SubmissionStatus.PENDING, scopeIds);
    }

    @Override
    public VehicleSubmission findById(Long id) {
        return submissionRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public VehicleSubmission approve(UserSession reviewer, Long submissionId, VehicleUpsertPayload reviewPayload) {
        VehicleSubmission submission = requirePendingSubmission(submissionId);
        checkReviewPermission(reviewer, submission);

        VehicleUpsertPayload payload = reviewPayload != null ? reviewPayload : parsePayload(submission.getRequestPayload());
        if (payload == null) {
            throw new BizException(ErrorCode.INVALID_PARAM, "Submission payload is missing");
        }

        if (submission.getActionType() == SubmissionActionType.CREATE) {
            ensureCreatePayloadImages(payload, submission.getImageId());
            payload.validate();
            vehicleService.create(
                    payload.toVehicle(),
                    payload.toVehicleConfig(),
                    payload.getImageIds(),
                    payload.getBrandId(),
                    payload.getBrandName(),
                    payload.getModelName(),
                    payload.getCompanyName(),
                    payload.getRegionProvince(),
                    payload.getRegionCity()
            );
        } else {
            Long vehicleId = submission.getVehicleId() != null ? submission.getVehicleId() : payload.getVehicleId();
            if (vehicleId == null) {
                throw new BizException(ErrorCode.INVALID_PARAM, "Target vehicle is required for update submission");
            }
            payload.setVehicleId(vehicleId);
            payload.validate();
            Vehicle vehicle = payload.toVehicle();
            vehicle.setId(vehicleId);
            vehicleService.update(
                    vehicle,
                    payload.toVehicleConfig(),
                    payload.getImageIds(),
                    payload.getBrandId(),
                    payload.getBrandName(),
                    payload.getModelName(),
                    payload.getCompanyName(),
                    payload.getRegionProvince(),
                    payload.getRegionCity()
            );
        }

        submission.setStatus(SubmissionStatus.APPROVED);
        submission.setReviewerId(reviewer.getUserId());
        submission.setReviewerUsername(reviewer.getUsername());
        submission.setReviewerDisplayName(reviewer.getDisplayName());
        submission.setReviewedAt(LocalDateTime.now());
        submission.setRejectCode(null);
        submission.setRejectReason(null);
        submission.setReviewPayload(writePayload(payload));
        return submissionRepository.save(submission);
    }

    @Override
    @Transactional
    public VehicleSubmission reject(UserSession reviewer, Long submissionId, String rejectCode, String rejectReason) {
        VehicleSubmission submission = requirePendingSubmission(submissionId);
        checkReviewPermission(reviewer, submission);
        if (!StringUtils.hasText(rejectReason)) {
            throw new BizException(ErrorCode.INVALID_PARAM, "Reject reason is required");
        }

        submission.setStatus(SubmissionStatus.REJECTED);
        submission.setReviewerId(reviewer.getUserId());
        submission.setReviewerUsername(reviewer.getUsername());
        submission.setReviewerDisplayName(reviewer.getDisplayName());
        submission.setReviewedAt(LocalDateTime.now());
        submission.setRejectCode(StringUtils.hasText(rejectCode) ? rejectCode.trim() : "OTHER");
        submission.setRejectReason(rejectReason.trim());
        return submissionRepository.save(submission);
    }

    private VehicleSubmission createSubmission(UserSession session,
                                               VehicleUpsertPayload payload,
                                               Long imageId,
                                               Long vehicleId,
                                               SubmissionActionType actionType) {
        VehicleSubmission submission = new VehicleSubmission();
        submission.setActionType(actionType);
        submission.setStatus(SubmissionStatus.PENDING);
        submission.setSubmitterId(session.getUserId());
        submission.setSubmitterUsername(session.getUsername());
        submission.setSubmitterDisplayName(session.getDisplayName());
        submission.setRegionId(payload.getRegionId());
        submission.setVehicleId(vehicleId);
        submission.setImageId(imageId);
        submission.setRequestPayload(writePayload(payload));
        return submissionRepository.save(submission);
    }

    private VehicleSubmission requirePendingSubmission(Long id) {
        VehicleSubmission submission = submissionRepository.findById(id).orElse(null);
        if (submission == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Submission not found");
        }
        if (submission.getStatus() != SubmissionStatus.PENDING) {
            throw new BizException(ErrorCode.INVALID_PARAM, "Submission has already been processed");
        }
        return submission;
    }

    private void checkReviewPermission(UserSession reviewer, VehicleSubmission submission) {
        if (reviewer.getRole() == UserRole.STATION) {
            return;
        }
        if (reviewer.getRole() != UserRole.REVIEWER) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "Review permission required");
        }
        if (reviewer.getReviewRegionId() == null || submission.getRegionId() == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "No review region assigned");
        }
        List<Long> scopeIds = resolveReviewScopeRegionIds(reviewer.getReviewRegionId());
        if (!scopeIds.contains(submission.getRegionId())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "Submission is outside your review region");
        }
    }

    private List<Long> resolveReviewScopeRegionIds(Long reviewRegionId) {
        if (reviewRegionId == null) {
            return Collections.emptyList();
        }
        List<Long> ids = new java.util.ArrayList<>();
        ids.add(reviewRegionId);
        List<Region> children = regionService.findChildren(reviewRegionId);
        if (children != null) {
            for (Region child : children) {
                if (child != null && child.getId() != null) {
                    ids.add(child.getId());
                }
            }
        }
        return ids;
    }

    private VehicleUpsertPayload parsePayload(String payloadJson) {
        if (!StringUtils.hasText(payloadJson)) {
            return null;
        }
        try {
            return objectMapper.readValue(payloadJson, VehicleUpsertPayload.class);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.INVALID_PARAM, "Invalid submission payload");
        }
    }

    private String writePayload(VehicleUpsertPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.INVALID_PARAM, "Unable to serialize submission payload");
        }
    }

    private void ensureCreatePayloadImages(VehicleUpsertPayload payload, Long imageId) {
        if (payload.getImageIds() == null || payload.getImageIds().isEmpty()) {
            payload.setImageIds(imageId == null ? Collections.emptyList() : List.of(imageId));
            return;
        }
        if (imageId != null && payload.getImageIds().stream().noneMatch(imageId::equals)) {
            payload.getImageIds().add(imageId);
        }
    }
}
