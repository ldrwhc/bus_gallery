package com.busgallery.busgallery.dto.request;

import lombok.Data;

@Data
public class ReviewRejectRequest {
    private String rejectCode;
    private String rejectReason;
}
