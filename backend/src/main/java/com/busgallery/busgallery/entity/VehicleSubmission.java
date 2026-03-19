package com.busgallery.busgallery.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "vehicle_submission")
public class VehicleSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 32)
    private SubmissionActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SubmissionStatus status;

    @Column(name = "submitter_id", nullable = false)
    private Long submitterId;

    @Column(name = "submitter_username", nullable = false, length = 64)
    private String submitterUsername;

    @Column(name = "submitter_display_name", length = 128)
    private String submitterDisplayName;

    @Column(name = "reviewer_id")
    private Long reviewerId;

    @Column(name = "reviewer_username", length = 64)
    private String reviewerUsername;

    @Column(name = "reviewer_display_name", length = 128)
    private String reviewerDisplayName;

    @Column(name = "region_id")
    private Long regionId;

    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Column(name = "image_id")
    private Long imageId;

    @Column(name = "request_payload", columnDefinition = "LONGTEXT", nullable = false)
    private String requestPayload;

    @Column(name = "review_payload", columnDefinition = "LONGTEXT")
    private String reviewPayload;

    @Column(name = "reject_code", length = 64)
    private String rejectCode;

    @Column(name = "reject_reason", length = 512)
    private String rejectReason;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
