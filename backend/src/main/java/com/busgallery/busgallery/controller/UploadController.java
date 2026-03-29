package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.auth.AuthContextHolder;
import com.busgallery.busgallery.auth.AuthPrincipal;
import com.busgallery.busgallery.auth.RequireLogin;
import com.busgallery.busgallery.auth.UserRole;
import com.busgallery.busgallery.dto.request.VehicleUpsertPayload;
import com.busgallery.busgallery.entity.Image;
import com.busgallery.busgallery.entity.Vehicle;
import com.busgallery.busgallery.entity.VehicleSubmission;
import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import com.busgallery.busgallery.service.ChunkUploadService;
import com.busgallery.busgallery.service.IdempotencyService;
import com.busgallery.busgallery.service.ImageService;
import com.busgallery.busgallery.service.SubmissionService;
import com.busgallery.busgallery.service.TradeBridgeService;
import com.busgallery.busgallery.service.UploadSecurityService;
import com.busgallery.busgallery.service.VehicleService;
import com.busgallery.busgallery.util.RequestIpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
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
    private final TradeBridgeService tradeBridgeService;
    private final IdempotencyService idempotencyService;
    private final UploadSecurityService uploadSecurityService;
    private final ChunkUploadService chunkUploadService;
    private final ObjectMapper objectMapper;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequireLogin
    public UploadResultResponse uploadVehicle(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "payload", required = false) String payloadJson,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest httpRequest
    ) {
        VehicleUpsertPayload payload = parsePayload(payloadJson);
        log.info("Upload request received: fileName={}, size={}, plate={}",
                file != null ? file.getOriginalFilename() : "null",
                file != null ? file.getSize() : -1,
                payload != null ? payload.getPlateNumber() : "null");
        AuthPrincipal session = AuthContextHolder.requirePrincipal();
        String clientIp = RequestIpUtil.resolveClientIp(httpRequest);
        uploadSecurityService.checkUploadQuotaAndRate(session, clientIp);
        return idempotencyService.runOnce(
                idempotencyKey,
                Duration.ofMinutes(10),
                () -> handleUpload(file, payload, session)
        );
    }

    @PostMapping("/chunk/init")
    @RequireLogin
    public ChunkUploadService.ChunkProgress initChunkUpload(@RequestBody ChunkInitRequest request,
                                                            HttpServletRequest httpRequest) {
        if (request == null) {
            throw new BizException(ErrorCode.INVALID_PARAM, "request body is required");
        }
        AuthPrincipal session = AuthContextHolder.requirePrincipal();
        String clientIp = RequestIpUtil.resolveClientIp(httpRequest);
        uploadSecurityService.checkUploadQuotaAndRate(session, clientIp);
        return chunkUploadService.init(
                session.getUserId(),
                request.getFileName(),
                request.getContentType(),
                request.getTotalSize(),
                request.getChunkSize(),
                request.getTotalChunks()
        );
    }

    @PostMapping(value = "/chunk/{uploadId}/part", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequireLogin
    public ChunkUploadService.ChunkProgress uploadChunkPart(@PathVariable String uploadId,
                                                            @RequestParam("index") int index,
                                                            @RequestPart("file") MultipartFile file) {
        AuthPrincipal session = AuthContextHolder.requirePrincipal();
        return chunkUploadService.uploadPart(uploadId, session.getUserId(), index, file);
    }

    @GetMapping("/chunk/{uploadId}/progress")
    @RequireLogin
    public ChunkUploadService.ChunkProgress chunkProgress(@PathVariable String uploadId) {
        AuthPrincipal session = AuthContextHolder.requirePrincipal();
        return chunkUploadService.progress(uploadId, session.getUserId());
    }

    @PostMapping("/chunk/{uploadId}/complete")
    @RequireLogin
    public UploadResultResponse completeChunkUpload(@PathVariable String uploadId,
                                                    @RequestBody ChunkCompleteRequest request,
                                                    @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        AuthPrincipal session = AuthContextHolder.requirePrincipal();
        VehicleUpsertPayload payload = requireChunkPayload(request);
        String effectiveIdempotencyKey = StringUtils.hasText(idempotencyKey)
                ? idempotencyKey.trim()
                : "chunk-complete:" + uploadId;
        return idempotencyService.runOnce(effectiveIdempotencyKey, Duration.ofMinutes(10), () -> {
            ChunkUploadService.CompletedChunkFile merged = chunkUploadService.complete(uploadId, session.getUserId());
            try {
                return handleUpload(merged.getMultipartFile(), payload, session);
            } finally {
                chunkUploadService.cleanup(uploadId);
            }
        });
    }

    @DeleteMapping("/chunk/{uploadId}")
    @RequireLogin
    public void cancelChunkUpload(@PathVariable String uploadId) {
        AuthPrincipal session = AuthContextHolder.requirePrincipal();
        chunkUploadService.cancel(uploadId, session.getUserId());
    }

    private UploadResultResponse handleUpload(MultipartFile file, VehicleUpsertPayload payload, AuthPrincipal session) {
        payload.validate();
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

        try {
            tradeBridgeService.resolveOrCreateByImageId(image.getId(), saved.getId());
        } catch (Exception ex) {
            log.warn("Failed to sync trade binding for direct upload imageId={}, vehicleId={}: {}",
                    image.getId(), saved.getId(), ex.getMessage());
        }

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

    private VehicleUpsertPayload requireChunkPayload(ChunkCompleteRequest request) {
        if (request == null || request.getPayload() == null) {
            throw new BizException(ErrorCode.INVALID_PARAM, "payload is required");
        }
        return request.getPayload();
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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChunkInitRequest {
        private String fileName;
        private String contentType;
        private long totalSize;
        private Long chunkSize;
        private Integer totalChunks;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChunkCompleteRequest {
        private VehicleUpsertPayload payload;
    }
}

