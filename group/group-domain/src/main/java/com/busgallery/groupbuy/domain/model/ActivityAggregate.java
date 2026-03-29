package com.busgallery.groupbuy.domain.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Activity aggregate used by group-buy domain.
 */
@Data
@Builder
public class ActivityAggregate {
    private Long activityId;
    private String goodsId;
    private String activityName;
    private int targetCount;
    private int validMinutes;
    private boolean enabled;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
