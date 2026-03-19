package com.busgallery.busgallery.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminOverviewResponse {
    private long totalUsers;
    private long stationUsers;
    private long reviewerUsers;
    private long normalUsers;
    private long pendingSubmissions;
    private long approvedSubmissions;
    private long rejectedSubmissions;
}
