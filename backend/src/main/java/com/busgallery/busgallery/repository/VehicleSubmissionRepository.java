package com.busgallery.busgallery.repository;

import com.busgallery.busgallery.entity.SubmissionStatus;
import com.busgallery.busgallery.entity.VehicleSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VehicleSubmissionRepository extends JpaRepository<VehicleSubmission, Long> {

    List<VehicleSubmission> findBySubmitterIdOrderByCreatedAtDesc(Long submitterId);

    List<VehicleSubmission> findByStatusOrderByCreatedAtAsc(SubmissionStatus status);

    List<VehicleSubmission> findByStatusAndRegionIdOrderByCreatedAtAsc(SubmissionStatus status, Long regionId);
    List<VehicleSubmission> findByStatusAndRegionIdInOrderByCreatedAtAsc(SubmissionStatus status, List<Long> regionIds);

    long countByStatus(SubmissionStatus status);

    long countBySubmitterIdAndStatus(Long submitterId, SubmissionStatus status);

    @Modifying
    @Query("update VehicleSubmission v set v.submitterDisplayName = :displayName where v.submitterId = :userId")
    int updateSubmitterDisplayNameByUserId(@Param("userId") Long userId, @Param("displayName") String displayName);

    @Modifying
    @Query("update VehicleSubmission v set v.reviewerDisplayName = :displayName where v.reviewerId = :userId")
    int updateReviewerDisplayNameByUserId(@Param("userId") Long userId, @Param("displayName") String displayName);
}
