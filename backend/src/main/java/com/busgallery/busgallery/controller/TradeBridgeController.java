package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.auth.AuthContextHolder;
import com.busgallery.busgallery.auth.AuthPrincipal;
import com.busgallery.busgallery.auth.RequireLogin;
import com.busgallery.busgallery.dto.response.TradeBindingResponse;
import com.busgallery.busgallery.entity.Image;
import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import com.busgallery.busgallery.service.ImageAccessService;
import com.busgallery.busgallery.service.ImageService;
import com.busgallery.busgallery.service.TradeBridgeService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * Trade bridge APIs for content platform to resolve/create trade bindings.
 */
@RestController
@RequestMapping("/api/trade-bridge")
@RequiredArgsConstructor
public class TradeBridgeController {

    private final TradeBridgeService tradeBridgeService;
    private final JdbcTemplate jdbcTemplate;
    private final ImageService imageService;
    private final ImageAccessService imageAccessService;

    /**
     * Resolve or create one image binding in trade_center.
     *
     * @param imageId  content image id
     * @param request  optional hints
     * @return binding payload including goodsId/activityId
     */
    @PostMapping("/images/{imageId}/binding")
    @RequireLogin
    public TradeBindingResponse resolveImageBinding(@PathVariable Long imageId,
                                                    @RequestBody(required = false) ResolveBindingRequest request) {
        Long preferredVehicleId = request == null ? null : request.getVehicleId();
        return tradeBridgeService.resolveOrCreateByImageId(imageId, preferredVehicleId);
    }

    /**
     * Query one existing goods binding snapshot.
     *
     * @param goodsId trade goods id
     * @return binding payload including imageId/activityId
     */
    @GetMapping("/goods/{goodsId}/binding")
    @RequireLogin
    public TradeBindingResponse queryGoodsBinding(@PathVariable String goodsId) {
        return tradeBridgeService.queryByGoodsId(goodsId);
    }

    /**
     * Download original image by paid trade record.
     * This is a permanent endpoint link, but permission is checked on every request.
     *
     * @param recordId trade record id
     * @return attachment stream
     */
    @GetMapping("/purchases/{recordId}/download")
    @RequireLogin
    public ResponseEntity<InputStreamResource> downloadPurchasedOriginal(@PathVariable String recordId) {
        AuthPrincipal principal = AuthContextHolder.requirePrincipal();
        PurchaseRow row = jdbcTemplate.query(
                "SELECT image_id, can_download FROM trade_center.trade_user_record " +
                        "WHERE record_id = ? AND app_user_id = ? LIMIT 1",
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    Number imageIdNum = (Number) rs.getObject("image_id");
                    Long imageId = imageIdNum == null ? null : imageIdNum.longValue();
                    return new PurchaseRow(imageId, rs.getInt("can_download"));
                },
                recordId,
                principal.getUserId()
        );
        if (row == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Trade record not found");
        }
        if (row.canDownload() != 1 || row.imageId() == null) {
            throw new BizException(ErrorCode.INVALID_PARAM, "This record is not downloadable yet");
        }

        Image image = imageService.findRawById(row.imageId());
        if (image == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Image not found");
        }
        String objectName = image.getObjectName();
        if (!StringUtils.hasText(objectName)) {
            objectName = imageAccessService.resolveObjectNameRef(image.getUrl());
        }
        if (!StringUtils.hasText(objectName)) {
            throw new BizException(ErrorCode.NOT_FOUND, "Original object not found");
        }

        String signedUrl = imageAccessService.signPrimaryObject(objectName);
        String token = extractAccessToken(signedUrl);
        if (!StringUtils.hasText(token)) {
            throw new BizException(ErrorCode.STORAGE_ERROR, "Failed to build download token");
        }
        ImageAccessService.SignedImageStream stream = imageAccessService.resolveStream(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + buildDownloadName(objectName, row.imageId()) + "\"")
                .contentLength(Math.max(0, stream.getContentLength()))
                .contentType(MediaType.parseMediaType(stream.getContentType()))
                .body(new InputStreamResource(stream.getInputStream()));
    }

    private String extractAccessToken(String signedUrl) {
        if (!StringUtils.hasText(signedUrl)) {
            return "";
        }
        int marker = signedUrl.indexOf("/api/images/access/");
        if (marker < 0) {
            return "";
        }
        return signedUrl.substring(marker + "/api/images/access/".length());
    }

    private String buildDownloadName(String objectName, Long imageId) {
        String ext = ".jpg";
        if (StringUtils.hasText(objectName)) {
            int dot = objectName.lastIndexOf('.');
            if (dot >= 0 && dot < objectName.length() - 1) {
                ext = objectName.substring(dot);
            }
        }
        return "original-" + imageId + ext;
    }

    private record PurchaseRow(Long imageId, int canDownload) {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResolveBindingRequest {
        private Long vehicleId;
    }
}
