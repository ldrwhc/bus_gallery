package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.auth.AuthContextHolder;
import com.busgallery.busgallery.auth.RequireLogin;
import com.busgallery.busgallery.auth.UserRole;
import com.busgallery.busgallery.auth.UserSession;
import com.busgallery.busgallery.dto.request.VehicleUpsertPayload;
import com.busgallery.busgallery.entity.Image;
import com.busgallery.busgallery.entity.Vehicle;
import com.busgallery.busgallery.entity.VehicleSubmission;
import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import com.busgallery.busgallery.service.IdempotencyService;
import com.busgallery.busgallery.service.ImageService;
import com.busgallery.busgallery.service.SubmissionService;
import com.busgallery.busgallery.service.VehicleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Slf4j
public class UploadController {

    private final VehicleService vehicleService;
    private final ImageService imageService;
    private final SubmissionService submissionService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequireLogin
    @Transactional(timeout = 30)
    public UploadResultResponse uploadVehicle(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "payload", required = false) String payloadJson,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        VehicleUpsertPayload payload = parsePayload(payloadJson);
        log.info("Upload request received: fileName={}, size={}, plate={}",
                file != null ? file.getOriginalFilename() : "null",
                file != null ? file.getSize() : -1,
                payload != null ? payload.getPlateNumber() : "null");
        return idempotencyService.runOnce(
                idempotencyKey,
                Duration.ofMinutes(10),
                () -> handleUpload(file, payload)
        );
    }

    private UploadResultResponse handleUpload(MultipartFile file, VehicleUpsertPayload payload) {
        payload.validate();

        UserSession session = AuthContextHolder.requireUser();
        Image metadata = new Image();
        metadata.setUploadUser(StringUtils.hasText(session.getDisplayName()) ? session.getDisplayName() : session.getUsername());
        metadata.setUploaderId(session.getUserId());
        metadata.setUploaderUsername(session.getUsername());
        metadata.setUploaderDisplayName(session.getDisplayName());

        Image image = imageService.uploadAndSave(file, metadata);

        if (session.getRole() == UserRole.USER) {
            VehicleSubmission submission = submissionService.submitCreate(session, payload, image.getId());
            return UploadResultResponse.pending(submission.getId());
        }

        payload.setImageIds(Collections.singletonList(image.getId()));
        Vehicle vehicle = payload.toVehicle();

        Vehicle saved = vehicleService.create(
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

        Vehicle detailVehicle = vehicleService.findById(saved.getId());
        var detailConfig = vehicleService.findConfigByVehicleId(saved.getId());
        List<Image> detailImages = imageService.listByVehicle(saved.getId());

        return UploadResultResponse.approved(VehicleController.assembleDetail(detailVehicle, detailConfig, detailImages));
    }

    private VehicleUpsertPayload parsePayload(String payloadJson) {
        if (!StringUtils.hasText(payloadJson)) {
            throw new BizException(ErrorCode.INVALID_PARAM, "payload is required");
        }
        try {
            return objectMapper.readValue(payloadJson, VehicleUpsertPayload.class);
        } catch (Exception e) {
            throw new BizException(ErrorCode.INVALID_PARAM, "Invalid payload format");
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadResultResponse {
        private String status;
        private Long submissionId;
        private VehicleController.VehicleDetailResponse detail;

        public static UploadResultResponse pending(Long submissionId) {
            return new UploadResultResponse("PENDING", submissionId, null);
        }

        public static UploadResultResponse approved(VehicleController.VehicleDetailResponse detail) {
            return new UploadResultResponse("APPROVED", null, detail);
        }
    }
}
