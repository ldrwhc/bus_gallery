package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.auth.UserRole;
import com.busgallery.busgallery.auth.AuthPrincipal;
import com.busgallery.busgallery.dto.request.VehicleUpsertPayload;
import com.busgallery.busgallery.entity.*;
import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import com.busgallery.busgallery.repository.VehicleSubmissionRepository;
import com.busgallery.busgallery.service.RegionService;
import com.busgallery.busgallery.service.SubmissionService;
import com.busgallery.busgallery.service.TradeBridgeService;
import com.busgallery.busgallery.service.VehicleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionServiceImpl implements SubmissionService {

    private final VehicleSubmissionRepository submissionRepository;
    private final VehicleService vehicleService;
    private final RegionService regionService;
    private final TradeBridgeService tradeBridgeService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public VehicleSubmission submitCreate(AuthPrincipal session, VehicleUpsertPayload payload, Long imageId) {
        payload.validate();
        return createSubmission(session, payload, imageId, null, SubmissionActionType.CREATE);
    }

    @Override
    @Transactional
    public VehicleSubmission submitUpdate(AuthPrincipal session,
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
    public List<VehicleSubmission> listInbox(AuthPrincipal session) {
        if (session.getRole() == UserRole.STATION || session.getRole() == UserRole.REVIEWER) {
            return listPendingForReview(session);
        }
        List<VehicleSubmission> own = submissionRepository.findBySubmitterIdOrderByCreatedAtDesc(session.getUserId());
        return own.stream()
                .filter(item -> item.getStatus() == SubmissionStatus.APPROVED || item.getStatus() == SubmissionStatus.REJECTED)
                .toList();
    }

    @Override
    public List<VehicleSubmission> listPendingForReview(AuthPrincipal session) {
        if (session.getRole() == UserRole.STATION) {
            return submissionRepository.findByStatusOrderByCreatedAtAsc(SubmissionStatus.PENDING);
        }
        if (session.getRole() != UserRole.REVIEWER || session.getReviewRegionId() == null) {
            return Collections.emptyList();
        }

        Long reviewProvinceId = resolveProvinceRegionId(session.getReviewRegionId());
        if (reviewProvinceId == null) {
            return Collections.emptyList();
        }
        List<Long> scopeIds = resolveReviewScopeRegionIds(reviewProvinceId);
        if (scopeIds.isEmpty()) {
            scopeIds = List.of(reviewProvinceId);
        }

        Map<Long, VehicleSubmission> merged = new LinkedHashMap<>();
        mergeSubmissions(
                merged,
                submissionRepository.findByStatusAndProvinceRegionIdInOrderByCreatedAtAsc(
                        SubmissionStatus.PENDING,
                        List.of(reviewProvinceId)
                ),
                false
        );
        mergeSubmissions(
                merged,
                submissionRepository.findByStatusAndRegionIdInOrderByCreatedAtAsc(SubmissionStatus.PENDING, scopeIds),
                true
        );
        mergeSubmissions(
                merged,
                submissionRepository.findByStatusAndRegionIdIsNullOrderByCreatedAtAsc(SubmissionStatus.PENDING),
                true
        );

        return merged.values().stream()
                .filter(item -> isInReviewerProvince(item, reviewProvinceId))
                .sorted(Comparator
                        .comparing(VehicleSubmission::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(VehicleSubmission::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    @Override
    public VehicleSubmission findById(Long id) {
        return submissionRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public VehicleSubmission approve(AuthPrincipal reviewer, Long submissionId, VehicleUpsertPayload reviewPayload) {
        VehicleSubmission submission = requirePendingSubmission(submissionId);
        checkReviewPermission(reviewer, submission);

        VehicleUpsertPayload payload = reviewPayload != null ? reviewPayload : parsePayload(submission.getRequestPayload());
        if (payload == null) {
            throw new BizException(ErrorCode.INVALID_PARAM, "Submission payload is missing");
        }

        Long effectiveVehicleId;
        if (submission.getActionType() == SubmissionActionType.CREATE) {
            ensureCreatePayloadImages(payload, submission.getImageId());
            payload.validate();
            Vehicle created = vehicleService.create(
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
            effectiveVehicleId = created != null ? created.getId() : null;
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
            effectiveVehicleId = vehicleId;
        }

        syncTradeBindingsQuietly(effectiveVehicleId, payload, submission);

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

    private void syncTradeBindingsQuietly(Long vehicleId, VehicleUpsertPayload payload, VehicleSubmission submission) {
        Set<Long> imageIds = new HashSet<>();
        if (payload != null && payload.getImageIds() != null) {
            imageIds.addAll(payload.getImageIds());
        }
        if (submission != null && submission.getImageId() != null) {
            imageIds.add(submission.getImageId());
        }
        imageIds.removeIf(Objects::isNull);
        for (Long imageId : imageIds) {
            try {
                tradeBridgeService.resolveOrCreateByImageId(imageId, vehicleId);
            } catch (Exception ex) {
                log.warn("Skip trade binding sync for imageId={}, vehicleId={}: {}", imageId, vehicleId, ex.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public VehicleSubmission reject(AuthPrincipal reviewer, Long submissionId, String rejectCode, String rejectReason) {
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

    private VehicleSubmission createSubmission(AuthPrincipal session,
                                               VehicleUpsertPayload payload,
                                               Long imageId,
                                               Long vehicleId,
                                               SubmissionActionType actionType) {
        RegionResolution resolution = resolveRegionFromPayload(payload);
        VehicleSubmission submission = new VehicleSubmission();
        submission.setActionType(actionType);
        submission.setStatus(SubmissionStatus.PENDING);
        submission.setSubmitterId(session.getUserId());
        submission.setSubmitterUsername(session.getUsername());
        submission.setSubmitterDisplayName(session.getDisplayName());
        submission.setRegionId(resolution.regionId);
        submission.setProvinceRegionId(resolution.provinceRegionId);
        submission.setCityRegionId(resolution.cityRegionId);
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

    private void checkReviewPermission(AuthPrincipal reviewer, VehicleSubmission submission) {
        if (reviewer.getRole() == UserRole.STATION) {
            return;
        }
        if (reviewer.getRole() != UserRole.REVIEWER) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "Review permission required");
        }
        Long reviewerProvinceId = resolveProvinceRegionId(reviewer.getReviewRegionId());
        if (reviewerProvinceId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "No review region assigned");
        }
        RegionResolution resolution = normalizeSubmissionRegion(submission, true);
        if (resolution.provinceRegionId == null || !reviewerProvinceId.equals(resolution.provinceRegionId)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "Submission is outside your review region");
        }
    }

    private List<Long> resolveReviewScopeRegionIds(Long reviewRegionId) {
        if (reviewRegionId == null) {
            return Collections.emptyList();
        }
        Set<Long> visited = new HashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        queue.add(reviewRegionId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (current == null || !visited.add(current)) {
                continue;
            }
            List<Region> children = regionService.findChildren(current);
            if (children == null || children.isEmpty()) {
                continue;
            }
            for (Region child : children) {
                if (child != null && child.getId() != null && !visited.contains(child.getId())) {
                    queue.add(child.getId());
                }
            }
        }
        return new ArrayList<>(visited);
    }

    private void mergeSubmissions(Map<Long, VehicleSubmission> merged,
                                  List<VehicleSubmission> source,
                                  boolean normalizeRegion) {
        if (source == null || source.isEmpty()) {
            return;
        }
        for (VehicleSubmission item : source) {
            if (item == null || item.getId() == null) {
                continue;
            }
            if (normalizeRegion) {
                normalizeSubmissionRegion(item, true);
            }
            merged.putIfAbsent(item.getId(), item);
        }
    }

    private boolean isInReviewerProvince(VehicleSubmission submission, Long reviewerProvinceId) {
        if (submission == null || reviewerProvinceId == null) {
            return false;
        }
        RegionResolution resolution = normalizeSubmissionRegion(submission, true);
        return reviewerProvinceId.equals(resolution.provinceRegionId);
    }

    private Long resolveProvinceRegionId(Long regionId) {
        if (regionId == null) {
            return null;
        }
        Long provinceId = regionService.resolveProvinceId(regionId);
        return provinceId != null ? provinceId : regionId;
    }

    private RegionResolution normalizeSubmissionRegion(VehicleSubmission submission, boolean persist) {
        if (submission == null) {
            return RegionResolution.empty();
        }
        RegionResolution resolved = resolveRegionFromSubmission(submission);
        boolean changed = !Objects.equals(submission.getRegionId(), resolved.regionId)
                || !Objects.equals(submission.getProvinceRegionId(), resolved.provinceRegionId)
                || !Objects.equals(submission.getCityRegionId(), resolved.cityRegionId);
        if (changed) {
            submission.setRegionId(resolved.regionId);
            submission.setProvinceRegionId(resolved.provinceRegionId);
            submission.setCityRegionId(resolved.cityRegionId);
            if (persist && submission.getId() != null) {
                submissionRepository.save(submission);
            }
        }
        return resolved;
    }

    private RegionResolution resolveRegionFromSubmission(VehicleSubmission submission) {
        Long regionId = submission.getRegionId();
        Long provinceRegionId = submission.getProvinceRegionId();
        Long cityRegionId = submission.getCityRegionId();

        RegionResolution payloadResolution = resolveRegionFromPayload(parsePayloadQuietly(submission.getRequestPayload()));
        if (regionId == null) {
            regionId = payloadResolution.regionId;
        }
        if (provinceRegionId == null) {
            provinceRegionId = payloadResolution.provinceRegionId;
        }
        if (cityRegionId == null) {
            cityRegionId = payloadResolution.cityRegionId;
        }

        if (regionId != null) {
            Long regionProvinceId = resolveProvinceRegionId(regionId);
            if (provinceRegionId == null) {
                provinceRegionId = regionProvinceId;
            }
            Region region = regionService.findById(regionId);
            if (region != null && region.getParentId() != null && cityRegionId == null) {
                cityRegionId = regionId;
            }
        }

        if (cityRegionId != null && provinceRegionId == null) {
            provinceRegionId = resolveProvinceRegionId(cityRegionId);
        }
        if (regionId == null) {
            regionId = cityRegionId != null ? cityRegionId : provinceRegionId;
        }
        if (provinceRegionId == null && regionId != null) {
            provinceRegionId = resolveProvinceRegionId(regionId);
        }
        if (cityRegionId == null && regionId != null && provinceRegionId != null && !regionId.equals(provinceRegionId)) {
            cityRegionId = regionId;
        }

        return new RegionResolution(regionId, provinceRegionId, cityRegionId);
    }

    private RegionResolution resolveRegionFromPayload(VehicleUpsertPayload payload) {
        if (payload == null) {
            return RegionResolution.empty();
        }
        Long regionId = payload.getRegionId();
        Long provinceRegionId = null;
        Long cityRegionId = null;

        if (regionId != null) {
            Region region = regionService.findById(regionId);
            if (region != null) {
                provinceRegionId = resolveProvinceRegionId(regionId);
                if (region.getParentId() != null && (provinceRegionId == null || !regionId.equals(provinceRegionId))) {
                    cityRegionId = regionId;
                }
                if (provinceRegionId == null) {
                    provinceRegionId = regionId;
                }
                return new RegionResolution(regionId, provinceRegionId, cityRegionId);
            }
        }

        String provinceName = trimToNull(payload.getRegionProvince());
        String cityName = trimToNull(payload.getRegionCity());
        if (provinceName == null && cityName == null) {
            return RegionResolution.empty();
        }

        Region province = provinceName == null ? null : regionService.findProvinceByName(provinceName);
        if (province == null && cityName != null) {
            province = regionService.findProvinceByName(cityName);
        }
        if (province != null) {
            provinceRegionId = province.getId();
        }

        Region city = null;
        if (cityName != null) {
            city = regionService.findCityByNameAndProvince(cityName, provinceRegionId);
            if (city != null) {
                cityRegionId = city.getId();
                if (provinceRegionId == null) {
                    provinceRegionId = resolveProvinceRegionId(cityRegionId);
                }
            }
        }

        regionId = cityRegionId != null ? cityRegionId : provinceRegionId;
        if (provinceRegionId == null && regionId != null) {
            provinceRegionId = resolveProvinceRegionId(regionId);
        }
        if (cityRegionId == null && regionId != null && provinceRegionId != null && !regionId.equals(provinceRegionId)) {
            cityRegionId = regionId;
        }
        return new RegionResolution(regionId, provinceRegionId, cityRegionId);
    }

    private VehicleUpsertPayload parsePayloadQuietly(String payloadJson) {
        if (!StringUtils.hasText(payloadJson)) {
            return null;
        }
        try {
            return objectMapper.readValue(payloadJson, VehicleUpsertPayload.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String trimToNull(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String value = text.trim();
        return value.isEmpty() ? null : value;
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

    private static final class RegionResolution {
        private final Long regionId;
        private final Long provinceRegionId;
        private final Long cityRegionId;

        private RegionResolution(Long regionId, Long provinceRegionId, Long cityRegionId) {
            this.regionId = regionId;
            this.provinceRegionId = provinceRegionId;
            this.cityRegionId = cityRegionId;
        }

        private static RegionResolution empty() {
            return new RegionResolution(null, null, null);
        }
    }
}
