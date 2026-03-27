package com.busgallery.busgallery.service;

import com.busgallery.busgallery.auth.AuthPrincipal;
import com.busgallery.busgallery.dto.request.VehicleUpsertPayload;
import com.busgallery.busgallery.entity.VehicleSubmission;

import java.util.List;

public interface SubmissionService {

    VehicleSubmission submitCreate(AuthPrincipal session, VehicleUpsertPayload payload, Long imageId);

    VehicleSubmission submitUpdate(AuthPrincipal session, VehicleUpsertPayload payload, Long vehicleId, Long imageId);

    List<VehicleSubmission> listInbox(AuthPrincipal session);

    List<VehicleSubmission> listPendingForReview(AuthPrincipal session);

    VehicleSubmission findById(Long id);

    VehicleSubmission approve(AuthPrincipal reviewer, Long submissionId, VehicleUpsertPayload reviewPayload);

    VehicleSubmission reject(AuthPrincipal reviewer, Long submissionId, String rejectCode, String rejectReason);
}
