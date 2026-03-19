package com.busgallery.busgallery.dto.response;

import com.busgallery.busgallery.dto.request.VehicleUpsertPayload;
import com.busgallery.busgallery.entity.SubmissionActionType;
import com.busgallery.busgallery.entity.SubmissionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SubmissionResponse {
    private Long id;
    private SubmissionActionType actionType;
    private SubmissionStatus status;

    private Long submitterId;
    private String submitterUsername;
    private String submitterDisplayName;

    private Long reviewerId;
    private String reviewerUsername;
    private String reviewerDisplayName;

    private Long regionId;
    private Long vehicleId;
    private Long imageId;
    private String imageUrl;
    private String imageThumbnailUrl;

    private String rejectCode;
    private String rejectReason;

    private VehicleUpsertPayload requestPayload;
    private VehicleUpsertPayload reviewPayload;

    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
