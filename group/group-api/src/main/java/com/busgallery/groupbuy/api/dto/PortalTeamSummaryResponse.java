package com.busgallery.groupbuy.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Active team summary for portal display.
 */
@Data
@Builder
public class PortalTeamSummaryResponse {
    private String teamId;
    private int targetCount;
    private int completeCount;
    private int lockCount;
    private LocalDateTime validEndTime;
}

