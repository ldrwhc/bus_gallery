package com.busgallery.busgallery.service;

import com.busgallery.busgallery.auth.UserSession;
import com.busgallery.busgallery.dto.request.VehicleUpsertPayload;
import com.busgallery.busgallery.entity.VehicleSubmission;

import java.util.List;

public interface SubmissionService {

    VehicleSubmission submitCreate(UserSession session, VehicleUpsertPayload payload, Long imageId);

    VehicleSubmission submitUpdate(UserSession session, VehicleUpsertPayload payload, Long vehicleId, Long imageId);

    List<VehicleSubmission> listInbox(UserSession session);

    List<VehicleSubmission> listPendingForReview(UserSession session);

    VehicleSubmission findById(Long id);

    VehicleSubmission approve(UserSession reviewer, Long submissionId, VehicleUpsertPayload reviewPayload);

    VehicleSubmission reject(UserSession reviewer, Long submissionId, String rejectCode, String rejectReason);
}
