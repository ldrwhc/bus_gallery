package com.busgallery.busgallery.repository;

import com.busgallery.busgallery.entity.SubmissionStatus;
import com.busgallery.busgallery.entity.VehicleSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleSubmissionRepository extends JpaRepository<VehicleSubmission, Long> {

    List<VehicleSubmission> findBySubmitterIdOrderByCreatedAtDesc(Long submitterId);

    List<VehicleSubmission> findByStatusOrderByCreatedAtAsc(SubmissionStatus status);

    List<VehicleSubmission> findByStatusAndRegionIdOrderByCreatedAtAsc(SubmissionStatus status, Long regionId);
    List<VehicleSubmission> findByStatusAndRegionIdInOrderByCreatedAtAsc(SubmissionStatus status, List<Long> regionIds);

    long countByStatus(SubmissionStatus status);
}
