package com.busgallery.groupbuy.domain.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Team order aggregate.
 */
@Data
@Builder
public class TeamOrderAggregate {
    private String teamId;
    private Long activityId;
    private String goodsId;
    private Long ownerUserId;
    private long originalPriceCents;
    private long deductionPriceCents;
    private long payPriceCents;
    private int targetCount;
    private int lockCount;
    private int completeCount;
    private String status;
    private LocalDateTime validStartTime;
    private LocalDateTime validEndTime;
}
